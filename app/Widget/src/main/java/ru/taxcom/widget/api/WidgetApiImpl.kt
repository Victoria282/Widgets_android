package ru.taxcom.widget.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ru.taxcom.cashdeskkit.data.statistics.request.StatisticFilter
import ru.taxcom.cashdeskkit.data.user.api.AuthRequest
import ru.taxcom.cashdeskkit.data.user.api.LoginResponse
import ru.taxcom.cashdeskkit.data.user.api.toUserData
import ru.taxcom.cashdeskkit.repositories.user.UserRepository
import ru.taxcom.taxcomkit.data.resource.ResourceStatus
import ru.taxcom.taxcomkit.data.user.UserData
import ru.taxcom.taxcomkit.network.Ktor
import ru.taxcom.taxcomkit.utils.internet.processHttpResponse
import javax.inject.Inject

class WidgetApiImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val ktor: Ktor
) : WidgetApi {

    private val authParameter = "Session-token"

    private fun createClient(cabinetId: Long): HttpClient {
        val client = ktor.ktorHttpClient
        client.plugin(HttpSend).intercept { request ->
            val originalCall = execute(request)
            when (originalCall.response.status.value) {
                AUTH_ERROR -> {
                    val user = userRepository.getCabinetUser(cabinetId) ?: userRepository.getUser()
                    val response = processHttpResponse<LoginResponse, UserData>(
                        response = {
                            cabinetLogin(
                                url = user.server,
                                authRequest = AuthRequest(
                                    login = user.login,
                                    password = user.password,
                                    cabinetId = user.currentCabinetId
                                )
                            )
                        },
                        converter = {
                            it.toUserData(
                                url = user.server,
                                login = user.login,
                                password = user.password,
                                currentCabinetId = user.currentCabinetId ?: 0L,
                                loginType = user.loginType
                            )
                        },
                        logOut = null
                    )
                    if (response.status == ResourceStatus.SUCCESS) {
                        response.data?.let {
                            userRepository.updateUserToken(it)
                            request.headers.remove(authParameter)
                            request.header(authParameter, it.token)
                        }
                        execute(request)
                    } else originalCall
                }

                else -> originalCall
            }
        }
        return client
    }

    override suspend fun loadStatistic(
        filter: StatisticFilter, userData: UserData, id: Long
    ): HttpResponse {
        val client = createClient(id)
        val user = userRepository.getCabinetUser(id) ?: userData
        return client.post {
            url("${user.server}v1/GetCashdeskStatisticByDay")
            header(authParameter, user.token)
            setBody(filter)
            contentType(ContentType.Application.Json)
        }
    }

    override suspend fun cabinetLogin(url: String, authRequest: AuthRequest): HttpResponse =
        ktor.ktorHttpClient.post {
            url("${url}v1/CabinetLogin")
            setBody(authRequest)
            contentType(ContentType.Application.Json)
        }

    override suspend fun loadDivisionsForFilter(currentCabinetId: Long, server: String): HttpResponse =
        createClient(currentCabinetId).get {
            url("${server}v2/departments/filter")
            contentType(ContentType.Application.Json)
        }

    override suspend fun loadOutletsForFilter(
        departmentId: Long?, currentCabinetId: Long, server: String
    ): HttpResponse = createClient(currentCabinetId).get {
        url("${server}v2/outlets/filter")
        parameter("model.departmentId", departmentId)
        contentType(ContentType.Application.Json)
    }

    companion object {
        private const val AUTH_ERROR = 401
    }
}