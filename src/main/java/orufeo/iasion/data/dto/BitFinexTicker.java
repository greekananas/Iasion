
package orufeo.iasion.data.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BitFinexTicker {

	private String mid;
	private String bid;
	private String ask;
	private String last_price;
	private String low;
	private String high;
	private String volume;
	private String timestamp;
	
}
