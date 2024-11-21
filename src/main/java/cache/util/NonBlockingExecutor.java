package cache.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 *
 * @author Manjunath Kustagi
 */
public class NonBlockingExecutor {
    private final ExecutorService executor;
 
    public NonBlockingExecutor(ExecutorService executor) {
        this.executor = executor;
    }
 
    public <R> NonBlockingFuture<R> submitNonBlocking(final Callable<R> userTask) {
        final NonBlockingFuture<R> nbFuture = new NonBlockingFuture<>();
        executor.submit(() -> {
            try {
                R result = userTask.call();
                nbFuture.setResult(result);
                return result;
            } catch (Exception e) {
                nbFuture.setFailure(e);
                throw e;
            }
        });
 
        return nbFuture;
    }
}
