package pl.devsite.icestreamer.tags;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;
import org.mapdb.serializer.GroupSerializer;

/**
 *
 * @author dmn
 */
public class Tags extends HashMap<String, String> implements Comparable<Tags> {

	public final static String PATH = "path";
	public final static String LENGTH = "length";
	public final static String SECONDS = "seconds";
	public final static String ARTIST = "artist";
	public final static String TITLE = "title";
	public final static String HHASHCODE = "hashcode";

	public Tags(Map<? extends String, ? extends String> m) {
		super(m);
	}

	private Tags() {
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		this.entrySet().stream().forEach((item) -> {
			result.append(item.getKey())
					.append(": ")
					.append(item.getValue())
					.append("\n");
		});
		return result.toString();
	}

	@Override
	public int compareTo(Tags o) {
		if (o == null) {
			return 1;
		}
		if (o instanceof Tags) {
			Tags other = (Tags) o;
			return this.get(PATH).compareTo(other.get(PATH));
		}
		return this.toString().compareTo(o.toString());
	}

	public static class CustomSerializer implements Serializer<Tags> {

		@Override
		public void serialize(@NotNull DataOutput2 out, @NotNull Tags value) throws IOException {
			Set<Entry<String, String>> entrySet = value.entrySet();

			out.writeUTF(Integer.toString(entrySet.size()));
			for (Entry<String, String> entry : entrySet) {
				out.writeUTF(entry.getKey());
				out.writeUTF(entry.getValue());
			}
		}

		@Override
		public Tags deserialize(@NotNull DataInput2 input, int available) throws IOException {

			Tags result = new Tags();

			String countString = input.readUTF();
//			System.out.println("> countString " + countString);

			for (int i = 0; i < Integer.parseInt(countString); i++) {
				String k = input.readUTF();
//				System.out.println("> " + k);
				String v = input.readUTF();
//				System.out.println("> " + v);
				result.put(k, v);
			}

			return result;
		}

		@Override
		public int fixedSize() {
			return -1;
		}
	}

	public boolean matches(Pattern pattern) {
		String path = get(PATH);
		if (path != null) {
			Matcher m = pattern.matcher(path);
			return m.matches();
		}
		return false;
	}

	private String getArtist() {
		return get(ARTIST);
	}

	private String getTitle() {
		return get(TITLE);
	}

	public String getPath() {
		return get(PATH);
	}

	public String getLength() {
		return get(LENGTH);
	}
	
	public String getHhashcode() {
		return get(HHASHCODE);
	}
	
	public String getSeconds() {
		return get(SECONDS);
	}

	public String getArtistAndTitle() {
		return getArtist() + " - " + getTitle();
	}
}
