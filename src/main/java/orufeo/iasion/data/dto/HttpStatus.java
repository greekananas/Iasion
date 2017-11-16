package orufeo.iasion.data.dto;

import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HttpStatus {
	
	private String status;
	private String code;
	private String reason;
	private HashMap<String, String> values;
	
	public HttpStatus(String status, String code, String reason) {
		
		this.status = status;
		this.code = code;
		this.reason = reason;
	}
	
	public HttpStatus(String status, String code, String reason, HashMap<String, String> values) {
		
		this.status = status;
		this.code = code;
		this.reason = reason;
		this.values = values;
	}

}
