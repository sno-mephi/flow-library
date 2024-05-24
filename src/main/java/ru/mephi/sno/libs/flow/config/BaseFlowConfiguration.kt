package ru.mephi.sno.libs.flow.config

import ru.mephi.sno.libs.flow.belly.FlowBuilder
import ru.mephi.sno.libs.flow.registry.FlowRegistry

/**
 * Один из базовых способов создания flow.
 * Регистрирует ваш flow в FlowRegistry.
 * Может быть полезно в связке с аннотацией @Component из Spring Framework и инъекцией фетчеров.
 */
abstract class BaseFlowConfiguration {

    open fun getName() = this::class.qualifiedName

    open fun flowBuilder(): FlowBuilder {
        val flowBuilder = FlowBuilder()
        flowBuilder.buildFlow()
        FlowRegistry.getInstance().register(getName(), flowBuilder)
        return flowBuilder
    }

    abstract fun FlowBuilder.buildFlow()
}