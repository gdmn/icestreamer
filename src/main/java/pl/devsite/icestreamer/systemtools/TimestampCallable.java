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
public class TimestampCallable implements Callable<Boolean> {

	private final Item item;
	private final Tags tags;

	public TimestampCallable(Item item, Tags tags) {
		this.item = item;
		this.tags = tags;
	}

	@Override
	public Boolean call() throws Exception {
		Boolean result = Soxi.needsUpdate(item.toString(), tags.get("icestreamer-update"));
		log.debug("checking timestamp of {}, needs update: {}", new Object[]{item, result});
		return result;
	}

}
