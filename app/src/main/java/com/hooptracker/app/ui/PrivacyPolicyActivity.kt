package com.hooptracker.app.ui

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hooptracker.app.databinding.ActivityPrivacyPolicyBinding

class PrivacyPolicyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrivacyPolicyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        setupWebView()
        loadPrivacyPolicy()
    }

    private fun setupWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = false
            builtInZoomControls = false
            displayZoomControls = false
            allowFileAccess = true
            allowContentAccess = false
        }
        binding.webView.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun loadPrivacyPolicy() {
        binding.webView.loadUrl("file:///android_asset/privacy_policy.html")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
