package pl.devsite.icestreamer.render;

import pl.devsite.icestreamer.item.Item;
import pl.devsite.icestreamer.item.FileItem;
import java.io.IOException;
import spark.Response;

/**
 *
 * @author dmn
 */
public class RenderFactory {

	public static Render create(String userAgent, Response response, Item item) throws IOException {
		if (item instanceof FileItem) {
			FileItem fileItem = (FileItem) item;
			if (fileItem.getName().toLowerCase().endsWith(".ogg")) {
				return new RenderOgg(response, item);
			}
			if (fileItem.getName().toLowerCase().endsWith(".mp3")) {
				if (userAgent != null && userAgent.startsWith("Music Player Daemon ")) {
					return new RenderMpegSimple(response, item);
				} else {
					return new RenderMpeg(response, item);
				}
			}
		}
		return new RenderPlain(response, item);
	}
}
