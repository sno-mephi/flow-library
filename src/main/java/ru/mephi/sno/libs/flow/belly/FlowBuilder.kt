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
     * Объединяет группу узлов, выполняет их последовательно
     */
    fun sequence(
        condition: (FlowContext) -> Boolean = { true },
        action: () -> Unit,
    ) {
        val lastStateNode = currentNode
        currentNode = currentNode.addSequenceNode(condition)
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
    fun initAndRun(
        flowContext: FlowContext = FlowContext(), // если не указан контекст, создается пустой по умолчанию
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        wait: Boolean = true,
        vararg objectsToReset: Any,
    ) {
        runBlocking {
            // TODO: подумать. может создавать джобу - лишнее? мы же уже сидим внутри скоупа
            val flowJob = launch(dispatcher) {
                initAndRunAsync(
                    flowContext = flowContext,
                    dispatcher = dispatcher,
                    objectsToReset = objectsToReset,
                )
            }
            if (wait) flowJob.join()
        }
    }

    /**
     * Асинхронная инициализация и запуск графа
     */
    suspend fun initAndRunAsync(
        flowContext: FlowContext = FlowContext(), // если не указан контекст, создается пустой по умолчанию
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        vararg objectsToReset: Any,
    ) {
        objectsToReset.forEach {
            // текущий экземпляр - новый, который передали в initAndRun()
            // кладем его в контекст, чтобы была возможность менять его по ходу выполнения графа
            if (!flowContext.containsBeanByType(it::class.java)) {
                flowContext.insertObject(it)
            }
        }
        run(currentNode, flowContext, dispatcher)
    }

    /**
     * Запускает граф без инициализации
     * (написано отдельно, так как метод рекурсивный)
     */
    private suspend fun run(
        node: FlowNode = currentNode,
        flowContext: FlowContext,
        dispatcher: CoroutineDispatcher,
    ) {
        withContext(dispatcher) {
            val toRun = mutableListOf<Any>()
            val currentNodeType = node.nodeType
            node.children.forEach { children ->
                // TODO: NodeType.FETCHER гарантирует ненулевой фетчер
                when (children.nodeType) {
                    NodeType.FETCHER -> toRun.add(children.fetcher!!)
                    NodeType.GROUP, NodeType.SEQUENCE -> toRun.add(children)
                    NodeType.WAIT -> {
                        val completedRun = toRun.map {
                            launch(dispatcher) {
                                resolveRunMechanics(it, flowContext, dispatcher)
                            }.also {
                                if (currentNodeType == NodeType.SEQUENCE) it.join()
                            }
                        }
                        completedRun.joinAll()
                        toRun.clear()
                        toRun.add(children)
                    }
                }
            }
            toRun.forEach {
                launch(dispatcher) {
                    resolveRunMechanics(it, flowContext, dispatcher)
                }.also {
                    if (currentNodeType == NodeType.SEQUENCE) it.join()
                }
            }
            toRun.clear()
        }
    }

    private suspend inline fun resolveRunMechanics(
        objectToResolve: Any,
        flowContext: FlowContext,
        dispatcher: CoroutineDispatcher,
    ) {
        when (objectToResolve) {
            is GeneralFetcher -> objectToResolve.fetchMechanics(flowContext)
            is FlowNode -> if (objectToResolve.condition.invoke(flowContext)) run(objectToResolve, flowContext, dispatcher)
        }
    }
}
