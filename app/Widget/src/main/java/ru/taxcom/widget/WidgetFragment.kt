package ru.taxcom.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.text.format.DateUtils
import android.view.View
import android.widget.RemoteViews
import dagger.android.AndroidInjection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.taxcom.cashdeskkit.database.CashdeskDatabase
import ru.taxcom.cashdeskkit.repositories.user.UserRepository
import ru.taxcom.logginmodule.screens.activities.SelectLoginTypeActivity
import ru.taxcom.taxcomkit.data.resource.ResourceStatus
import ru.taxcom.taxcomkit.utils.factories.scopeMainProduce
import ru.taxcom.taxcomkit.utils.formatter.MoneyFormatter.formatMoney
import ru.taxcom.taxcomkit.utils.formatter.MoneyFormatter.formatNumber
import ru.taxcom.widget.data.ConfigureData
import ru.taxcom.widget.data.WidgetInfo
import ru.taxcom.widget.data.WidgetSize
import ru.taxcom.widget.data.WidgetTheme
import ru.taxcom.widget.presenter.WidgetPresenter
import ru.taxcom.widget.storage.WidgetStorage
import ru.taxcom.widget.utils.ACTION_WIDGET_INITIAL
import ru.taxcom.widget.utils.ACTION_WIDGET_LOGOUT
import ru.taxcom.widget.utils.ACTION_WIDGET_SET
import ru.taxcom.widget.utils.ACTION_WIDGET_SETTING_START
import ru.taxcom.widget.utils.ACTION_WIDGET_UPDATE
import ru.taxcom.widget.utils.DEFAULT_MONEY_VALUE
import ru.taxcom.widget.utils.EMPTY_MONEY
import ru.taxcom.widget.utils.FLAG
import ru.taxcom.widget.utils.NO_ACCESS_FLAG
import ru.taxcom.widget.utils.PROGRESS_KEY
import ru.taxcom.widget.utils.WIDGET_ACTION_DIVIDER_KEY
import ru.taxcom.widget.utils.WIDGET_SIZE_PREFIX_LENGTH
import ru.taxcom.widget.utils.cabinetTitle
import ru.taxcom.widget.utils.getClass
import ru.taxcom.widget.utils.getLayout
import ru.taxcom.widget.utils.getWidgetClass
import ru.taxcom.widget.utils.getWidgetPrefix
import ru.taxcom.widget.utils.getWidgetTitle
import ru.taxcom.widget.utils.hasPrefix
import ru.taxcom.widget.utils.views
import javax.inject.Inject

open class WidgetFragment : AppWidgetProvider() {

    @Inject
    lateinit var database: CashdeskDatabase

    @Inject
    lateinit var widgetStorage: WidgetStorage

    @Inject
    lateinit var widgetPresenter: WidgetPresenter

    @Inject
    lateinit var userRepository: UserRepository

    private var prefix: String? = null

    override fun onUpdate(ctx: Context, widgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        AndroidInjection.inject(this, ctx)
        super.onUpdate(ctx, widgetManager, appWidgetIds)
        appWidgetIds.forEach {
            val widget = widgetStorage.getWidget(it.toString()) ?: return
            loadWidget(ctx, it.toString(), widget.widgetSize?.getWidgetPrefix() ?: return@forEach)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)
        super.onReceive(context, intent)
        var action = intent.action ?: ""
        var id: String? = null
        if (action.contains(WIDGET_ACTION_DIVIDER_KEY)) {
            val index = action.indexOf(WIDGET_ACTION_DIVIDER_KEY) + 1
            id = action.substring(index, action.length - WIDGET_SIZE_PREFIX_LENGTH)
            action = action.replace(":$id", "")
        }
        if (action.hasPrefix()) {
            prefix = action.substring(action.length - WIDGET_SIZE_PREFIX_LENGTH)
            action = action.replace("$prefix", "")
        }
        when (action) {
            ACTION_WIDGET_LOGOUT -> logoutWidget(context)
            ACTION_WIDGET_UPDATE -> updateWidget(context, id ?: return, prefix ?: return)
            ACTION_WIDGET_SET -> loadWidget(context, id ?: return, prefix ?: return)
            ACTION_WIDGET_INITIAL -> loadWidget(context, id ?: return, prefix ?: return)
        }
    }

    private fun updateWidget(ctx: Context, id: String, prefix: String) = scopeMainProduce().launch {
        val widget = widgetStorage.getWidget(id) ?: return@launch
        val configure = widgetStorage.getConfiguration(id) ?: return@launch

        widgetStorage.registerUpdateWidget(id, prefix)
        val appWidgetManager = AppWidgetManager.getInstance(ctx)
        val views = ctx.views(widget.transparency, prefix.getLayout(configure.theme))
        ctx.setWidgetCallback(id, views, prefix)

        setProgressState(views)
        appWidgetManager.updateAppWidget(id.toInt(), views)
        delay(1 * DateUtils.SECOND_IN_MILLIS)
        widgetPresenter.loadWidgetState(id)
    }

    private fun loadWidget(ctx: Context, id: String, prefix: String) = scopeMainProduce().launch {
        val widget = widgetStorage.getWidget(id) ?: return@launch
        val configure = widgetStorage.getConfiguration(id) ?: return@launch

        val appWidgetManager = AppWidgetManager.getInstance(ctx)
        val views = ctx.views(widget.transparency, prefix.getLayout(configure.theme))
        ctx.setWidgetCallback(id, views, prefix)

        if (configure.cabinet?.id?.toLong() == null) {
            setAuthState(ctx, views, id)
            appWidgetManager.updateAppWidget(id.toInt(), views)
            return@launch
        }

        if (configure.departmentAndOutletAccess == NO_ACCESS_FLAG) {
            setNoAccessState(ctx, views, configure)
            appWidgetManager.updateAppWidget(id.toInt(), views)
            return@launch
        }

        val userLogin = try {
            userRepository.currentUser.value?.data?.login
        } catch (_: Exception) {
            null
        }

        if (userLogin != null && configure.login != userLogin) {
            setNotConfiguredState(id, ctx, views)
            appWidgetManager.updateAppWidget(id.toInt(), views)
            return@launch
        }

        widgetStorage.registerUpdateWidget(id, prefix)

        if (widget.status == ResourceStatus.ERROR) setErrorState(ctx, views, id)
        else setContentState(ctx, widget, views)
        appWidgetManager.updateAppWidget(id.toInt(), views)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { widgetStorage.clearConfiguration(it.toString()) }
    }

    private fun logoutWidget(ctx: Context) {
        if (database.widgetDao().getWidgets().isEmpty()) return
        val appWidgetManager = AppWidgetManager.getInstance(ctx)
        database.widgetDao().getWidgets().forEach {
            val configureWidget = widgetStorage.getConfiguration(it.widgetId)
            val views = ctx.views(
                configureWidget?.transparency ?: return,
                it.prefix.getLayout(WidgetTheme.valueOf(it.theme))
            )
            setAuthState(ctx, views, it.widgetId)
            appWidgetManager.updateAppWidget(it.widgetId.toInt(), views)
        }
    }

    private fun setProgressState(views: RemoteViews) = with(views) {
        setViewVisibility(R.id.progress_bar, View.VISIBLE)
        setViewVisibility(R.id.reload_icon, View.GONE)
        setViewVisibility(R.id.blur_group, View.GONE)
    }

    private fun setNotConfiguredState(id: String, ctx: Context, views: RemoteViews) = with(views) {
        setTextViewText(R.id.cabinet_title, ctx.cabinetTitle())
        setViewVisibility(R.id.reload_icon, View.INVISIBLE)
        setViewVisibility(R.id.date_value, View.INVISIBLE)
        setViewVisibility(R.id.date_icon, View.INVISIBLE)
        setViewVisibility(R.id.date_group, View.VISIBLE)
        setViewVisibility(R.id.content, View.INVISIBLE)
        setViewVisibility(R.id.progress_bar, View.GONE)
        setViewVisibility(R.id.status_value, View.GONE)
        setViewVisibility(R.id.auth_button, View.GONE)
        setViewVisibility(R.id.blur_group, View.GONE)
        setViewVisibility(R.id.message, View.VISIBLE)
        setTextViewText(R.id.message, ctx.getString(R.string.widget_not_configured_subtitle))
        widgetStorage.unRegisterUpdateWidget(id)
    }

    private fun setErrorState(ctx: Context, views: RemoteViews, id: String) =
        with(views) {
            val config = widgetStorage.getConfiguration(id) ?: return@with
            val widgetData = widgetStorage.getWidget(id)
            setTextViewText(
                R.id.cabinet_title,
                ctx.cabinetTitle(config.getWidgetTitle())
            )
            setViewVisibility(R.id.content, View.VISIBLE)
            setViewVisibility(R.id.message, View.GONE)
            setViewVisibility(R.id.auth_button, View.GONE)
            setViewVisibility(R.id.progress_bar, View.GONE)
            setViewVisibility(R.id.reload_icon, View.VISIBLE)
            setViewVisibility(R.id.date_icon, View.VISIBLE)
            setViewVisibility(R.id.date_value, View.VISIBLE)
            setViewVisibility(R.id.blur_group, View.GONE)
            setViewVisibility(R.id.date_group, View.VISIBLE)
            setTextViewText(R.id.date_value, widgetData?.date)
            widgetData?.let {
                if (config.widgetSize == WidgetSize.SMALL)
                    setTextViewText(
                        R.id.date_value, ctx.getString(R.string.widget_connection_error)
                    )
                else setViewVisibility(R.id.status_value, View.VISIBLE)
            } ?: run {
                setTextViewText(R.id.message, ctx.getString(R.string.widget_error))
                setViewVisibility(R.id.message, View.VISIBLE)
            }
        }

    private fun setNoAccessState(ctx: Context, views: RemoteViews, config: ConfigureData) =
        with(views) {
            setTextViewText(R.id.cabinet_title, ctx.cabinetTitle(config.getWidgetTitle()))
            setTextViewText(R.id.message, ctx.getString(R.string.widget_no_access_message))
            setViewVisibility(R.id.message, View.VISIBLE)
            setViewVisibility(R.id.auth_button, View.GONE)
            setViewVisibility(R.id.content, View.INVISIBLE)
            setViewVisibility(R.id.status_value, View.GONE)
            setViewVisibility(R.id.blur_group, View.GONE)
            setViewVisibility(R.id.progress_bar, View.GONE)
            setViewVisibility(R.id.date_group, View.VISIBLE)
            setViewVisibility(R.id.date_icon, View.INVISIBLE)
            setViewVisibility(R.id.date_value, View.INVISIBLE)
            setViewVisibility(R.id.reload_icon, View.INVISIBLE)
        }

    private fun setAuthState(ctx: Context, views: RemoteViews, id: String) = with(views) {
        setTextViewText(R.id.message, ctx.getString(R.string.widget_cash_desk_auth_title))
        val root = Intent(ctx, SelectLoginTypeActivity::class.java)
        val auth = PendingIntent.getActivity(ctx, 0, root, FLAG)
        setTextViewText(R.id.cabinet_title, ctx.cabinetTitle())
        setViewVisibility(R.id.date_group, View.INVISIBLE)
        setViewVisibility(R.id.auth_button, View.VISIBLE)
        setViewVisibility(R.id.status_value, View.GONE)
        setViewVisibility(R.id.content, View.INVISIBLE)
        setOnClickPendingIntent(R.id.auth_button, auth)
        setViewVisibility(R.id.progress_bar, View.GONE)
        setViewVisibility(R.id.reload_icon, View.GONE)
        setViewVisibility(R.id.blur_group, View.GONE)
        setViewVisibility(R.id.message, View.VISIBLE)
        widgetStorage.unRegisterUpdateWidget(id)
    }

    private fun setContentState(ctx: Context, data: WidgetInfo?, views: RemoteViews) = with(views) {
        setViewVisibility(R.id.blur_group, View.GONE)
        setViewVisibility(R.id.progress_bar, View.GONE)
        setViewVisibility(R.id.reload_icon, View.VISIBLE)
        setViewVisibility(R.id.auth_button, View.GONE)
        setViewVisibility(R.id.message, View.GONE)
        setViewVisibility(R.id.content, View.VISIBLE)
        setViewVisibility(R.id.date_group, View.VISIBLE)
        setViewVisibility(R.id.status_value, View.GONE)
        setViewVisibility(R.id.date_icon, View.VISIBLE)
        setViewVisibility(R.id.date_value, View.VISIBLE)
        data ?: return
        setTextViewText(R.id.refund_value, formatMoney(data.refunds ?: EMPTY_MONEY))
        setTextViewText(R.id.revenue_value, formatMoney(data.revenue ?: EMPTY_MONEY))
        setTextViewText(R.id.avg_receipts_value, formatMoney(data.avg ?: EMPTY_MONEY))
        setTextViewText(R.id.cabinet_title, ctx.cabinetTitle(data.widgetTitle))
        setTextViewText(R.id.count_receipts_value, data.receipts.toString())
        setTextViewText(R.id.date_value, data.date)

        setViewVisibility(R.id.refund_group, data.refundSwitchedOn)
        setViewVisibility(R.id.revenue_group, data.revenueSwitchedOn)
        setViewVisibility(R.id.receipts_group, data.countReceiptsSwitchedOn)
        setViewVisibility(R.id.receipts_avg_group, data.avgReceiptsSwitchedOn)

        if (data.widgetSize != WidgetSize.LARGE) return

        if (data.cardPercent == 0L && data.cashPercent == 0L) {
            setViewVisibility(R.id.progress_view_empty, View.VISIBLE)
            setViewVisibility(R.id.progress_view, View.GONE)
        } else {
            setViewVisibility(R.id.progress_view_empty, View.GONE)
            setViewVisibility(R.id.progress_view, View.VISIBLE)
            setInt(
                R.id.progress_view,
                PROGRESS_KEY,
                data.cardPercent?.toInt() ?: DEFAULT_MONEY_VALUE
            )
        }
        setTextViewText(
            R.id.card_percent_value,
            ctx.getString(R.string.widget_percent_value, data.cardPercent)
        )
        setTextViewText(
            R.id.cash_percent_value,
            ctx.getString(R.string.widget_percent_value, data.cashPercent)
        )
        setTextViewText(
            R.id.card_value, formatNumber(data.cardValue ?: EMPTY_MONEY)
        )
        setTextViewText(
            R.id.cash_value, formatNumber(data.cashValue ?: EMPTY_MONEY)
        )
    }

    private fun Context.setWidgetCallback(id: String, views: RemoteViews, prefix: String) {
        val settingsIntent = Intent(this, prefix.getClass())
        settingsIntent.action = "$ACTION_WIDGET_SETTING_START:$id"
        settingsIntent.addFlags(FLAG_ACTIVITY_CLEAR_TASK)
        val settings = PendingIntent.getActivity(this, 0, settingsIntent, FLAG)

        val updateIntent = Intent(this, prefix.getWidgetClass()::class.java)
        updateIntent.action = "$ACTION_WIDGET_UPDATE:$id$prefix"
        val update = PendingIntent.getBroadcast(this, 0, updateIntent, FLAG)

        views.setOnClickPendingIntent(R.id.reload_icon, update)
        views.setOnClickPendingIntent(R.id.settings_icon, settings)
        views.setOnClickPendingIntent(R.id.reload_shimmer_icon, update)
    }
}