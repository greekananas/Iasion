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
import orufeo.iasion.data.dto.BitFinexPosition;
import orufeo.iasion.data.dto.BitFinexTransferStatus;
import orufeo.iasion.data.mail.MailMessagePojo;
import orufeo.iasion.data.objects.storage.Exchange;
import orufeo.iasion.data.objects.storage.UserAccount;
import orufeo.iasion.data.objects.storage.Wallet;
import orufeo.iasion.exception.ActivePositionsException;
import orufeo.iasion.exception.BalancesException;
import orufeo.iasion.exception.BuyException;
import orufeo.iasion.exception.ClosePositionException;
import orufeo.iasion.exception.EquityCheckException;
import orufeo.iasion.exception.MaxTriesExceededException;
import orufeo.iasion.exception.OrderStatusException;
import orufeo.iasion.exception.TransferException;
import orufeo.iasion.utils.Mailer;
import orufeo.iasion.wao.BitfinexWao;

public class LongProcessBeanImpl implements LongProcessBean {

	@Setter private ExchangeBo exchangeBo;
	@Setter private UserAccountBo userAccountBo;
	@Setter private WalletBo walletBo;
	@Setter private BitfinexWao bitfinexWao;

	@Setter private String BITFINEX_CODE;     			// bitfinex
	@Setter private Integer ORDERCLOSE_MAXTRY; 			// 500
	@Setter private Integer ORDERCLOSE_WAITINGTIME;     // 500 ms
	@Setter private Integer ORDERSTATUS_MAXTRY; 		// 500
	@Setter private Integer ORDERSTATUS_WAITINGTIME;    // 500 ms
	@Setter private Integer TRANSFER_MAXTRY; 			// 500
	@Setter private Integer TRANSFER_WAITINGTIME;   	// 500 ms
	@Setter private Integer ORDERBUY_MAXTRY; 			// 500
	@Setter private Integer ORDERBUY_WAITINGTIME;   	// 500 ms
	@Setter private Double ORDERBUY_THRESHOLD;      	// 0.995

	private static Logger log = Logger.getLogger(LongProcessBeanImpl.class);

	@Override
	public void process(Wallet wallet) {

		Exchange exchange = exchangeBo.get(wallet.getData().getExchangeGuid());

		if (BITFINEX_CODE.toLowerCase().equals(exchange.getData().getCode().toLowerCase())) { 
			try {
				longProcessBitfinex(wallet);
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

	private void longProcessBitfinex(Wallet wallet) throws Exception {

		UserAccount user = userAccountBo.get(wallet.getData().getUserGuid());
		String iasionSymbol = wallet.getData().getCurrencyLabel().toLowerCase()+wallet.getData().getQuoteCurrencyLabel().toLowerCase();

		String apiKey = user.getData().getExchKeys().get(BITFINEX_CODE.toLowerCase()).getApiKey();
		String secretKey = user.getData().getExchKeys().get(BITFINEX_CODE.toLowerCase()).getSecretKey();

		Double solde = wallet.getData().getQuoteCurrencyValue(); 

		Long orderId = 0L;
		BitFinexPosition orderPositionToClose = null;

		try {

			//GET active positions
			List<BitFinexPosition> activePositions = bitfinexWao.getActivePositions(apiKey, secretKey);

			for (BitFinexPosition position : activePositions) {
				if (iasionSymbol.equals(position.getSymbol().toLowerCase())) {

					orderPositionToClose = position;

					//close position and update the wallet values
					closePosition(position, apiKey, secretKey, solde, wallet,  0);

					//we store the wallet in Iasion
					walletBo.update(wallet, Long.toString(orderId));

					//move from margin wallet to exchange wallet
					Double amountTransfered = proceedTransfer(wallet, user, orderId, apiKey, secretKey);

					//check the equity on the exchange wallet
					Double availableEquity = getAvailableEquity(wallet, user, orderId, apiKey, secretKey);

					if (amountTransfered != availableEquity ) 
						throw new EquityCheckException("Equity on exchange wallet is not equal to what we just transfered from trading wallet");

					Double price = Double.valueOf(bitfinexWao.getTicker(iasionSymbol).getAsk());

					BitFinexOrderStatus buyOrder = buyOrder(iasionSymbol, Double.toString(wallet.getData().getCurrencyValue()/price), "0", "exchange market", apiKey, secretKey, 0); 

					orderId = buyOrder.getOrder_id();

					orderId = proceedBuyOrder(user, wallet, iasionSymbol, price,  apiKey, secretKey);

					//we store the wallet in Iasion
					walletBo.update(wallet, Long.toString(orderId));

					break;
				}
			}

		} catch (ActivePositionsException e) {
			log.error("Impossible to get the active positions for user "+user.getData().getFirstname()+" "+user.getData().getLastname()+": "+e.getMessage());
			sendMail(user, orderId, "ACTIVE_POSITIONS", "Impossible to get your active positions during the long process with BitFinex. Order impossible to execute.");
		} catch (ClosePositionException e) {
			log.error("Impossible to close position of ID "+orderPositionToClose.getId()+" for user "+user.getData().getFirstname()+" "+user.getData().getLastname()+": "+e.getMessage());
			sendMail(user, orderId, "CLOSE_POSITION", "Impossible to close your active position "+orderPositionToClose.getId()+", pair="+orderPositionToClose.getSymbol()+", amount="+orderPositionToClose.getAmount()+" during the long process with BitFinex. Close order impossible to execute.");
		} catch (OrderStatusException e) {
			log.error("Impossible to get your close order status for order "+orderId+": "+e);	
			sendMail(user, orderId, "CONNECTION_PROBLEM ?", "Impossible to check the status of a close order (probably a connection problem) during the long process with BitFinex.");
		} catch (EquityCheckException e) {
			log.error("Check failed between Exchange wallet equity value and transfered value from trading wallet after closure order "+orderId+": "+e);	
			sendMail(user, orderId, "EQUITY_CHECK", "Check failed between Exchange wallet equity value and transfered value from trading wallet after closure order "+orderId+". ");
		} catch (MaxTriesExceededException e) {
			log.error("Max number of trials to check a close order status reached: "+e.getMessage());
			sendMail(user, orderId, e.getErrorCode(), "Impossible to check the status of a close order (Max number of trials reached) during the long process with BitFinex. ");
		} catch (TransferException e) {
			log.error("Impossible to transfer currency for user "+user.getData().getFirstname()+" "+user.getData().getLastname()+": "+e.getMessage());
			sendMail(user, orderId, "TRANSFER_FUND", "Impossible to transfer money from wallet to wallet during the long process with BitFinex, after the close order ID "+orderId);
		} catch (BuyException e) {
			log.error("Impossible to buy for user "+user.getData().getFirstname()+" "+user.getData().getLastname()+": "+e.getMessage());
			sendMail(user, orderId, "BUY_ORDER", "Impossible to buy, after the close order ID  "+orderId);
		} catch (BalancesException e) {
			log.error("Impossible to get balances from wallets on bitfinex: "+e.getMessage());
			sendMail(user, orderId, "TRANSFER_FUND", "Impossible to get Balances during the long process with BitFinex, after the close order ID "+orderId);
		}  catch (Exception e) {
			log.error("Undefined exception: "+e.getMessage());
		}

	}

	private void closePosition(BitFinexPosition position, String apiKey, String secretKey, Double solde, Wallet wallet, Integer index) throws OrderStatusException, Exception {

		BitFinexOrderStatus closeReturn = proceedClosure(position, apiKey, secretKey, index); 

		Long orderId = closeReturn.getOrder_id();

		//check the execution of the order 
		BitFinexOrderStatus statusClose = bitfinexWao.orderStatus(orderId, apiKey, secretKey);

		Double remaining = Double.valueOf(statusClose.getRemaining_amount());
		Double executed = Double.valueOf(statusClose.getExecuted_amount());

		executed = loopCheckStatus(remaining, executed, orderId, apiKey, secretKey, 0);

		//once executed, we update the iasion wallet
		solde = solde-executed*Double.valueOf(statusClose.getAvg_execution_price());

		wallet.getData().setCurrencyValue(wallet.getData().getCurrencyValue()+executed);
		wallet.getData().setQuoteCurrencyValue(solde);

	}

	private Double loopCheckStatus(Double remaining, Double executed, Long orderId, String apiKey, String secretKey, int tryNumber) throws MaxTriesExceededException, OrderStatusException, InterruptedException {

		if ( tryNumber < ORDERSTATUS_MAXTRY) {				

			if (remaining > 0 ) {

				Thread.sleep(ORDERSTATUS_WAITINGTIME);

				BitFinexOrderStatus status = bitfinexWao.orderStatus(orderId, apiKey, secretKey);
				remaining = Double.valueOf(status.getRemaining_amount());
				executed = Double.valueOf(status.getExecuted_amount());
				return loopCheckStatus(remaining, executed, orderId, apiKey, secretKey, ++tryNumber);

			} else 
				return executed;

		} else 
			throw new MaxTriesExceededException("Too many tries for order status check before completion", "ORDERSTATUS_MAXTRY");
	}

	private Double proceedTransfer(Wallet wallet, UserAccount user, Long orderId, String apiKey, String secretKey) throws TransferException, InterruptedException, IOException {

		Boolean transfered = false;
		Boolean checked = false; 
		
		List<BitFinexTransferStatus> statusTransfer = null;

		// we loop until it is done

		for ( int i=0; i < TRANSFER_MAXTRY ; i++) {

			Thread.sleep(TRANSFER_WAITINGTIME);
			try {
				statusTransfer = bitfinexWao.transfer("trading", "exchange", wallet.getData().getQuoteCurrencyLabel().toLowerCase(), wallet.getData().getQuoteCurrencyValue(), apiKey, secretKey);

				if ("error".equals(statusTransfer.get(0).getStatus().toLowerCase())) {
					sendMail(user, orderId, "TRANSFER_FUND", "Transfer order between your wallets during the long process with BitFinex returned an error, following the close order ID:"+orderId+". Try number: "+i);
				} else { //success	

					transfered = true;
					
					for ( int j=0; j < TRANSFER_MAXTRY ; j++) {
						try { 
							for (BitFinexBalanceStatus balance : bitfinexWao.getBalances(apiKey, secretKey) ) {
								if ( "exchange".equals(balance.getType()) && balance.getCurrencyLabel().equals(wallet.getData().getQuoteCurrencyLabel().toLowerCase())) {
									return Double.valueOf(balance.getAvailable());
								}
							}
						} catch (BalancesException e) {
							sendMail(user, orderId, "TRANSFER_FUND", "Impossible to check transfered value in exchange wallet, following the close order ID:"+orderId+". Try number: "+j);
						}
					}
				}

			} catch (TransferException e) {
				log.error("Impossible to transfer currency from margin to exchange for user "+user.getData().getFirstname()+" "+user.getData().getLastname()+"   try #"+i);
			}
		}
		//if here the transfer + check was not completed
		sendMail(user, orderId, "TRANSFER_FUND", "Transfer order between your wallets during the long process, following the close order ID:"+orderId+". Exhausted number of retries . Transfer status: "+transfered+", check status:"+checked);
		throw new TransferException("Transfer status: "+transfered+" check status: "+ checked+" during transfer ");

	}

	private Double getAvailableEquity(Wallet wallet, UserAccount user, Long orderId, String apiKey, String secretKey) throws BalancesException, IOException {

		List<BitFinexBalanceStatus> statusBalances = bitfinexWao.getBalances(apiKey, secretKey);

		Double availableEquity = null;

		for (BitFinexBalanceStatus balance : statusBalances) {

			if ("exchange".equals(balance.getType()) 
					&& balance.getCurrencyLabel().toLowerCase().equals(wallet.getData().getQuoteCurrencyLabel().toLowerCase())
					&& Double.valueOf(balance.getAvailable())>= wallet.getData().getQuoteCurrencyValue()
					) {
				availableEquity = Double.valueOf(balance.getAvailable());
			} 
		}

		if (null == availableEquity) {
			sendMail(user, orderId, "TRANSFER_FUND", "After transfer, balance value is not available in the targeted wallet following the close order ID:"+orderId+". Process stopped, no buy order sent.");
			throw new BalancesException("Balance not available");
		}

		return availableEquity;
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
			throw new MaxTriesExceededException("Too many tries for buy order status check before completion", "ORDERBUYSTATUS_MAXTRY");
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


	private BitFinexOrderStatus proceedClosure(BitFinexPosition position, String apiKey, String secretKey, Integer index) throws Exception {

		if (index < ORDERCLOSE_MAXTRY) {

			Thread.sleep(ORDERCLOSE_WAITINGTIME);

			try {
				return bitfinexWao.closePosition(position, apiKey, secretKey);
			} catch (ClosePositionException e) {
				return proceedClosure(position, apiKey, secretKey, ++index);
			}

		} 

		throw new ClosePositionException("Impossible to close position");

	}

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
