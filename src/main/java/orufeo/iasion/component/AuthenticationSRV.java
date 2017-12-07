package orufeo.iasion.component;

import java.util.Map;

import org.apache.log4j.Logger;
import org.mule.api.annotations.param.Payload;
import org.springframework.dao.EmptyResultDataAccessException;

import lombok.Setter;
import orufeo.iasion.bo.UserAccountBo;
import orufeo.iasion.data.dto.Authentication;

public class AuthenticationSRV {
	
	private static Logger log = Logger.getLogger(AuthenticationSRV.class);
	
	@Setter private UserAccountBo userAccountBo;
	
	public Authentication authenticate(@Payload Map<String, String> args) {

		String login = args.get("login");
		String password = args.get("password");
		String token = args.get("token");

		// Requete en base pour authent
		// le token doit être salé entre le login et le mdp et hashé en SHA2

		Authentication authentication = new Authentication();

		try {
			if (null != login && null != password) {
				authentication = userAccountBo.authenticate(login, password);
			} else if (null != token) {
				authentication = userAccountBo.authenticate(token);
			}
		} catch (EmptyResultDataAccessException e) {
			log.error("authenticate: Wrong credentials", e);
		}

		return authentication;
	}

}
