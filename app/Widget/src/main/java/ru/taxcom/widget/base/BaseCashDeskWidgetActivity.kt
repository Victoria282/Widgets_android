package ru.taxcom.widget.base

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import ru.taxcom.cashdeskreport.data.filter.Filter
import ru.taxcom.cashdeskreport.ui.fragment.SelectListOfFilterFragment
import ru.taxcom.logginmodule.screens.activities.SelectLoginTypeActivity
import ru.taxcom.taxcomkit.data.user.UserData
import ru.taxcom.taxcomkit.ui.baseactivity.TaxcomDaggerAppCompatActivity
import ru.taxcom.taxcomkit.utils.hideKeyboard
import ru.taxcom.widget.R
import ru.taxcom.widget.configure.ConfigureWidgetFragment
import ru.taxcom.widget.data.WidgetSize
import ru.taxcom.widget.databinding.WidgetActivityLayoutBinding
import ru.taxcom.widget.navigation.InnerNavigator

open class BaseCashDeskWidgetActivity : TaxcomDaggerAppCompatActivity(), InnerNavigator {

    private lateinit var binding: WidgetActivityLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = WidgetActivityLayoutBinding.inflate(layoutInflater)
        val view = binding.root
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > SINGLE_FRAGMENT_IN_BACK_STACK)
                    supportFragmentManager.popBackStack()
                else finish()
            }
        })
        setContentView(view)
    }

    internal fun createConfigure(size: WidgetSize, name: String? = null) = supportFragmentManager
        .beginTransaction()
        .replace(binding.widgetConfigure.id, ConfigureWidgetFragment.newInstance(size))
        .addToBackStack(name)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .commit()

    override fun navToBack(name: String?) {
        hideKeyboard()
        if (name == null) supportFragmentManager.popBackStack()
        else supportFragmentManager.popBackStack(name, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    override fun navToSelectListOfFilter(
        key: String, filter: Filter, userData: UserData?
    ) = SelectListOfFilterFragment.newInstance(
        key, filter, userData, isNeedFinishActivity = true
    ).navigateTo()

    override fun navToLogin() = SelectLoginTypeActivity.start(this@BaseCashDeskWidgetActivity)

    private fun Fragment.navigateTo(name: String? = null) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.widget_configure, this)
            .addToBackStack(name)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit()
    }

    companion object {
        private const val SINGLE_FRAGMENT_IN_BACK_STACK = 1
    }
}