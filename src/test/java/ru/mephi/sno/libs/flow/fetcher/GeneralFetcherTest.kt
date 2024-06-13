package ru.mephi.sno.libs.flow.fetcher

import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import ru.mephi.sno.libs.flow.belly.FlowBuilder
import ru.mephi.sno.libs.flow.belly.FlowContext
import ru.mephi.sno.libs.flow.belly.InjectData

class GeneralFetcherTest {

    @Test
    fun `test stopFlow()`() {
        class TestFetcherA: GeneralFetcher() {
            @InjectData
            fun doFetch() = "a"
        }

        class TestFetcherB: GeneralFetcher() {
            @InjectData
            fun doFetch(): String {
                if (1 > 0) stopFlow(
                    StopFlowInfo(
                        message = "test",
                    )
                )
                return "b"
            }
        }

        val testFetcherA = TestFetcherA()
        val testFetcherB = TestFetcherB()

        val testFlowBuilder = FlowBuilder()

        val flowContext = FlowContext()

        fun FlowBuilder.buildFlow() {
            sequence {
                fetch(testFetcherA)
                fetch(testFetcherB)
            }
        }

        testFlowBuilder.buildFlow()
        testFlowBuilder.initAndRun(
            flowContext,
            Dispatchers.Default,
            true,
        )
        assertEquals(
            "a",
            flowContext.get<String>(),
        )

        flowContext.get<SystemFields>()?.stopFlowInfo!!.apply {
            assertEquals(
                true,
                shouldStopFlowExecution(),
            )
            assertEquals(
                "test",
                message,
            )
            assertEquals(
                TestFetcherB::class.java,
                byFetcher(),
            )
        }
    }

    @Test
    fun `Throwable in flow on failure test`() {
        class BadFetcher: GeneralFetcher() {
            @InjectData
            fun doFetch(): String {
                throw RuntimeException("test")
            }
        }
        val testFetcher = BadFetcher()
        val testFlowBuilder = FlowBuilder()
        val flowContext = FlowContext()
        fun FlowBuilder.buildFlow() {
            sequence {
                fetch(testFetcher)
            }
        }
        testFlowBuilder.buildFlow()
        testFlowBuilder.initAndRun(
            flowContext,
            Dispatchers.Default,
            true,
        )
        assertNotNull(flowContext.get<SystemFields>()?.exception)
    }
}