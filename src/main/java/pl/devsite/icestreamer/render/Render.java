package pl.devsite.icestreamer.render;

import java.io.IOException;

/**
 *
 * @author dmn
 */
public interface Render {

	static final int CHUNK_SIZE = 1024 * 16;

	void send() throws IOException;
}
