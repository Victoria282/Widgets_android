package ru.taxcom.widget.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.taxcom.cashdeskkit.data.user.WidgetEntity
import ru.taxcom.cashdeskkit.database.CashdeskDatabase
import ru.taxcom.widget.data.ConfigureData
import ru.taxcom.widget.data.WidgetInfo
import ru.taxcom.widget.manager.WidgetManager
import ru.taxcom.widget.utils.WIDGET_ID_KEY
import ru.taxcom.widget.utils.WIDGET_PREFIX_KEY
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WidgetStorageImpl @Inject constructor(
    private val database: CashdeskDatabase,
    private val ctx: Context
) : WidgetStorage {

    private val sharedPreferences = ctx.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    override fun saveWidget(id: String, info: WidgetInfo) {
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString("$WIDGET_ID_KEY:$id", Json.encodeToString(info)).apply()
    }

    override fun getWidget(id: String): WidgetInfo? = try {
        Json.decodeFromString(sharedPreferences.getString("$WIDGET_ID_KEY:$id", null) ?: "")
    } catch (e: Exception) {
        null
    }

    override fun saveConfiguration(config: ConfigureData?, widgetId: String) {
        database.widgetDao().insertWidget(
            WidgetEntity(
                widgetId = widgetId,
                prefix = config?.widgetPrefix ?: return,
                theme = config.theme.toString()
            )
        )
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        val configurationString = Json.encodeToString(config)
        editor.putString(widgetId, configurationString).apply()
    }

    override fun getConfiguration(widgetId: String): ConfigureData? = try {
        Json.decodeFromString(sharedPreferences.getString(widgetId, null) ?: "")
    } catch (e: Exception) {
        ConfigureData()
    }

    override fun clearConfiguration(widgetId: String) {
        sharedPreferences.edit().remove("$WIDGET_ID_KEY:$widgetId").apply()
        sharedPreferences.edit().remove(widgetId).apply()
        database.widgetDao().deleteWidget(widgetId)
        unRegisterUpdateWidget(widgetId)
    }

    override fun unRegisterUpdateWidget(widgetId: String) {
        WorkManager.getInstance(ctx).cancelUniqueWork("$UNIQUE_WORK_MANAGER_NAME$widgetId")
    }

    override fun registerUpdateWidget(widgetId: String, widgetPrefix: String?) {
        val data = Data.Builder()
        data.putString(WIDGET_ID_KEY, widgetId)
        data.putString(WIDGET_PREFIX_KEY, widgetPrefix)

        val update = OneTimeWorkRequestBuilder<WidgetManager>()
        update.setInitialDelay(MINUTES_30, TimeUnit.MINUTES)
        update.setInputData(data.build())
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "$UNIQUE_WORK_MANAGER_NAME$widgetId",
            ExistingWorkPolicy.REPLACE,
            update.build()
        )
    }

    companion object {
        private const val UNIQUE_WORK_MANAGER_NAME = "widget_worker_manager"
        private const val PREFERENCE_NAME = "WIDGET_PREFERENCE_NAME"
        private const val MINUTES_30 = 30L
    }
}