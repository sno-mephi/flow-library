package ru.mephi.sno.libs.flow.fetcher

import ru.mephi.sno.libs.flow.belly.Mutable

/**
 * Представляет системные поля в контексте флоу
 */
@Mutable
data class SystemFields(
    /** флаг принудительной остановки флоу. Если false то флоу принудительно останавливается **/
    var stopFlowInfo: StopFlowInfo? = null,
    var exception: Throwable? = null,
)