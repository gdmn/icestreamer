package pl.devsite.icestreamer.tags;

import java.io.File;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import pl.devsite.icestreamer.item.Item;
import pl.devsite.icestreamer.item.ItemFactory;
import pl.devsite.icestreamer.systemtools.SoxiExecutor;
import pl.devsite.icestreamer.systemtools.SoxiTimestampCheckExecutor;

/**
 *
 * @author dmn
 */
public class TagsService {

	private final HTreeMap<Integer, Tags> tagsMap;
	private final DB db;
	private static TagsService instance;
	private static final Logger logger = Logger.getLogger(TagsService.class.getName());

	public static TagsService getInstance() {
		if (instance == null) {
			throw new RuntimeException("Not initialized");
		}
		return instance;
	}

	public static void initialize(String defaultDatabase) {
		if (instance != null) {
			throw new RuntimeException("Already initialized");
		}
		instance = new TagsService(defaultDatabase);
	}

	private TagsService(String defaultDatabase) {
		db = DBMaker.fileDB(new File(defaultDatabase))
				.transactionDisable()
				.asyncWriteEnable()
				.executorEnable()
				.closeOnJvmShutdown()
				.make();

		tagsMap = db.hashMapCreate("tags")
				.keySerializer(Serializer.INTEGER)
				.valueSerializer(new Tags.CustomSerializer())
				.makeOrGet();
	}

	public Tags getTags(Item item) {
		logger.log(Level.INFO, "searching for {0}, keySet contains? {1}", new Object[]{item, Boolean.toString(tagsMap.keySet().contains(item.hashCode()))});
		Tags tags = tagsMap.get(item.hashCode());
		try {
			boolean needsUpdate = tags == null;// || SoxiTimestampCheckExecutor.getInstance().submit(item, tags).get();
			if (!needsUpdate) {
				logger.log(Level.FINEST, "found tags in db for {0}", item);
				logger.log(Level.FINEST, "tags:\n{0}", tags);
				return tags;
			} else {
				Future<Tags> futureTags = SoxiExecutor.getInstance().submit(item);

				tags = futureTags.get();
				tagsMap.put(item.hashCode(), tags);
				db.commit();
				return tags;
			}
		} catch (InterruptedException | ExecutionException ex) {
			Logger.getLogger(TagsService.class.getName()).log(Level.SEVERE, null, ex);
		}

		return null;
	}

	public Collection<Tags> values() {
		return tagsMap.values();
	}

	public void clean() {
		ItemFactory itemFactory = new ItemFactory();
		Stream<Entry<Integer, Tags>> notExisting = tagsMap.entrySet()
				.parallelStream()
				.filter(e -> e.getValue().get("path") != null)
				.filter(e -> {
					Tags tags = e.getValue();
					Item item = itemFactory.create(tags.get("path"));
					if (item == null) {
						logger.log(Level.WARNING, "Item is null for {0}", e.getValue());
					}
					return item == null || !item.exists();
				});

		notExisting.forEach(e -> {
			logger.log(Level.INFO, "Removing from tags database {0}", e.getValue());
			tagsMap.remove(e.getKey());
		});

		db.commit();

		Stream<Entry<Integer, Tags>> updateNeeded = tagsMap.entrySet()
				.parallelStream()
				.filter(e -> e.getValue().get("path") != null)
				.filter(e -> {
					Tags tags = e.getValue();
					Item item = itemFactory.create(tags.get("path"));
					boolean needsUpdate = false;
					try {
						needsUpdate = SoxiTimestampCheckExecutor.getInstance().submit(item, tags).get();
					} catch (InterruptedException | ExecutionException ex) {
					}
					return needsUpdate;
				});

		updateNeeded.forEach(e -> {
			Tags tags = e.getValue();
			Item item = itemFactory.create(tags.get("path"));
			Future<Tags> futureTags = SoxiExecutor.getInstance().submit(item);
			try {
				tags = futureTags.get();
				tagsMap.put(item.hashCode(), tags);
				db.commit();
			} catch (InterruptedException | ExecutionException ex) {
			}
		});

		db.commit();
	}

	public void clear() {
		tagsMap.clear();
		db.commit();
	}

}
