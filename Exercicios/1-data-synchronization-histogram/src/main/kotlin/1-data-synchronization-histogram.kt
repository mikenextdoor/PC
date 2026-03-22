import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Incrementally computes a histogram, i.e, incrementally counts the frequency of values.
 */
class Histogram<T> {

    private val mutex: Lock = ReentrantLock()
    private val map = mutableMapOf<T, Long>()

    /**
     * Adds a value to the histogram computation.
     * Returns the current count for that value.
     */
    fun addValue(value: T): Long {
        mutex.withLock {
            val newCount = (map[value] ?: 0L) + 1
            map[value] = newCount
            return newCount
        }
    }

    /**
     * Adds a set of values to the histogram computation.
     */
    fun addValues(value: Collection<T>) {
        mutex.withLock {
            for (i in value) {
                map[i] = (map[i] ?: 0L) + 1
            }
        }
    }

    /**
     * Gets the current count for a given value.
     */
    fun getCountForValue(value: T): Long {
        mutex.withLock {
            return map[value] ?: 0
        }
    }

    /**
     * Gets a snapshot of the current histogram computation.
     */
    fun getSnapshot(): Map<T, Long> {
        mutex.withLock {
            return map.toMap()
        }
    }
}