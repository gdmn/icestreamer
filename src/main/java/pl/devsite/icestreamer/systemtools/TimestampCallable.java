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
public class TimestampCallable implements Callable<Boolean> {

	private static final Logger logger = Logger.getLogger(TimestampCallable.class.getName());

	private final Item item;
	private final Tags tags;

	public TimestampCallable(Item item, Tags tags) {
		this.item = item;
		this.tags = tags;
	}

	@Override
	public Boolean call() throws Exception {
		Boolean result = Soxi.needsUpdate(item.toString(), tags.get("icestreamer-update"));
		logger.log(Level.INFO, "checking timestamp of {0}, needs update: {1}", new Object[]{item, result});
		return result;
	}

}
