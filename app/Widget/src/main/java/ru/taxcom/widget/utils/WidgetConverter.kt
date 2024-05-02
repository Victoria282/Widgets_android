package ru.taxcom.widget.utils

import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import ru.taxcom.cashdeskkit.data.statistics.response.Statistic
import ru.taxcom.taxcomkit.data.resource.ResourceStatus
import ru.taxcom.taxcomkit.utils.formatter.Formatter.SIMPLE_DATE_TIME
import ru.taxcom.taxcomkit.utils.formatter.Formatter.parseTimestampToDataTimePresent
import ru.taxcom.widget.data.ConfigureData
import ru.taxcom.widget.data.WidgetInfo
import ru.taxcom.widget.data.WidgetSize
import kotlin.math.roundToLong

fun Statistic.convert(configureData: ConfigureData, status: ResourceStatus, flag: Int? = null) =
    WidgetInfo(
        status = status,
        accessFlag = flag,
        avg = this.avgReceipt,
        receipts = this.receipts,
        revenue = this.incomeTotal,
        refunds = this.outcomeTotal,
        cashValue = this.incomeCash,
        cardValue = this.incomeCard,
        theme = configureData.theme,
        widgetSize = configureData.widgetSize,
        transparency = configureData.transparency,
        widgetTitle = configureData.getWidgetTitle(),
        date = currentWidgetDate(configureData.widgetSize),
        cardPercent = this.incomeCard?.percent(this.incomeTotal ?: EMPTY_MONEY) ?: 0,
        cashPercent = this.incomeCash?.percent(this.incomeTotal ?: EMPTY_MONEY) ?: 0,
        refundSwitchedOn = if (configureData.refundSwitchedOn == true || configureData.widgetSize == WidgetSize.LARGE)
            VISIBLE
        else {
            if (configureData.widgetSize == WidgetSize.SMALL) GONE
            else INVISIBLE
        },
        revenueSwitchedOn = if (configureData.revenueSwitchedOn == true || configureData.widgetSize == WidgetSize.LARGE)
            VISIBLE else {
            if (configureData.widgetSize == WidgetSize.SMALL) GONE
            else INVISIBLE
        },
        avgReceiptsSwitchedOn = if (configureData.avgReceiptsSwitchedOn == true || configureData.widgetSize == WidgetSize.LARGE)
            VISIBLE else {
            if (configureData.widgetSize == WidgetSize.SMALL) GONE
            else INVISIBLE
        },
        countReceiptsSwitchedOn = if (configureData.countReceiptsSwitchedOn == true || configureData.widgetSize == WidgetSize.LARGE)
            VISIBLE else {
            if (configureData.widgetSize == WidgetSize.SMALL) GONE
            else INVISIBLE
        }
    )

fun Double.percent(total: Double): Long? {
    if (total == EMPTY_MONEY) return 0L
    return ((this.toFloat() * 100f) / total.toFloat()).takeIf { it != 0F }?.roundToLong()
}

fun currentWidgetDate(widgetSize: WidgetSize?) = if (widgetSize == WidgetSize.SMALL)
    parseTimestampToDataTimePresent(System.currentTimeMillis(), pattern = SIMPLE_DATE_TIME)
else parseTimestampToDataTimePresent(System.currentTimeMillis())