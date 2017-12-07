/**
 * 
 */
package orufeo.iasion.transformer;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;

/**
 * @author Alexis ANASTASSIADES
 *
 */
public class Json2JsonPTransformer extends AbstractMessageTransformer {

	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding)
			throws TransformerException {

		message.setOutboundProperty("Access-Control-Allow-Origin", "*");

		String retour = null;

		String inboundProperty = message.getInboundProperty("callback");
		if (null != inboundProperty) {
			retour = inboundProperty + "(" + message.getPayload() + ");";
		} else {
			retour = (String) message.getPayload();
		}

		return retour;
	}

}
