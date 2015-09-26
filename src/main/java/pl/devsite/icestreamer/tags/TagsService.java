package pl.devsite.icestreamer.tags;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
	private final HTreeMap<String, List<Tags>> searchCache;
	private final DB db, dbMemory;
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
		try {
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

			dbMemory = DBMaker
					.memoryDB()
					.make();

			searchCache = dbMemory
					.hashMapCreate("searchCache")
					.expireAfterAccess(10, TimeUnit.MINUTES)
					.expireAfterWrite(10, TimeUnit.MINUTES)
					.executorEnable()
					.make();
		} catch (Exception e) {
			throw new RuntimeException("Initialization problem", e);
		}
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
				tags.put(Tags.HHASHCODE, "h" + Integer.toHexString(item.hashCode()));
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

	public Stream<Tags> stream() {
		return tagsMap.values().parallelStream();
	}

	public void clean() {
		logger.info("cleaning phase 1/7");
		ItemFactory itemFactory = new ItemFactory();
		Stream<Entry<Integer, Tags>> notExisting = tagsMap.entrySet()
				.parallelStream()
				.filter(e -> e.getValue().getPath() != null)
				.filter(e -> {
					Tags tags = e.getValue();
					Item item = itemFactory.create(tags.getPath());
					if (item == null) {
						logger.log(Level.WARNING, "Item is null for {0}", e.getValue());
					}
					return item == null || !item.exists();
				});

		logger.info("cleaning phase 2/7");
		notExisting.forEach(e -> {
			logger.log(Level.INFO, "Removing from tags database {0}", e.getValue());
			tagsMap.remove(e.getKey());
		});

		logger.info("cleaning phase 3/7");
		db.commit();

		logger.info("cleaning phase 4/7");
		Stream<Entry<Integer, Tags>> updateNeeded = tagsMap.entrySet()
				.parallelStream()
				.filter(e -> e.getValue().getPath() != null)
				.filter(e -> {
					Tags tags = e.getValue();
					Item item = itemFactory.create(tags.getPath());
					boolean needsUpdate = false;
					try {
						needsUpdate = SoxiTimestampCheckExecutor.getInstance().submit(item, tags).get();
					} catch (InterruptedException | ExecutionException ex) {
					}
					return needsUpdate;
				});

		logger.info("cleaning phase 5/7");
		updateNeeded.forEach(e -> {
			Tags tags = e.getValue();
			Item item = itemFactory.create(tags.getPath());
			Future<Tags> futureTags = SoxiExecutor.getInstance().submit(item);
			try {
				tags = futureTags.get();
				tags.put(Tags.HHASHCODE, "h" + Integer.toHexString(item.hashCode()));
				tagsMap.put(item.hashCode(), tags);
			} catch (InterruptedException | ExecutionException ex) {
			}
		});

		logger.info("cleaning phase 6/7");
		db.commit();
		db.compact();
		db.commit();
		db.getEngine().clearCache();
		
		logger.info("cleaning phase 7/7");		
		searchCache.clear();
		dbMemory.compact();
		dbMemory.commit();
		dbMemory.getEngine().clearCache();

		logger.info("cleaning finished");
	}

	public void clear() {
		tagsMap.clear();
		db.commit();
	}

	public Tags get(Integer key) {
		return tagsMap.get(key);
	}

	public int size() {
		return tagsMap.size();
	}

	public List<Tags> filterAndSort(String regex) {
		if (regex == null || regex.isEmpty()) {
			return Collections.emptyList();
		}

		List<Tags> fromCache = searchCache.get(regex);
		if (fromCache == null) {
			logger.log(Level.FINE, "not found in search cache {0}", regex);

			Stream<Tags> input = this.stream();
			Pattern pattern = Pattern.compile(regex);

			List<Tags> tagsList = input
					.filter(tags -> {
						return tags.matches(pattern);
					})
					.sorted()
					.collect(Collectors.toList());

			searchCache.put(regex, tagsList);
			return tagsList;
		} else {
			logger.log(Level.FINE, "found in search cache {0}", regex);
			return fromCache;
		}
	}
}
