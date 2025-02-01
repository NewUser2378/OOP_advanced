package info.kgeorgiy.ja.kupriyanov.iterative;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.function.Function;
import java.util.ArrayList;
import java.util.List;

/**
 * ParallelMapperImpl implements the ParallelMapper interface.
 */
public class ParallelMapperImpl implements ParallelMapper {

    private final Task solver;

    /**
     * Constructs a ParallelMapperImpl object with the specified number of threads.
     *
     * @param numberOfThreads The number of threads to be used for parallel mapping.
     */
    public ParallelMapperImpl(int numberOfThreads) {
        this.solver = new Task(numberOfThreads);
    }

    /**
     * Maps the given function over the elements of the provided list in parallel.
     *
     * @param function The function to be applied to each element of the list.
     * @param list     The list of elements to be mapped.
     * @param <T>      The type of elements in the input list.
     * @param <R>      The type of elements in the resulting list.
     * @return A list containing the results of applying the function to each element of the input list.
     * @throws InterruptedException If any thread is interrupted during execution.
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list)
            throws InterruptedException {
        List<Synchronizer<R>> threads = new ArrayList<>(list.size());
        List<R> result = new ArrayList<>(list.size());

        for (T item : list) {
            Synchronizer<R> thread = new Synchronizer<>();
            threads.add(thread);
            solver.execute(() -> {
                try {
                    R mappedResult = function.apply(item);
                    thread.set(mappedResult);
                } catch (Exception e) {
                    thread.setException(e);
                }
            });
        }

        for (Synchronizer<R> thread : threads) {
            result.add(thread.get());
        }
        return result;
    }

    /**
     * Shuts down the parallel mapper, terminating all threads.
     */
    @Override
    public void close() {
        solver.shutdown();
    }
}

/**
 * a class for synchronizing access to a result from multiple threads.
 *
 * @param <T> The type of the result to be synchronized.
 */
class Synchronizer<T> {
    private T result;
    private boolean isDone = false;

    /**
     * Sets the result and notifies waiting threads.
     *
     * @param result The result to be set.
     */
    synchronized void set(T result) {
        this.result = result;
        isDone = true;
        notifyAll();
    }

    /**
     * Sets an exception and notifies waiting threads.
     *
     * @param exception The exception to be set.
     */
    synchronized void setException(Exception exception) {
        isDone = true;
        notifyAll();
    }

    /**
     * Waits until the result is available, then returns it.
     *
     * @return The result.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    synchronized T get() throws InterruptedException {
        while (!isDone) {
            wait();
        }
        return result;
    }
}

/**
 * Task is a class representing a pool of worker threads.
 * Represents a task manager that handles execution of Runnable tasks by a pool of threads.
 */
class Task {
    private final List<Thread> threads = new ArrayList<>();
    private final List<Runnable> queue = new ArrayList<>();
    private boolean flag = false;

    /**
     * Constructs a Task object with the specified number of threads.
     *
     * @param numberOfThreads The number of threads to be created for task execution.
     */
    Task(int numberOfThreads) {
        for (int i = 0; i < numberOfThreads; i++) {
            Thread thread = new Thread(new Worker());
            thread.start();
            threads.add(thread);
        }
    }

    /**
     * Executes a runnable task. The task will be queued for execution by one of the threads.
     *
     * @param task The runnable task to be executed.
     */
    synchronized void execute(Runnable task) {
        if (!flag) {
            synchronized (queue) {
                queue.add(task);
                queue.notify();
            }
        }
    }

    /**
     * Shuts down all threads managed by this task manager.
     * Interrupts all threads to stop ongoing tasks.
     */
    synchronized void shutdown() {
        flag = true;
        for (Thread thread : threads) {
            thread.interrupt();
        }
    }

    /**
     * Private class representing to execute tasks from a queue.
     * This class implements the {@link Runnable} interface, allowing to execute in a separate thread.
     */
    private class Worker implements Runnable {

        /**
         * method that retrieves tasks from the queue and executes until the flag is set to true and the queue is empty.
         */
        @Override
        public void run() {
            while (true) {
                Runnable task;
                synchronized (queue) {
                    while (queue.isEmpty() && !flag) {
                        try {
                            queue.wait();
                        } catch (InterruptedException ignored) {
                        }

                    }
                    if (flag && queue.isEmpty()) {
                        return;
                    }
                    task = queue.remove(0);
                }
                try {
                    task.run();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
