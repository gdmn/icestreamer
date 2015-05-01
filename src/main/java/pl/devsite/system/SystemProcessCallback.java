package pl.devsite.system;

/**
 *
 * @author dmn
 */
public interface SystemProcessCallback<T> {
	void results(T value);
}
