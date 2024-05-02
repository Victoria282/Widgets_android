package ru.taxcom.widget.presenter

import android.app.Application
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.taxcom.cashdeskkit.data.statistics.request.StatisticFilter
import ru.taxcom.cashdeskkit.data.statistics.response.Statistics
import ru.taxcom.cashdeskkit.repositories.user.UserRepository
import ru.taxcom.cashdeskkit.utils.toPresentation
import ru.taxcom.cashdeskreport.data.filter.getTime
import ru.taxcom.taxcomkit.data.resource.Resource
import ru.taxcom.taxcomkit.data.resource.ResourceStatus
import ru.taxcom.widget.data.ConfigureData
import ru.taxcom.widget.repository.WidgetRepository
import ru.taxcom.widget.storage.WidgetStorage
import ru.taxcom.widget.utils.ACTION_WIDGET_SET
import ru.taxcom.widget.utils.convert
import ru.taxcom.widget.utils.getItemId
import ru.taxcom.widget.utils.getItemType
import ru.taxcom.widget.utils.getWidgetClass
import javax.inject.Inject

class WidgetPresenterImpl @Inject constructor(
    private val widgetRepository: WidgetRepository,
    private val userRepository: UserRepository,
    private val widgetStorage: WidgetStorage,
    private val ctx: Application
) : WidgetPresenter {

    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + job)

    override fun loadWidgetState(widgetId: String) {
        coroutineScope.launch {
            val configure = widgetStorage.getConfiguration(widgetId)
            val params = getStatisticParams(widgetId)
            val cabinetId = configure?.cabinet?.id?.toLong() ?: return@launch

            try {
                val cabinetUser = userRepository.getCabinetUser(cabinetId)
                cabinetUser?.let {
                    val statistics = widgetRepository.loadStatistic(params, it, cabinetId)
                    val access = it.departmentAndOutletAccess
                    setStatistic(ctx, access, widgetId, statistics, configure)
                } ?: run {
                    val user = widgetRepository.loginCabinet(cabinetId)
                    if (user.status != ResourceStatus.SUCCESS) return@launch
                    val statistics = widgetRepository.loadStatistic(
                        params, user.data ?: return@run, cabinetId
                    )
                    val access = user.data?.departmentAndOutletAccess
                    setStatistic(ctx, access, widgetId, statistics, configure)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun setStatistic(
        ctx: Context, access: Int?, id: String, data: Resource<Statistics>, config: ConfigureData
    ) {
        val result = data.data?.toPresentation()
        val cachedWidget = widgetStorage.getWidget(id)
        val widget = result?.convert(config, data.status, access) ?: cachedWidget
            ?.copy(status = data.status, date = cachedWidget.date) ?: return
        widgetStorage.saveWidget(id, widget)
        val prefix = config.widgetPrefix ?: return
        val intent = Intent(ctx, prefix.getWidgetClass()::class.java)
        intent.action = "$ACTION_WIDGET_SET:$id${config.widgetPrefix}"
        ctx.sendBroadcast(intent)
    }

    private fun getStatisticParams(widgetId: String): StatisticFilter {
        val data = widgetStorage.getConfiguration(widgetId)
        val itemType = data?.getItemType()
        return StatisticFilter(
            dateTo = data?.period?.getTime()?.to,
            dateFrom = data?.period?.getTime()?.from,
            itemType = itemType?.id,
            itemId = itemType?.getItemId(data)?.toString()
        )
    }
}