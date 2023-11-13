package ru.mephi.sno.libs.flow.belly

/**
 * Требуется для инжекта данных из сессии флоу в метод фетчера
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class InjectData
