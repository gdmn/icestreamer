package pl.devsite.icestreamer.tags;

import com.google.gson.annotations.SerializedName;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mapdb.Serializer;

/**
 *
 * @author dmn
 */
public class Tags extends HashMap<String, String> implements Comparable<Tags> {
	public final static String PATH = "path";

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

	public static class CustomSerializer extends Serializer<Tags> implements Serializable {

		@Override
		public void serialize(DataOutput out, Tags value) throws IOException {
			Set<Entry<String, String>> entrySet = value.entrySet();

			out.writeUTF(Integer.toString(entrySet.size()));
			for (Entry<String, String> entry : entrySet) {
				out.writeUTF(entry.getKey());
				out.writeUTF(entry.getValue());
			}
		}

		@Override
		public Tags deserialize(DataInput in, int available) throws IOException {

			Tags result = new Tags();

			String countString = in.readUTF();
//			System.out.println("> countString " + countString);

			for (int i = 0; i < Integer.parseInt(countString); i++) {
				String k = in.readUTF();
//				System.out.println("> " + k);
				String v = in.readUTF();
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
	
	public String getPath() {
		return get(PATH);
	}
}
