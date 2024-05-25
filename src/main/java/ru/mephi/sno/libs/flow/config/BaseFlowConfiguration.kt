package ru.mephi.sno.libs.flow.config

import ru.mephi.sno.libs.flow.belly.FlowBuilder
import ru.mephi.sno.libs.flow.registry.FlowRegistry
import ru.mephi.sno.libs.flow.util.Helps
import kotlin.reflect.KClass

/**
 * Один из базовых способов создания flow.
 * Регистрирует ваш flow в FlowRegistry.
 * Может быть полезно в связке со Spring Framework и инъекцией фетчеров.
 */
abstract class BaseFlowConfiguration(
    private val origin: KClass<out BaseFlowConfiguration>
) {
    open fun getName(): String = Helps.classStringForm(origin.java)

    open fun flowBuilder(): FlowBuilder {
        val flowBuilder = FlowBuilder()
        flowBuilder.buildFlow()
        FlowRegistry.getInstance().register(getName(), origin.java, flowBuilder)
        return flowBuilder
    }

    abstract fun FlowBuilder.buildFlow()
}