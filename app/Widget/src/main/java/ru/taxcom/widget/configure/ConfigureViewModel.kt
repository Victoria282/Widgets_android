package ru.taxcom.widget.configure

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.taxcom.cashdeskkit.data.filter.ItemOfFilterList
import ru.taxcom.cashdeskkit.data.statistics.request.StatisticFilter
import ru.taxcom.cashdeskkit.data.statistics.response.Statistics
import ru.taxcom.cashdeskkit.repositories.user.UserRepository
import ru.taxcom.cashdeskkit.utils.toPresentation
import ru.taxcom.cashdeskreport.controller.ReportController
import ru.taxcom.cashdeskreport.data.filter.Filter
import ru.taxcom.cashdeskreport.data.filter.Period
import ru.taxcom.cashdeskreport.data.filter.getTime
import ru.taxcom.taxcomkit.data.resource.Resource
import ru.taxcom.taxcomkit.data.resource.ResourceStatus
import ru.taxcom.taxcomkit.data.user.UserData
import ru.taxcom.taxcomkit.repository.settings.AppTheme
import ru.taxcom.taxcomkit.repository.settings.PinCodeRepository
import ru.taxcom.widget.data.ConfigureData
import ru.taxcom.widget.data.WidgetSize
import ru.taxcom.widget.data.WidgetTheme
import ru.taxcom.widget.repository.WidgetRepository
import ru.taxcom.widget.storage.WidgetStorage
import ru.taxcom.widget.utils.convert
import ru.taxcom.widget.utils.getItemId
import ru.taxcom.widget.utils.getItemType
import ru.taxcom.widget.utils.needInitCheckboxState
import javax.inject.Inject

class ConfigureViewModel @Inject constructor(
    private val pinCodeRepository: PinCodeRepository,
    private val reportController: ReportController,
    private val widgetRepository: WidgetRepository,
    private val userRepository: UserRepository,
    private val widgetStorage: WidgetStorage
) : ViewModel() {

    private val _periodFilter: MutableStateFlow<List<ItemOfFilterList>?> = MutableStateFlow(null)
    val periodFilter get() = _periodFilter.asStateFlow()

    private val _widgetData: MutableStateFlow<ConfigureData?> =
        MutableStateFlow(ConfigureData())
    val widgetData get() = _widgetData.asStateFlow()

    private val _userData: MutableStateFlow<Resource<UserData?>?> = MutableStateFlow(null)
    val userData get() = _userData.asStateFlow()

    private val _isLoggedIn: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val isNeedAuth get() = _isLoggedIn.asStateFlow()

    private val _widgetStatistic = MutableStateFlow<Resource<Statistics>?>(null)
    val widgetStatistic get() = _widgetStatistic.asStateFlow()

    private val _appTheme = MutableLiveData<AppTheme?>()
    val appTheme get() = _appTheme

    private var widgetId: Int? = null

    init {
        getAppTheme()
        viewModelScope.launch {
            _isLoggedIn.emit(widgetRepository.isLoggedIn())
        }
    }

    private fun getAppTheme() = viewModelScope.launch {
        pinCodeRepository.getAppTheme().collectLatest {
            _appTheme.value = it
        }
    }

    fun restoreConfig(widgetId: Int, widgetSize: WidgetSize) = viewModelScope.launch {
        if (this@ConfigureViewModel.widgetId == widgetId) return@launch
        this@ConfigureViewModel.widgetId = widgetId
        val widgetData = getWidgetConfiguration()
        widgetData?.widgetSize = widgetSize
        widgetData?.needInitCheckboxState()
        if (_isLoggedIn.value == false) return@launch
        if (widgetData?.login != null && widgetData.login != widgetRepository.getCurrentCabinet().login) {
            widgetData.division = null
            widgetData.cabinet = null
            widgetData.outlet = null
        } else try {
            if (widgetData?.cabinet == null) widgetData?.cabinet = getCurrentAppCabinet()
            val cabinetId = widgetData?.cabinet?.id ?: return@launch
            val cabinetInfo = userRepository.getCabinetUser(cabinetId.toLong())
            widgetData.departmentAndOutletAccess = cabinetInfo?.departmentAndOutletAccess
            _userData.emit(Resource.success(cabinetInfo))
        } catch (_: Exception) {
        }

        _widgetData.value = widgetData
        setPeriodSelected(widgetData?.period ?: Period.TODAY)
        if (_isLoggedIn.value == false) return@launch
        getPeriodList()
    }

    private fun getWidgetConfiguration() = widgetStorage.getConfiguration(widgetId.toString())

    fun setFilterType(filter: Filter) = when (filter.filterType) {
        Filter.FilterType.DIVISIONS -> {
            removeOutlet()
            _widgetData.value = _widgetData.value?.copy(
                division = filter.listOfFilter.first()
            )
        }

        Filter.FilterType.OUTLETS -> _widgetData.value = _widgetData.value?.copy(
            outlet = filter.listOfFilter.first()
        )

        Filter.FilterType.CABINET -> {
            val cabinetId = filter.listOfFilter.first().id
            loginCabinet(cabinetId.toLong())
            removeDivision()
            removeOutlet()
            _widgetData.value = _widgetData.value?.copy(
                cabinet = filter.listOfFilter.first()
            )
        }

        else -> {}
    }

    fun setPeriodSelected(period: Period) {
        _widgetData.value = _widgetData.value?.copy(
            period = period
        )
    }

    fun getCabinetFilter() = Filter(
        filterType = Filter.FilterType.CABINET,
        listOfFilter = listOf((_widgetData.value?.cabinet ?: getCurrentAppCabinet()))
    )

    fun getDivisionFilter(): Filter = Filter(filterType = Filter.FilterType.DIVISIONS,
        listOfFilter = _widgetData.value?.division?.let { listOf(it) } ?: listOf())

    fun getOutletFilter(): Filter = Filter(filterType = Filter.FilterType.OUTLETS,
        listOfFilter = _widgetData.value?.outlet?.let { listOf(it) } ?: listOf(),
        departmentId = _widgetData.value?.division?.id?.toLongOrNull())

    fun updateTransparency(progress: Int) {
        _widgetData.value = _widgetData.value?.copy(transparency = progress)
    }

    fun updateRefundSwitchedOn(isChecked: Boolean) {
        _widgetData.value = _widgetData.value?.copy(refundSwitchedOn = isChecked)
    }

    fun updateRevenueSwitchedOn(isChecked: Boolean) {
        _widgetData.value = _widgetData.value?.copy(revenueSwitchedOn = isChecked)
    }

    fun updateCountReceiptsSwitchedOn(isChecked: Boolean) {
        _widgetData.value = _widgetData.value?.copy(countReceiptsSwitchedOn = isChecked)
    }

    fun updateAvgReceiptsSwitchedOn(isChecked: Boolean) {
        _widgetData.value = _widgetData.value?.copy(avgReceiptsSwitchedOn = isChecked)
    }

    fun updateTheme(theme: WidgetTheme) {
        _widgetData.value = _widgetData.value?.copy(theme = theme)
    }

    fun removeDivision() {
        _widgetData.value = _widgetData.value?.copy(division = null)
        removeOutlet()
    }

    fun removeOutlet() {
        _widgetData.value = _widgetData.value?.copy(outlet = null)
    }

    fun removeCabinet() {
        _widgetData.value = _widgetData.value?.copy(cabinet = null)
        removeDivision()
        removeOutlet()
    }

    fun saveWidgetConfiguration(widgetId: String, widgetPrefix: String) = viewModelScope.launch {
        val configuration = _widgetData.value
        configuration?.widgetId = widgetId
        configuration?.widgetPrefix = widgetPrefix
        configuration?.login = widgetRepository.userLogin()
        loadStatistic(configuration ?: return@launch)
    }

    private fun loadStatistic(configuration: ConfigureData) = viewModelScope.launch {
        val cabinetId = configuration.cabinet?.id?.toLong() ?: return@launch
        val params = createStatisticParams(configuration)
        val user = userRepository.getCabinetUser(cabinetId)
        user ?: return@launch
        _widgetStatistic.emit(Resource.loading())
        val result = widgetRepository.loadStatistic(params, user, cabinetId)
        _widgetStatistic.emit(result)
    }

    fun saveStatistic(data: Statistics?) {
        val result = data?.toPresentation()
        val configure = _widgetData.value ?: return
        val widget =
            result?.convert(configure, ResourceStatus.SUCCESS, configure.departmentAndOutletAccess)
                ?: return
        widgetStorage.saveWidget(configure.widgetId ?: return, widget)
        widgetStorage.saveConfiguration(configure, configure.widgetId ?: return)
    }

    private fun createStatisticParams(data: ConfigureData): StatisticFilter {
        val itemType = data.getItemType()
        return StatisticFilter(
            dateTo = data.period?.getTime()?.to,
            dateFrom = data.period?.getTime()?.from,
            itemType = itemType.id,
            itemId = itemType.getItemId(data)?.toString()
        )
    }

    fun isEnableToSwitchToggle(): Boolean {
        val widgetData = _widgetData.value
        var countActive = 0
        countActive += if (widgetData?.countReceiptsSwitchedOn == true) 1 else 0
        countActive += if (widgetData?.avgReceiptsSwitchedOn == true) 1 else 0
        countActive += if (widgetData?.revenueSwitchedOn == true) 1 else 0
        countActive += if (widgetData?.refundSwitchedOn == true) 1 else 0

        return if (widgetData?.widgetSize == WidgetSize.SMALL) countActive <= 2
        else countActive <= 3
    }

    private fun loginCabinet(cabinetId: Long) = viewModelScope.launch {
        _userData.value = Resource.loading()
        val result = try {
            widgetRepository.loginCabinet(cabinetId)
        } catch (e: Exception) {
            Resource.error()
        }
        _userData.emit(result)
        _widgetData.value = _widgetData.value?.copy(
            departmentAndOutletAccess = result.data?.departmentAndOutletAccess
        )
    }

    private fun getPeriodList() = viewModelScope.launch {
        _periodFilter.value = reportController.getFilterList(Filter.FilterType.PERIOD).data
    }

    private fun getCurrentAppCabinet() = ItemOfFilterList(
        id = widgetRepository.getCurrentCabinet().currentCabinetId.toString(),
        title = widgetRepository.getCurrentCabinet().shortName
    )
}