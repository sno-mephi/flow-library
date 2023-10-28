package ru.mephi.sno.libs.flow.belly

import ru.mephi.sno.libs.flow.fetcher.GeneralFetcher

/**
 * Представляет собой узел графа
 */
class FlowNode(
    val fetcher: GeneralFetcher?,
    val children: MutableList<FlowNode>,
    val parents: MutableList<FlowNode>,
    val nodeType: NodeType,
    val condition: (FlowContext) -> Boolean = { true },
) {

    private fun addParentNode(parentNode: FlowNode) {
        this.parents.add(parentNode)
    }

    private fun addChildrenNode(childrenNode: FlowNode): FlowNode {
        this.children.add(childrenNode)
        return childrenNode
    }

    private fun addWGNode(
        fetcher: GeneralFetcher? = null,
        children: MutableList<FlowNode> = mutableListOf(),
        parents: MutableList<FlowNode> = mutableListOf(this),
        nodeType: NodeType,
        condition: (FlowContext) -> Boolean,
    ): FlowNode {
        return addChildrenNode(
            FlowNode(
                fetcher = fetcher,
                children = children,
                parents = parents,
                nodeType = nodeType,
                condition = condition,
            ),
        )
    }

    internal fun addFetcher(generalFetcher: GeneralFetcher): FlowNode {
        return addChildrenNode(
            FlowNode(
                fetcher = generalFetcher,
                children = mutableListOf(),
                parents = mutableListOf(this),
                nodeType = NodeType.FETCHER,
            ),
        )
    }

    internal fun addWaitNode(
        condition: (FlowContext) -> Boolean,
    ): FlowNode {
        return addWGNode(
            nodeType = NodeType.WAIT,
            condition = condition,
        )
    }

    internal fun addGroupNode(
        condition: (FlowContext) -> Boolean,
    ): FlowNode {
        return addWGNode(
            nodeType = NodeType.GROUP,
            condition = condition,
        )
    }
}
