package pl.devsite.icestreamer;

import java.io.File;

class ItemFactory {

	public Item create(String something) {
		File t = new File(something);
		if (t.exists() && t.canRead() && t.isFile()) {
			return new FileItem(t);
		}
		return null;
	}
}
