package pl.devsite.icestreamer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class Items {

	Map<Integer, Item> items = new HashMap<>();
	ItemFactory factory = new ItemFactory();
	List<Item> fed = new ArrayList<>();

	public void feed(String... things) {
		for (String thing : things) {
			Item i = factory.create(thing);
			if (i != null) {
				fed.add(i);
				items.put(i.hashCode(), i);
			}
		}
	}

	public void feed(Item... things) {
		for (Item thing : things) {
			fed.add(thing);
			items.put(thing.hashCode(), thing);
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

	public void clean() {
		Map<Integer, Item> map = items.entrySet()
				.parallelStream()
				.filter(e -> e.getValue().exists())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		items = map;
	}

}
