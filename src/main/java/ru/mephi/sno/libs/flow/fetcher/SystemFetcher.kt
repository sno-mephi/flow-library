package ru.mephi.sno.libs.flow.fetcher

import org.apache.commons.lang3.SerializationUtils
import org.slf4j.LoggerFactory
import ru.mephi.sno.libs.flow.belly.FlowContext
import ru.mephi.sno.libs.flow.belly.InjectData
import ru.mephi.sno.libs.flow.belly.Mutable
import java.io.Serializable
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.javaType

/**
 * Если класс наследуется от данного, его можно использовать к качестве фетчера.
 * Исполняемый метод должен быть помечен аннотацией @InjectData
 */
open class SystemFetcher {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Метод, который запускает метод помеченный как @InjectData,
     * внедряя в него нужные бины из контекста
     */
    fun fetchMechanics(flowContext: FlowContext) {
        // получаем все методы с аннотацией @InjectData
        val methods = this::class.declaredMemberFunctions.filter { it.hasAnnotation<InjectData>() }

        if (methods.isEmpty()) {
            throw NoSuchMethodException("can't find method with InjectData annotation in ${this::class}")
        }

        if (methods.size > 1) {
            throw NoSuchMethodException("too many methods with annotation InjectData in ${this::class}")
        }

        val doFetchMethod = methods.first()
        val params = getParamsFromFlow(doFetchMethod, flowContext)

        val fetchResult = fetchCall(flowContext, doFetchMethod, params)
        fetchResult?.let { flowContext.insertObject(it) }
    }

    protected fun getParamsFromFlow(
        method: KFunction<*>,
        flowContext: FlowContext,
    ): MutableList<Any?> {
        val paramTypes = method.parameters
            .map { it.type.javaType as Class<*> }
        val nonCloneableObjects = mutableListOf<Any>()
        val params = mutableListOf<Any?>()

        paramTypes
            .let { it.subList(1, it.size) }
            .forEach { paramType ->
                val injectedObject = flowContext.getBeanByType(paramType)
                // если мы отметили тип как изменяемый (@Mutable), то позволяем его менять во время выполнения
                if (paramType.kotlin.hasAnnotation<Mutable>()) {
                    params.add(injectedObject)
                } else {
                    params.add(
                        tryToClone(injectedObject),
                    )

                    if (injectedObject != null && !isCloneable(injectedObject)) {
                        nonCloneableObjects.add(injectedObject)
                    }
                }
            }

        if (nonCloneableObjects.isNotEmpty()) {
            log.warn(
                "Flow contains non-cloneable objects: {}. Inject original instead into method {}",
                nonCloneableObjects,
                method.name,
            )
        }

        return params
    }

    /**
     * Метод для запуска фетчера
     * Возвращает объект, который должен вернуться в контекст
     */
    open fun fetchCall(
        flowContext: FlowContext,
        doFetchMethod: KFunction<*>,
        params: MutableList<Any?>,
    ): Any? {
        return doFetchMethod.call(this, *params.toTypedArray())
    }

    /**
     * Клонирует объект, если это возможно (если от data class или Serializable). Возвращает его копию.
     * Если невозможно клонировать - возвращает сам этот объект
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> tryToClone(obj: T?): T? {
        obj ?: return null

        return when {
            obj::class.isData -> {
                val copy = obj::class.memberFunctions.firstOrNull { it.name == "copy" }
                val instanceParam = copy?.instanceParameter
                instanceParam?.let {
                    copy.callBy(mapOf(it to obj)) as T
                }
            }
            obj is Serializable -> SerializationUtils.clone(obj) as T
            else -> obj
        }
    }

    private fun isCloneable(obj: Any) = obj is Serializable || obj::class.isData
}
