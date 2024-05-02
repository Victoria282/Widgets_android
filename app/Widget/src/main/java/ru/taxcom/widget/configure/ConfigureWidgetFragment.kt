package ru.taxcom.widget.configure

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.taxcom.cashdeskkit.data.filter.ItemOfFilterList
import ru.taxcom.cashdeskreport.data.filter.Filter
import ru.taxcom.cashdeskreport.data.filter.Period
import ru.taxcom.cashdeskreport.extensions.addChips
import ru.taxcom.cashdeskreport.extensions.getName
import ru.taxcom.taxcomkit.data.resource.ResourceStatus
import ru.taxcom.taxcomkit.repository.settings.AppTheme
import ru.taxcom.taxcomkit.ui.snackbar.Snackbar
import ru.taxcom.widget.R
import ru.taxcom.widget.base.BaseCashDeskWidgetFragment
import ru.taxcom.widget.data.WidgetSize
import ru.taxcom.widget.data.WidgetTheme
import ru.taxcom.widget.databinding.WidgetConfigireLayoutBinding
import ru.taxcom.widget.utils.ACTION_WIDGET_INITIAL
import ru.taxcom.widget.utils.WIDGET_ACTION_DIVIDER_KEY
import ru.taxcom.widget.utils.WIDGET_SIZE_KEY
import ru.taxcom.widget.utils.getWidgetClass
import ru.taxcom.widget.utils.getWidgetPrefix
import ru.taxcom.widget.utils.widgetSize
import ru.taxcom.cashdeskreport.R as CashDeskR

class ConfigureWidgetFragment : BaseCashDeskWidgetFragment<WidgetConfigireLayoutBinding>() {

    private val viewModel: ConfigureViewModel by viewModels { viewModelFactory }

    private val widgetId: String?
        get() {
            val intent = activity?.intent
            if (intent == null || TextUtils.isEmpty(intent.action)) {
                activity?.finish()
                return null
            }
            var action = intent.action
            var id: String? = null
            if (action?.contains(WIDGET_ACTION_DIVIDER_KEY) == true) {
                val index = action.indexOf(WIDGET_ACTION_DIVIDER_KEY) + 1
                id = action.substring(index, action.length)
                action = action.replace(":$id", "")
            }
            when (action) {
                AppWidgetManager.ACTION_APPWIDGET_CONFIGURE -> {
                    val bundle = intent.extras
                    if (bundle != null) id = bundle.getInt(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID
                    ).toString()
                }
            }
            if (TextUtils.isEmpty(id)) {
                activity?.finish()
                return null
            }
            return id
        }

    private val onCabinetClick = View.OnClickListener {
        innerNavigator?.navToSelectListOfFilter(FILTER_KEY, viewModel.getCabinetFilter())
    }

    private val onDivisionClick = View.OnClickListener {
        if (viewModel.userData.value?.status == ResourceStatus.LOADING) return@OnClickListener
        innerNavigator?.navToSelectListOfFilter(
            key = FILTER_KEY,
            viewModel.getDivisionFilter(),
            viewModel.userData.value?.data
        )
    }

    private val onOutletClick = View.OnClickListener {
        if (viewModel.userData.value?.status == ResourceStatus.LOADING) return@OnClickListener
        innerNavigator?.navToSelectListOfFilter(
            key = FILTER_KEY,
            viewModel.getOutletFilter(),
            viewModel.userData.value?.data
        )
    }

    private val transparencyBarListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            binding.transparencyValue.text = getString(R.string.widget_percent_value, progress)
            viewModel.updateTransparency(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    private val tabSelectedListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            val theme = if (tab?.text == getString(R.string.widget_cash_desk_theme_light_title))
                WidgetTheme.LIGHT
            else WidgetTheme.DARK
            viewModel.updateTheme(theme)
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {}
        override fun onTabReselected(tab: TabLayout.Tab?) {}
    }

    private var accountingTypeChipBox: Map<ItemOfFilterList, Chip> = emptyMap()

    override fun inflateViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): WidgetConfigireLayoutBinding =
        WidgetConfigireLayoutBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTheme()
        initListeners()
        initObservers()
        initConfigureScreen()
    }

    override fun onResume() {
        super.onResume()
        setFragmentResultListeners()
        viewModel.restoreConfig(
            widgetId?.toInt() ?: AppWidgetManager.INVALID_APPWIDGET_ID,
            widgetSize()
        )
    }

    private fun initListeners() = with(binding) {
        transparencyBar.setOnSeekBarChangeListener(transparencyBarListener)
        themeTabLayout.addOnTabSelectedListener(tabSelectedListener)
        divisionTitle.setOnClickListener(onDivisionClick)
        cabinetTitle.setOnClickListener(onCabinetClick)
        outletTitle.setOnClickListener(onOutletClick)
    }

    private fun initConfigureScreen() = with(binding) {
        initToggles()
        saveConfigureWidget.setOnClickListener { saveWidgetSettings() }
        needAuthButton.setOnClickListener { innerNavigator?.navToLogin() }
        when (widgetSize()) {
            WidgetSize.SMALL -> configureValuesTitle.text =
                getString(R.string.widget_cash_desk_values_small_widget_title)

            WidgetSize.MEDIUM -> configureValuesTitle.text =
                getString(R.string.widget_cash_desk_values_medium_widget_title)

            WidgetSize.LARGE -> configureGroup.visibility = View.GONE
        }
    }

    private fun initToggles() = with(binding.widgetParametersLayout) {
        revenueToggle.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateRevenueSwitchedOn(isChecked)
            if (!viewModel.isEnableToSwitchToggle()) viewModel.updateRevenueSwitchedOn(false)
        }
        refundsToggle.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateRefundSwitchedOn(isChecked)
            if (!viewModel.isEnableToSwitchToggle()) viewModel.updateRefundSwitchedOn(false)
        }
        countReceiptsToggle.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateCountReceiptsSwitchedOn(isChecked)
            if (!viewModel.isEnableToSwitchToggle()) viewModel.updateCountReceiptsSwitchedOn(false)
        }
        avgReceiptsToggle.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateAvgReceiptsSwitchedOn(isChecked)
            if (!viewModel.isEnableToSwitchToggle()) viewModel.updateAvgReceiptsSwitchedOn(false)
        }
    }

    private fun initTogglesState(
        isUserDataExist: Boolean,
        revenueOn: Boolean?,
        refundOn: Boolean?,
        avgReceiptsOn: Boolean?,
        countReceiptsOn: Boolean?
    ) = with(binding.widgetParametersLayout) {
        refundsToggle.isChecked = refundOn == true
        revenueToggle.isChecked = revenueOn == true
        avgReceiptsToggle.isChecked = avgReceiptsOn == true
        countReceiptsToggle.isChecked = countReceiptsOn == true

        val isEnabledToSave = isUserDataExist &&
                (revenueOn == true || refundOn == true || avgReceiptsOn == true || countReceiptsOn == true)
        val isLoading = viewModel.userData.value?.status == ResourceStatus.LOADING
        binding.saveConfigureWidget.isEnabled =
            (widgetSize() == WidgetSize.LARGE || isEnabledToSave) && isLoading.not()
    }

    private fun initTheme() = viewModel.appTheme.observe(viewLifecycleOwner) { theme ->
        when (theme ?: return@observe) {
            AppTheme.LIGHT ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

            AppTheme.DARK ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun initObservers() = with(binding) {
        lifecycleScope.launch {
            viewModel.isNeedAuth.collectLatest {
                it ?: return@collectLatest
                configureGroup.isVisible = it
                contentGroup.isVisible = it
                needAuthGroup.isVisible = !it
            }
        }
        lifecycleScope.launch {
            viewModel.widgetStatistic.collectLatest {
                when (it?.status ?: return@collectLatest) {
                    ResourceStatus.LOADING -> progressBar.visibility = View.VISIBLE
                    ResourceStatus.ERROR -> {
                        progressBar.visibility = View.GONE
                        Snackbar.showNegativeSnackBar(
                            subtitleText = getString(R.string.widget_error),
                            activity = requireActivity(),
                            rootView = root,
                            color = ContextCompat.getColor(requireContext(), R.color.AccentColor)
                        )
                    }

                    ResourceStatus.SUCCESS -> {
                        progressBar.visibility = View.GONE
                        viewModel.saveStatistic(it.data)
                        finishConfigureScreen()
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.widgetData.collectLatest { widget ->
                widget ?: return@collectLatest
                transparencyBar.progress = widget.transparency
                initTogglesState(
                    widget.cabinet != null || widget.division != null || widget.outlet != null,
                    widget.revenueSwitchedOn,
                    widget.refundSwitchedOn,
                    widget.avgReceiptsSwitchedOn,
                    widget.countReceiptsSwitchedOn
                )
                initDivision(widget.division)
                initCabinet(widget.cabinet)
                selectTheme(widget.theme)
                initOutlet(widget.outlet)
            }
        }
        lifecycleScope.launch {
            viewModel.periodFilter.collect { items ->
                items ?: return@collect
                accountingTypeChipBox = items.associateWith { item ->
                    inflateChip(item) {
                        val period =
                            Period.values().find { it.getName(requireContext()) == item.title }
                        viewModel.setPeriodSelected(period ?: Period.TODAY)
                    }.also { periodChipGroup.addView(it) }
                }
                val checked = items.find { it.id.toInt() == viewModel.widgetData.value?.period?.id }
                accountingTypeChipBox[checked ?: items.first()]?.isChecked = true
            }
        }
        lifecycleScope.launch {
            viewModel.userData.collect {
                progressBar.isVisible = it?.status == ResourceStatus.LOADING
                saveConfigureWidget.isEnabled = it?.status != ResourceStatus.LOADING
            }
        }
    }

    private fun finishConfigureScreen() {
        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        activity?.setResult(Activity.RESULT_OK, resultValue)

        val updateWidget = Intent(activity, getWidgetClass())
        updateWidget.action = "$ACTION_WIDGET_INITIAL:$widgetId${widgetSize().getWidgetPrefix()}"

        updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        activity?.sendBroadcast(updateWidget)
        activity?.finish()
    }

    private fun selectTheme(theme: WidgetTheme?) = with(binding.themeTabLayout) {
        val tabPosition = if (theme == WidgetTheme.LIGHT) LIGHT_TAB_POSITION else DARK_TAB_POSITION
        selectTab(getTabAt(tabPosition))
    }

    private fun inflateChip(item: ItemOfFilterList, action: () -> Unit): Chip = with(binding) {
        val chip: Chip = LayoutInflater.from(root.context).inflate(
            CashDeskR.layout.cashdesk_report_chip_selectable_layout, root, false
        ) as Chip
        chip.id = View.NO_ID
        chip.text = item.title
        chip.setOnClickListener { action() }
        return@with chip
    }

    private fun initDivision(division: ItemOfFilterList?) = with(binding.divisionChipGroup) {
        isVisible = division != null
        addChips(listOf(division ?: return@with), onRemoveClick = { viewModel.removeDivision() })
    }

    private fun initCabinet(cabinet: ItemOfFilterList?) = with(binding.cabinetChipGroup) {
        isVisible = cabinet != null
        addChips(
            listOf(cabinet ?: return@with),
            onRemoveClick = { viewModel.removeCabinet() },
            isNeedRemoveButton = false
        )
    }

    private fun initOutlet(outlet: ItemOfFilterList?) = with(binding.outletChipGroup) {
        isVisible = outlet != null
        addChips(listOf(outlet ?: return@with), onRemoveClick = { viewModel.removeOutlet() })
    }

    private fun setFragmentResultListeners() = setFragmentResultListener(FILTER_KEY) { _, result ->
        result.getString(FILTER_KEY)
            ?.let { Json.decodeFromString<Filter>(it) }
            ?.let { viewModel.setFilterType(it) }
    }

    private fun saveWidgetSettings() {
        viewModel.saveWidgetConfiguration(widgetId ?: return, widgetSize().getWidgetPrefix())
    }

    companion object {
        private const val FILTER_KEY = "filter_key"
        private const val LIGHT_TAB_POSITION = 0
        private const val DARK_TAB_POSITION = 1

        fun newInstance(size: WidgetSize) = ConfigureWidgetFragment().apply {
            arguments = bundleOf(WIDGET_SIZE_KEY to Json.encodeToString(size))
        }
    }
}