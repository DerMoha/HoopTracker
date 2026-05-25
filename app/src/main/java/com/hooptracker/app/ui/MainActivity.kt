package com.hooptracker.app.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.hooptracker.app.HoopTrackerApplication
import com.hooptracker.app.R
import com.hooptracker.app.data.DailyStats
import com.hooptracker.app.data.GoalProgress
import com.hooptracker.app.data.Preferences
import com.hooptracker.app.data.ShotStats
import com.hooptracker.app.data.ShotType
import com.hooptracker.app.data.StatsPeriod
import com.hooptracker.app.databinding.ActivityMainBinding
import com.hooptracker.app.service.ShotTrackingService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as HoopTrackerApplication).repository)
    }
    private lateinit var preferences: Preferences

    private var trackingService: ShotTrackingService? = null
    private var isServiceBound = false
    private var isTracking = false
    private var currentSessionId: Long? = null
    private var currentShotType = ShotType.GENERAL
    private var selectedPeriod = StatsPeriod.TODAY
    private var selectedStatsShotType: ShotType? = null
    private var pendingStartTracking = false
    private var pendingSessionId: Long? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ShotTrackingService.LocalBinder
            trackingService = binder.getService()
            trackingService?.setShotType(currentShotType)
            trackingService?.setSessionId(currentSessionId)
            isServiceBound = true

            if (pendingStartTracking) {
                beginTrackingAfterBind()
            } else {
                updateTrackingState(trackingService?.isTrackingActive() == true || preferences.trackingActive)
                trackingService?.refreshSessionStats()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
            isServiceBound = false
            if (!pendingStartTracking) {
                updateTrackingState(false)
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startTrackingService()
        } else {
            Toast.makeText(this, R.string.permissions_required_message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferences = (application as HoopTrackerApplication).preferences
        currentSessionId = preferences.currentSessionId
        isTracking = preferences.trackingActive

        setupUI()
        observeData()
        setupChart()
        updateSelectedPeriod(StatsPeriod.TODAY)
        updateDashboardFilter(null)
        updateTrackingButton()

        if (preferences.trackingActive) {
            connectToTrackingService(ensureServiceRunning = true)
        }
    }

    private fun setupUI() {
        binding.btnHelp.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, ShotHistoryActivity::class.java))
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnStartTracking.setOnClickListener {
            if (isTracking) stopTrackingService() else checkPermissionsAndStart()
        }

        binding.btnManualHit.setOnClickListener {
            recordManualShot(isHit = true)
        }

        binding.btnManualMiss.setOnClickListener {
            recordManualShot(isHit = false)
        }

        binding.btnUndo.setOnClickListener {
            viewModel.undoLastShot { undone ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        if (undone) R.string.shot_undone else R.string.nothing_to_undo,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.btnExport.setOnClickListener {
            viewModel.exportToCSV()
        }

        binding.btnClearData.setOnClickListener {
            showClearDataDialog()
        }

        binding.chipToday.setOnClickListener { updateSelectedPeriod(StatsPeriod.TODAY) }
        binding.chipWeek.setOnClickListener { updateSelectedPeriod(StatsPeriod.WEEK) }
        binding.chipMonth.setOnClickListener { updateSelectedPeriod(StatsPeriod.MONTH) }
        binding.chipYear.setOnClickListener { updateSelectedPeriod(StatsPeriod.YEAR) }

        binding.chipAllShots.setOnClickListener { updateDashboardFilter(null) }
        binding.chipStatsGeneral.setOnClickListener { updateDashboardFilter(ShotType.GENERAL) }
        binding.chipStats3PT.setOnClickListener { updateDashboardFilter(ShotType.THREE_POINTER) }
        binding.chipStatsMidRange.setOnClickListener { updateDashboardFilter(ShotType.MID_RANGE) }
        binding.chipStatsLayup.setOnClickListener { updateDashboardFilter(ShotType.LAYUP) }
        binding.chipStatsFT.setOnClickListener { updateDashboardFilter(ShotType.FREE_THROW) }

        binding.chipGeneral.setOnClickListener { updateRecordingShotType(ShotType.GENERAL) }
        binding.chip3PT.setOnClickListener { updateRecordingShotType(ShotType.THREE_POINTER) }
        binding.chipMidRange.setOnClickListener { updateRecordingShotType(ShotType.MID_RANGE) }
        binding.chipLayup.setOnClickListener { updateRecordingShotType(ShotType.LAYUP) }
        binding.chipFT.setOnClickListener { updateRecordingShotType(ShotType.FREE_THROW) }
    }

    private fun observeData() {
        viewModel.allShots.observe(this) { shots ->
            val isEmpty = shots.isEmpty()
            binding.dashboardEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }

        viewModel.stats.observe(this) { stats ->
            updateStatsDisplay(stats)
        }

        viewModel.chartData.observe(this) { data ->
            updateChart(data)
        }

        viewModel.currentStreak.observe(this) { streak ->
            val streakLabel = if (streak.count == 0) {
                "0"
            } else {
                getString(
                    if (streak.isHitStreak) R.string.streak_makes else R.string.streak_misses,
                    streak.count
                )
            }
            binding.tvStreak.text = streakLabel
            binding.tvStreak.setTextColor(
                ContextCompat.getColor(
                    this,
                    when {
                        streak.count == 0 -> R.color.text_secondary
                        streak.isHitStreak -> R.color.success
                        else -> R.color.error
                    }
                )
            )
        }

        viewModel.goalProgress.observe(this) { goalProgress ->
            updateGoalDisplay(goalProgress)
        }

        viewModel.exportFile.observe(this) { file ->
            file?.let {
                shareCSVFile(it)
                viewModel.clearExportFile()
            }
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.isExporting.observe(this) { exporting ->
            binding.btnExport.isEnabled = !exporting
            binding.btnExport.text = getString(
                if (exporting) R.string.exporting else R.string.export_shot_data
            )
        }
    }

    private fun updateSelectedPeriod(period: StatsPeriod) {
        selectedPeriod = period
        binding.chipToday.isChecked = period == StatsPeriod.TODAY
        binding.chipWeek.isChecked = period == StatsPeriod.WEEK
        binding.chipMonth.isChecked = period == StatsPeriod.MONTH
        binding.chipYear.isChecked = period == StatsPeriod.YEAR
        viewModel.setSelectedPeriod(period)
    }

    private fun updateDashboardFilter(shotType: ShotType?) {
        selectedStatsShotType = shotType
        binding.chipAllShots.isChecked = shotType == null
        binding.chipStatsGeneral.isChecked = shotType == ShotType.GENERAL
        binding.chipStats3PT.isChecked = shotType == ShotType.THREE_POINTER
        binding.chipStatsMidRange.isChecked = shotType == ShotType.MID_RANGE
        binding.chipStatsLayup.isChecked = shotType == ShotType.LAYUP
        binding.chipStatsFT.isChecked = shotType == ShotType.FREE_THROW
        viewModel.setStatsFilterShotType(shotType)
    }

    private fun updateRecordingShotType(shotType: ShotType) {
        currentShotType = shotType
        trackingService?.setShotType(shotType)
    }

    private fun updateStatsDisplay(stats: ShotStats) {
        binding.tvHits.text = stats.hits.toString()
        binding.tvMisses.text = stats.misses.toString()
        binding.tvTotal.text = stats.total.toString()
        binding.tvPercentage.text = String.format(Locale.getDefault(), "%.1f%%", stats.percentage)
        binding.progressCircular.progress = stats.percentage.toInt()
        binding.tvStatsSubtitle.text = getString(
            R.string.stats_period_label,
            formatPeriodLabel(selectedPeriod),
            formatShotTypeLabel(selectedStatsShotType)
        )
        binding.tvStatsEmpty.visibility = if (stats.total == 0) View.VISIBLE else View.GONE
    }

    private fun updateGoalDisplay(goalProgress: GoalProgress) {
        binding.tvShotsGoalValue.text = getString(
            R.string.goal_shots_value,
            goalProgress.currentShots,
            goalProgress.goal.targetShots
        )
        binding.tvPercentageGoalValue.text = getString(
            R.string.goal_percentage_value,
            goalProgress.currentPercentage,
            goalProgress.goal.targetPercentage
        )
        binding.progressShotsGoal.progress = goalProgress.shotsProgress.coerceIn(0f, 100f).toInt()
        binding.progressPercentageGoal.progress = goalProgress.percentageProgress.coerceIn(0f, 100f).toInt()

        binding.tvGoalsStatus.text = when {
            goalProgress.goal.targetShots <= 0 || goalProgress.goal.targetPercentage <= 0f -> {
                getString(R.string.goal_empty_message)
            }
            goalProgress.isComplete -> getString(R.string.goal_complete_message)
            else -> getString(R.string.goal_progress_message)
        }
    }

    private fun setupChart() {
        binding.chart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            setNoDataText("")
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = ContextCompat.getColor(context, R.color.text_secondary)
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(context, R.color.divider)
                textColor = ContextCompat.getColor(context, R.color.text_secondary)
                axisMinimum = 0f
                axisMaximum = 100f
            }

            axisRight.isEnabled = false
        }
    }

    private fun updateChart(chartPoints: List<DailyStats>) {
        binding.tvChartSubtitle.text = getString(
            R.string.chart_period_subtitle,
            formatPeriodLabel(selectedPeriod),
            formatShotTypeLabel(selectedStatsShotType)
        )

        if (selectedPeriod == StatsPeriod.TODAY) {
            binding.chart.clear()
            binding.chart.visibility = View.GONE
            binding.tvChartEmpty.visibility = View.VISIBLE
            binding.tvChartEmpty.text = getString(R.string.chart_today_message)
            return
        }

        val hasChartContent = chartPoints.any { it.stats.total > 0 }
        if (!hasChartContent) {
            binding.chart.clear()
            binding.chart.visibility = View.GONE
            binding.tvChartEmpty.visibility = View.VISIBLE
            binding.tvChartEmpty.text = getString(R.string.chart_empty_message)
            return
        }

        val entries = chartPoints.mapIndexed { index, point ->
            BarEntry(index.toFloat(), point.stats.percentage)
        }

        val dataSet = BarDataSet(entries, "").apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.primary)
            valueTextColor = ContextCompat.getColor(this@MainActivity, R.color.text_primary)
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value > 0f) "${value.toInt()}%" else ""
                }
            }
        }

        val datePattern = if (selectedPeriod == StatsPeriod.YEAR) "MMM" else "M/d"
        val dateFormat = SimpleDateFormat(datePattern, Locale.getDefault())
        binding.chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index in chartPoints.indices) dateFormat.format(chartPoints[index].date) else ""
            }
        }

        binding.chart.data = BarData(dataSet)
        binding.chart.visibility = View.VISIBLE
        binding.tvChartEmpty.visibility = View.GONE
        binding.chart.animateY(500)
        binding.chart.invalidate()
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {
            startTrackingService()
        } else {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun recordManualShot(isHit: Boolean) {
        if (isHit) {
            viewModel.recordHit(currentSessionId, currentShotType)
        } else {
            viewModel.recordMiss(currentSessionId, currentShotType)
        }
        trackingService?.refreshSessionStats()
    }

    private fun startTrackingService() {
        if (pendingStartTracking) return

        pendingStartTracking = true
        binding.tvTrackingStatus.text = getString(R.string.tracking_status_starting)

        if (preferences.autoStartSession && currentSessionId == null) {
            viewModel.startSession { sessionId ->
                pendingSessionId = sessionId
                persistSessionId(sessionId)
                connectToTrackingService(ensureServiceRunning = true)
            }
        } else {
            connectToTrackingService(ensureServiceRunning = true)
        }
    }

    private fun connectToTrackingService(ensureServiceRunning: Boolean) {
        val intent = Intent(this, ShotTrackingService::class.java)

        if (ensureServiceRunning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

        if (!isServiceBound) {
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else if (pendingStartTracking) {
            beginTrackingAfterBind()
        }
    }

    private fun beginTrackingAfterBind() {
        val service = trackingService ?: return
        service.setShotType(currentShotType)
        service.setSessionId(currentSessionId)

        when (service.startTracking()) {
            ShotTrackingService.StartTrackingResult.STARTED,
            ShotTrackingService.StartTrackingResult.ALREADY_RUNNING -> {
                pendingStartTracking = false
                pendingSessionId = null
                updateTrackingState(true)
                service.refreshSessionStats()
            }

            ShotTrackingService.StartTrackingResult.UNAVAILABLE -> {
                pendingStartTracking = false
                rollbackPendingSession()
                updateTrackingState(false)
                binding.tvTrackingStatus.text = getString(R.string.tracking_status_error)
                Toast.makeText(this, R.string.voice_tracking_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rollbackPendingSession() {
        pendingSessionId?.let { sessionId ->
            viewModel.endSession(sessionId)
            if (currentSessionId == sessionId) {
                persistSessionId(null)
            }
        }
        pendingSessionId = null
    }

    private fun stopTrackingService() {
        pendingStartTracking = false
        pendingSessionId = null
        trackingService?.stopTracking()

        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }

        currentSessionId?.let { sessionId ->
            viewModel.endSession(sessionId)
            persistSessionId(null)
        }

        updateTrackingState(false)
        viewModel.refreshStats()
    }

    private fun persistSessionId(sessionId: Long?) {
        currentSessionId = sessionId
        preferences.currentSessionId = sessionId
        trackingService?.setSessionId(sessionId)
    }

    private fun updateTrackingState(active: Boolean) {
        isTracking = active
        preferences.trackingActive = active
        updateTrackingButton()
    }

    private fun updateTrackingButton() {
        if (isTracking) {
            binding.btnStartTracking.text = getString(R.string.stop_tracking)
            binding.btnStartTracking.backgroundTintList = ContextCompat.getColorStateList(this, R.color.error)
            binding.trackingIndicator.visibility = View.VISIBLE
            binding.tvTrackingStatus.text = getString(R.string.tracking_status_on)
        } else {
            binding.btnStartTracking.text = getString(R.string.start_tracking)
            binding.btnStartTracking.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
            binding.trackingIndicator.visibility = View.GONE
            if (!pendingStartTracking) {
                binding.tvTrackingStatus.text = getString(R.string.tracking_status_ready)
            }
        }
    }

    private fun shareCSVFile(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, getString(R.string.export_shot_data)))
    }

    private fun showClearDataDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_data_title)
            .setMessage(R.string.clear_data_message)
            .setPositiveButton(R.string.clear) { _, _ ->
                viewModel.clearAllData()
                persistSessionId(null)
                trackingService?.resetSessionStats()
                Toast.makeText(this, R.string.all_data_cleared, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun formatPeriodLabel(period: StatsPeriod): String = when (period) {
        StatsPeriod.TODAY -> getString(R.string.stats_period_today)
        StatsPeriod.WEEK -> getString(R.string.stats_period_week)
        StatsPeriod.MONTH -> getString(R.string.stats_period_month)
        StatsPeriod.YEAR -> getString(R.string.stats_period_year)
    }

    private fun formatShotTypeLabel(shotType: ShotType?): String = when (shotType) {
        null -> getString(R.string.all_shots_filter)
        ShotType.GENERAL -> getString(R.string.general_shot_type)
        ShotType.THREE_POINTER -> getString(R.string.three_point_shot_type)
        ShotType.MID_RANGE -> getString(R.string.mid_range_shot_type)
        ShotType.LAYUP -> getString(R.string.layup_shot_type)
        ShotType.FREE_THROW -> getString(R.string.free_throw_shot_type)
    }

    override fun onResume() {
        super.onResume()
        if (preferences.trackingActive && !isServiceBound) {
            connectToTrackingService(ensureServiceRunning = true)
        }
        isTracking = preferences.trackingActive
        updateTrackingButton()
        viewModel.refreshStats()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
        }
    }
}
