import java.io.Closeable
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class ThreadScope(private val name: String, private val threadBuilder: Thread.Builder, private val maxThreads: Int) : Closeable {

    private val mutex: Lock = ReentrantLock()
    private val condition = mutex.newCondition()

    private var closed = false
    private var cancelled = false

    private var aliveThreads = 0

    private var activeThreads = mutableListOf<Thread>()
    private var waitingThreads = ArrayDeque<Thread>()
    private var childScopes = mutableListOf<ThreadScope>()

    // Creates and conditionally starts a new thread in the scope, if the scope is not closed.
    fun newThread(runnable: Runnable): Thread? {
        mutex.withLock {
            if (closed) return null

            val thread = threadBuilder.unstarted{
                try {
                    runnable.run()
                } finally {
                    mutex.withLock {
                        aliveThreads--
                        activeThreads.remove(Thread.currentThread())

                        while (waitingThreads.isNotEmpty() && aliveThreads < maxThreads) {
                            val next = waitingThreads.removeFirst()
                            activeThreads.add(next)
                            aliveThreads++
                            next.start()
                        }
                        condition.signalAll()
                    }
                }
            }

            if (aliveThreads < maxThreads) {
                activeThreads.add(thread)
                aliveThreads++
                thread.start()
            } else {
                waitingThreads.addLast(thread)
            }

            return thread
        }
    }

    // Creates a new child scope, if the current scope is not closed.
    fun newChildScope(name: String): ThreadScope? {
        mutex.withLock {
            if (closed) return null

            val child = ThreadScope(name, threadBuilder, maxThreads)
            childScopes.add(child)

            return child
        }
    }

    // Closes the current scope, disallowing the creation of any further thread
    // or child scope.
    override fun close() {
        mutex.withLock {
            if (closed) return
            closed = true
        }
    }

    // Waits until all threads and child scopes have completed
    @Throws(InterruptedException::class)
    fun join(timeout: Duration): Boolean {
        mutex.withLock {
            val deadline = System.currentTimeMillis() + timeout.inWholeMilliseconds

            while(true) {
                val children = childScopes.toList()
                val childDone = children.all { it.join(Duration.ZERO) }

                if (aliveThreads == 0 && childDone) {
                    return true
                }

                val remaining = deadline - System.currentTimeMillis()
                if (remaining <= 0) {
                    return false
                }

                condition.await(remaining, TimeUnit.MILLISECONDS)
            }
        }
    }

    // Interrupts all threads in the scope and cancels all child scopes.
    fun cancel() {
        val threads: List<Thread>
        val children: List<ThreadScope>

        mutex.withLock {
            if (cancelled) return

            cancelled = true
            closed = true

            threads = activeThreads.toMutableList()
            children = childScopes.toMutableList()

            condition.signalAll()
        }

        for (thread in threads) {
            thread.interrupt()
        }

        for (child in children) {
            child.cancel()
        }
    }
}