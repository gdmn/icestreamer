package pl.devsite.icestreamer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Items {

	Map<Integer, Item> items = new HashMap<>();
	ItemFactory factory = new ItemFactory();
	List<Item> fed = new ArrayList<>();

	public void feed(String... things) {
		for (String thing : things) {
			Item i = factory.create(thing);
			fed.add(i);
			items.put(i.hashCode(), i);
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

}
