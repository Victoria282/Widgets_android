package ru.taxcom.widget.repository

import ru.taxcom.cashdeskkit.data.statistics.request.StatisticFilter
import ru.taxcom.cashdeskkit.data.statistics.response.Statistics
import ru.taxcom.taxcomkit.data.resource.Resource
import ru.taxcom.taxcomkit.data.user.UserData

interface WidgetRepository {
    suspend fun loadStatistic(filter: StatisticFilter, user: UserData, id: Long): Resource<Statistics>
    suspend fun loginCabinet(cabinetId: Long): Resource<UserData>
    suspend fun isLoggedIn(): Boolean
    fun getCurrentCabinet(): UserData
    fun userLogin(): String
}