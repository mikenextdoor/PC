import java.io.Closeable
import kotlin.time.Duration

/**
 * An _event topic_ to where:
 * - Producing parties can publish events.
 * - Consuming parties can register subscribers. Each subscriber should receive the events published to the topic.
 */
class BoundedEventTopic<T>(
    capacity: Int,
) : Closeable {

    /**
     * Publishes an event to the topic. If capacity is exceeded, then the oldest event is discarded.
     */
    fun publish(event: T): PublishResult {
        TODO("To be implemented")
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
        TODO("To be implemented")
    }

    /**
     * Closes the topic.
     * When a topic is closed, then any `publish` or `read` on a subscription will fail.
     */
    override fun close() {
        TODO("To be implemented")
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