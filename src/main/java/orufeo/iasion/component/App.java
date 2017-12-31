package orufeo.iasion.component;

import orufeo.iasion.bo.MacdBo;
import orufeo.iasion.bo.MacdBoImpl;
import orufeo.iasion.data.objects.analysis.MacdSettings;
import orufeo.iasion.data.objects.analysis.MacdTrend;
import orufeo.iasion.data.objects.analysis.MacdTrigger;

public class App {

	public static void main(String... args) throws Exception {
		
		MacdBo macdBo = new MacdBoImpl();
		
		Integer aggregateBig = 12;
		Integer aggregateSmall = 4;
		
		MacdSettings macdSettings = new MacdSettings();
		
		macdSettings.setFastLength(2);
		macdSettings.setSlowLength(6);
		macdSettings.setMACDLength(9);
		
		macdBo.init();
		
		MacdTrend matrixTrend = macdBo.analyzeTrend("BTC", "USD", aggregateBig, "bitfinex", macdSettings);
		MacdTrigger matrixTrigger = macdBo.analyzeTrigger("BTC", "USD", aggregateSmall, "bitfinex", macdSettings, matrixTrend);
		
		
	}
	
}
