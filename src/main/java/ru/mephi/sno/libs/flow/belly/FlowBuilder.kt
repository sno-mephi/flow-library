package ru.mephi.sno.libs.flow.belly

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 * Основной класс для сборки и запуска графа
 */
class FlowBuilder {

    companion object {
        private val log = LoggerFactory.getLogger(FlowBuilder::class.java)
    }

    private var currentNode: FlowNode = FlowNode(
        GeneralFetcher(),
        mutableListOf(),
        mutableListOf(),
        NodeType.GROUP,
    )

    /**
     * Объединяет группу узлов
     */
    fun group(
        condition: (FlowContext) -> Boolean = { true },
        action: () -> Unit,
    ) {
        val lastStateNode = currentNode
        currentNode = currentNode.addGroupNode(condition)
        action()
        currentNode = lastStateNode
    }

    /**
     * Объединяет группу узлов. Запускается после того, как выполнятся все верхние узлы
     */
    fun whenComplete(
        condition: (FlowContext) -> Boolean = { true },
        action: () -> Unit,
    ) {
        val lastStateNode = currentNode
        currentNode = currentNode.addWaitNode(condition)
        action()
        currentNode = lastStateNode
    }

    /**
     * Выполняет фетчер
     */
    fun fetch(fetcherInstance: GeneralFetcher) {
        // fetcherInstance.fetchMechanics() // вызывает метод фетчера, помеченный аннотацией @InjectData
        // вызываться должно после построения графа!
        currentNode.addFetcher(fetcherInstance)
    }

    // Инициализация и запуск графа с ожиданием окончания выполнения
    // TODO: настройка диспетчера корутин??
    fun initAndRun(
        flowContext: FlowContext = FlowContext(),
        vararg objectsToReset: Any,
    ) {
        runBlocking {
            val flowJob = launch {
                initAndRunAsync(
                    flowContext = flowContext,
                    objectsToReset = objectsToReset,
                )
            }

            flowJob.join()
        }
    }

    /**
     * Асинхронная инициализация и запуск графа
     */
    suspend fun initAndRunAsync(
        flowContext: FlowContext = FlowContext(),
        vararg objectsToReset: Any,
    ) {
        objectsToReset.forEach {
            // текущий экземпляр - новый, который передали в initAndRun()
            // кладем его в контекст, чтобы была возможность менять его по ходу выполнения графа
            if (!flowContext.containsBeanByType(it::class.java)) {
                flowContext.insertObject(it)
            }
        }
        run(currentNode, flowContext)
    }

    /**
     * Запускает граф без инициализации
     * (написано отдельно, так как метод рекурсивный)
     */
    private suspend fun run(
        node: FlowNode = currentNode,
        flowContext: FlowContext,
    ) {
        coroutineScope {
            val toRun = mutableListOf<Any>()
            node.children.forEach { children ->
                // TODO: NodeType.FETCHER гарантирует ненулевой фетчер
                if (children.nodeType == NodeType.FETCHER) {
                    toRun.add(children.fetcher!!)
                } else if (children.nodeType == NodeType.GROUP) {
                    toRun.add(children)
                } else if (children.nodeType == NodeType.WAIT) {
                    val completedRun = toRun.map {
                        async {
                            when (it) {
                                is GeneralFetcher -> it.fetchMechanics(flowContext)
                                is FlowNode -> if (it.condition.invoke(flowContext)) run(it, flowContext)
                            }
                        }
                    }
                    completedRun.awaitAll()
                    toRun.clear()
                    toRun.add(children)
                }
            }
            toRun.forEach {
                launch {
                    when (it) {
                        is GeneralFetcher -> it.fetchMechanics(flowContext)
                        is FlowNode -> if (it.condition.invoke(flowContext)) run(it, flowContext)
                    }
                }
            }
            toRun.clear()
        }
    }
}
