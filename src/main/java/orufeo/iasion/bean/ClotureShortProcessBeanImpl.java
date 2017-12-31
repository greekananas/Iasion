package orufeo.iasion.bean;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import lombok.Setter;
import orufeo.iasion.bo.ExchangeBo;
import orufeo.iasion.bo.UserAccountBo;
import orufeo.iasion.bo.WalletBo;
import orufeo.iasion.data.dto.BitFinexOrderStatus;
import orufeo.iasion.data.dto.BitFinexPosition;
import orufeo.iasion.data.mail.MailMessagePojo;
import orufeo.iasion.data.objects.storage.Exchange;
import orufeo.iasion.data.objects.storage.UserAccount;
import orufeo.iasion.data.objects.storage.Wallet;
import orufeo.iasion.exception.ActivePositionsException;
import orufeo.iasion.exception.ClosePositionException;
import orufeo.iasion.exception.MaxTriesExceededException;
import orufeo.iasion.exception.OrderStatusException;
import orufeo.iasion.utils.Mailer;
import orufeo.iasion.wao.BitfinexWao;

public class ClotureShortProcessBeanImpl implements ClotureShortProcessBean {

	@Setter private ExchangeBo exchangeBo;
	@Setter private UserAccountBo userAccountBo;
	@Setter private WalletBo walletBo;
	@Setter private BitfinexWao bitfinexWao;

	@Setter private String BITFINEX_CODE;     			// bitfinex
	@Setter private Integer ORDERCLOSE_MAXTRY; 			// 500
	@Setter private Integer ORDERCLOSE_WAITINGTIME;   	// 500 ms
	@Setter private Integer ORDERSTATUS_MAXTRY; 		// 500
	@Setter private Integer ORDERSTATUS_WAITINGTIME;    // 500 ms
	
	private static Logger log = Logger.getLogger(ClotureShortProcessBeanImpl.class);


	@Override
	public void process(Wallet wallet) {

		Exchange exchange = exchangeBo.get(wallet.getData().getExchangeGuid());

		if (BITFINEX_CODE.toLowerCase().equals(exchange.getData().getCode().toLowerCase())) { 
			try {
				clotureShortProcessBitfinex(wallet);
			} catch (Exception e ) {
				log.error("clotureShortProcess for BitFinex Error: ", e);
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
			throw new MaxTriesExceededException("Too many tries for order status check before completion", previousStatus.getOrder_id().toString());
	}


	private void clotureShortProcessBitfinex(Wallet wallet) throws InterruptedException, IOException  {

		UserAccount user = userAccountBo.get(wallet.getData().getUserGuid());
		String iasionSymbol = wallet.getData().getCurrencyLabel().toLowerCase()+wallet.getData().getQuoteCurrencyLabel().toLowerCase();

		String apiKey = user.getData().getExchKeys().get(BITFINEX_CODE.toLowerCase()).getApiKey();
		String secretKey = user.getData().getExchKeys().get(BITFINEX_CODE.toLowerCase()).getSecretKey();

		BitFinexPosition orderPositionToClose = null;
		
		try {
			//GET active positions
			List<BitFinexPosition> activePositions = bitfinexWao.getActivePositions(apiKey, secretKey);

			for (BitFinexPosition position : activePositions) {
				if (iasionSymbol.equals(position.getSymbol().toLowerCase())) {

					orderPositionToClose = position;
					
					//close position and update the wallet values
					closePosition( wallet, position, apiKey, secretKey,  0);
					
				}
			}
		} catch (ActivePositionsException e) {
			log.error("Impossible to get the active positions for user "+user.getData().getFirstname()+" "+user.getData().getLastname()+": "+e.getMessage());
			sendMail(user, 0L, "ACTIVE_POSITIONS", "Impossible to get your active positions during the cloture short process with BitFinex. Order impossible to execute.");
		} catch (ClosePositionException e) {
			log.error("Impossible to close position of ID "+orderPositionToClose.getId()+" for user "+user.getData().getFirstname()+" "+user.getData().getLastname()+": "+e.getMessage());
			sendMail(user, 0L, "CLOSE_POSITION", "Impossible to close your active position "+orderPositionToClose.getId()+", pair="+orderPositionToClose.getSymbol()+", amount="+orderPositionToClose.getAmount()+" during the cloture short process with BitFinex. Close order impossible to execute.");
		} catch (MaxTriesExceededException e) {
			log.error("Max number of trials to check a close order status reached: "+e.getMessage());
			sendMail(user, Long.valueOf(e.getErrorCode()), "MAXTRIES_EXCEPTION", "Impossible to check the status of a close order (Max number of trials reached) during the cloture short  process with BitFinex. ");
		} catch (OrderStatusException e) {
			log.error("Impossible to get your close order status for order "+Long.valueOf(e.getErrorCode())+": "+e);	
			sendMail(user, Long.valueOf(e.getErrorCode()), "CONNECTION_PROBLEM ?", "Impossible to check the status of a close order (probably a connection problem) during the cloture short  process with BitFinex.");
		}
	}
	
	private void closePosition(Wallet wallet, BitFinexPosition position, String apiKey, String secretKey, Integer index) throws InterruptedException, ClosePositionException, MaxTriesExceededException, OrderStatusException {

				
		BitFinexOrderStatus closeReturn = proceedClosure(position, apiKey, secretKey, index); 

		Long orderId = closeReturn.getOrder_id();

		//check the execution of the order 
		BitFinexOrderStatus statusClose = loopCheckStatus(closeReturn, apiKey, secretKey, 0);

		//once executed, we update the iasion wallet
		Double amount = Double.valueOf(closeReturn.getOriginal_amount())* Double.valueOf(statusClose.getAvg_execution_price());
		
		wallet.getData().setQuoteCurrencyValue(wallet.getData().getQuoteCurrencyValue()-amount);
		wallet.getData().setCurrencyValue(wallet.getData().getCurrencyValue()+amount);
		
		walletBo.update(wallet, Double.toString(orderId));

	}
	
	private BitFinexOrderStatus proceedClosure(BitFinexPosition position, String apiKey, String secretKey, Integer index) throws ClosePositionException, InterruptedException {

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
