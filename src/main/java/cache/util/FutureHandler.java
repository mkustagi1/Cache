package cache.util;

/**
 *
 * @author Manjunath Kustagi
 */
public interface FutureHandler<R> {
    void onSuccess(R result);
    void onFailure(Throwable e);
}
