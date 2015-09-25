package pl.devsite.icestreamer.item;

import pl.devsite.icestreamer.tags.TagsService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import pl.devsite.icestreamer.tags.Tags;

public class Items {

	private ItemFactory factory = new ItemFactory();

	public List<Item> feed(String... things) {
		final List<Item> fed = new ArrayList<>();
		Item[] mapped = Arrays.stream(things).parallel()
				.map(thing -> factory.create(thing))
				.filter(thing -> thing != null)
				.toArray(Item[]::new);
		Arrays.stream(mapped)
				.forEach(i -> {
					fed.add(i);
					refreshTags(i);
				});
		return fed;
	}

	public List<Item> feed(Item... things) {
		final List<Item> fed = new ArrayList<>();
		for (Item thing : things) {
			fed.add(thing);
			refreshTags(thing);
		}
		return fed;
	}

	public int size() {
		return TagsService.getInstance().size();
	}

	public Item get(Integer key) {
		Tags tags = TagsService.getInstance().get(key);
		if (tags == null) {
			return null;
		}
		return factory.create(tags.getPath());
	}

	protected void refreshTags(Item i) {
		TagsService.getInstance().getTags(i);
	}

}
