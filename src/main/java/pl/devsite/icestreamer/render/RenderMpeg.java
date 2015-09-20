package pl.devsite.icestreamer.render;

import pl.devsite.icestreamer.item.Item;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import spark.Response;

/**
 *
 * @author dmn
 */
public class RenderMpeg implements Render {

	private static final Logger logger = Logger.getLogger(RenderMpeg.class.getName());
	private final Item item;
	private final Response response;

	public RenderMpeg(Response response, Item item) throws IOException {
		this.response = response;
		this.item = item;
	}

	private void sendIcyStreamTitle(String title, int chunkSize, OutputStream responseStream) throws IOException {
		byte[] fileBuffer = new byte[chunkSize];
		Arrays.fill(fileBuffer, (byte) 0);
		String s = "StreamTitle='" + title + "';";
		int length = 1 + s.length() / 16;
		fileBuffer[0] = (byte) length;
		for (int i = 0; i < s.length(); i++) {
			fileBuffer[i + 1] = (byte) s.charAt(i);
		}
		responseStream.write(fileBuffer, 0, length * 16 + 1);
	}
	
	public static void icyHeaders(Response response, Item item) {
		response.header("Cache-Control", "no-cache");
		response.header("icy-notice1", "<BR>This stream requires <a href=\"http://www.icecast.org/3rdparty.php\">a media player that support Icecast</a><BR>");
		response.header("icy-notice2", "iceserver");
		response.header("Server", "iceserver");
		response.header("icy-name", item.getName());
		response.header("icy-description", item.toString());
		response.header("icy-genre", "various");
		response.header("icy-pub", "1");
		//add("icy-url", value);		
	}

	@Override
	public void send() throws IOException {
		logger.log(Level.INFO, "Requested {0}", item.toString());
		response.type("audio/mpeg");
		response.header("icy-metaint", "" + CHUNK_SIZE);
		
		icyHeaders(response, item);
		
		int n;
		boolean sendHeader = false;

		try (InputStream in = item.getInputStream(); OutputStream out = response.raw().getOutputStream()) {
			byte[] fileBuffer = new byte[CHUNK_SIZE];
			int lastWrote = 0;
			while ((n = in.read(fileBuffer, 0, lastWrote < CHUNK_SIZE ? CHUNK_SIZE - lastWrote : CHUNK_SIZE)) > 0) {
				out.write(fileBuffer, 0, n);
				lastWrote += n;
				while (lastWrote >= CHUNK_SIZE) {
					lastWrote -= CHUNK_SIZE;
				}
				if (lastWrote == 0) {
					if (sendHeader) {
						out.write(0);
					} else {
						sendHeader = true;
						sendIcyStreamTitle(item.getName(), CHUNK_SIZE, out);
					}
				} else {
				}
			}
			Arrays.fill(fileBuffer, (byte) 0);
			out.write(fileBuffer, 0, CHUNK_SIZE - lastWrote);
			out.write(0);
		}
	}

}
