package ru.mephi.sno.libs.flow.fetcher

import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import ru.mephi.sno.libs.flow.belly.FlowBuilder
import ru.mephi.sno.libs.flow.belly.FlowContext
import ru.mephi.sno.libs.flow.belly.FlowContextElement
import ru.mephi.sno.libs.flow.belly.InjectData
import kotlin.coroutines.coroutineContext

class GeneralFetcherTest {

    @Test
    fun `test stopFlow()`() {
        class TestFetcherA: GeneralFetcher() {
            @InjectData
            fun doFetch() = "a"
        }

        class TestFetcherB: GeneralFetcher() {
            @InjectData
            suspend fun doFetch(): String {
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

    @Test
    fun `getting FlowContext from CoroutineContext test`() {
        class TestFetcher: GeneralFetcher() {
            @InjectData
            suspend fun doFetch(): String {
                val flowContext = coroutineContext[FlowContextElement]?.flowContext!!
                return flowContext.get<String>() + "_success"
            }
        }
        val testFetcher = TestFetcher()
        val flowContext = FlowContext().apply {
            insertObject("12345")
        }
        val testFlowBuilder = FlowBuilder()
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
        assertEquals(flowContext.get<String>(), "12345_success")
    }

    @Test
    fun `parallel getting FlowContext from CoroutineContext test`() {
        class TestFetcher : GeneralFetcher() {
            @InjectData
            suspend fun doFetch(): String {
                val flowContext = coroutineContext[FlowContextElement]?.flowContext!!
                return flowContext.get<String>() + "_success"
            }
        }

        val count = 1000
        val testFetcher = TestFetcher()
        val testFlowBuilder = FlowBuilder()
        fun FlowBuilder.buildFlow() {
            sequence {
                fetch(testFetcher)
            }
        }
        testFlowBuilder.buildFlow()

        val scope = CoroutineScope(Dispatchers.Default)

        val contexts = List(count) { index ->
            FlowContext().apply {
                insertObject("context_$index")
            }
        }
        val jobs = List(count) { index ->
            scope.async {
                testFlowBuilder.initAndRun(
                    contexts[index],
                    Dispatchers.IO,
                    true,
                )
            }
        }
        runBlocking { jobs.awaitAll() }

        for(i in 0..<count)
            assertEquals("context_${i}_success", contexts[i].get<String>())
    }
}