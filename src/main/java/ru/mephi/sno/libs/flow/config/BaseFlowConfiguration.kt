package ru.mephi.sno.libs.flow.config

import ru.mephi.sno.libs.flow.belly.FlowBuilder
import ru.mephi.sno.libs.flow.registry.FlowRegistry
import java.util.*

/**
 * Один из базовых способов создания flow.
 * Регистрирует ваш flow в FlowRegistry.
 * Может быть полезно в связке со Spring Framework и инъекцией фетчеров.
 */
abstract class BaseFlowConfiguration {

    private val guid = UUID.randomUUID()

    open fun getName() = guid.toString()

    open fun flowBuilder(): FlowBuilder {
        val flowBuilder = FlowBuilder()
        flowBuilder.buildFlow()
        FlowRegistry.getInstance().register(getName(), guid, flowBuilder)
        return flowBuilder
    }

    abstract fun FlowBuilder.buildFlow()
}