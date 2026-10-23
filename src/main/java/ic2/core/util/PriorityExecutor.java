package ic2.core.util;

import java.util.Collection;
import java.util.concurrent.*;
import java.util.List;

public class PriorityExecutor {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void execute(Runnable task) { executor.execute(task); }
    public void executeAll(Collection<Runnable> tasks) { for(Runnable r: tasks) executor.execute(r); }
    public <T> Future<T> submit(Callable<T> task) { return executor.submit(task); }
    public <T> RunnableFuture<T> makeTask(Callable<T> callable) {
        FutureTask<T> task = new FutureTask<>(callable);
        return new RunnableFuture<T>() {
            @Override public void run() { task.run(); }
            @Override public boolean cancel(boolean mayInterruptIfRunning) { return task.cancel(mayInterruptIfRunning); }
            @Override public boolean isCancelled() { return task.isCancelled(); }
            @Override public boolean isDone() { return task.isDone(); }
            @Override public T get() throws InterruptedException, ExecutionException { return task.get(); }
            @Override public T get(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException { return task.get(timeout, unit); }
        };
    }
}
