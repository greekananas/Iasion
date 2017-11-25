package orufeo.iasion.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mule.api.annotations.param.Payload;

import lombok.Setter;
import orufeo.iasion.bo.ExchangeBo;
import orufeo.iasion.bo.MacdBo;
import orufeo.iasion.bo.UserAccountBo;
import orufeo.iasion.bo.WalletBo;
import orufeo.iasion.data.dto.BitFinexOrderStatus;
import orufeo.iasion.data.dto.BitFinexPosition;
import orufeo.iasion.data.dto.HttpStatus;
import orufeo.iasion.data.objects.analysis.MacdSettings;
import orufeo.iasion.data.objects.analysis.MacdTrend;
import orufeo.iasion.data.objects.analysis.MacdTrigger;
import orufeo.iasion.data.objects.storage.Exchange;
import orufeo.iasion.data.objects.storage.UserAccount;
import orufeo.iasion.data.objects.storage.Wallet;
import orufeo.iasion.wao.BitfinexWao;

public class OrchestratorSRV {

	@Setter private String ORCHESTRATOR_TOKEN;
	@Setter private MacdBo macdBo;
	@Setter private UserAccountBo userAccountBo;
	@Setter private WalletBo walletBo;
	@Setter private ExchangeBo exchangeBo;
	@Setter private BitfinexWao bitfinexWao;

	@Setter private String BITFINEX_CODE;     // bitfinex
	
	private static Logger log = Logger.getLogger(OrchestratorSRV.class);

	public HttpStatus macdAnalysis(@Payload Map<String, String> args) {

		String token = args.get("token");		
		Integer aggregateBig = Integer.valueOf(args.get("aggregateBig"));  //12;
		Integer aggregateSmall = Integer.valueOf(args.get("aggregateSmall"));  //4;
		Integer fastLength = Integer.valueOf(args.get("fastLength"));  //2;
		Integer slowLength = Integer.valueOf(args.get("slowLength"));  //6;
		Integer macdLength = Integer.valueOf(args.get("macdLength"));  //9;
		String currency = args.get("currency");  //"BTC";
		String quoteCurrency = args.get("quoteCurrency");  //"USD";
		String exchange = args.get("exchange");  //"Kraken";

		if (ORCHESTRATOR_TOKEN.equals(token)) {

			MacdSettings macdSettings = new MacdSettings();

			macdSettings.setFastLength(fastLength);
			macdSettings.setSlowLength(slowLength);
			macdSettings.setMACDLength(macdLength);

			MacdTrend matrixTrend = macdBo.analyzeTrend(currency, quoteCurrency, aggregateBig, exchange, macdSettings);
			MacdTrigger matrixTrigger = macdBo.analyzeTrigger(currency, quoteCurrency, aggregateSmall, exchange, macdSettings, matrixTrend);

			String signal = matrixTrigger.getTrigger().get(matrixTrigger.getTrigger().size()-1);

			List<UserAccount> users = userAccountBo.get();

			//iterate on all users
			for (UserAccount user : users) {

				//retrieve all wallets on all exchanges for the user
				List<Wallet> wallets = walletBo.getForUser(user.getMetadata().getGuid());

				//iterate on his wallets to check if has one or more wallets, matching the currency and quotecurrency relevant to this analysis
				List<Wallet> matchs = new ArrayList<Wallet>();
				for (Wallet wallet : wallets) {
					if (wallet.getData().getQuoteCurrencyLabel().equals(quoteCurrency) && wallet.getData().getCurrencyLabel().equals(currency)) {
						matchs.add(wallet);
					}
				}

				//we have the wallets that we need to impact for the user, let's do it
				for (Wallet wallet : matchs) {

					if ("long".equals(signal)) {

						longProcess(wallet);

					} else if ("short".equals(signal)) {

					} else if ("prise long".equals(signal)) {

					} else if ("prise short".equals(signal)) {

					} else if ("cloture long".equals(signal)) {

					} else if ("cloture short".equals(signal)) {

					} 

				}

			}

			return new HttpStatus("OK", "200", "");

		} else
			return new HttpStatus("Wrong Token", "500", "");
	}

	private void longProcess(Wallet wallet) {

		Exchange exchange = exchangeBo.get(wallet.getData().getExchangeGuid());

		if (BITFINEX_CODE.equals(exchange.getData().getCode())) { 

			try {
				longProcessBitfinex(wallet);
			}catch (Exception e ) {
				log.error("longProcess for BitFinex Error: ", e);
			}

		}

	}

	private void longProcessBitfinex(Wallet wallet) throws Exception {

		UserAccount user = userAccountBo.get(wallet.getData().getUserGuid());

		String apiKey = user.getData().getExchKeys().get(BITFINEX_CODE).getApiKey();
		String secretKey = user.getData().getExchKeys().get(BITFINEX_CODE).getSecretKey();
		
		Double solde = wallet.getData().getCurrencyValue(); // -----> ? check if this is the right field

		//GET active positions
		List<BitFinexPosition> activePositions = bitfinexWao.getActivePositions(apiKey, secretKey);
		
		String iasionSymbol = wallet.getData().getQuoteCurrencyLabel().toUpperCase()+wallet.getData().getCurrencyLabel().toUpperCase();
		
		for (BitFinexPosition position : activePositions) {
			if (iasionSymbol.equals(position.getSymbol().toUpperCase())) {

				BitFinexOrderStatus closeReturn = bitfinexWao.closePosition(position, apiKey, secretKey);  //------------------------> TODO
				
				Long orderId = closeReturn.getOrder_id();
				
				//check the execution of the order ----> WHILE ? BUT HOW MANY TIMES ? HOW LONG ?
				bitfinexWao.orderStatus(orderId, apiKey, secretKey);
				
				//once executed, we update the iasion wallet
				solde = solde+bitfinexWao.orderStatus(orderId, apiKey, secretKey).getExecuted_amount();
				
				wallet.getData().setCurrencyValue(solde);
				
				//we store the wallet in Iasion
				walletBo.update(wallet, Long.toString(orderId));
				
				//move  
				
			}
		}
		


	}




}
