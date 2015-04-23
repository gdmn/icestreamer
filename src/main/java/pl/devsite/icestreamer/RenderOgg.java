package pl.devsite.icestreamer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import spark.Response;

/**
 *
 * @author dmn
 */
public class RenderOgg implements Render {

	private static final Logger logger = Logger.getLogger(RenderOgg.class.getName());
	private final Item item;
	private final Response response;

	public RenderOgg(Response response, Item item) throws IOException {
		this.response = response;
		this.item = item;
	}

	@Override
	public void send() throws IOException {
		logger.log(Level.INFO, "Requested {0}", item.toString());
		response.type("application/ogg");
		RenderMpeg.icyHeaders(response, item);
		try (InputStream in = item.getInputStream(); OutputStream out = response.raw().getOutputStream()) {
			RenderPlain.pump(in, out);
		}
	}	
}
