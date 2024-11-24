package ru.mephi.sno.libs.flow.belly

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class FlowContextElement(val flowContext: FlowContext) :
    AbstractCoroutineContextElement(FlowContextElement) {
    companion object Key : CoroutineContext.Key<FlowContextElement>
}