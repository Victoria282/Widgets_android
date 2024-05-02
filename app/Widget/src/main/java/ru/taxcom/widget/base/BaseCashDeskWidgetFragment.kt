package ru.taxcom.widget.base

import android.content.Context
import androidx.viewbinding.ViewBinding
import ru.taxcom.cashdeskkit.ui.fragments.base.BaseCashdeskFragment
import ru.taxcom.widget.navigation.InnerNavigator

abstract class BaseCashDeskWidgetFragment<T : ViewBinding> : BaseCashdeskFragment<T>() {

    internal var innerNavigator: InnerNavigator? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        innerNavigator = context as? InnerNavigator
    }

    override fun onDetach() {
        super.onDetach()
        innerNavigator = null
    }
}