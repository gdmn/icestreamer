package pl.devsite.icestreamer;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dmn
 */
public class ItemsTest {

	class FakeItem implements Item {

		@Override
		public InputStream getInputStream() throws IOException {
			return null;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public boolean matches(Pattern pattern) {
			return false;
		}

		@Override
		public boolean exists() {
			return false;
		}

		@Override
		public int compareTo(Item o) {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		public Map<String, String> getTags() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}
	};

	/**
	 * Test of clean method, of class Items.
	 */
	@Test
	public void testClean() {
		Item itemNotExisting = new FakeItem() {
			@Override
			public boolean exists() {
				return false;
			}
		};
		Item itemExisting = new FakeItem() {
			@Override
			public boolean exists() {
				return true;
			}
		};
		Items instance = new Items();

		instance.feed(itemExisting, itemNotExisting);
		assertEquals(2, instance.size());

		instance.clean();
		assertEquals(1, instance.size());
	}

}
