import java.io.Closeable
import java.util.LinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import java.time.Duration

/**
 * An _event topic_ to where:
 * - Producing parties can publish events.
 * - Consuming parties can register subscribers. Each subscriber should receive the events published to the topic.
 */
class BoundedEventTopic<T>(
    private val capacity: Int,
) : Closeable {

    /**
     * Publishes an event to the topic. If capacity is exceeded, then the oldest event is discarded.
     */
    init {
        require(capacity > 0)
    }

    private val mutex: Lock = ReentrantLock()
    private val notEmpty: Condition = mutex.newCondition()
    private val buffer = LinkedList<Pair<Long, T>>()
    private var nextIndex = 0L
    private var closed = false

    /**
     * Publishes an event to the topic. If capacity is exceeded, then the oldest event is discarded.
     */
    fun publish(event: T): PublishResult {
        mutex.withLock {
            if (closed) return PublishResult.Closed

            val idx = nextIndex
            nextIndex += 1

            buffer.addLast(idx to event)
            if (buffer.size > capacity) {
                buffer.removeFirst()
            }
            notEmpty.signalAll()
            return PublishResult.Success
        }
    }

    /**
     * The result of a `publish` call.
     */
    sealed interface PublishResult {
        // Publish was done successfully
        data object Success : PublishResult

        // Publish cannot be done because the topic is closed
        data object Closed : PublishResult
    }

    /**
     * Subscribes to the topic, returning a `Subscription`, or `null` if the topic is closed.
     */
    fun subscribe(): Subscription<T>? {
        mutex.withLock {
            if (closed) return null
            val startIndex = buffer.firstOrNull()?.first ?: nextIndex
            return object: Subscription<T> {
                private var nextToRead = startIndex
                private var isClosed = false

                override fun read(timeout: Duration): Subscription.ReadResult<T> {
                    mutex.withLock {
                        if (closed || isClosed) return Subscription.ReadResult.Closed

                        var remaining = timeout.toNanos()

                        while (true) {
                            if (closed || isClosed) return Subscription.ReadResult.Closed
                            val first = buffer.firstOrNull()

                            if (first != null && nextToRead < first.first) {
                                nextToRead = first.first
                            }
                            val event = buffer.firstOrNull { it.first == nextToRead }
                            if (event != null) {
                                nextToRead++
                                return Subscription.ReadResult.Success(event.second, event.first)
                            }
                            if (remaining <= 0) {
                                return Subscription.ReadResult.Timeout
                            }
                            remaining = notEmpty.awaitNanos(remaining)
                        }
                    }
                }
                override fun close() {
                    mutex.withLock {
                        isClosed = true
                        notEmpty.signalAll()
                    }
                }
            }
        }
    }


    /**
     * Closes the topic.
     * When a topic is closed, then any `publish` or `read` on a subscription will fail.
     */
    override fun close() {
        mutex.withLock {
            closed = true
            notEmpty.signalAll()
        }
    }

    /**
     * Represents a subscription to the topic.
     */
    interface Subscription<T> : Closeable {
        /**
         * Reads the next event from the topic, in the context of the current subscription.
         * MUST support timeout.
         * MUST implement the JVM interrupt protocol.
         */
        @Throws(InterruptedException::class)
        fun read(timeout: Duration): ReadResult<T>

        /**
         * Represents the result of a read on a subscription.
         */
        sealed interface ReadResult<out T> {
            /**
             * Read cannot be done because topic is closed.
             */
            data object Closed : ReadResult<Nothing>

            /**
             * Read was not done because the timeout was exceeded.
             */
            data object Timeout : ReadResult<Nothing>

            /**
             * Read was done successfully and the event was returned.
             */
            data class Success<T>(
                // The read event
                val event: T,
                // Index for the read event
                val startIndex: Long,
            ) : ReadResult<T>
        }
    }
}