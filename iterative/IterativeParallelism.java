package info.kgeorgiy.ja.kupriyanov.iterative;
import info.kgeorgiy.java.advanced.iterative.NewScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;


public class IterativeParallelism implements NewScalarIP {
    private final ParallelMapper mapper;

    /**
     * Initializes IterativeParallelism class with no arguments.
     */
    public IterativeParallelism() {
        this.mapper = null;
    }

    /**
     * Initializes IterativeParallelism class with a ParallelMapper object as an argument.
     *
     * @param mapper A ParallelMapper object to be assigned
     */
    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }


    /**
     * Performs a base parallel computation operation on the list if we have not null mapper
     *
     * @param counter the number of threads to use
     * @param list        the list to operate on
     * @param function    the operation to make on each sublist
     * @param getter      the operation to get the final result from intermediate results
     * @param step        the step size for dividing the list
     * @param <T>         the type of elements in the list
     * @param <R>         the type of result
     * @return the result of the computation
     * @throws InterruptedException if any thread is interrupted
     */

    private <T, R> R baseFunction(int counter,
                                  List<? extends T> list,
                                  Function<Stream<? extends T>, R> function,
                                  Function<Stream<? extends R>, R> getter,
                                  int step) throws InterruptedException {
        if (counter < 1) {
            throw new IllegalArgumentException("Thread count must be at least 1.");
        }

        if (list == null) {
            throw new IllegalArgumentException("List must not be null.");
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("List must not empty.");
        }
        counter = Math.max(1, Math.min(counter, list.size()));
        List<R> results;
        List <Stream<? extends T>> partitions = new ArrayList<>(counter);
        if (mapper!=null) {
            for (int t = 0; t < counter; t++) {
                final int index = t;
                List<T> sublist = new ArrayList<>();
                int i = index * step;
                while (i < list.size()) {
                    sublist.add(list.get(i));
                    i += counter * step;
                }
                partitions.add(sublist.stream());
            }
            results = mapper.map(function, partitions);
        } else {
            results  = new ArrayList<>(Collections.nCopies(counter, null));
            List<Thread> partitions1 = new ArrayList<>();
            for (int t = 0; t < counter; t++) {
                final int index = t;
                int allThreads = counter;
                Thread thread = new Thread(() -> {
                    List<T> sublist = new ArrayList<>();
                    int i = index * step;
                    while(i < list.size()) {
                        sublist.add(list.get(i));
                        i += allThreads * step;
                    }
                    if (!sublist.isEmpty()) {
                        results.set(index, function.apply(sublist.stream()));
                    }
                });
                thread.start();
                partitions1.add(thread);

            }

            for (Thread thread : partitions1) {
                thread.join();
            }
        }

        return getter.apply(results.stream().filter(Objects::nonNull));
    }
    /**
     * Finds the maximum element in the list using parallel computation.
     *
     * @param numberOfThreads          the number of threads to use
     * @param list       the list to find the maximum element from
     * @param comparator the comparator to compare elements
     * @param step       the step size for dividing the list
     * @param <T>        the type of elements in the list
     * @return the maximum element in the list
     * @throws InterruptedException if any thread is interrupted
     */

    public <T> T maximum(int numberOfThreads, List<? extends T> list, Comparator<? super T> comparator, int step) throws InterruptedException {
        return baseFunction(numberOfThreads, list,
                stream -> stream.filter(Objects::nonNull).max(comparator)
                        .orElseThrow(() -> new IllegalArgumentException("List must not be empty")),
                stream -> stream.filter(Objects::nonNull).max(comparator)
                        .orElseThrow(() -> new IllegalArgumentException("List must not be empty")),
                step);
    }
    /**
     * Finds the minimum element in the list using parallel computation.
     *
     * @param numberOfThreads           the number of threads to use
     * @param list       the list to find the minimum element from
     * @param comparator the comparator to compare elements
     * @param step       the step size for dividing the list
     * @param <T>        the type of elements in the list
     * @return the minimum element in the list
     * @throws InterruptedException if any thread is interrupted
     */

    public <T> T minimum(int numberOfThreads, List<? extends T> list, Comparator<? super T> comparator, int step) throws InterruptedException {
        return maximum(numberOfThreads, list, Collections.reverseOrder(comparator), step);
    }

    /**
     * Checks if all elements in the list satisfy the given predicate using parallel computation.
     *
     * @param numberOfThreads         the number of threads to use
     * @param list      the list to check
     * @param predicate the predicate to test elements
     * @param step      the step size for dividing the list
     * @param <T>       the type of elements in the list
     * @return true if all elements satisfy the predicate, false otherwise
     * @throws InterruptedException if any thread is interrupted
     */

    public <T> boolean all(int numberOfThreads, List<? extends T> list, Predicate<? super T> predicate, int step) throws InterruptedException {
        return baseFunction(numberOfThreads, list,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(item -> item),
                step);
    }


    /**
     * Checks if any element in the list satisfies the given predicate using parallel computation.
     *
     * @param numberOfThreads         the number of threads to use
     * @param list      the list to check
     * @param predicate the predicate to test elements
     * @param step      the step size for dividing the list
     * @param <T>       the type of elements in the list
     * @return true if any element satisfies the predicate, false otherwise
     * @throws InterruptedException if any thread is interrupted
     */

    public <T> boolean any(int numberOfThreads, List<? extends T> list, Predicate<? super T> predicate, int step) throws InterruptedException {
        return !all(numberOfThreads, list, predicate.negate(), step);
    }

    /**
     * Counts the number of elements in the list that satisfy the given predicate using parallel computation.
     *
     * @param numberOfThreads        the number of threads to use
     * @param list      the list to count elements from
     * @param predicate the predicate to test elements
     * @param step      the step size for dividing the list
     * @param <T>       the type of elements in the list
     * @return the number of elements satisfying the predicate
     * @throws InterruptedException if any thread is interrupted
     */

    public <T> int count(int numberOfThreads, List<? extends T> list, Predicate<? super T> predicate, int step) throws InterruptedException {
        return baseFunction(numberOfThreads, list,
                stream -> (int) stream.filter(predicate).count(),
                stream -> stream.mapToInt(Integer::intValue).sum(),
                step);
    }
}
