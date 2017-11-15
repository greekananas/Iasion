package orufeo.iasion.data.dto;

import org.codehaus.jackson.annotate.JsonAutoDetect;

import lombok.Data;

@JsonAutoDetect
@Data
public class Authentication {

	private Integer id;
	private String login;
	private boolean authenticated;
	private String token;
	private String firstname;
	private String lastname;
	private String role;
	private String reason;

	public Authentication() {
		this.authenticated = false;
		this.reason = "BO Identification problem";
	}

}