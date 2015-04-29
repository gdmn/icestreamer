package pl.devsite.icestreamer;

import com.google.gson.annotations.SerializedName;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FileItem implements Item {

	@SerializedName("name")
	String canonicalPath;

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
			//this.canonicalPath = "file:/"+file.getCanonicalPath();
			this.canonicalPath = file.getCanonicalPath();
		} catch (IOException ex) {
			Logger.getLogger(FileItem.class.getName()).log(Level.SEVERE, null, ex);
		}
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
		return fileName();
	}

	private String fileName() {
		if (canonicalPath != null) {
			return canonicalPath.substring(canonicalPath.lastIndexOf('/') + 1);
		}
		return null;
	}

	private String dirName() {
		if (canonicalPath != null) {
			return canonicalPath.substring(0, canonicalPath.lastIndexOf('/'));
		}
		return null;
	}

	@Override
	public boolean matches(Pattern pattern) {
		if (canonicalPath != null) {
			Matcher m = pattern.matcher(canonicalPath);
			return m.matches();
		}
		return false;
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

}
