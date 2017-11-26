/*
 * 2014 A-Mano
 */
package orufeo.iasion.data.mail;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author ananas
 *
 */
@XmlRootElement(name = "MailMessagePojo")
public class MailMessagePojo implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1419951731883492184L;

	public static final String CC_SENDER_TITLE = "iasion_sender";

	@Getter
	@Setter
	private String email;
	@Getter
	@Setter
	private String replyTo;
	@Getter
	@Setter
	private String fromAddress;
	@Getter
	@Setter
	private String subject;
	@Getter
	@Setter
	private String cc;
	@Getter
	@Setter
	private String bcc;
	@Getter
	@Setter
	private String body;
	@Getter
	@Setter
	private String htmlBody;
	@Getter
	@Setter
	private String ccSender;
	@Getter
	@Setter
	private List<Attachement> attachements = new ArrayList<MailMessagePojo.Attachement>();
	@Getter
	@Setter
	private List<Attachement> inlinesImg = new ArrayList<MailMessagePojo.Attachement>();
	@Getter
	@Setter
	private String login;
	@Getter
	@Setter
	private String siret;
	@Getter
	@Setter
	private String uid;

	public MailMessagePojo() {
		super();
	}

	/**
	 * @param email
	 * @param subject
	 * @param body
	 * @param attachements
	 */
	public MailMessagePojo(String email, String subject, String body,
			String htmlBody, String ccSender) {
		super();
		this.email = email;
		this.subject = subject;
		this.body = body;
		this.htmlBody = htmlBody;
		this.ccSender = ccSender;
	}

	/**
	 * @param attachements
	 *            the attachements to set
	 */
	public void addAttachement(byte[] stringDataHandler, String filename,
			String contentType) {
		this.attachements.add(new Attachement(stringDataHandler, filename,
				contentType));
	}

	/**
	 * 
	 * @param stringDataHandler
	 * @param filename
	 * @param contentType
	 */
	public void clearAttachement() {
		this.attachements.clear();
	}

	/**
	 * @param inlinesImg
	 *            the attachements to set
	 */
	public void addInlinesImg(byte[] stringDataHandler, String filename,
			String contentType) {
		this.inlinesImg.add(new Attachement(stringDataHandler, filename,
				contentType));
	}

	/**
	 * 
	 * @param stringDataHandler
	 * @param filename
	 * @param contentType
	 */
	public void clearInlinesImg() {
		this.inlinesImg.clear();
	}

	/**
	 * 
	 * Permet la sérialisation des dataHandlers
	 * 
	 * @author belattafSiret
	 *
	 */
	public static class Attachement implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6703805036636032073L;

		@Getter
		private byte[] stringDataHandler;
		@Getter
		@Setter
		private String fileName;
		@Getter
		@Setter
		private String contentType;

		/**
		 * Constructeur vide
		 */
		public Attachement() {
			super();
		}

		/**
		 * Constructeur fields
		 * 
		 * @param stringDataHandler
		 * @param filename
		 * @param contentType
		 */
		public Attachement(byte[] stringDataHandler, String filename,
				String contentType) {
			this.stringDataHandler = stringDataHandler;
			setFileName(filename);
			setContentType(contentType);
		}

		/**
		 * 
		 * @param stringDataHandler
		 */
		public void setStringDataHandler(byte[] stringDataHandler) {
			byte[] stringDH = stringDataHandler;
			this.stringDataHandler = stringDH;
		}

		/**
		 * 
		 * @param dh
		 * @param size
		 */
		public void setStringDataHandler(DataHandler dh, Long size) {
			try {
				InputStream in = dh.getInputStream();
				stringDataHandler = new byte[size.intValue()];

				in.read(stringDataHandler);

				setStringDataHandler(stringDataHandler);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("fromAddress : " + this.fromAddress).append("\n")
				.append("email : " + this.email).append("\n")
		.append("cc : " + this.cc).append("\n")
		.append("bcc : " + this.bcc).append("\n")
		.append("ccSender : " + this.ccSender).append("\n")
		.append("fromAddress : " + this.fromAddress).append("\n")
		.append("replyTo : " + this.replyTo).append("\n")
		.append("subject : " + this.subject).append("\n")
		.append("body : " + this.body).append("\n")
		.append("htmlBody : " + this.htmlBody).append("\n")
		.append("login : " + this.login).append("\n")
		.append("siret : " + this.siret).append("\n")
		.append("Nb attachements : " + this.attachements.size());

		return strBuilder.toString();
	}
}
