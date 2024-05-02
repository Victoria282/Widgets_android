package ru.taxcom.widget.ui.activity

import android.os.Bundle
import ru.taxcom.widget.base.BaseCashDeskWidgetActivity
import ru.taxcom.widget.data.WidgetSize

class CashDeskMediumWidgetActivity : BaseCashDeskWidgetActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createConfigure(WidgetSize.MEDIUM)
    }
}