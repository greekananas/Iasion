package orufeo.iasion.bo;

import orufeo.iasion.data.objects.analysis.MacdSettings;
import orufeo.iasion.data.objects.analysis.MacdTrend;
import orufeo.iasion.data.objects.analysis.MacdTrigger;

public interface MacdBo {

	//http://www.dummies.com/personal-finance/investing/stocks-trading/how-to-calculate-exponential-moving-average-in-trading/
	
	MacdTrend analyzeTrend(String currency, String quoteCurrency, int aggregate, String exchange, MacdSettings macdSettings);
	
	MacdTrigger analyzeTrigger(String currency, String quoteCurrency, int aggregate, String exchange, MacdSettings macdSettings, MacdTrend trends);
	
	void init();
}
