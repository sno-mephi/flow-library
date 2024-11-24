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

    override suspend fun fetchCall(
        flowContext: FlowContext,
        doFetchMethod: KFunction<*>,
        params: MutableList<Any?>,
    ): Any? {
        val systemFields = getFlowContext().get<SystemFields>() ?: SystemFields().also { flowContext.insertObject(it) }
        if (systemFields.stopFlowInfo?.shouldStopFlowExecution() == true) {
            return null
        }

        return runCatching {
            super.fetchCall(flowContext, doFetchMethod, params)?.takeIf {
                flowContext.get<SystemFields>()?.stopFlowInfo?.shouldStopFlowExecution() != true
            }
        }.onFailure { e ->
            insetException(e)
            onFailure(e)
        }.getOrNull()
    }

    private suspend fun insetException(e: Throwable) {
        val systemFields = getFlowContext().get<SystemFields>() ?: SystemFields()
        systemFields.apply {
            this.exception = e
            getFlowContext().insertObject(this)
        }
    }

    protected open suspend fun onFailure(e: Throwable) {
        log.error("ERROR: $e")
        log.debug(e.stackTraceToString())
        stopFlow()
    }

    /**
     * Прерывает дальнейшее выполнение графа в рамках сессии (прогонки графа)
     * После остановки текущий фетчер (и параллельные ему) уже ничего не возвращают во флоу,
     * а последующие - не выполняются.
     */
    protected open suspend fun stopFlow(
        stopFlowInfo: StopFlowInfo = StopFlowInfo()
    ) {
        val systemFields = getFlowContext().get<SystemFields>() ?: SystemFields()
        systemFields.apply {
            this.stopFlowInfo = StopFlowInfo(
                origin = stopFlowInfo,
                // TODO: test class check!
                fromFetcher = this@GeneralFetcher::class.java,
                shouldStopFlowExecution = true,
            )
            getFlowContext().insertObject(this)
        }
    }
}