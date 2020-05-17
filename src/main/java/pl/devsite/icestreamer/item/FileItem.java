package pl.devsite.icestreamer.item;

import lombok.extern.slf4j.Slf4j;
import pl.devsite.icestreamer.tags.Tags;
import pl.devsite.icestreamer.tags.TagsService;
import com.google.gson.annotations.SerializedName;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.logging.Level;


@Slf4j
public class FileItem implements Item {

	@SerializedName("name")
	String canonicalPath;
	@SerializedName("hashcode")
	String hashcodeSerialized;

	private static TagsService tagsService = TagsService.getInstance();

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 97 * hash + Objects.hashCode(this.canonicalPath);
		hash = 97 * hash + Objects.hashCode(this.getClass().getCanonicalName());
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final FileItem other = (FileItem) obj;
		return Objects.equals(this.canonicalPath, other.canonicalPath);
	}

	@Override
	public String toString() {
		return canonicalPath;
	}

	public FileItem(File file) {
		try {
			this.canonicalPath = file.getCanonicalPath();
			this.hashcodeSerialized = "h" + Integer.toHexString(this.hashCode());
		} catch (IOException ex) {
			log.error("", ex);
		}
	}

	public FileItem(String canonicalPath) {
		this.canonicalPath = canonicalPath;
		this.hashcodeSerialized = "h" + Integer.toHexString(this.hashCode());
	}

	@Override
	public InputStream getInputStream() throws IOException {
		try {
			return new BufferedInputStream(new FileInputStream(new File(canonicalPath)));
		} catch (FileNotFoundException ex) {
			throw new IOException(ex);
		}
	}

	@Override
	public String getName() {
		return TagsService.getInstance().getTags(this).getArtistAndTitle();
	}

	public String getPath() {
		return canonicalPath;
	}

	private String dirName() {
		if (canonicalPath != null) {
			return canonicalPath.substring(0, canonicalPath.lastIndexOf(File.separator));
		}
		return null;
	}

	@Override
	public int compareTo(Item o) {
		if (o == null) {
			return 1;
		}
		if (o instanceof FileItem) {
			FileItem other = (FileItem) o;
			return this.canonicalPath.compareTo(other.canonicalPath);
		}
		return this.toString().compareTo(o.toString());
	}

	@Override
	public boolean exists() {
		return new File(canonicalPath).exists();
	}

	@Override
	public Tags getTags() {
		return tagsService.getTags(this);
	}

}
