package com.hooptracker.app.ui

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.hooptracker.app.databinding.ActivityHelpBinding

class HelpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Help & Guide"

        setupWebView()
        loadHelpContent()
    }

    private fun setupWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = false
            builtInZoomControls = false
            displayZoomControls = false
        }
    }

    private fun loadHelpContent() {
        val html = """
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            padding: 16px;
            line-height: 1.6;
            color: #212121;
            background: #F5F5F5;
        }
        h1 {
            color: #FF6B35;
            font-size: 28px;
            margin-top: 0;
        }
        h2 {
            color: #004E89;
            font-size: 22px;
            margin-top: 24px;
            border-bottom: 2px solid #FF6B35;
            padding-bottom: 8px;
        }
        h3 {
            color: #FF6B35;
            font-size: 18px;
            margin-top: 16px;
        }
        .section {
            background: white;
            padding: 16px;
            margin: 12px 0;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .command {
            background: #E8F5E9;
            padding: 8px 12px;
            margin: 4px 0;
            border-radius: 4px;
            border-left: 4px solid #4CAF50;
        }
        .tip {
            background: #FFF3E0;
            padding: 12px;
            margin: 8px 0;
            border-radius: 4px;
            border-left: 4px solid #FF9800;
        }
        .warning {
            background: #FFEBEE;
            padding: 12px;
            margin: 8px 0;
            border-radius: 4px;
            border-left: 4px solid #F44336;
        }
        .emoji {
            font-size: 20px;
        }
        ul {
            padding-left: 20px;
        }
        li {
            margin: 8px 0;
        }
        code {
            background: #F5F5F5;
            padding: 2px 6px;
            border-radius: 3px;
            font-family: monospace;
            color: #004E89;
        }
        .cheat-sheet {
            background: #E3F2FD;
            padding: 12px;
            margin: 8px 0;
            border-radius: 4px;
            font-family: monospace;
            font-size: 14px;
        }
    </style>
</head>
<body>

<h1>🏀 HoopTracker Guide</h1>

<div class="section">
    <h2>🚀 Quick Start</h2>
    <ol>
        <li>Tap <b>"Start Voice Tracking"</b></li>
        <li>Connect your <b>Bluetooth headphones</b> (optional)</li>
        <li>Start shooting!</li>
        <li>Say <b>"hit"</b> or <b>"miss"</b> after each shot</li>
    </ol>
    <div class="tip">
        <b>💡 Tip:</b> The app works great with music playing. Your tunes won't be interrupted!
    </div>
</div>

<div class="section">
    <h2>🎤 Voice Commands</h2>

    <h3>Basic Commands</h3>
    <div class="command"><b>To record a MAKE:</b><br>
    Say: "hit", "make", "made", "good", or "in"</div>

    <div class="command"><b>To record a MISS:</b><br>
    Say: "miss", "missed", "no good", or "brick"</div>

    <div class="command"><b>To UNDO last shot:</b><br>
    Say: "undo", "cancel", or "take back"</div>

    <h3>Advanced: Shot Types</h3>
    <p>Combine shot type + result:</p>
    <ul>
        <li><code>"three pointer hit"</code> - Made 3PT</li>
        <li><code>"free throw miss"</code> - Missed FT</li>
        <li><code>"layup make"</code> - Made layup</li>
        <li><code>"mid range miss"</code> - Missed mid-range</li>
    </ul>
</div>

<div class="section">
    <h2>📊 Understanding Your Stats</h2>

    <h3>Streak Display</h3>
    <p><span class="emoji">🔥</span> = Hot streak (consecutive makes)<br>
    <span class="emoji">❄️</span> = Cold streak (consecutive misses)</p>

    <h3>Time Periods</h3>
    <ul>
        <li><b>Today:</b> Stats for today only</li>
        <li><b>Week:</b> Last 7 days</li>
        <li><b>Month:</b> Last 30 days</li>
        <li><b>Year:</b> Last 365 days</li>
    </ul>

    <h3>Shot Types</h3>
    <ul>
        <li><b>General:</b> Mixed shooting practice</li>
        <li><b>3-Pointer:</b> Shots from behind the arc</li>
        <li><b>Mid-Range:</b> Jump shots inside the arc</li>
        <li><b>Layup:</b> Close-range finishing</li>
        <li><b>Free Throw:</b> From the free throw line</li>
    </ul>
</div>

<div class="section">
    <h2>⚙️ Settings Explained</h2>

    <h3>Haptic Feedback</h3>
    <p><b>ON:</b> Phone vibrates when you record a shot</p>
    <ul>
        <li>Makes: 1 quick vibration</li>
        <li>Misses: 3 quick vibrations</li>
    </ul>

    <h3>Voice Feedback</h3>
    <p><b>ON:</b> Phone says "Hit" or "Miss" out loud after each shot</p>

    <h3>Auto-start Session</h3>
    <p><b>ON:</b> Automatically creates a practice session when you start tracking</p>

    <h3>Daily Goals</h3>
    <ul>
        <li><b>Shot Goal:</b> Target number of shots per day (10-500)</li>
        <li><b>Percentage Goal:</b> Target shooting percentage (10-100%)</li>
    </ul>
</div>

<div class="section">
    <h2>🎯 Using Shot Types</h2>

    <p>Track different parts of your game separately:</p>

    <div class="tip">
        <b>Example Practice:</b><br>
        1. Select "Free Throw"<br>
        2. Shoot 50 free throws<br>
        3. Check your FT percentage<br>
        4. Switch to "3-Pointer"<br>
        5. Practice three-pointers<br>
        6. Compare your stats!
    </div>
</div>

<div class="section">
    <h2>📖 Shot History</h2>

    <p>Access via the <b>history icon</b> (clock) at the top</p>

    <ul>
        <li>View every shot you've recorded</li>
        <li>See timestamps and shot types</li>
        <li><b>Swipe left/right</b> to delete a shot</li>
    </ul>

    <div class="warning">
        <b>⚠️ Warning:</b> Deleted shots cannot be recovered!
    </div>
</div>

<div class="section">
    <h2>↶ Undo Feature</h2>

    <p>Made a mistake? Three ways to undo:</p>
    <ol>
        <li>Tap the <b>↶ Undo</b> button</li>
        <li>Say <b>"undo"</b> via voice</li>
        <li>Tap <b>"Undo"</b> in the notification</li>
    </ol>

    <div class="tip">
        <b>💡 Note:</b> Only removes the LAST shot recorded
    </div>
</div>

<div class="section">
    <h2>📤 Exporting Your Data</h2>

    <ol>
        <li>Tap <b>"Export CSV"</b> button</li>
        <li>Choose where to share (Email, Drive, etc.)</li>
        <li>Your data is saved as a spreadsheet</li>
    </ol>

    <p><b>What's included:</b></p>
    <ul>
        <li>Every shot with exact timestamp</li>
        <li>Result (Hit/Miss)</li>
        <li>Shot type</li>
        <li>Session information</li>
    </ul>
</div>

<div class="section">
    <h2>🔥 Practice Plans</h2>

    <h3>Plan 1: Form Focus (30 min)</h3>
    <ul>
        <li>Shot Type: <b>Mid-Range</b></li>
        <li>Goal: 50 shots at 60%+</li>
        <li>Focus: Perfect form every shot</li>
    </ul>

    <h3>Plan 2: Game Simulation (45 min)</h3>
    <ul>
        <li>Shot Type: <b>Switch between all</b></li>
        <li>Goal: 100 total shots at 50%+</li>
        <li>Focus: Real game variety</li>
    </ul>

    <h3>Plan 3: Free Throw Master (20 min)</h3>
    <ul>
        <li>Shot Type: <b>Free Throw</b></li>
        <li>Goal: 50 shots at 80%+</li>
        <li>Focus: Consistent routine</li>
    </ul>

    <h3>Plan 4: Three-Point Challenge (60 min)</h3>
    <ul>
        <li>Shot Type: <b>3-Pointer</b></li>
        <li>Goal: 100 shots at 40%+</li>
        <li>Focus: Move around the arc</li>
    </ul>
</div>

<div class="section">
    <h2>💡 Tips & Tricks</h2>

    <div class="tip">
        <b>Use Bluetooth Headphones</b><br>
        Better voice recognition and your music keeps playing!
    </div>

    <div class="tip">
        <b>Speak Clearly</b><br>
        No need to shout - just speak normally and wait for confirmation
    </div>

    <div class="tip">
        <b>Track Consistently</b><br>
        Use the app every practice to see real improvement over time
    </div>

    <div class="tip">
        <b>Set Realistic Goals</b><br>
        Start with achievable targets and increase as you improve
    </div>
</div>

<div class="section">
    <h2>🔧 Troubleshooting</h2>

    <h3>Voice Not Working?</h3>
    <ul>
        <li>Check microphone permission is granted</li>
        <li>Speak a bit louder/clearer</li>
        <li>Reduce background noise</li>
        <li>Try reconnecting Bluetooth</li>
        <li>Use manual buttons temporarily</li>
    </ul>

    <h3>Tracking Stops?</h3>
    <ul>
        <li>Disable battery optimization for HoopTracker</li>
        <li>Keep app in foreground</li>
        <li>Restart tracking if needed</li>
    </ul>

    <h3>No Vibration?</h3>
    <ul>
        <li>Check Haptic Feedback is ON in settings</li>
        <li>Make sure phone isn't in silent mode</li>
        <li>Some phones don't support vibration</li>
    </ul>
</div>

<div class="section">
    <h2>📋 Quick Reference</h2>
    <div class="cheat-sheet">
<b>VOICE COMMANDS</b>
━━━━━━━━━━━━━━━━━━━━
Make:  hit | make | made
       good | in

Miss:  miss | missed
       no good | brick

Undo:  undo | cancel
       take back

<b>SHOT TYPES</b>
━━━━━━━━━━━━━━━━━━━━
"three pointer [result]"
"free throw [result]"
"layup [result]"
"mid range [result]"

<b>BUTTONS</b>
━━━━━━━━━━━━━━━━━━━━
✓ Hit    - Record make
✗ Miss   - Record miss
↶ Undo   - Remove last
🕐 History - View shots
⚙️ Settings - Customize
📤 Export  - Download CSV
    </div>
</div>

<div class="section">
    <h2>🔒 Privacy</h2>

    <p><b>Your data is 100% private:</b></p>
    <ul>
        <li>✅ Stored only on your phone</li>
        <li>✅ No internet required</li>
        <li>✅ No tracking or analytics</li>
        <li>✅ No ads</li>
        <li>✅ No account needed</li>
    </ul>

    <div class="tip">
        <b>Backup Tip:</b> Export your data regularly to keep a backup!
    </div>
</div>

<div class="section" style="text-align: center; margin-top: 32px;">
    <h2>🏀 Now Go Practice!</h2>
    <p style="font-size: 18px; color: #004E89;">
        The app tracks the stats,<br>
        but <b>YOU</b> make the shots!
    </p>
    <p style="color: #757575; margin-top: 16px;">
        Practice consistently, track honestly,<br>
        and watch yourself improve! 🔥
    </p>
</div>

</body>
</html>
        """.trimIndent()

        binding.webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
