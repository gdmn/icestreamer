package pl.devsite.icestreamer.systemtools;

import pl.devsite.icestreamer.Tags;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.icestreamer.Item;

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
		logger.log(Level.INFO, "getting tags of {0}", item);
		Tags tags = new Tags(Soxi.getTags(item.toString()));
		
		return tags;
	}
	
}
