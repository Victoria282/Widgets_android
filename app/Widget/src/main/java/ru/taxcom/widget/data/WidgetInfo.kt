package ru.taxcom.widget.data

import android.view.View
import kotlinx.serialization.Serializable
import ru.taxcom.taxcomkit.data.resource.ResourceStatus
import ru.taxcom.widget.utils.DEFAULT_TRANSPARENCY

@Serializable
data class WidgetInfo(
    val avg: Double? = null,
    val date: String? = null,
    val receipts: Int? = null,
    val refunds: Double? = null,
    val revenue: Double? = null,
    val accessFlag: Int? = null,
    val cardValue: Double? = null,
    val cashValue: Double? = null,
    val cardPercent: Long? = null,
    val cashPercent: Long? = null,
    var widgetTitle: String? = null,
    val widgetSize: WidgetSize? = null,
    val status: ResourceStatus? = null,
    var refundSwitchedOn: Int = View.VISIBLE,
    var revenueSwitchedOn: Int = View.VISIBLE,
    val theme: WidgetTheme = WidgetTheme.LIGHT,
    val transparency: Int = DEFAULT_TRANSPARENCY,
    var avgReceiptsSwitchedOn: Int = View.VISIBLE,
    var countReceiptsSwitchedOn: Int = View.VISIBLE
)