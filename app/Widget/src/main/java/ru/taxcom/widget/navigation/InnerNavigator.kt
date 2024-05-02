package ru.taxcom.widget.navigation

import ru.taxcom.cashdeskreport.data.filter.Filter
import ru.taxcom.taxcomkit.data.user.UserData
import ru.taxcom.taxcomkit.ui.basefragment.CommonNavigator

interface InnerNavigator : CommonNavigator {
    fun navToSelectListOfFilter(key: String, filter: Filter, userData: UserData? = null)
    fun navToLogin()
}