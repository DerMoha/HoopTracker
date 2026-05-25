package com.hooptracker.app.ui

import android.content.Intent
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
    private var isBindingGoalValues = false
    private var legacyGoalSynced = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = (application as HoopTrackerApplication).preferences

        binding.toolbar.setNavigationOnClickListener { finish() }
        setupUI()
        observeGoalSettings()
        loadTogglePreferences()
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

        binding.tvPrivacyPolicy.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }

        binding.sliderDailyShots.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !isBindingGoalValues) {
                preferences.dailyShotGoal = value.toInt()
                binding.tvDailyShotsValue.text = value.toInt().toString()
                updateGoalFromInputs()
            }
        }

        binding.sliderDailyPercentage.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !isBindingGoalValues) {
                preferences.dailyPercentageGoal = value
                binding.tvDailyPercentageValue.text = "${value.toInt()}%"
                updateGoalFromInputs()
            }
        }
    }

    private fun observeGoalSettings() {
        viewModel.goalProgress.observe(this) { goalProgress ->
            if (!legacyGoalSynced) {
                val shouldSeedLegacyGoal =
                    goalProgress.goal.targetShots == 100 &&
                        goalProgress.goal.targetPercentage == 50f &&
                        (preferences.dailyShotGoal != 100 || preferences.dailyPercentageGoal != 50f)

                legacyGoalSynced = true

                if (shouldSeedLegacyGoal) {
                    viewModel.updateGoal(preferences.dailyShotGoal, preferences.dailyPercentageGoal)
                    return@observe
                }
            }

            isBindingGoalValues = true
            binding.sliderDailyShots.value = goalProgress.goal.targetShots.toFloat()
            binding.tvDailyShotsValue.text = goalProgress.goal.targetShots.toString()
            binding.sliderDailyPercentage.value = goalProgress.goal.targetPercentage
            binding.tvDailyPercentageValue.text = "${goalProgress.goal.targetPercentage.toInt()}%"
            isBindingGoalValues = false
        }
    }

    private fun loadTogglePreferences() {
        binding.switchHaptic.isChecked = preferences.hapticFeedbackEnabled
        binding.switchVoice.isChecked = preferences.voiceFeedbackEnabled
        binding.switchAutoSession.isChecked = preferences.autoStartSession
    }

    private fun updateGoalFromInputs() {
        viewModel.updateGoal(
            binding.sliderDailyShots.value.toInt(),
            binding.sliderDailyPercentage.value
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
