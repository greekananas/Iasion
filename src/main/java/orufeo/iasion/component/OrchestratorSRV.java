package orufeo.iasion.component;

import java.util.List;
import java.util.Map;

import org.mule.api.annotations.param.Payload;

import lombok.Setter;
import orufeo.iasion.bo.MacdBo;
import orufeo.iasion.bo.UserAccountBo;
import orufeo.iasion.data.dto.HttpStatus;
import orufeo.iasion.data.objects.analysis.MacdSettings;
import orufeo.iasion.data.objects.analysis.MacdTrend;
import orufeo.iasion.data.objects.analysis.MacdTrigger;
import orufeo.iasion.data.objects.storage.UserAccount;

public class OrchestratorSRV {

	@Setter public String ORCHESTRATOR_TOKEN;
	@Setter public MacdBo macdBo;
	@Setter public UserAccountBo userAccountBo;

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

			for (UserAccount user : users) {

				if ("long".equals(signal)) {

				} else if ("short".equals(signal)) {

				} else if ("prise long".equals(signal)) {

				} else if ("prise short".equals(signal)) {

				} else if ("cloture long".equals(signal)) {

				} else if ("cloture short".equals(signal)) {

				} 

			}
			
			return new HttpStatus("OK", "200", "");

		} else
			return new HttpStatus("Wrong Token", "500", "");
	}
}
