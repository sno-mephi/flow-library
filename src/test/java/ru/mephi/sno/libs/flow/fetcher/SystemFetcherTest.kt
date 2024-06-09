package ru.mephi.sno.libs.flow.fetcher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.mephi.sno.libs.flow.belly.FlowBuilder
import ru.mephi.sno.libs.flow.belly.FlowContext
import ru.mephi.sno.libs.flow.belly.InjectData
import kotlin.reflect.KFunction

class SystemFetcherTest {

    @Test
    fun `test no InjectData annotation in Fetcher class`() {
        class FetcherWithoutInjectAnnotation : SystemFetcher()
        val testFetcher = FetcherWithoutInjectAnnotation()
        val testFlowBuilder = FlowBuilder()

        fun FlowBuilder.buildFlow() {
            group {
                fetch(testFetcher)
            }
        }
        testFlowBuilder.buildFlow()

        assertThrows<NoSuchMethodException> {
            testFlowBuilder.initAndRun()
        }
    }

    @Test
    fun `test too many InjectData methods in Fetcher class`() {
        class FetcherWithManyInjectData : SystemFetcher() {
            @InjectData
            fun doFetch1() {}

            @InjectData
            fun doFetch2() {}
        }

        val testFetcher = FetcherWithManyInjectData()
        val testFlowBuilder = FlowBuilder()

        fun FlowBuilder.buildFlow() {
            group {
                fetch(testFetcher)
            }
        }
        testFlowBuilder.buildFlow()

        assertThrows<NoSuchMethodException> {
            testFlowBuilder.initAndRun()
        }
    }

    @Test
    fun someTest() {
        open class StringInsertFetcher: SystemFetcher() {
            override fun fetchCall(
                flowContext: FlowContext,
                doFetchMethod: KFunction<*>,
                params: MutableList<Any?>
            ): Any? {
                flowContext.insertObject("test override general fetcher")
                return super.fetchCall(flowContext, doFetchMethod, params)
            }
        }

        class TestFetcher: StringInsertFetcher() {
            @InjectData
            fun doFetch1() {}
        }

        val testFetcher = TestFetcher()
        val flowContext = FlowContext()
        val testFlowBuilder = FlowBuilder()

        fun FlowBuilder.buildFlow() {
            group {
                fetch(testFetcher)
            }
        }
        testFlowBuilder.buildFlow()

        testFlowBuilder.initAndRun(
            flowContext = flowContext
        )
        
        val stringVal = flowContext.get<String?>()

        assertEquals("test override general fetcher", stringVal)
    }
}
