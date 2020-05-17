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

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class TagsService {

	private final HTreeMap<Integer, Tags> tagsMap;
	private final HTreeMap<String, List<Tags>> searchCache;
	private final DB db, dbMemory;
	private static TagsService instance;

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
					.executorEnable()
        .transactionEnable()
					.fileMmapEnableIfSupported()
					.closeOnJvmShutdown()
					.make();

			tagsMap = db.hashMap("tags")
					.keySerializer(Serializer.INTEGER)
					.valueSerializer(new Tags.CustomSerializer())
					.createOrOpen();

			dbMemory = DBMaker
					.memoryDB()
					.make();

			searchCache = dbMemory
					.<String, List<Tags>>hashMap("searchCache")
					.expireAfterGet(10, TimeUnit.MINUTES)
					.expireAfterCreate(10, TimeUnit.MINUTES)
    			    .expireMaxSize(128)
					.keySerializer(Serializer.STRING)
					.valueSerializer(new Tags.TagsListSerializer())
					.<String, List<Tags>>create();
		} catch (Exception e) {
			throw new RuntimeException("Initialization problem", e);
		}
	}

	public Tags getTags(Item item) {
		log.info("searching for {}, keySet contains? {}", new Object[]{item, Boolean.toString(tagsMap.keySet().contains(item.hashCode()))});
		Tags tags = tagsMap.get(item.hashCode());
		try {
			boolean needsUpdate = tags == null;// || SoxiTimestampCheckExecutor.getInstance().submit(item, tags).get();
			if (!needsUpdate) {
				log.trace("found tags in db for {}", item);
				log.trace("tags:\n{}", tags);
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
			log.error("", ex);
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
		log.info("cleaning phase 1/7");
		ItemFactory itemFactory = new ItemFactory();
		Stream<Entry<Integer, Tags>> notExisting = tagsMap.entrySet()
				.parallelStream()
				.filter(e -> e.getValue().getPath() != null)
				.filter(e -> {
					Tags tags = e.getValue();
					Item item = itemFactory.create(tags.getPath());
					if (item == null) {
						log.warn("Item is null for {}", e.getValue());
					}
					return item == null || !item.exists();
				});

		log.info("cleaning phase 2/7");
		notExisting.forEach(e -> {
			log.info("Removing from tags database {}", e.getValue());
			tagsMap.remove(e.getKey());
		});

		log.info("cleaning phase 3/7");
		db.commit();

		log.info("cleaning phase 4/7");
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

		log.info("cleaning phase 5/7");
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

		log.info("cleaning phase 6/7");
		db.commit();
		db.commit();

		log.info("cleaning phase 7/7");		
		searchCache.clear();
		dbMemory.commit();

		log.info("cleaning finished");
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
	
	public int searchCacheSize() {
		return searchCache.size();
	}

	public List<Tags> filterAndSort(String regex) {
		if (regex == null || regex.isEmpty()) {
			return Collections.emptyList();
		}

		List<Tags> fromCache = searchCache.get(regex);
		if (fromCache == null) {
			log.debug("not found in search cache {}", regex);

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
			log.debug("found in search cache {}", regex);
			return fromCache;
		}
	}
}
