package ru.mephi.sno.libs.flow.fetcher

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.mephi.sno.libs.flow.belly.FlowBuilder
import ru.mephi.sno.libs.flow.belly.InjectData

class GeneralFetcherTest {

    @Test
    fun `test no InjectData annotation in Fetcher class`() {
        class FetcherWithoutInjectAnnotation : GeneralFetcher()
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
        class FetcherWithManyInjectData : GeneralFetcher() {
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
}
