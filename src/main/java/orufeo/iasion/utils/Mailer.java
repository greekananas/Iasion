package orufeo.iasion.utils;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;

import orufeo.iasion.data.mail.MailMessagePojo;

public class Mailer {

	public static void sendMail( String uri, MailMessagePojo mail) throws IOException  {

		ObjectMapper mapper = new ObjectMapper();
		
		HttpClient httpclient = HttpClientBuilder.create().build();

		// Create a local instance of cookie store
		CookieStore cookieStore = new BasicCookieStore();

		// Create local HTTP context
		HttpContext localContext = new BasicHttpContext();
		// Bind custom cookie store to the local context
		localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

		HttpPost httppost = new HttpPost(uri);

		StringEntity requestEntity = new StringEntity(
				mapper.writeValueAsString(mail),
				ContentType.APPLICATION_JSON);

		httppost.setEntity(requestEntity);

		// Pass local context as a parameter
		HttpResponse response = httpclient.execute(httppost, localContext);
		HttpEntity entityResp = response.getEntity();

		/*		List<Cookie> cookies = ((CookieStore) localContext.getAttribute(HttpClientContext.COOKIE_STORE)).getCookies();
		for (int i = 0; i < cookies.size(); i++) {
			System.out.println("Local cookie: " + cookies.get(i));
		}
		 */
		// Consume response content
		EntityUtils.consume(entityResp);

	}
	
}
