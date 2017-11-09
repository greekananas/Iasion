package orufeo.iasion.bo;

import orufeo.iasion.data.objects.MacdSettings;
import orufeo.iasion.data.objects.Order;
import orufeo.iasion.data.objects.WalletSettings;

public interface MacdBo {

	//http://www.dummies.com/personal-finance/investing/stocks-trading/how-to-calculate-exponential-moving-average-in-trading/
	
	Order analyseCryptoPair(String currency, String quoteCurrency, int aggregate, String exchange, MacdSettings macdSettings, WalletSettings walletSettings);
	
	void init();
}
