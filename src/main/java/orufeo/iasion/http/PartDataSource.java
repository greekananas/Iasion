/**
 * 
 */
package orufeo.iasion.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import org.apache.log4j.Logger;
import org.mule.transport.http.multipart.MultiPartInputStream.MultiPart;
import org.mule.transport.http.multipart.Part;

/**
 * @author Alexis ANASTASSIADES alexis - at - ezanas.com
 *
 */
public class PartDataSource implements DataSource {

	private static Logger log = Logger.getLogger(PartDataSource.class);

	private Part part;

	public PartDataSource(Part part) {
		this.part = part;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return part.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException("getOutputStream");
	}

	@Override
	public String getContentType() {
		return part.getContentType();
	}

	@Override
	public String getName() {
		if (part instanceof MultiPart) {
//			log.debug("#### getName :: MULTIPART : "
//					+ ((MultiPart) part).getContentDispositionFilename());
			return ((MultiPart) part).getContentDispositionFilename();
		} else {
			return part.getName();
		}
	}

	public Part getPart() {
		return part;
	}

}
