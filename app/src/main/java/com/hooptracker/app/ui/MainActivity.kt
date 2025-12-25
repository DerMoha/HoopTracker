package com.hooptracker.app.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.hooptracker.app.HoopTrackerApplication
import com.hooptracker.app.R
import com.hooptracker.app.databinding.ActivityMainBinding
import com.hooptracker.app.service.ShotTrackingService
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as HoopTrackerApplication).repository)
    }

    private var trackingService: ShotTrackingService? = null
    private var isServiceBound = false
    private var isTracking = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ShotTrackingService.LocalBinder
            trackingService = binder.getService()
            trackingService?.setRepository((application as HoopTrackerApplication).repository)
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
            isServiceBound = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startTrackingService()
        } else {
            Toast.makeText(this, "Permissions required for voice tracking", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeData()
        setupChart()
    }

    private fun setupUI() {
        binding.btnStartTracking.setOnClickListener {
            if (isTracking) {
                stopTrackingService()
            } else {
                checkPermissionsAndStart()
            }
        }

        binding.btnManualHit.setOnClickListener {
            viewModel.recordHit()
        }

        binding.btnManualMiss.setOnClickListener {
            viewModel.recordMiss()
        }

        binding.btnClearData.setOnClickListener {
            showClearDataDialog()
        }

        binding.chipToday.setOnClickListener {
            updateSelectedPeriod("today")
        }

        binding.chipWeek.setOnClickListener {
            updateSelectedPeriod("week")
        }

        binding.chipMonth.setOnClickListener {
            updateSelectedPeriod("month")
        }

        binding.chipYear.setOnClickListener {
            updateSelectedPeriod("year")
        }

        // Default selection
        updateSelectedPeriod("today")
    }

    private fun observeData() {
        viewModel.todayStats.observe(this) { stats ->
            updateStatsDisplay(stats, "today")
        }

        viewModel.weeklyStats.observe(this) { stats ->
            updateStatsDisplay(stats, "week")
        }

        viewModel.monthlyStats.observe(this) { stats ->
            updateStatsDisplay(stats, "month")
        }

        viewModel.yearlyStats.observe(this) { stats ->
            updateStatsDisplay(stats, "year")
        }

        viewModel.weeklyChartData.observe(this) { data ->
            updateChart(data, "week")
        }

        viewModel.monthlyChartData.observe(this) { data ->
            updateChart(data, "month")
        }
    }

    private fun updateSelectedPeriod(period: String) {
        // Update chip selection
        binding.chipToday.isChecked = period == "today"
        binding.chipWeek.isChecked = period == "week"
        binding.chipMonth.isChecked = period == "month"
        binding.chipYear.isChecked = period == "year"

        // Update display based on current data
        when (period) {
            "today" -> viewModel.todayStats.value?.let { updateStatsDisplay(it, period) }
            "week" -> {
                viewModel.weeklyStats.value?.let { updateStatsDisplay(it, period) }
                viewModel.weeklyChartData.value?.let { updateChart(it, period) }
            }
            "month" -> {
                viewModel.monthlyStats.value?.let { updateStatsDisplay(it, period) }
                viewModel.monthlyChartData.value?.let { updateChart(it, period) }
            }
            "year" -> viewModel.yearlyStats.value?.let { updateStatsDisplay(it, period) }
        }
    }

    private fun updateStatsDisplay(stats: com.hooptracker.app.data.ShotStats, period: String) {
        val isSelected = when (period) {
            "today" -> binding.chipToday.isChecked
            "week" -> binding.chipWeek.isChecked
            "month" -> binding.chipMonth.isChecked
            "year" -> binding.chipYear.isChecked
            else -> false
        }

        if (!isSelected) return

        binding.tvHits.text = stats.hits.toString()
        binding.tvMisses.text = stats.misses.toString()
        binding.tvTotal.text = stats.total.toString()
        binding.tvPercentage.text = String.format("%.1f%%", stats.percentage)

        // Update circular progress
        binding.progressCircular.progress = stats.percentage.toInt()
    }

    private fun setupChart() {
        binding.chart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setScaleEnabled(false)
            setPinchZoom(false)
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

    private fun updateChart(dailyStats: List<com.hooptracker.app.data.DailyStats>, period: String) {
        val isSelected = when (period) {
            "week" -> binding.chipWeek.isChecked
            "month" -> binding.chipMonth.isChecked
            else -> false
        }

        if (!isSelected) return

        val entries = dailyStats.mapIndexed { index, daily ->
            BarEntry(index.toFloat(), daily.stats.percentage)
        }

        val dataSet = BarDataSet(entries, "Shooting %").apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.primary)
            valueTextColor = ContextCompat.getColor(this@MainActivity, R.color.text_primary)
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value > 0) "${value.toInt()}%" else ""
                }
            }
        }

        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        binding.chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index in dailyStats.indices) {
                    dateFormat.format(dailyStats[index].date)
                } else ""
            }
        }

        binding.chart.data = BarData(dataSet)
        binding.chart.animateY(500)
        binding.chart.invalidate()
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )

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

    private fun startTrackingService() {
        val intent = Intent(this, ShotTrackingService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Start tracking after binding
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            trackingService?.startTracking()
        }, 500)

        isTracking = true
        updateTrackingButton()
    }

    private fun stopTrackingService() {
        trackingService?.stopTracking()

        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }

        isTracking = false
        updateTrackingButton()
        viewModel.refreshStats()
    }

    private fun updateTrackingButton() {
        if (isTracking) {
            binding.btnStartTracking.text = "Stop Voice Tracking"
            binding.btnStartTracking.setBackgroundColor(
                ContextCompat.getColor(this, R.color.error)
            )
            binding.trackingIndicator.visibility = View.VISIBLE
        } else {
            binding.btnStartTracking.text = "Start Voice Tracking"
            binding.btnStartTracking.setBackgroundColor(
                ContextCompat.getColor(this, R.color.primary)
            )
            binding.trackingIndicator.visibility = View.GONE
        }
    }

    private fun showClearDataDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Data")
            .setMessage("Are you sure you want to delete all shot records? This cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                viewModel.clearAllData()
                trackingService?.resetSessionStats()
                Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
        }
    }
}
