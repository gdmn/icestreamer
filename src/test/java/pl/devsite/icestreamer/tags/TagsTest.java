package pl.devsite.icestreamer.tags;

import pl.devsite.icestreamer.tags.Tags;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

/**
 *
 * @author dmn
 */
public class TagsTest {

	private HTreeMap<Integer, Tags> tagsMap;
	private DB db;

	public TagsTest() {
	}

	@BeforeClass
	public static void setUpClass() {
	}

	@AfterClass
	public static void tearDownClass() {
	}

	@Before
	public void setUp() {
		db = DBMaker.memoryDB()
				.transactionDisable()
				.asyncWriteEnable()
//				.executorEnable()
				.make();

		tagsMap = db.hashMapCreate("tags")
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(new Tags.CustomSerializer())
				.makeOrGet();
	}

	@After
	public void tearDown() {
		db.close();
	}

	private Tags exampleTags() {
		Map<String, String> map = new HashMap<>();
		String id = "ID" + (new Random()).nextInt();
		for (int i = 0; i < (new Random()).nextInt(5)+4; i++) {
			StringBuilder value = new StringBuilder();
			for (int j = 0; j < 10; j++) {
				value.append("value " + i + ", ");
			}
			map.put(id + ", key " + i, value.toString());
		}
		Tags result = new Tags(map);
		return result;
	}

	@Test
	public void testSerialization() {
		Tags[] tagArray = new Tags[10];
		for (int i = 0; i < tagArray.length; i++) {
			tagArray[i] = exampleTags();
			assertEquals("for i=" + i + "\n"
					+ "0: " + tagArray[0] + "\n"
					+ i + ": " + tagArray[i], i == 0, tagArray[i].equals(tagArray[0]));
		}
		for (int i = 0; i < tagArray.length; i++) {
			tagsMap.put(i, tagArray[i]);
		}
		db.commit();

		for (int i = 0; i < tagArray.length; i++) {
			Tags fromDb = tagsMap.get(i);
			assertEquals(tagArray[i], fromDb);
		}
	}

}
