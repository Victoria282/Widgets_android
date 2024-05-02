package ru.taxcom.widget.api

import io.ktor.client.statement.HttpResponse
import ru.taxcom.cashdeskkit.data.statistics.request.StatisticFilter
import ru.taxcom.cashdeskkit.data.user.api.AuthRequest
import ru.taxcom.taxcomkit.data.user.UserData

interface WidgetApi {
    suspend fun loadStatistic(filter: StatisticFilter, userData: UserData, id: Long): HttpResponse
    suspend fun loadOutletsForFilter(departmentId: Long?, currentCabinetId: Long, server: String): HttpResponse
    suspend fun cabinetLogin(url: String, authRequest: AuthRequest): HttpResponse
    suspend fun loadDivisionsForFilter(currentCabinetId: Long, server: String): HttpResponse
}