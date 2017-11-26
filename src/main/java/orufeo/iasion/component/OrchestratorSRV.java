package orufeo.iasion.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mule.api.annotations.param.Payload;

import lombok.Setter;
import orufeo.iasion.bean.LongProcessBean;
import orufeo.iasion.bo.MacdBo;
import orufeo.iasion.bo.UserAccountBo;
import orufeo.iasion.bo.WalletBo;
import orufeo.iasion.data.dto.HttpStatus;
import orufeo.iasion.data.objects.analysis.MacdSettings;
import orufeo.iasion.data.objects.analysis.MacdTrend;
import orufeo.iasion.data.objects.analysis.MacdTrigger;
import orufeo.iasion.data.objects.storage.UserAccount;
import orufeo.iasion.data.objects.storage.Wallet;

public class OrchestratorSRV {

	@Setter private String ORCHESTRATOR_TOKEN;     //Security
	@Setter private MacdBo macdBo;
	@Setter private UserAccountBo userAccountBo;
	@Setter private WalletBo walletBo;
	@Setter private LongProcessBean longProcessBean;

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

			MacdTrend matrixTrend = macdBo.analyzeTrend(currency.toUpperCase(), quoteCurrency.toUpperCase(), aggregateBig, exchange, macdSettings);
			MacdTrigger matrixTrigger = macdBo.analyzeTrigger(currency.toUpperCase(), quoteCurrency.toUpperCase(), aggregateSmall, exchange, macdSettings, matrixTrend);

			String signal = matrixTrigger.getTrigger().get(matrixTrigger.getTrigger().size()-1);

			List<UserAccount> users = userAccountBo.get();

			//iterate on all users
			for (UserAccount user : users) {

				//retrieve all wallets on all exchanges for the user
				List<Wallet> wallets = walletBo.getForUser(user.getMetadata().getGuid());

				//iterate on his wallets to check if has one or more wallets, matching the currency and quotecurrency relevant to this analysis
				List<Wallet> matchs = new ArrayList<Wallet>();
				for (Wallet wallet : wallets) {
					if (wallet.getData().getQuoteCurrencyLabel().toUpperCase().equals(quoteCurrency.toUpperCase()) && wallet.getData().getCurrencyLabel().toUpperCase().equals(currency.toUpperCase())) {
						matchs.add(wallet);
					}
				}

				//we have the wallets that we need to impact for the user, let's do it
				for (Wallet wallet : matchs) {

					if ("long".equals(signal)) {

						longProcessBean.processLong(wallet);

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


	

}
