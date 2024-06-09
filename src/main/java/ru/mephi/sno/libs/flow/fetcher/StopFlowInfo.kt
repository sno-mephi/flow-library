package ru.mephi.sno.libs.flow.fetcher

data class StopFlowInfo(
    val message: String = "",
    val info: List<String> = listOf(),
) {
    private lateinit var byFetcher: Class<out GeneralFetcher>
    private var shouldStopFlowExecution = false

    internal constructor(
        origin: StopFlowInfo,
        fromFetcher: Class<out GeneralFetcher>,
        shouldStopFlowExecution: Boolean,
    ) : this(origin.message, origin.info) {
        this.byFetcher = fromFetcher
        this.shouldStopFlowExecution = shouldStopFlowExecution
    }

    fun byFetcher() = byFetcher
    fun shouldStopFlowExecution() = shouldStopFlowExecution
}