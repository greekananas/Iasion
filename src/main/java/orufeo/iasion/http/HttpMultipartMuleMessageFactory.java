/*
 * $Id: HttpMultipartMuleMessageFactory.java ******* 2012-06-14 19:00:07Z ******** $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package orufeo.iasion.http;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.transport.http.HttpRequest;
import org.mule.transport.http.multipart.MultiPartInputStream;
import org.mule.transport.http.multipart.MultiPartInputStream.MultiPart;
import org.mule.transport.http.multipart.Part;

/**
 * 
 * Correction Alexis ANASTASSIADES ___ alexis - at - ezanas.com
 *
 */
public class HttpMultipartMuleMessageFactory extends HttpMuleMessageFactory {

	private static Logger log = Logger.getLogger(HttpMultipartMuleMessageFactory.class);

	//private Collection<Part> parts;
	private ConcurrentHashMap<Long, Collection<Part>> hmParts = new ConcurrentHashMap<Long, Collection<Part>>();

	public HttpMultipartMuleMessageFactory(MuleContext context) {
		super(context);
	}

	@Override
	protected Object extractPayloadFromHttpRequest(HttpRequest httpRequest)
			throws IOException {
		Object body = null;

		if (httpRequest.getContentType().contains("multipart/form-data")) {
			MultiPartInputStream in = new MultiPartInputStream(
					httpRequest.getBody(), httpRequest.getContentType(), null);
			//ThreadID to store the parts (to be Thread Safe)
//			if (log.isDebugEnabled()) {
//				Thread currentThread = Thread.currentThread();
//				log.debug("ThreadID : " + currentThread.getId());
//				log.debug("hmParts size A: " + hmParts.size());
//			}
			Long tid = Thread.currentThread().getId();
			
			// We need to store this so that the headers for the part can be
			// read
//			if (log.isDebugEnabled()) log.debug("extractPayloadFromHttpRequest :: init du parts");
			Collection<Part> parts = in.getParts();
			hmParts.put(tid, parts);
			
//			log.debug("hmParts size B : " + hmParts.size());
			
//			if (log.isDebugEnabled()) log.debug("extractPayloadFromHttpRequest :: parts == null ? " + (null == parts));
			for (Part part : parts) {
				MultiPart mPart = (MultiPart) part;
				if (null != mPart.getContentDispositionFilename()
						&& !part.getName().equals("payload")) {
					body = part.getInputStream();
					break;
				}
			}
			if (body == null) {
				throw new IllegalArgumentException(
						"Body part containing file is null. Abort");
			}
		} else {
			body = super.extractPayloadFromHttpRequest(httpRequest);
		}

		return body;
	}

	@Override
	protected void addAttachments(DefaultMuleMessage message, Object transportMessage) throws Exception {
//		if (log.isDebugEnabled()) log.debug("addAttachments");
		
		//ThreadID to get the parts (to be Thread Safe)
		Long tid = Thread.currentThread().getId();
		Collection<Part> parts = hmParts.get(tid);
		if (parts != null) {
//			if (log.isDebugEnabled()) log.debug("addAttachments :: parts size ? " + (parts.size()));
			Map<String, Object> headers = new HashMap<String, Object>();
			try {
				for (Part part : parts) {
					MultiPart mPart = (MultiPart) part;
//					if (log.isDebugEnabled()) log.debug("#### addAttachments :: " + mPart.getName()+ " - " + mPart.getContentDispositionFilename());
					if (null != mPart.getContentDispositionFilename()
							&& !part.getName().equals("payload")) {
//						log.debug("### addAttachments :: adding a file : " + mPart.getName());
//						log.debug("### taille de la pièce jointe : " + mPart.getSize());
						ByteArrayDataSource bads = new ByteArrayDataSource(
								mPart.getInputStream(), mPart.getContentType());
						bads.setName(mPart.getContentDispositionFilename());
						DataHandler dataHandler = new DataHandler(bads);
						message.addInboundAttachment(mPart.getName(),
								dataHandler);
					}
					// tweak pour ajouter des headers dans les propoerties du
					// message.
					else if (null == mPart.getContentDispositionFilename()
							&& !part.getName().equals("payload")) {
//						log.debug("### addAttachments :: Not a file, just adding in inbounds properties : " + mPart.getName());
						headers.put(
								part.getName(),
								new String(IOUtils.toByteArray(part
										.getInputStream())));
					}
				}
			} finally {
				// Attachments are the last thing to get processed
//				log.debug("hmParts size C : " + hmParts.size());
				parts.clear();
				parts = null;
				hmParts.remove(tid);
//				log.debug("hmParts size D : " + hmParts.size());
				// tweak pour ajouter des headers dans les propoerties du
				// message.
				message.addInboundProperties(headers);
			}
		}
	}

	@Override
	protected void convertMultiPartHeaders(Map<String, Object> headers) {
		//ThreadID to get the parts (to be Thread Safe)
		Long tid = Thread.currentThread().getId();
		Collection<Part> parts = hmParts.get(tid);
//		log.debug("hmParts size F : " + hmParts.size());
		if (parts != null) {
			for (Part part : parts) {
				if (part instanceof MultiPart) {
					MultiPart mPart = (MultiPart) part;
					if (null == mPart.getContentDispositionFilename()) {
						try {
							headers.put(
									part.getName(),
									new String(IOUtils.toByteArray(part
											.getInputStream())));
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						for (String name : part.getHeaderNames()) {
							headers.put(name, part.getHeader(name));
						}
						break;
					}
				}

			}
		}
	}

}
