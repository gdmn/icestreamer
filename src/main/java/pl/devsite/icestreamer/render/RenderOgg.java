package pl.devsite.icestreamer.render;

import lombok.extern.slf4j.Slf4j;
import pl.devsite.icestreamer.item.Item;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;

import spark.Response;

/**
 *
 * @author dmn
 */
@Slf4j
public class RenderOgg implements Render {

	private final Item item;
	private final Response response;

	public RenderOgg(Response response, Item item) throws IOException {
		this.response = response;
		this.item = item;
	}

	@Override
	public void send() throws IOException {
		log.info("Requested {}", item.toString());
		response.type("application/ogg");
		RenderMpeg.icyHeaders(response, item);
		try (InputStream in = item.getInputStream(); OutputStream out = response.raw().getOutputStream()) {
			RenderPlain.pump(in, out);
		}
	}	
}
