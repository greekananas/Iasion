package orufeo.iasion.data.objects.storage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalletData {

	private String exchangeGuid;
	private String userGuid;
	private String quoteCurrencyLabel;  //EUR, USD,...
	private Double quoteCurrencyValue;
	private String currencyLabel; 		//BTC, ETH,...
	private Double currencyValue;	
	
}
