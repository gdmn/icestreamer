package pl.devsite.icestreamer.systemtools;

import pl.devsite.icestreamer.tags.Tags;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import pl.devsite.icestreamer.item.Item;

/**
 *
 * @author dmn
 */
public class SoxiTimestampCheckExecutor {
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private static final SoxiTimestampCheckExecutor instance = new SoxiTimestampCheckExecutor();
	
	private SoxiTimestampCheckExecutor() {}
	
	public static SoxiTimestampCheckExecutor getInstance() {
		return instance;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public Future<Boolean> submit(Callable<Boolean> task) {
		return executorService.submit(task);
	}

	public Future<Boolean> submit(Item item, Tags tags) {
		return this.submit(new TimestampCallable(item, tags));
	}

}
