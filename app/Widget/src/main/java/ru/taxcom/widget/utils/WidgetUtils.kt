package ru.taxcom.widget.utils

import android.app.PendingIntent
import android.content.Context
import android.widget.RemoteViews
import androidx.fragment.app.Fragment
import ru.taxcom.taxcomkit.utils.getArgument
import ru.taxcom.widget.R
import ru.taxcom.widget.data.ConfigureData
import ru.taxcom.widget.data.WidgetSize
import ru.taxcom.widget.data.WidgetState
import ru.taxcom.widget.data.WidgetTheme
import ru.taxcom.widget.ui.LargeCashDeskWidget
import ru.taxcom.widget.ui.MediumCashDeskWidget
import ru.taxcom.widget.ui.SmallCashDeskWidget
import ru.taxcom.widget.ui.activity.CashDeskLargeWidgetActivity
import ru.taxcom.widget.ui.activity.CashDeskMediumWidgetActivity
import ru.taxcom.widget.ui.activity.CashDeskSmallWidgetActivity

internal fun calculateTransparency(transparency: Int): Int = 255 - transparency * 255 / 100

internal fun Context.views(transparency: Int = DEFAULT_TRANSPARENCY, layout: Int): RemoteViews =
    RemoteViews(this.packageName, layout).apply {
        setInt(R.id.root, TRANSPARENCY_SYSTEM_METHOD_KEY, calculateTransparency(transparency))
        setInt(R.id.footer, TRANSPARENCY_SYSTEM_METHOD_KEY, calculateTransparency(transparency))
    }

internal fun Fragment.widgetSize() =
    getArgument<WidgetSize>(WIDGET_SIZE_KEY, false) ?: WidgetSize.SMALL

internal fun Fragment.getWidgetClass() = when (widgetSize()) {
    WidgetSize.MEDIUM -> MediumCashDeskWidget::class.java
    WidgetSize.SMALL -> SmallCashDeskWidget::class.java
    WidgetSize.LARGE -> LargeCashDeskWidget::class.java
}

internal fun WidgetSize.getWidgetPrefix(): String = when (this) {
    WidgetSize.SMALL -> WIDGET_S_SIZE_PREFIX
    WidgetSize.LARGE -> WIDGET_L_SIZE_PREFIX
    WidgetSize.MEDIUM -> WIDGET_M_SIZE_PREFIX
}

fun ConfigureData.getItemType(): WidgetState =
    if (this.division?.id != null) WidgetState.DIVISION
    else if (this.outlet?.id != null) WidgetState.OUTLET
    else WidgetState.CABINET

fun ConfigureData.getWidgetTitle(): String? =
    this.outlet?.title ?: this.division?.title ?: this.cabinet?.title

fun WidgetState.getItemId(widgetData: ConfigureData): Int? = when (this) {
    WidgetState.OUTLET -> widgetData.outlet?.id?.toIntOrNull()
    WidgetState.DIVISION -> widgetData.division?.id?.toIntOrNull()
    else -> widgetData.cabinet?.id?.toIntOrNull()
}

fun ConfigureData.needInitCheckboxState() {
    if (this.widgetSize == WidgetSize.LARGE) return
    if (this.revenueSwitchedOn == true || this.refundSwitchedOn == true || this.countReceiptsSwitchedOn == true || this.avgReceiptsSwitchedOn == true)
        return
    this.revenueSwitchedOn = true
    this.avgReceiptsSwitchedOn = true
    if (this.widgetSize == WidgetSize.SMALL) return
    this.countReceiptsSwitchedOn = true
}

fun Context.cabinetTitle(title: String? = null) = title.takeIf { it?.isNotEmpty() == true }
    ?: this.getString(R.string.widget_default_title)

fun String.getLayout(theme: WidgetTheme): Int = when (this) {
    WIDGET_S_SIZE_PREFIX -> if (theme == WidgetTheme.LIGHT)
        R.layout.small_cash_desk_widget
    else R.layout.small_cash_desk_widget_dark

    WIDGET_M_SIZE_PREFIX -> if (theme == WidgetTheme.LIGHT)
        R.layout.medium_cash_desk_widget
    else R.layout.medium_cash_desk_widget_dark

    else -> if (theme == WidgetTheme.LIGHT)
        R.layout.large_cash_desk_widget
    else R.layout.large_cash_desk_widget_dark
}

fun String.getClass() = when (this) {
    WIDGET_S_SIZE_PREFIX -> CashDeskSmallWidgetActivity::class.java
    WIDGET_M_SIZE_PREFIX -> CashDeskMediumWidgetActivity::class.java
    else -> CashDeskLargeWidgetActivity::class.java
}

fun String.getWidgetClass() = when (this) {
    WIDGET_S_SIZE_PREFIX -> SmallCashDeskWidget()
    WIDGET_M_SIZE_PREFIX -> MediumCashDeskWidget()
    else -> LargeCashDeskWidget()
}

fun String.hasPrefix(): Boolean =
    this.contains(WIDGET_S_SIZE_PREFIX) || this.contains(WIDGET_M_SIZE_PREFIX) || this.contains(
        WIDGET_L_SIZE_PREFIX
    )

internal const val EMPTY_MONEY = 0.0
internal const val NO_ACCESS_FLAG = 2
internal const val DEFAULT_MONEY_VALUE = 50
internal const val DEFAULT_TRANSPARENCY = 15
internal const val WIDGET_S_SIZE_PREFIX = "_s"
internal const val WIDGET_M_SIZE_PREFIX = "_m"
internal const val WIDGET_L_SIZE_PREFIX = "_l"
internal const val PROGRESS_KEY = "setProgress"
internal const val WIDGET_SIZE_PREFIX_LENGTH = 2
internal const val WIDGET_ACTION_DIVIDER_KEY = ":"
internal const val WIDGET_ID_KEY = "widget_id_key"
internal const val WIDGET_SIZE_KEY = "widget_size_key"
internal const val WIDGET_PREFIX_KEY = "widget_prefix_key"
public const val ACTION_WIDGET_LOGOUT = "action.WIDGET_LOGOUT"
internal const val TRANSPARENCY_SYSTEM_METHOD_KEY = "setImageAlpha"
const val ACTION_WIDGET_SET = "android.appwidget.action.APPWIDGET_SET"
internal const val ACTION_WIDGET_UPDATE = "android.appwidget.action.APPWIDGET_UPDATE"
internal const val ACTION_WIDGET_INITIAL = "android.appwidget.action.APPWIDGET_INITIAL"
internal const val ACTION_WIDGET_SETTING_START = "android.appwidget.action.SETTING_START"
internal const val FLAG = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE