package orufeo.iasion.data.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BitFinexPosition {

	private Long id;
	private String symbol;
	private String status;
	private String base;
	private String amount;
	private String timestamp;
	private String swap;
	private String pl;
	
}
