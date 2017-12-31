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
import orufeo.iasion.exception.MaxTriesExceededException;
import orufeo.iasion.exception.OrderStatusException;
import orufeo.iasion.exception.SellException;
import orufeo.iasion.utils.Mailer;
import orufeo.iasion.wao.BitfinexWao;

public class PriseShortProcessBeanImpl implements PriseShortProcessBean {

	@Setter private ExchangeBo exchangeBo;
	@Setter private UserAccountBo userAccountBo;
	@Setter private WalletBo walletBo;
	@Setter private BitfinexWao bitfinexWao;

	@Setter private String BITFINEX_CODE;     			// bitfinex
	@Setter private Integer ORDERSELL_MAXTRY; 			// 500
	@Setter private Integer ORDERSELL_WAITINGTIME;   	// 500 ms
	@Setter private Integer ORDERSTATUS_MAXTRY; 		// 500
	@Setter private Integer ORDERSTATUS_WAITINGTIME;    // 500 ms
	@Setter private Double BITFINEX_LEVER;				// 1.0

	private static Logger log = Logger.getLogger(PriseShortProcessBeanImpl.class);


	@Override
	public void process(Wallet wallet) {

		Exchange exchange = exchangeBo.get(wallet.getData().getExchangeGuid());

		if (BITFINEX_CODE.toLowerCase().equals(exchange.getData().getCode().toLowerCase())) { 
			try {
				priseShortProcessBitfinex(wallet);
			} catch (Exception e ) {
				log.error("clotureLongProcess for BitFinex Error: ", e);
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

	private BitFinexOrderStatus sellOrder(String symbol, String amount, String price, String type, String apiKey, String secretKey, Integer tryNumber) throws SellException, InterruptedException {

		if (tryNumber < ORDERSELL_MAXTRY) {

			Thread.sleep(ORDERSELL_WAITINGTIME);

			try {
				return bitfinexWao.sell(symbol, amount, price, type, apiKey, secretKey);
			} catch (SellException e) {
				return sellOrder(symbol, amount,price, type, apiKey, secretKey, ++tryNumber);
			}
		}

		throw new SellException("Impossible to execute a buy order");

	}
	
	private BitFinexOrderStatus loopCheckStatus(BitFinexOrderStatus previousStatus, String apiKey, String secretKey, int tryNumber) throws MaxTriesExceededException, OrderStatusException, InterruptedException {
		
		Double remaining = Double.valueOf(previousStatus.getRemaining_amount());
		
		if ( tryNumber < ORDERSTATUS_MAXTRY) {				

			if (remaining > 0 ) {

				Thread.sleep(ORDERSTATUS_WAITINGTIME);

				BitFinexOrderStatus status = bitfinexWao.orderStatus(previousStatus.getOrder_id(), apiKey, secretKey);
			
				return loopCheckStatus(status, apiKey, secretKey, ++tryNumber);

			} else 
				return previousStatus;

		} else 
			throw new MaxTriesExceededException("Too many tries for order status check before completion", "ORDERSTATUS_MAXTRY");
	}


	private void priseShortProcessBitfinex(Wallet wallet) throws IOException, InterruptedException {

		UserAccount user = userAccountBo.get(wallet.getData().getUserGuid());
		String iasionSymbol = wallet.getData().getCurrencyLabel().toLowerCase()+wallet.getData().getQuoteCurrencyLabel().toLowerCase();

		String apiKey = user.getData().getExchKeys().get(BITFINEX_CODE.toLowerCase()).getApiKey();
		String secretKey = user.getData().getExchKeys().get(BITFINEX_CODE.toLowerCase()).getSecretKey();

		String currencyLabel = wallet.getData().getCurrencyLabel();	   			 // "BTC"
		Double currencyValue = wallet.getData().getCurrencyValue();    			 // 1 BTC
		String quoteCurrencyLabel = wallet.getData().getQuoteCurrencyLabel();    // "EUR"
		Double quoteCurrencyValue = wallet.getData().getQuoteCurrencyValue();    // 1 EUR

		Long orderId = null;

		//check initial conditions
		if (quoteCurrencyValue > 0 && currencyValue==0) {

			try {

				//recovers the balance in BTC from the bitfinex wallet
				Double bitFinexBalance = checkBalanceForCurrency(user,quoteCurrencyLabel, apiKey, secretKey );

				//if the bitfinex wallet is equals/superior to the Iasion wallet, it means we have EUR to spend
				if (quoteCurrencyValue <= bitFinexBalance) {
					
					Double bid = Double.valueOf(bitfinexWao.getTicker(iasionSymbol).getBid());
						
					//we sell on bitfinex
					BitFinexOrderStatus sellOrderStatus = sellOrder(iasionSymbol, Double.toString(quoteCurrencyValue*BITFINEX_LEVER/bid), "0", "market", apiKey, secretKey, 0);
					
					orderId = sellOrderStatus.getOrder_id();

					//We loop on the order status, to ensure it is complete
					sellOrderStatus = loopCheckStatus(sellOrderStatus, apiKey, secretKey, 0);
					
					//we store the final numeric data
					Double executedAmount = Double.valueOf(sellOrderStatus.getExecuted_amount());
					Double avgExecutionPrice = Double.valueOf(sellOrderStatus.getAvg_execution_price());

					//we update the Iasion wallet values
					Double updatedQuoteCurrencyValue = wallet.getData().getQuoteCurrencyValue()+(executedAmount*avgExecutionPrice);
					Double updatedCurrencyValue = wallet.getData().getCurrencyValue()-executedAmount;

					wallet.getData().setQuoteCurrencyValue(updatedQuoteCurrencyValue);
					wallet.getData().setCurrencyValue(updatedCurrencyValue);

					walletBo.update(wallet, Long.toString(orderId));
					
				} //else we do nothing

			} catch (BalancesException e) {
				log.error("Impossible to check the "+currencyLabel+" balance on Bitfinex for "+user.getData().getFirstname()+" "+user.getData().getLastname()+": "+e.getMessage());
				if (!"NO_CURRENCY_IN_WALLETS".equals(e.getErrorCode()))
					sendMail(user, 0L, "CHECK_BALANCE", e.getMessage());
			} catch (SellException e) {
				log.error("Impossible to sell for user "+user.getData().getFirstname()+" "+user.getData().getLastname()+": "+e.getMessage());
				sendMail(user, orderId, "SELL_ORDER", "Impossible to sell, order id "+orderId);
			} catch (MaxTriesExceededException e) {
				log.error("Max number of trials to check a sell order status reached: "+e.getMessage());
				sendMail(user, orderId, e.getErrorCode(), "Impossible to check the status of a sell order (Max number of trials reached) during the prise short process with BitFinex. ");
			} catch (OrderStatusException e) {
				log.error("Impossible to get your sell order status for order "+orderId+": "+e);	
				sendMail(user, orderId, "CONNECTION_PROBLEM ?", "Impossible to check the status of a sell order (probably a connection problem) during the prise short process with BitFinex.");
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
