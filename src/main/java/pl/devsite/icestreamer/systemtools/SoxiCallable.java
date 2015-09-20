package pl.devsite.icestreamer.systemtools;

import pl.devsite.icestreamer.tags.Tags;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.icestreamer.item.Item;

/**
 *
 * @author dmn
 */
public class SoxiCallable implements Callable<Tags> {
	private static final Logger logger = Logger.getLogger(SoxiCallable.class.getName());
	
	private final Item item;

	public SoxiCallable(Item item) {
		this.item = item;
	}	

	@Override
	public Tags call() throws Exception {
		logger.log(Level.FINE, "getting tags of {0} using soxi", item);
		Tags tags = new Tags(Soxi.getTags(item.toString()));
		logger.log(Level.FINEST, "tags:\n{0}", tags);
		
		return tags;
	}
	
}
