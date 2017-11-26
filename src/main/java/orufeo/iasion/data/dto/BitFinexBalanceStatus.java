package orufeo.iasion.data.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BitFinexBalanceStatus {
	
	private String type;
	private String currencyLabel;
	private Double amount;
	private Double available;

}
