package ru.taxcom.widget.manager

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import ru.taxcom.widget.utils.ACTION_WIDGET_UPDATE
import ru.taxcom.widget.utils.WIDGET_ID_KEY
import ru.taxcom.widget.utils.WIDGET_PREFIX_KEY
import ru.taxcom.widget.utils.getWidgetClass

class WidgetManager(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val id = inputData.getString(WIDGET_ID_KEY)
        val prefix = inputData.getString(WIDGET_PREFIX_KEY) ?: ""
        val widgetToUpdate = prefix.getWidgetClass()

        val updateWidget = Intent(context, widgetToUpdate::class.java)
        updateWidget.action = "$ACTION_WIDGET_UPDATE:$id$prefix"

        context.sendBroadcast(updateWidget)
        return Result.success()
    }
}