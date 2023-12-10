package ru.mephi.sno.libs.flow.belly

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

class FlowBuilderTest {

    companion object {
        private val log = LoggerFactory.getLogger(FlowBuilderTest::class.java)
    }

    // Фетчер для тестирования всяких group, whenComplete и прочего подобного
    class TimeTestFetcher(
        private val marker: String = "test-fetcher",
        private val pauseTime: Long = 1000,
    ) : GeneralFetcher() {
        @InjectData
        fun doFetch() {
            runBlocking {
                delay(pauseTime)
            }
            log.info("fetcher {} has completed his execution.", marker)
        }
    }

    // Кладет в контекст строку strToReturn
    class ReturnToContextTestFetcher(
        private val strToReturn: String,
    ) : GeneralFetcher() {
        @InjectData
        fun doFetch(): String {
            return strToReturn
        }
    }

    // Извлекает из контекста строку
    open class GetFromContextTestFetcher : GeneralFetcher() {
        open lateinit var byContext: String

        @InjectData
        fun doFetch(contextualObj: String) {
            byContext = contextualObj
        }
    }

    @Test
    fun `group() test without conditions and context`() {
        val testFetcher1 = TimeTestFetcher("test-fetcher#1")
        val testFetcher2 = TimeTestFetcher("test-fetcher#2")
        val testFetcher3 = TimeTestFetcher("test-fetcher#3")
        val testFetcher4 = TimeTestFetcher("test-fetcher#4")
        val testFlowBuilder = FlowBuilder()

        fun FlowBuilder.buildFlow() {
            group {
                fetch(testFetcher1)
                fetch(testFetcher2)
                fetch(testFetcher3)
                fetch(testFetcher4)
            }
        }
        testFlowBuilder.buildFlow()

        val elapsedTime = runWithTimer {
            testFlowBuilder.initAndRun()
        }
        assertTrue(elapsedTime > 1000)
        assertTrue(elapsedTime < 2500)
    }

    @Test
    fun `whenComplete() test without conditions and context`() {
        val testFetcher1 = TimeTestFetcher("test-fetcher#1")
        val testFetcher2 = TimeTestFetcher("test-fetcher#2")
        val testFetcher3 = TimeTestFetcher("test-fetcher#3")
        val testFlowBuilder1 = FlowBuilder()
        val testFlowBuilder2 = FlowBuilder()
        val testFlowBuilder3 = FlowBuilder()

        fun FlowBuilder.buildFlow1() {
            group {
                fetch(testFetcher1)
                whenComplete {
                    fetch(testFetcher2)
                    whenComplete {
                        fetch(testFetcher3)
                    }
                }
            }
        }
        testFlowBuilder1.buildFlow1()

        val elapsedTime1 = runWithTimer {
            testFlowBuilder1.initAndRun()
        }

        // по сути эта конфигурация ничем не отличается от buildFlow1()
        fun FlowBuilder.buildFlow2() {
            group {
                fetch(testFetcher1)
                whenComplete {
                    fetch(testFetcher2)
                }
                whenComplete {
                    fetch(testFetcher3)
                }
            }
        }
        testFlowBuilder2.buildFlow2()

        val elapsedTime2 = runWithTimer {
            testFlowBuilder2.initAndRun()
        }

        fun FlowBuilder.buildFlow3() {
            group {
                fetch(testFetcher1)
                whenComplete {
                    fetch(testFetcher2)
                }
                fetch(testFetcher3)
            }
        }
        testFlowBuilder3.buildFlow3()

        val elapsedTime3 = runWithTimer {
            testFlowBuilder3.initAndRun()
        }

        assertTrue(elapsedTime1 > 3000)

        assertTrue(elapsedTime2 > 3000)

        assertTrue(elapsedTime3 > 2000)
        assertTrue(elapsedTime3 < 3000)
    }

    @Test
    fun `fetchers return (insert into context) and get from context test`() {
        val testFetcher1 = ReturnToContextTestFetcher(strToReturn = "hello world!")
        val testFetcherWithValue1 = GetFromContextTestFetcher()
        val testFetcherWithValue2 = GetFromContextTestFetcher()
        val testFlowBuilder1 = FlowBuilder()
        val testFlowBuilder2 = FlowBuilder()

        fun FlowBuilder.buildFlow1() {
            group {
                fetch(testFetcher1)
                whenComplete {
                    fetch(testFetcherWithValue1)
                }
            }
        }
        testFlowBuilder1.buildFlow1()

        // запускаем с пустым контекстом
        testFlowBuilder1.initAndRun(FlowContext())

        fun FlowBuilder.buildFlow2() {
            group {
                fetch(testFetcherWithValue2)
            }
        }
        testFlowBuilder2.buildFlow2()
        testFlowBuilder2.initAndRun(
            FlowContext(),
            Dispatchers.Default,
            true,
            "And we are obliged to be born..",
        )

        assertEquals("hello world!", testFetcherWithValue1.byContext)
        assertEquals("And we are obliged to be born..", testFetcherWithValue2.byContext)
    }

    @Test
    fun `sequence() test without conditions and context`() {
        val testFetcher1 = TimeTestFetcher("test-fetcher#1", pauseTime = 500)
        val testFetcher2 = TimeTestFetcher("test-fetcher#2", pauseTime = 500)
        val testFetcher3 = TimeTestFetcher("test-fetcher#3", pauseTime = 500)
        val testFetcher4 = TimeTestFetcher("test-fetcher#4", pauseTime = 500)
        val testFetcher5 = TimeTestFetcher("test-fetcher#5", pauseTime = 500)
        val testFlowBuilder = FlowBuilder()

        fun FlowBuilder.buildFlow() {
            sequence {
                fetch(testFetcher1)
                fetch(testFetcher2)
                fetch(testFetcher3)
                fetch(testFetcher4)
                fetch(testFetcher5)
            }
        }
        testFlowBuilder.buildFlow()

        val elapsedTime = runWithTimer {
            testFlowBuilder.initAndRun()
        }
        assertTrue(elapsedTime > 2500)
        assertTrue(elapsedTime < 3700)
    }

    @Test
    fun `sequence() + when() test without conditions and context`() {
        val testFetcher1 = TimeTestFetcher("test-fetcher#1", pauseTime = 500)
        val testFetcher2 = TimeTestFetcher("test-fetcher#2", pauseTime = 500)
        val testFetcher3 = TimeTestFetcher("test-fetcher#3", pauseTime = 500)
        val testFetcher4 = TimeTestFetcher("test-fetcher#4", pauseTime = 500)
        val testFetcher5 = TimeTestFetcher("test-fetcher#5", pauseTime = 500)
        val testFetcher6 = TimeTestFetcher("test-fetcher#6", pauseTime = 500)
        val testFlowBuilder = FlowBuilder()

        // по-хорошему, так писать плохо; смысл от этого whenComplete?
        fun FlowBuilder.buildFlow() {
            sequence {
                fetch(testFetcher1)
                fetch(testFetcher2)
                fetch(testFetcher3)
                fetch(testFetcher4)
                fetch(testFetcher5)
                whenComplete {
                    fetch(testFetcher6)
                }
            }
        }
        testFlowBuilder.buildFlow()

        val elapsedTime = runWithTimer {
            testFlowBuilder.initAndRun()
        }
        assertTrue(elapsedTime > 3000)
        assertTrue(elapsedTime < 4500)
    }

    @Test
    fun `the order of execution sequence() must be consistent`() {
        class TestFetcher(
            val num: Int,
        ) : GeneralFetcher() {
            @InjectData
            fun doFetch(str: String): String {
                val newStrVal = str + "$num"
                log.info("new str in flow-context: $newStrVal")
                return newStrVal
            }
        }

        val testFetcher1 = TestFetcher(1)
        val testFetcher2 = TestFetcher(2)
        val testFetcher3 = TestFetcher(3)
        val testFetcher4 = TestFetcher(4)
        val testFetcher5 = TestFetcher(5)
        val testFlowBuilder = FlowBuilder()

        val flowContext = FlowContext()

        fun FlowBuilder.buildFlow() {
            sequence {
                fetch(testFetcher1)
                fetch(testFetcher2)
                fetch(testFetcher3)
                fetch(testFetcher4)
                fetch(testFetcher5)
            }
        }
        testFlowBuilder.buildFlow()

        testFlowBuilder.initAndRun(
            flowContext,
            Dispatchers.Default,
            false,
            "",
        )

        assertEquals(
            flowContext.get<String>(),
            "12345",
        )
    }

    /** Выполняет задачу и возвращает время выполнения**/
    private fun runWithTimer(
        action: () -> Any,
    ): Long {
        val startTime = System.currentTimeMillis()
        action()
        val endTime = System.currentTimeMillis()
        return endTime - startTime
    }
}
