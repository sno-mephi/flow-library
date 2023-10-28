package ru.mephi.sno.libs.flow.belly

import org.slf4j.LoggerFactory

/**
 * Представляет контекст графа
 */
data class FlowContext(
    private val contextMap: MutableMap<String, Any?> = mutableMapOf(),
) {

    // TODO: настройка написания логов для этой либы?
    companion object {
        private val log = LoggerFactory.getLogger(FlowContext::class.java)
    }

    /**
     * Добавляет в контекст объект, если он не нулевой
     */
    fun insertObject(obj: Any?) {
        obj?.let {
            val objClass = obj.javaClass
            contextMap[objClass.name] = obj
        }
    }

    /**
     * Извлекает из контекста объект по типу, если такой есть; если нет то возвращается null
     */
    fun getBeanByType(clazz: Class<*>): Any? {
        if (!containsBeanByType(clazz)) {
            log.warn("Context doesn't contains object with type ${clazz.name}")
            return null
        }
        return contextMap[clazz.name]
    }

    /**
     * Более короткий аналог getBeanByType: просто вызовите flowContext.get<ObjClass>(), чтобы получить
     * нужный вам экземпляр из контекста
     */
    inline fun <reified T> get() = this.getBeanByType(T::class.java) as T?

    fun containsBeanByType(clazz: Class<*>): Boolean {
        return contextMap.contains(clazz.name)
    }

    /**
     * Очищает контекст
     */
    fun clear() {
        contextMap.clear()
    }
}
