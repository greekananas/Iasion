package orufeo.iasion.data.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BitFinexPosition {

	private Long id;
	private String symbol;
	private String status;
	private Double base;
	private Double amount;
	private String timestamp;
	private Double swap;
	private Double pl;
	
}
