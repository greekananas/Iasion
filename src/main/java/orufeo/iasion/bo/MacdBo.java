package orufeo.iasion.bo;

import orufeo.iasion.data.objects.analysis.MacdSettings;
import orufeo.iasion.data.objects.analysis.MacdTrend;

public interface MacdBo {

	//http://www.dummies.com/personal-finance/investing/stocks-trading/how-to-calculate-exponential-moving-average-in-trading/
	
	MacdTrend analyzeTrend(String currency, String quoteCurrency, int aggregate, String exchange, MacdSettings macdSettings);
	
	MacdTrend analyzeTrigger(String currency, String quoteCurrency, int aggregate, String exchange, MacdSettings macdSettings, MacdTrend trends);
	
	void init();
}
