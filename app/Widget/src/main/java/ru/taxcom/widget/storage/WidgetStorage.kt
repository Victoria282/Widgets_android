package ru.taxcom.widget.storage

import ru.taxcom.widget.data.ConfigureData
import ru.taxcom.widget.data.WidgetInfo

interface WidgetStorage {
    fun getWidget(id: String): WidgetInfo?
    fun clearConfiguration(widgetId: String)
    fun unRegisterUpdateWidget(widgetId: String)
    fun saveWidget(id: String, info: WidgetInfo)
    fun getConfiguration(widgetId: String): ConfigureData?
    fun saveConfiguration(config: ConfigureData?, widgetId: String)
    fun registerUpdateWidget(widgetId: String, widgetPrefix: String?)
}