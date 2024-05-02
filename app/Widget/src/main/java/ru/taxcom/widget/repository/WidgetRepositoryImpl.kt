package ru.taxcom.widget.repository

import ru.taxcom.cashdeskkit.data.statistics.request.StatisticFilter
import ru.taxcom.cashdeskkit.data.statistics.response.Statistics
import ru.taxcom.cashdeskkit.data.user.api.AuthRequest
import ru.taxcom.cashdeskkit.data.user.api.LoginResponse
import ru.taxcom.cashdeskkit.data.user.api.toUserData
import ru.taxcom.cashdeskkit.repositories.user.UserRepository
import ru.taxcom.taxcomkit.data.resource.Resource
import ru.taxcom.taxcomkit.data.user.UserData
import ru.taxcom.taxcomkit.utils.internet.processHttpResponse
import ru.taxcom.widget.api.WidgetApi
import javax.inject.Inject

class WidgetRepositoryImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val widgetApi: WidgetApi
) : WidgetRepository {

    override suspend fun isLoggedIn(): Boolean = userRepository.isLoggedIn()
    override fun getCurrentCabinet(): UserData = userRepository.getUser()
    override fun userLogin(): String = userRepository.getUser().login

    override suspend fun loadStatistic(
        filter: StatisticFilter, user: UserData, id: Long
    ): Resource<Statistics> = processHttpResponse { widgetApi.loadStatistic(filter, user, id) }

    override suspend fun loginCabinet(cabinetId: Long): Resource<UserData> {
        val httpResponse = widgetApi.cabinetLogin(
            userRepository.getUser().server,
            AuthRequest(
                cabinetId = cabinetId,
                login = userRepository.getUser().login,
                password = userRepository.getUser().password
            )
        )
        return processHttpResponse<LoginResponse, UserData>(
            response = {
                httpResponse
            },
            converter = {
                val result = it.toUserData(
                    url = userRepository.getUser().server,
                    login = userRepository.getUser().login,
                    password = userRepository.getUser().password,
                    currentCabinetId = cabinetId,
                    loginType = userRepository.getUser().loginType
                )
                userRepository.saveUser(result, false)
                result
            }
        )
    }
}