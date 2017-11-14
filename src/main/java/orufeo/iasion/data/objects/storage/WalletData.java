package orufeo.iasion.data.objects.storage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalletData {

	private String exchangeGuid;
	private String userGuid;
	private String quoteCurrencyLabel;   //BTC, ETH,...
	private Double quoteCurrencyValue;
	private String currencyLabel; 		//EUR, USD,...
	private Double currencyValue;	
	
}
