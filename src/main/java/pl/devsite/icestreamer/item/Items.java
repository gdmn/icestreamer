package pl.devsite.icestreamer.item;

import pl.devsite.icestreamer.tags.TagsService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Items {

	Map<Integer, Item> items = new HashMap<>();
	ItemFactory factory = new ItemFactory();
	List<Item> fed = new ArrayList<>();

	public void feed(String... things) {
		Item[] mapped = Arrays.stream(things).parallel()
				.map(thing -> factory.create(thing))
				.filter(thing -> thing != null)
				.toArray(Item[]::new);
		Arrays.stream(mapped)
				.forEach(i -> {
					fed.add(i);
					items.put(i.hashCode(), i);
					refreshTags(i);
				});
	}

	public void feed(Item... things) {
		for (Item thing : things) {
			fed.add(thing);
			items.put(thing.hashCode(), thing);
			refreshTags(thing);
		}
	}

	public void merge(Items mergeWith) {
		items.putAll(mergeWith.items);
	}

	public List<Item> getList() {
		return fed;
	}

	public int size() {
		return items.size();
	}

	public Item get(Integer key) {
		return items.get(key);
	}

	public void clear() {
		items.clear();
	}

	protected void refreshTags(Item i) {
		TagsService.getInstance().getTags(i);
	}

	public Map<Integer, Item> getItems() {
		return items;
	}
	
	
}
