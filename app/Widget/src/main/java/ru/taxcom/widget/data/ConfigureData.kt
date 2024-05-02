package ru.taxcom.widget.data

import kotlinx.serialization.Serializable
import ru.taxcom.cashdeskkit.data.filter.ItemOfFilterList
import ru.taxcom.cashdeskreport.data.filter.Period
import ru.taxcom.widget.utils.DEFAULT_TRANSPARENCY

@Serializable
data class ConfigureData(
    var login: String? = null,
    val period: Period? = null,
    var widgetId: String? = null,
    var widgetPrefix: String? = null,
    var widgetSize: WidgetSize? = null,
    val widgetState: WidgetState? = null,
    var outlet: ItemOfFilterList? = null,
    val refundSwitchedOn: Boolean? = null,
    var cabinet: ItemOfFilterList? = null,
    var division: ItemOfFilterList? = null,
    var revenueSwitchedOn: Boolean? = null,
    var avgReceiptsSwitchedOn: Boolean? = null,
    val theme: WidgetTheme = WidgetTheme.LIGHT,
    var departmentAndOutletAccess: Int? = null,
    var countReceiptsSwitchedOn: Boolean? = null,
    val transparency: Int = DEFAULT_TRANSPARENCY
)

enum class WidgetTheme { LIGHT, DARK }

enum class WidgetSize { SMALL, MEDIUM, LARGE }

enum class WidgetState(val id: Int) { CABINET(3), DIVISION(2), OUTLET(0) }