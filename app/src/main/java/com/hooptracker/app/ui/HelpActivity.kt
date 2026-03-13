package com.hooptracker.app.ui

import android.os.Bundle
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import com.hooptracker.app.databinding.ActivityHelpBinding

class HelpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        setupWebView()
        loadHelpContent()
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

    private fun loadHelpContent() {
        binding.webView.loadUrl("file:///android_asset/help_guide.html")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
