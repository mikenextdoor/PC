import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread

class HistogramStressTest {

    @Test
    fun concurrentAddValue() {

        val histogram = Histogram<Int>()

        val threadCount = 20
        val iterations = 10000
        val threads = mutableListOf<Thread>()

        repeat(threadCount) {
            threads += thread {
                repeat(iterations) {
                    histogram.addValue(1)
                }
            }
        }

        threads.forEach { it.join() }

        val expected = threadCount * iterations
        val actual = histogram.getCountForValue(1)

        assertEquals(expected.toLong(), actual)
    }

    @Test
    fun concurrentMultipleKeys() {

        val histogram = Histogram<Int>()

        val threadCount = 10
        val iterations = 5000
        val threads = mutableListOf<Thread>()

        repeat(threadCount) { value ->
            threads += thread {
                repeat(iterations) {
                    histogram.addValue(value)
                }
            }
        }

        threads.forEach { it.join() }

        val snapshot = histogram.getSnapshot()

        for (i in 0 until threadCount) {
            assertEquals(iterations.toLong(), snapshot[i])
        }
    }

    @Test
    fun concurrentAddValues() {

        val histogram = Histogram<Int>()

        val values = listOf(1, 2, 3, 4, 5)
        val threadCount = 30
        val iterations = 2000

        val threads = mutableListOf<Thread>()

        repeat(threadCount) {
            threads += thread {
                repeat(iterations) {
                    histogram.addValues(values)
                }
            }
        }

        threads.forEach { it.join() }

        val snapshot = histogram.getSnapshot()
        val expected = (threadCount * iterations).toLong()

        for (v in values) {
            assertEquals(expected, snapshot[v])
        }
    }

    @Test
    fun concurrentReadsAndWrites() {

        val histogram = Histogram<Int>()
        val threads = mutableListOf<Thread>()

        repeat(10) {
            threads += thread {
                repeat(10000) {
                    histogram.addValue(1)
                }
            }
        }

        repeat(10) {
            threads += thread {
                repeat(10000) {
                    histogram.getCountForValue(1)
                    histogram.getSnapshot()
                }
            }
        }

        threads.forEach { it.join() }

        assertEquals(100000, histogram.getCountForValue(1))
    }
}