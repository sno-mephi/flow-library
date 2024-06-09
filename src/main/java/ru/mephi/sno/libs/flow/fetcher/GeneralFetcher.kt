package ru.mephi.sno.libs.flow.fetcher

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.mephi.sno.libs.flow.belly.FlowContext
import kotlin.reflect.KFunction

/**
 * Класс-обертка над SystemFetcher. Возможности:
 *  - Обработка ошибок
 *  - Возможность остановки графа (на уровне фетчеров)
 */
open class GeneralFetcher: SystemFetcher() {
    protected val log: Logger = LoggerFactory.getLogger(this::class.java)
    protected lateinit var flowContext: FlowContext

    override fun fetchCall(
        flowContext: FlowContext,
        doFetchMethod: KFunction<*>,
        params: MutableList<Any?>,
    ): Any? {
        this.flowContext = flowContext
        val systemFields = this.flowContext.get<SystemFields>() ?: SystemFields()
        if (systemFields.stopFlowInfo?.shouldStopFlowExecution() == true) {
            return null
        }

        return runCatching {
            super.fetchCall(flowContext, doFetchMethod, params)
        }.onFailure { e ->
            onFailure(e)
        }.getOrNull()
    }

    protected open fun onFailure(e: Throwable) {
        log.error("ERROR: $e")
        log.debug(e.stackTraceToString())
        stopFlowNextExecution()
    }

    /**
     * Прерывает дальнейшее выполнение графа в рамках сессии (прогонки графа)
     */
    @Synchronized
    protected open fun stopFlowNextExecution(
        stopFlowInfo: StopFlowInfo = StopFlowInfo()
    ) {
        val systemFields = flowContext.get<SystemFields>() ?: SystemFields()
        systemFields.apply {
            this.stopFlowInfo = StopFlowInfo(
                origin = stopFlowInfo,
                // TODO: test class check!
                fromFetcher = this@GeneralFetcher::class.java,
                shouldStopFlowExecution = true,
            )
            flowContext.insertObject(this)
        }
    }
}