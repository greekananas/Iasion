package orufeo.iasion.component;

import orufeo.iasion.bo.MacdBo;
import orufeo.iasion.bo.MacdBoImpl;
import orufeo.iasion.data.objects.MacdSettings;
import orufeo.iasion.data.objects.WalletSettings;

public class App {

	public static void main(String... args) throws Exception {
		
		MacdBo macdBo = new MacdBoImpl();
		
		MacdSettings macdSettings = new MacdSettings();
		
		macdSettings.setFastLength(2);
		macdSettings.setSlowLength(6);
		macdSettings.setMACDLength(9);
		
		WalletSettings walletSettings = new WalletSettings();
		walletSettings.setCommission(0d);
		walletSettings.setEquity(10d);
		
		macdBo.init();
		
		macdBo.analyseCryptoPair("BTC", "USD", 12, "Kraken", macdSettings, walletSettings);
		
	}
	
}
