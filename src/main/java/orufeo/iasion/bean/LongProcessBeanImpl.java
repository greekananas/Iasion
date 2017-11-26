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
import orufeo.iasion.exception.ClosePositionException;
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
	@Setter private Integer ORDERSTATUS_MAXTRY; 		// 500
	@Setter private Integer ORDERSTATUS_WAITINGTIME;    // 500 ms

	private static Logger log = Logger.getLogger(LongProcessBeanImpl.class);

	@Override
	public void processLong(Wallet wallet) {

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

		String apiKey = user.getData().getExchKeys().get(BITFINEX_CODE.toLowerCase()).getApiKey();
		String secretKey = user.getData().getExchKeys().get(BITFINEX_CODE.toLowerCase()).getSecretKey();

		Double solde = wallet.getData().getCurrencyValue(); // -----> ? check if this is the right field

		Long orderId = 0L;
		BitFinexPosition orderPositionToClose = null;

		try {

			//GET active positions
			List<BitFinexPosition> activePositions = bitfinexWao.getActivePositions(apiKey, secretKey);

			String iasionSymbol = wallet.getData().getQuoteCurrencyLabel().toUpperCase()+wallet.getData().getCurrencyLabel().toUpperCase();

			for (BitFinexPosition position : activePositions) {
				if (iasionSymbol.equals(position.getSymbol().toUpperCase())) {

					orderPositionToClose = position;
					BitFinexOrderStatus closeReturn = bitfinexWao.closePosition(position, apiKey, secretKey);  //------------------------> TODO ATTENTION AUX VALEURS NEGATIVES ?

					orderId = closeReturn.getOrder_id();

					//check the execution of the order 
					BitFinexOrderStatus statusClose = bitfinexWao.orderStatus(orderId, apiKey, secretKey);
					Double remaining = statusClose.getRemaining_amount();
					Double executed = statusClose.getExecuted_amount();

					executed = loopCheckStatus(remaining, executed, orderId, apiKey, secretKey, 0);

					//once executed, we update the iasion wallet
					solde = solde+executed;
					
					wallet.getData().setQuoteCurrencyValue(0d);
					wallet.getData().setCurrencyValue(solde);

					//we store the wallet in Iasion
					walletBo.update(wallet, Long.toString(orderId));

					//move from margin wallet to exchange wallet
					List<BitFinexTransferStatus> statusTransfer = bitfinexWao.transfer("FROM?????", "exchange", wallet.getData().getCurrencyLabel().toUpperCase(), wallet.getData().getCurrencyValue(), apiKey, secretKey);

					if ("error".equals(statusTransfer.get(0).getStatus().toLowerCase())) {
						sendMail(user, orderId, "TRANSFER_FUND", "Transfer order between your wallets during the long process with BitFinex returned an error, following the close order ID:"+orderId+". Process stopped, no buy order sent.");
						return;
					}
					
					//check the equity on the exchange wallet
					List<BitFinexBalanceStatus> statusBalances = bitfinexWao.getBalances(apiKey, secretKey);
					
					Double availableEquity = 0d;
					
					for (BitFinexBalanceStatus balance : statusBalances) {
		
						if ("exchange".equals(balance.getType()) && balance.getCurrencyLabel().toUpperCase().equals(wallet.getData().getCurrencyLabel().toUpperCase())) {
							availableEquity = balance.getAvailable();
						}
					}
					
					BitFinexOrderStatus buyOrder = bitfinexWao.buy(iasionSymbol, "amount????", "price?????", "exchange what????", apiKey, secretKey);
					
					Double executedBuy = loopCheckStatus(buyOrder.getRemaining_amount(), buyOrder.getExecuted_amount(), buyOrder.getOrder_id(), apiKey, secretKey, 0);
					
					//TODO METTRE A JOUR L'EQUITY DISPO
					

					break;
				}
			}

		} catch (ActivePositionsException e) {
			log.error("Impossible to get the active positions for user "+user.getData().getFirstname()+" "+user.getData().getLastname()+": "+e.getMessage());
			sendMail(user, orderId, "ACTIVE_POSITIONS", "Impossible to get your active positions during the long process with BitFinex. Order impossible to execute.");
		} catch (ClosePositionException e) {
			log.error("Impossible to close position of ID "+orderPositionToClose.getId()+" for user "+user.getData().getFirstname()+" "+user.getData().getLastname()+": "+e.getMessage());
			sendMail(user, orderId, "CLOSE_POSITION", "Impossible to close your active position "+orderPositionToClose.getId()+" during the long process with BitFinex. Close order impossible to execute.");
		} catch (OrderStatusException e) {
			log.error("Impossible to get your close order status for order "+orderId+": "+e);	
			sendMail(user, orderId, "CONNECTION_PROBLEM ?", "Impossible to check the status of a close order (probably a connection problem) during the long process with BitFinex. Will try to cancel the order automatically now but only once.");
			try {
				bitfinexWao.cancelOrder(orderId, apiKey, secretKey);
			} catch (Exception e2) {
				log.error("Cancel close order Exception: "+e2.getMessage());
				sendMail(user, orderId, "CANCEL_ORDER", "Impossible to cancel the close order during the long process with BitFinex, after a connection problem. ");
			}
		} catch (MaxTriesExceededException e) {
			log.error("Max number of trials to check a close order status reached: "+e.getMessage());
			sendMail(user, orderId, "ORDER_CHECK_STATUS", "Impossible to check the status of a close order (Max number of trials reached) during the long process with BitFinex. Will try to cancel the order automatically now but only once.");
			try {
				bitfinexWao.cancelOrder(orderId, apiKey, secretKey);
			} catch (Exception e2) {
				log.error("Cancel close order Exception: "+e2.getMessage());
				sendMail(user, orderId, "CANCEL_ORDER", "Impossible to cancel the close order during the long process with BitFinex, after a max number of trials to check the status. ");
			}
		} catch (TransferException e) {
			log.error("Impossible to transfer currency for user "+user.getData().getFirstname()+" "+user.getData().getLastname()+": "+e.getMessage());
			sendMail(user, orderId, "TRANSFER_FUND", "Impossible to transfer money from wallet to wallet during the long process with BitFinex, after the close order ID "+orderId);
		
		}

	}

	private Double loopCheckStatus(Double remaining, Double executed, Long orderId, String apiKey, String secretKey, int tryNumber) throws Exception {

		if ( tryNumber < ORDERSTATUS_MAXTRY) {				

			if (remaining > 0 ) {

				Thread.sleep(ORDERSTATUS_WAITINGTIME);

				BitFinexOrderStatus status = bitfinexWao.orderStatus(orderId, apiKey, secretKey);
				remaining = status.getRemaining_amount();
				executed = status.getExecuted_amount();
				return loopCheckStatus(remaining, executed, orderId, apiKey, secretKey, ++tryNumber);

			} else 
				return executed;

		} else 
			throw new MaxTriesExceededException("Too many tries for order status check before completion", "ORDERSTATUS_MAXTRY");
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
