/**
 * Unbounded First-In-First-Out (FIFO) queue without blocking operations and
 * a _weakly consistent_ iterator.
 */
class ThreadSafeNonBlockingQueue<T> : Iterable<T> {

    /**
     * Adds an element to the _end_ of the queue.
     */
    fun add(value: T) {
        TODO("To be implemented")
    }

    /**
     * Removes and returns the element in the _front_ of the queue, or returns `null` if the queue is empty.
     */
    fun removeOrNull(): T? {
        TODO("To be implemented")
    }

    /**
     * Returns an iterator to the queue.
     */
    override fun iterator(): WeaklyConsistentIterator<T> {
        TODO("To be implemented")
    }

    /**
     * Iterator to the queue, starting from the front and moving to the end, providing a _weakly consistent_ behavior,
     * as described in
     * [https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/package-summary.html#Weakly](Concurrent Collections):
     * - Iteration MUST not block other operations in the queue.
     * - A call to `next` must succeed if it was proceeded by a call to `hasNext`, which returned `true`.
     * - MAY return an element that was already removed when the `next` method is called.
     * That is, use of the iterator in a `for` statement will never result in an exception.
     * It is assumed that an iterator instance will not be called concurrently from multiple-threads.
     */
    class WeaklyConsistentIterator<T> : Iterator<T> {
        /**
         * Returns `true` if there are still more elements to iterate, `false` otherwise.
         */
        override fun hasNext(): Boolean {
            TODO("To be implemented")
        }

        /**
         * Returns the next element on the iteration.
         * - MUST return an element if the call was preceded with an `hasNext` call that returned `true`.
         * - MAY throw `NoSuchElementException` when not preceded with an `hasNext` call that returned `true`.
         * - MUST not throw `ConcurrentModificationException`.
         */
        override fun next(): T {
            TODO("To be implemented")
        }
    }
}