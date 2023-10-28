package ru.mephi.sno.libs.flow.belly

// WGNode - узел типа WAIT/GROUP
/**
 * Содержит тип флоу
 */
enum class NodeType {
    /**
     * Узел ожидания - ждет завершения всех верхних фетчеров + объединяет узлы в группу; fetcher = null
     */
    WAIT,

    /**
     * Узел, объединяющий заданные узлы в группу; fetcher = null
     */
    GROUP,

    /**
     * Выполняющий узел; fetcher != null
     */
    FETCHER,
}
