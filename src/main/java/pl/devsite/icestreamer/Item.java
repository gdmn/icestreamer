package pl.devsite.icestreamer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.regex.Pattern;

public interface Item extends Comparable<Item> {

	@Override
	String toString();

	@Override
	int hashCode();

	@Override
	boolean equals(Object obj);
	
	InputStream getInputStream() throws IOException;
	
	String getName();
	
	boolean matches(Pattern pattern);
	
	boolean exists();
	
	Map<String, String> getTags();
}
