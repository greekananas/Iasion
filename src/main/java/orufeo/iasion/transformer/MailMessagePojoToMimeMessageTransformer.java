package orufeo.iasion.transformer;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transport.email.MailProperties;
import org.mule.transport.email.MailUtils;
import org.mule.transport.email.SmtpConnector;
import org.mule.util.MapUtils;
import org.mule.util.TemplateParser;

import orufeo.iasion.data.mail.MailMessagePojo;
import orufeo.iasion.data.mail.MailMessagePojo.Attachement;

/**
 * Composes an e-mail notification message to be sent based on the Book Order.
 */
public class MailMessagePojoToMimeMessageTransformer extends
		AbstractMessageTransformer {

	/**
	 * logger used by this class
	 */
	private final Log logger = LogFactory.getLog(getClass());

	private TemplateParser templateParser = TemplateParser
			.createMuleStyleParser();

	@Override
	public Object transformMessage(MuleMessage message, String encoding)
			throws TransformerException {

		SmtpConnector connector = (SmtpConnector) endpoint.getConnector();

		MailMessagePojo mmp = (MailMessagePojo) message.getPayload();

//		logger.debug("### getFromAddress : " + connector.getFromAddress());
//		logger.debug("### getFromAddress : "
//				+ endpoint.getProperty("fromAddress"));
//		logger.debug(connector.getName());
		String to = mmp.getEmail();

		logger.info("Destinataire:" + to + "/");

		String cc = mmp.getCc();
		String bcc = mmp.getBcc();
		String from = (null != mmp.getFromAddress()) ? mmp.getFromAddress()
				: (String) endpoint.getProperty("fromAddress");
		String replyTo = (null != mmp.getReplyTo()) ? mmp.getReplyTo()
				: (String) endpoint.getProperty("replyToAddresses");
		String subject = mmp.getSubject();
		String ccSender = mmp.getCcSender();
		String contentType = message
				.getOutboundProperty(MailProperties.CONTENT_TYPE_PROPERTY);

		Properties headers = new Properties();
		Properties customHeaders = connector.getCustomHeaders();

		if (customHeaders != null && !customHeaders.isEmpty()) {
			headers.putAll(customHeaders);
		}

		Properties otherHeaders = (Properties) message
				.getOutboundProperty(MailProperties.CUSTOM_HEADERS_MAP_PROPERTY);
		if (otherHeaders != null && !otherHeaders.isEmpty()) {

			headers.putAll(templateParser.parse(
					new TemplateParser.TemplateCallback() {
						@Override
						public Object match(String token) {
							return muleContext.getRegistry()
									.lookupObject(token);
						}
					}, otherHeaders));

		}
		// ajouter le header ccSender
		if (ccSender != null) {
			headers.put(MailMessagePojo.CC_SENDER_TITLE, ccSender);
		}

	//	if (logger.isDebugEnabled()) {
			StringBuffer buf = new StringBuffer();
			buf.append("Constructing email using:\n");
			buf.append("To: ").append(to);
			buf.append("From: ").append(from);
			buf.append("CC: ").append(cc);
			buf.append("BCC: ").append(bcc);
			buf.append("Subject: ").append(subject);
			buf.append("ReplyTo: ").append(replyTo);
			buf.append("Content type: ").append(contentType);
			buf.append("Payload type: ").append(
					message.getPayload().getClass().getName());
			buf.append("Custom Headers: ").append(
					MapUtils.toString(headers, false));
			buf.append("ccSender : ").append(ccSender);
			logger.info(buf.toString());
	//	}

		try {
			MimeMessage email = new MimeMessage(
					((SmtpConnector) endpoint.getConnector())
							.getSessionDetails(endpoint).getSession());

			email.setRecipients(Message.RecipientType.TO,
					MailUtils.stringToInternetAddresses(to));

			// sent date
			email.setSentDate(Calendar.getInstance().getTime());

			if (org.apache.commons.lang.StringUtils.isNotBlank(from)) {
				email.setFrom(MailUtils.stringToInternetAddresses(from)[0]);
			}

			if (org.apache.commons.lang.StringUtils.isNotBlank(cc)) {
				email.setRecipients(Message.RecipientType.CC,
						MailUtils.stringToInternetAddresses(cc));
			}

			if (org.apache.commons.lang.StringUtils.isNotBlank(bcc)) {
				email.setRecipients(Message.RecipientType.BCC,
						MailUtils.stringToInternetAddresses(bcc));
			}

			if (org.apache.commons.lang.StringUtils.isNotBlank(replyTo)) {
				email.setReplyTo(MailUtils.stringToInternetAddresses(replyTo));
			}

			email.setSubject(subject, "UTF-8");

			for (Iterator<Entry<Object, Object>> iterator = headers.entrySet()
					.iterator(); iterator.hasNext();) {
				Map.Entry<Object, Object> entry = iterator.next();
				email.setHeader(entry.getKey().toString(), entry.getValue()
						.toString());
			}

			setContent(email, contentType, message);

			return email;
		} catch (Exception e) {
			throw new TransformerException(this, e);
		}
	}

	/**
	 * 
	 * @param msg
	 * @param contentType
	 * @param message
	 * @throws Exception
	 */
	protected void setContent(Message msg, String contentType,
			MuleMessage message) throws Exception {

		MailMessagePojo mmp = (MailMessagePojo) message.getPayload();

		/*
		 * - Alternative - text - related - html - img
		 */
		Multipart alternativeMultipart = new MimeMultipart("alternative");

		// Alternative contient le text d'abord
		MimeBodyPart body = new MimeBodyPart();
		body.setContent(mmp.getBody(), "text/plain;charset=UTF-8");
		alternativeMultipart.addBodyPart(body);

		// Container pour Alternative pour ajout du related
		MimeBodyPart alternativeContainer = new MimeBodyPart();
		alternativeMultipart.addBodyPart(alternativeContainer);

		Multipart relatedMultipart = new MimeMultipart("related");
		// MimeBodyPart relatedContainer = new MimeBodyPart();
		// relatedMultipart.addBodyPart(relatedContainer);

		MimeBodyPart htmlBody = new MimeBodyPart();
		htmlBody.setContent(mmp.getHtmlBody(), "text/html;charset=UTF-8");
		relatedMultipart.addBodyPart(htmlBody);

		// relatedContainer.setContent(alternativeMultipart);

		for (Attachement inlineImg : mmp.getInlinesImg()) {
			MimeBodyPart imageBodyPart = new MimeBodyPart();
			imageBodyPart.setDataHandler(new DataHandler(
					new ByteArrayDataSource(inlineImg.getStringDataHandler(),
							inlineImg.getContentType())));
			imageBodyPart.setFileName(inlineImg.getFileName());
			imageBodyPart.setHeader("Content-ID", "<" + inlineImg.getFileName()
					+ ">");

			relatedMultipart.addBodyPart(imageBodyPart);
		}
		// Ajout du related à l'Alternative
		alternativeContainer.setContent(relatedMultipart);

		// Traitement des pièces jointes
		if (mmp.getAttachements().size() > 0) {
			// TODO à refaire
			MimeBodyPart mixedContainer = new MimeBodyPart();
			mixedContainer.setContent(alternativeMultipart);

			Multipart mixedMultipart = new MimeMultipart("mixed");
			mixedMultipart.addBodyPart(mixedContainer);

			for (Attachement att : mmp.getAttachements()) {
				MimeBodyPart fich = new MimeBodyPart();
				fich.setDataHandler(new DataHandler(new ByteArrayDataSource(att
						.getStringDataHandler(), att.getContentType())));
				fich.setFileName(att.getFileName());
				fich.setHeader("Content-ID", "<" + att.getFileName() + ">");

				mixedMultipart.addBodyPart(fich);
			}

			msg.setContent(mixedMultipart);
		} else {
			msg.setContent(alternativeMultipart);
		}
	}

}
