package pl.devsite.icestreamer.render;

import java.io.File;

import lombok.extern.slf4j.Slf4j;
import pl.devsite.icestreamer.item.Item;
import pl.devsite.icestreamer.item.FileItem;
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
public class RenderPlain implements Render {

	private final Item item;
	private final Response response;

	public RenderPlain(Response response, Item item) throws IOException {
		this.response = response;
		this.item = item;
	}

	public static void pump(InputStream in, OutputStream out) throws IOException {
		int n;
		byte[] fileBuffer = new byte[CHUNK_SIZE];
		while ((n = in.read(fileBuffer, 0, CHUNK_SIZE)) > 0) {
			out.write(fileBuffer, 0, n);
		}
	}

	@Override
	public void send() throws IOException {
		log.info("Requested {}", item.toString());
		response.type("application/octet-stream");
		if (item instanceof FileItem) {
			FileItem fileItem = (FileItem) item;
			String filePath = fileItem.getPath();
			if (filePath.toLowerCase().endsWith(".ogg")) {
				response.type("application/ogg");
			}
			if (filePath.toLowerCase().endsWith(".mp3")) {
				response.type("audio/mpeg");
			}
			String name = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
			response.header("Content-Disposition", "inline; filename=\"" + name + "\"");
		}
		try (InputStream in = item.getInputStream(); OutputStream out = response.raw().getOutputStream()) {
			pump(in, out);
		}
	}

}
