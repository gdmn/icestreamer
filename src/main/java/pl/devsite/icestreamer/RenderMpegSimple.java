package pl.devsite.icestreamer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import spark.Response;

/**
 *
 * @author dmn
 */
public class RenderMpegSimple implements Render {

	private final Item item;
	private final Response response;

	public RenderMpegSimple(Response response, Item item) throws IOException {
		this.response = response;
		this.item = item;
	}

	@Override
	public void send() throws IOException {
		response.type("audio/mpeg");
		RenderMpeg.icyHeaders(response, item);
		try (InputStream in = item.getInputStream(); OutputStream out = response.raw().getOutputStream()) {
			RenderPlain.pump(in, out);
		}
	}	
}
