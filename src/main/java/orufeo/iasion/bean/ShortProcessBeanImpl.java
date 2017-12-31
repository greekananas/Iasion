package orufeo.iasion.bean;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import lombok.Setter;
import orufeo.iasion.bo.ExchangeBo;
import orufeo.iasion.bo.UserAccountBo;
import orufeo.iasion.bo.WalletBo;
import orufeo.iasion.data.dto.BitFinexBalanceStatus;
import orufeo.iasion.data.dto.BitFinexOrderStatus;
import orufeo.iasion.data.dto.BitFinexTransferStatus;
import orufeo.iasion.data.mail.MailMessagePojo;
import orufeo.iasion.data.objects.storage.Exchange;
import orufeo.iasion.data.objects.storage.UserAccount;
import orufeo.iasion.data.objects.storage.Wallet;
import orufeo.iasion.exception.BalancesException;
import orufeo.iasion.exception.EquityCheckException;
import orufeo.iasion.exception.MaxTriesExceededException;
import orufeo.iasion.exception.OrderStatusException;
import orufeo.iasion.exception.SellException;
import orufeo.iasion.exception.TransferException;
import orufeo.iasion.utils.Mailer;
import orufeo.iasion.wao.BitfinexWao;

public class ShortProcessBeanImpl implements ShortProcessBean {

	@Setter private ExchangeBo exchangeBo;
	@Setter private UserAccountBo userAccountBo;
	@Setter private WalletBo walletBo;
	@Setter private BitfinexWao bitfinexWao;

	@Setter private String BITFINEX_CODE;     			// bitfinex
	@Setter private Integer ORDERSELL_MAXTRY; 			// 500
	@Setter private Integer ORDERSELL_WAITINGTIME;   	// 500 ms
	@Setter private Integer ORDERSTATUS_MAXTRY; 		// 500
	@Setter private Integer ORDERSTATUS_WAITINGTIME;    // 500 ms
	@Setter private Integer TRANSFER_MAXTRY; 			// 500
	@Setter private Integer TRANSFER_WAITINGTIME;   	// 500 ms
	@Setter private Double BITFINEX_LEVER;   					// 1.0

	private static Logger log = Logger.getLogger(ShortProcessBeanImpl.class);


	@Override
	public void process(Wallet wallet) {

		Exchange exchange = exchangeBo.get(wallet.getData().getExchangeGuid());

		if (BITFINEX_CODE.toLowerCase().equals(exchange.getData().getCode().toLowerCase())) { 
			try {
				shortProcessBitfinex(wallet);
			} catch (Exception e ) {
				log.error("longProcess for BitFinex Error: ", e);
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
/*
	private BitFinexOrderStatus loopCheckStatus(Double remaining, Double executed, Double avgExecutionPrice, Long orderId, BitFinexOrderStatus previousStatus,  String apiKey, String secretKey, int tryNumber) throws MaxTriesExceededException, OrderStatusException, InterruptedException {

		if ( tryNumber < ORDERSTATUS_MAXTRY) {				

			if (remaining > 0 ) {

				Thread.sleep(ORDERSTATUS_WAITINGTIME);

				BitFinexOrderStatus status = bitfinexWao.orderStatus(orderId, apiKey, secretKey);
				remaining = Double.valueOf(status.getRemaining_amount());
				executed = Double.valueOf(status.getExecuted_amount());
				avgExecutionPrice = Double.valueOf(status.getAvg_execution_price());

				return loopCheckStatus(remaining, executed, avgExecutionPrice, orderId, status, apiKey, secretKey, ++tryNumber);

			} else 
				return previousStatus;

		} else 
			throw new MaxTriesExceededException("Too many tries for order status check before completion", "ORDERSTATUS_MAXTRY");
	}
*/
	
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


	private void shortProcessBitfinex(Wallet wallet) throws IOException {

		UserAccount user = userAccountBo.get(wallet.getData().getUserGuid());
		String iasionSymbol = wallet.getData().getCurrencyLabel().toLowerCase()+wallet.getData().getQuoteCurrencyLabel().toLowerCase();

		String apiKey = user.getData().getExchKeys().get(BITFINEX_CODE.toLowerCase()).getApiKey();
		String secretKey = user.getData().getExchKeys().get(BITFINEX_CODE.toLowerCase()).getSecretKey();

		String currencyLabel = wallet.getData().getCurrencyLabel();	   //"BTC"
		Double currencyValue = wallet.getData().getCurrencyValue();    // 1 BTC

		Long orderId = null;

		if (currencyValue > 0) {

			try {

				//recovers the balance in BTC from the bitfinex wallet
				Double bitFinexBalance = checkBalanceForCurrency(user,currencyLabel, apiKey, secretKey );

				//if the bitfinex wallet is equals/superior to the Iasion wallet, it means we have BTC to sell
				if (currencyValue <= bitFinexBalance) {

					//we sell on bitfinex
					BitFinexOrderStatus sellOrderStatus = sellOrder(iasionSymbol, Double.toString(currencyValue), "0", "exchange market", apiKey, secretKey, 0);

					orderId = sellOrderStatus.getOrder_id();

					//We loop on the order status, to ensure it is complete
					sellOrderStatus = loopCheckStatus(sellOrderStatus, apiKey, secretKey, 0);
					
					//we store the final numeric data
					Double originalAmount = Double.valueOf(sellOrderStatus.getOriginal_amount());
					Double avgExecutionPrice = Double.valueOf(sellOrderStatus.getAvg_execution_price());

					//we update the Iasion wallet values
					Double updatedQuoteCurrencyValue = wallet.getData().getQuoteCurrencyValue()+(originalAmount*avgExecutionPrice);
					Double updatedCurrencyValue = wallet.getData().getCurrencyValue()-originalAmount;

					wallet.getData().setQuoteCurrencyValue(updatedQuoteCurrencyValue);
					wallet.getData().setCurrencyValue(updatedCurrencyValue);

					walletBo.update(wallet, Long.toString(orderId));

					//move from Exchange wallet to margin wallet
					Double amountTransfered = proceedTransfer(wallet, user, orderId, apiKey, secretKey);

					//check the equity on the exchange wallet
					Double availableEquity = getAvailableEquity(wallet, user, orderId, apiKey, secretKey);

					if (amountTransfered != availableEquity ) 
						throw new EquityCheckException("Equity on exchange wallet is not equal to what we just transfered from exchange wallet");
					
					Double bid = Double.valueOf(bitfinexWao.getTicker(iasionSymbol).getBid());
					
					BitFinexOrderStatus sellOrderStatus2 = sellOrder(iasionSymbol, Double.toString(updatedQuoteCurrencyValue*BITFINEX_LEVER/bid), "0", "market", apiKey, secretKey, 0);
					
					//We loop on the order status, to ensure it is complete
					sellOrderStatus2 = loopCheckStatus(sellOrderStatus2, apiKey, secretKey, 0);
					
					//we store the final numeric data
					Double executed2 = Double.valueOf(sellOrderStatus2.getExecuted_amount());
					Double avgExecutionPrice2 = Double.valueOf(sellOrderStatus2.getAvg_execution_price());

					wallet.getData().setQuoteCurrencyValue(wallet.getData().getQuoteCurrencyValue()+(avgExecutionPrice2*executed2));
					wallet.getData().setCurrencyValue(wallet.getData().getCurrencyValue()-executed2); 									//currencyValue should be 0
					
					walletBo.update(wallet);
					
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
				sendMail(user, orderId, e.getErrorCode(), "Impossible to check the status of a sell order (Max number of trials reached) during the long process with BitFinex. ");
			} catch (OrderStatusException e) {
				log.error("Impossible to get your sell order status for order "+orderId+": "+e);	
				sendMail(user, orderId, "CONNECTION_PROBLEM ?", "Impossible to check the status of a sell order (probably a connection problem) during the long process with BitFinex.");
			} catch (EquityCheckException e) {
				log.error("Check failed between Margin wallet equity value and transfered value from exchange wallet after sell order "+orderId+": "+e);	
				sendMail(user, orderId, "EQUITY_CHECK", "Check failed between Margin wallet equity value and transfered value from exchange wallet after sell order "+orderId+"");
			}  catch (TransferException e) {
				log.error("Impossible to transfer currency for user "+user.getData().getFirstname()+" "+user.getData().getLastname()+": "+e.getMessage());
				sendMail(user, orderId, "TRANSFER_FUND", "Impossible to transfer money from wallet to wallet during the short process with BitFinex, after the short order ID "+orderId);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}


	}

	private Double getAvailableEquity(Wallet wallet, UserAccount user, Long orderId, String apiKey, String secretKey) throws BalancesException, IOException {

		List<BitFinexBalanceStatus> statusBalances = bitfinexWao.getBalances(apiKey, secretKey);

		Double availableEquity = null;

		for (BitFinexBalanceStatus balance : statusBalances) {

			if ("trading".equals(balance.getType()) 
					&& balance.getCurrencyLabel().toLowerCase().equals(wallet.getData().getQuoteCurrencyLabel().toLowerCase())
					&& Double.valueOf(balance.getAvailable())>= wallet.getData().getQuoteCurrencyValue()
					) {
				availableEquity = Double.valueOf(balance.getAvailable());
			} 
		}

		if (null == availableEquity) {
			sendMail(user, orderId, "TRANSFER_FUND", "After transfer, balance value is not available in the targeted wallet following the sell order ID: "+orderId);
			throw new BalancesException("Balance not available");
		}

		return availableEquity;
	}
	
	
	private Double proceedTransfer(Wallet wallet, UserAccount user, Long orderId, String apiKey, String secretKey) throws TransferException, InterruptedException, IOException {

		List<BitFinexTransferStatus> statusTransfer = null;

		Boolean transfered = false;
		Boolean checked = false; 
		
		// we loop until it is done
		for ( int i=0; i < TRANSFER_MAXTRY ; i++) {

			Thread.sleep(TRANSFER_WAITINGTIME);
			try {
				statusTransfer = bitfinexWao.transfer("exchange", "trading", wallet.getData().getQuoteCurrencyLabel().toLowerCase(), wallet.getData().getQuoteCurrencyValue(), apiKey, secretKey);

				if ("error".equals(statusTransfer.get(0).getStatus().toLowerCase())) {
					sendMail(user, orderId, "TRANSFER_FUND", "Transfer order between your wallets during the long process with BitFinex returned an error, following the close order ID:"+orderId+". Try number: "+i);
				} else { //success

					transfered = true;
						
					//we loop the check process
					for ( int j=0; j < TRANSFER_MAXTRY ; j++) {
						try { 
							for (BitFinexBalanceStatus balance : bitfinexWao.getBalances(apiKey, secretKey) ) {
								if ( "trading".equals(balance.getType()) && balance.getCurrencyLabel().equals(wallet.getData().getQuoteCurrencyLabel().toLowerCase())) {
									return Double.valueOf(balance.getAvailable());
								}
							}
						} catch (BalancesException e) {
							sendMail(user, orderId, "TRANSFER_FUND", "Impossible to check transfered value in margin wallet, following the close order ID:"+orderId+". Try number: "+j);
						}	
					}
				}

			} catch (TransferException e) {
				log.error("Impossible to transfer currency from exchange to margin for user "+user.getData().getFirstname()+" "+user.getData().getLastname()+"   try #"+i);
			}

		}

		//if here the transfer + check was not completed
		sendMail(user, orderId, "TRANSFER_FUND", "Transfer order between your wallets during the short process, following the close order ID:"+orderId+". Exhausted number of retries. Transfer status: "+transfered+", check status:"+checked);
		throw new TransferException("Transfer status: "+transfered+" check status: "+ checked+" during transfer ");

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
