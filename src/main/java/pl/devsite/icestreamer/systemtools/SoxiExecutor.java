package pl.devsite.icestreamer.systemtools;

import pl.devsite.icestreamer.Tags;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import pl.devsite.icestreamer.Item;

/**
 *
 * @author dmn
 */
public class SoxiExecutor {
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private static final SoxiExecutor instance = new SoxiExecutor();
	
	private SoxiExecutor() {}
	
	public static SoxiExecutor getInstance() {
		return instance;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	private Future<Tags> submit(Callable<Tags> task) {
		return executorService.submit(task);
	}

	public Future<Tags> submit(Item item) {
		return this.submit(new SoxiCallable(item));
	}

}
