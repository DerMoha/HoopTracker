package com.hooptracker.app.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.hooptracker.app.HoopTrackerApplication
import com.hooptracker.app.data.Preferences
import com.hooptracker.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferences: Preferences
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as HoopTrackerApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = (application as HoopTrackerApplication).preferences

        binding.toolbar.setNavigationOnClickListener { finish() }
        setupUI()
        loadPreferences()
    }

    private fun setupUI() {
        binding.switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            preferences.hapticFeedbackEnabled = isChecked
        }

        binding.switchVoice.setOnCheckedChangeListener { _, isChecked ->
            preferences.voiceFeedbackEnabled = isChecked
        }

        binding.switchAutoSession.setOnCheckedChangeListener { _, isChecked ->
            preferences.autoStartSession = isChecked
        }

        binding.sliderDailyShots.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                preferences.dailyShotGoal = value.toInt()
                binding.tvDailyShotsValue.text = value.toInt().toString()
            }
        }

        binding.sliderDailyPercentage.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                preferences.dailyPercentageGoal = value
                binding.tvDailyPercentageValue.text = "${value.toInt()}%"
            }
        }
    }

    private fun loadPreferences() {
        binding.switchHaptic.isChecked = preferences.hapticFeedbackEnabled
        binding.switchVoice.isChecked = preferences.voiceFeedbackEnabled
        binding.switchAutoSession.isChecked = preferences.autoStartSession

        binding.sliderDailyShots.value = preferences.dailyShotGoal.toFloat()
        binding.tvDailyShotsValue.text = preferences.dailyShotGoal.toString()

        binding.sliderDailyPercentage.value = preferences.dailyPercentageGoal
        binding.tvDailyPercentageValue.text = "${preferences.dailyPercentageGoal.toInt()}%"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
