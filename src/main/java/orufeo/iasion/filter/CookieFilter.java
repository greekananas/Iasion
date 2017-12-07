package orufeo.iasion.filter;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

import lombok.Getter;
import lombok.Setter;
import orufeo.iasion.bo.UserAccountBo;
import orufeo.iasion.data.dto.Authentication;
import orufeo.iasion.utils.CookiesValueTL;

public class CookieFilter implements Filter {

	private static Logger log = Logger.getLogger(CookieFilter.class);

	@Getter @Setter private String security;
	@Getter @Setter private UserAccountBo userAccountBo;
	@Getter @Setter private ObjectMapper objectMapper;

	public boolean accept(MuleMessage message) {

		boolean acceptLogin = false;

		Authentication authenticate = new Authentication();

		try {
            
			Cookie[] inboundProperty = (Cookie[]) message.getInboundProperty("cookies");
			String inboundRelativePath = (String) message.getInboundProperty("http.relative.path");
			String realIp = (String) message.getInboundProperty("X-Real-IP");
			Map<String, String> queryParams = (Map<String, String>) message.getInboundProperty("http.query.params");
			String context = (String) message.getInboundProperty("http.context.path");
			String payload = message.getPayloadAsString();
			HashMap<String, String> cookieMap = new HashMap<String, String>();	

			//INBOUND scoped properties:
			//MULE_REMOTE_CLIENT_ADDRESS=86.246.115.8
		    //X-Forwarded-For=86.246.115.8
		    //X-Real-IP=86.246.115.8
			

			if (inboundRelativePath.contains("authenticate")  ) {  
				CookiesValueTL.set(cookieMap);
				return true;
			}
			
			if (null != inboundRelativePath) {

				if  (null != inboundProperty) {

					String token = "";

					for (Cookie cookie : inboundProperty) {

						if (log.isDebugEnabled()) {
							log.debug("########## Cookie name="+cookie.getName()+", value="+cookie.getValue());
						}

						if ("token".equals(cookie.getName())) {
							token = cookie.getValue().trim();
						} 
						
						cookieMap.put(cookie.getName(), cookie.getValue().trim());
					}

					CookiesValueTL.set(cookieMap);

					authenticate = userAccountBo.authenticate(token);

					if (authenticate.isAuthenticated())
						acceptLogin = true;
					
				} 
			}

		} catch (Exception e) {
			log.warn("Iasion CookieFilter::accept Filtre inboundProperty Error: ",e);
		}

		return acceptLogin;

	}

	

}
