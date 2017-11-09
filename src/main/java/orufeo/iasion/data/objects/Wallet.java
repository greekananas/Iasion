package orufeo.iasion.data.objects;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Wallet {

	private String exchange;
	private String exchangeApiKey;
	private List<CurrencyPosition> positions;
	private List<Order> orderHistory;
	
	
}
