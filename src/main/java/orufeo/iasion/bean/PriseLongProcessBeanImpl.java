package orufeo.iasion.bean;

import java.io.IOException;

import org.apache.log4j.Logger;

import lombok.Setter;
import orufeo.iasion.bo.ExchangeBo;
import orufeo.iasion.bo.UserAccountBo;
import orufeo.iasion.bo.WalletBo;
import orufeo.iasion.data.dto.BitFinexBalanceStatus;
import orufeo.iasion.data.dto.BitFinexOrderStatus;
import orufeo.iasion.data.mail.MailMessagePojo;
import orufeo.iasion.data.objects.storage.Exchange;
import orufeo.iasion.data.objects.storage.UserAccount;
import orufeo.iasion.data.objects.storage.Wallet;
import orufeo.iasion.exception.BalancesException;
import orufeo.iasion.exception.BuyException;
import orufeo.iasion.exception.MaxTriesExceededException;
import orufeo.iasion.exception.OrderStatusException;
import orufeo.iasion.utils.Mailer;
import orufeo.iasion.wao.BitfinexWao;

public class PriseLongProcessBeanImpl implements PriseLongProcessBean {

	@Setter private ExchangeBo exchangeBo;
	@Setter private UserAccountBo userAccountBo;
	@Setter private WalletBo walletBo;
	@Setter private BitfinexWao bitfinexWao;

	@Setter private String BITFINEX_CODE;     			// bitfinex
	@Setter private Integer ORDERBUY_MAXTRY; 			// 500
	@Setter private Integer ORDERBUY_WAITINGTIME;   	// 500 ms
	@Setter private Integer ORDERBUY_THRESHOLD;
	@Setter private Integer ORDERSTATUS_MAXTRY; 		// 500
	@Setter private Integer ORDERSTATUS_WAITINGTIME;    // 500 ms

	private static Logger log = Logger.getLogger(PriseLongProcessBeanImpl.class);


	@Override
	public void process(Wallet wallet) {

		Exchange exchange = exchangeBo.get(wallet.getData().getExchangeGuid());

		if (BITFINEX_CODE.toLowerCase().equals(exchange.getData().getCode().toLowerCase())) { 
			try {
				priseLongProcessBitfinex(wallet);
			} catch (Exception e ) {
				log.error("priseLongProcess for BitFinex Error: ", e);
			}
		}

	}

	/***************************
	 * 
	 * 
	 *  PRIVATE METHODS
	 * 
	 * 
	 ***************************/

	private BitFinexOrderStatus buyOrder(String iasionSymbol, String amount, String price, String type, String apiKey, String secretKey, Integer tryNumber) throws BuyException, InterruptedException {

		if (tryNumber < ORDERBUY_MAXTRY) {

			Thread.sleep(ORDERBUY_WAITINGTIME);

			try {
				return bitfinexWao.buy(iasionSymbol, amount,price, type, apiKey, secretKey);
			} catch (BuyException e) {
				return buyOrder(iasionSymbol, amount,price, type, apiKey, secretKey, ++tryNumber);
			}
		}

		throw new BuyException("Impossible to execute a buy order");

	}

	private BitFinexOrderStatus loopCheckStatusAfterBuy(Double remaining, Double originalAmount, Long orderId, String apiKey, String secretKey, int tryNumber) throws MaxTriesExceededException, OrderStatusException, InterruptedException {

		if ( tryNumber < ORDERBUY_MAXTRY) {				

			Thread.sleep(ORDERBUY_WAITINGTIME);

			BitFinexOrderStatus status = bitfinexWao.orderStatus(orderId, apiKey, secretKey);

			if (remaining >= (1-ORDERBUY_THRESHOLD*originalAmount) ) {

				remaining = Double.valueOf(status.getRemaining_amount());
				return loopCheckStatusAfterBuy(remaining, originalAmount,  orderId, apiKey, secretKey, ++tryNumber);

			} else 
				return status;


		} else 
			throw new MaxTriesExceededException("Too many tries for buy order status check before completion", Long.toString(orderId));
	}

	private Long proceedBuyOrder(UserAccount user, Wallet wallet, String iasionSymbol, Double price,  String apiKey, String secretKey) throws InterruptedException, BuyException, OrderStatusException, MaxTriesExceededException, IOException {

		BitFinexOrderStatus buyOrder = buyOrder(iasionSymbol, Double.toString(wallet.getData().getCurrencyValue()/price), "0", "exchange market", apiKey, secretKey, 0); 

		Long orderId = buyOrder.getOrder_id();

		BitFinexOrderStatus statusBuyOrder = loopCheckStatusAfterBuy(Double.valueOf(buyOrder.getRemaining_amount()), Double.valueOf(buyOrder.getOriginal_amount()), buyOrder.getOrder_id(), apiKey, secretKey, 0);

		if (Double.valueOf(statusBuyOrder.getRemaining_amount()) < (1-ORDERBUY_THRESHOLD*Double.valueOf(statusBuyOrder.getOriginal_amount())) )  {

			wallet.getData().setQuoteCurrencyValue(wallet.getData().getQuoteCurrencyValue()-(Double.valueOf(statusBuyOrder.getAvg_execution_price())*Double.valueOf(statusBuyOrder.getExecuted_amount())));
			wallet.getData().setCurrencyValue(wallet.getData().getCurrencyValue()+Double.valueOf(statusBuyOrder.getExecuted_amount()));

			if (0d != Double.valueOf(statusBuyOrder.getRemaining_amount()) ) {
				try {
					bitfinexWao.cancelOrder(orderId, apiKey, secretKey);
				} catch (Exception e2) {
					log.error("Cancel close order Exception: "+e2.getMessage());
					sendMail(user, orderId, "CANCEL_ORDER", "Impossible to cancel the close order during the long process with BitFinex, after a buy order id: "+orderId);
				}
			}

		} else {
			wallet.getData().setQuoteCurrencyValue(wallet.getData().getQuoteCurrencyValue()-(Double.valueOf(statusBuyOrder.getAvg_execution_price())*Double.valueOf(statusBuyOrder.getExecuted_amount())));
			wallet.getData().setCurrencyValue(wallet.getData().getCurrencyValue()+Double.valueOf(statusBuyOrder.getExecuted_amount()));
		}

		return orderId;
	}


	private void priseLongProcessBitfinex(Wallet wallet) throws IOException, InterruptedException {

		UserAccount user = userAccountBo.get(wallet.getData().getUserGuid());
		String iasionSymbol = wallet.getData().getCurrencyLabel().toLowerCase()+wallet.getData().getQuoteCurrencyLabel().toLowerCase();

		String apiKey = user.getData().getExchKeys().get(BITFINEX_CODE.toLowerCase()).getApiKey();
		String secretKey = user.getData().getExchKeys().get(BITFINEX_CODE.toLowerCase()).getSecretKey();

		String currencyLabel = wallet.getData().getCurrencyLabel();	   			 //"BTC"
		Double currencyValue = wallet.getData().getCurrencyValue();    			 // 1 BTC
		String quoteCurrencyLabel = wallet.getData().getQuoteCurrencyLabel();	 //"EUR"
		Double quoteCurrencyValue = wallet.getData().getQuoteCurrencyValue();    // 1 EUR

		//Initial condition to proceed
		if (quoteCurrencyValue>0 && currencyValue>=0 ) {

			try {
				//recovers the balance in BTC from the bitfinex wallet
				Double bitFinexBalance = checkBalanceForCurrency(user,quoteCurrencyLabel, apiKey, secretKey );

				if (bitFinexBalance >= quoteCurrencyValue ) {

					Double ask = Double.valueOf(bitfinexWao.getTicker(iasionSymbol).getAsk());

					Long orderId = this.proceedBuyOrder(user, wallet, iasionSymbol, ask, apiKey, secretKey);

					//we store the wallet in Iasion
					walletBo.update(wallet, Long.toString(orderId));
				}

			} catch (BalancesException e) {
				log.error("Impossible to check the "+currencyLabel+" balance on Bitfinex for "+user.getData().getFirstname()+" "+user.getData().getLastname()+": "+e.getMessage());
				if (!"NO_CURRENCY_IN_WALLETS".equals(e.getErrorCode()))
					sendMail(user, 0L, "CHECK_BALANCE", e.getMessage());
			} catch (BuyException e) {
				log.error("Impossible to buy for user "+user.getData().getFirstname()+" "+user.getData().getLastname()+": "+e.getMessage());
				sendMail(user, 0L, "BUY_ORDER", "Impossible to buy in a prise Long Process ");
			} catch (MaxTriesExceededException e) {
				log.error("Max number of trials to check a sell order status reached: "+e.getMessage());
				sendMail(user,  Long.valueOf(e.getErrorCode()), e.getErrorCode(), "Impossible to check the status of a sell order (Max number of trials reached) during the long process with BitFinex. ");
			} catch (OrderStatusException e) {
				log.error("Impossible to get your sell order status for order "+e.getErrorCode()+": "+e);	
				sendMail(user, Long.valueOf(e.getErrorCode()), "CONNECTION_PROBLEM ?", "Impossible to check the status of a sell order (probably a connection problem) during the long process with BitFinex.");
			}

		}

	}



	private Double checkBalanceForCurrency(UserAccount account, String currencyLabel, String apiKey, String secretKey) throws BalancesException, IOException {

		for (BitFinexBalanceStatus balance : bitfinexWao.getBalances(apiKey, secretKey) ) {
			if ( "exchange".equals(balance.getType()) && balance.getCurrencyLabel().equals(currencyLabel.toLowerCase())) {
				return Double.valueOf(balance.getAvailable());
			}
		}

		throw new BalancesException("No "+currencyLabel+" currency found in the wallets of user:"+account.getData().getFirstname()+" "+account.getData().getLastname(), "NO_CURRENCY_IN_WALLETS");

	}

	private void sendMail(UserAccount account, Long orderId, String reason, String message) throws IOException {
		//we alert the admins
		//*****************
		log.info("ENVOI DE MAIL");
		MailMessagePojo payload = new MailMessagePojo();

		payload.setEmail(account.getData().getLogin());
		if (!"chenav@yahoo.com".equals(account.getData().getLogin()))
			payload.setBcc("chenav@yahoo.com");
		payload.setSubject("[IASION - "+reason+"] Critical problem for orderId: "+orderId);
		payload.setBody(message); 
		payload.setHtmlBody("<html><head/><body>"+message+"<br> Please check your account and act accordingly.</body></html>");

		Mailer.sendMail("http://127.0.0.1:5000/tosmtp", payload);
		log.info("ENVOI DE MAIL - DONE !");
		//*****************
	}


}
