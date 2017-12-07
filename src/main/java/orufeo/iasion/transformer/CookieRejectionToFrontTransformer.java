package orufeo.iasion.transformer;

import java.util.Map;

import org.apache.log4j.Logger;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;

import orufeo.iasion.data.dto.CookieFilterRejection;
import orufeo.iasion.utils.CookiesValueTL;

public class CookieRejectionToFrontTransformer extends AbstractMessageTransformer {

	private static Logger log = Logger.getLogger(CookieRejectionToFrontTransformer.class);

	@Override
	public Object transformMessage(MuleMessage message, String encoding)
			throws TransformerException {

			Map<String, String> cookieMap = CookiesValueTL.get();

			CookieFilterRejection reject = new CookieFilterRejection();

			reject.setErrorMessage("You don't have enough superpowers to do that action !");
			reject.setErrorCode("403");

			return reject;

	}

}
