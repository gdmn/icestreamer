package pl.devsite.icestreamer.systemtools;

import lombok.extern.slf4j.Slf4j;
import pl.devsite.icestreamer.tags.Tags;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import pl.devsite.icestreamer.item.Item;

/**
 *
 * @author dmn
 */
@Slf4j
public class SoxiCallable implements Callable<Tags> {
	
	private final Item item;

	public SoxiCallable(Item item) {
		this.item = item;
	}	

	@Override
	public Tags call() throws Exception {
		log.info("getting tags of {} using soxi", item);
		Tags tags = new Tags(Soxi.getTags(item.toString()));
		log.trace("tags:\n{}", tags);
		
		return tags;
	}
	
}
