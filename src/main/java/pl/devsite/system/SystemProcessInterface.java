package pl.devsite.system;

/**
 *
 * @author dmn
 */
public interface SystemProcessInterface<T> {
	void execute(String options, SystemProcessCallback<T> callback);
}
