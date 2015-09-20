package pl.devsite.icestreamer.render;

import pl.devsite.icestreamer.item.Item;
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
public class RenderMpegSimple implements Render {

	private static final Logger logger = Logger.getLogger(RenderMpegSimple.class.getName());
	private final Item item;
	private final Response response;

	public RenderMpegSimple(Response response, Item item) throws IOException {
		this.response = response;
		this.item = item;
	}

	@Override
	public void send() throws IOException {
		logger.log(Level.INFO, "Requested {0}", item.toString());
		response.type("audio/mpeg");
		RenderMpeg.icyHeaders(response, item);
		try (InputStream in = item.getInputStream(); OutputStream out = response.raw().getOutputStream()) {
			RenderPlain.pump(in, out);
		}
	}	
}
