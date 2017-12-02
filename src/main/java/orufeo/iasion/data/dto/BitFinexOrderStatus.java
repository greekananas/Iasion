package orufeo.iasion.data.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BitFinexOrderStatus {
	
	private Long id;
	private String symbol;
	private String exchange;
	private String price;
	private String avg_execution_price;
	private String side;
	private String type;
	private String timestamp;
	private Boolean is_live;
	private Boolean is_cancelled;
	private Boolean is_hidden;
	private Boolean was_forced;
	private String original_amount;
	private String remaining_amount;
	private String executed_amount;
	private Long order_id;

}
