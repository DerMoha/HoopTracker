# 🏀 HoopTracker

A powerful Android app for tracking basketball shots using voice commands. Practice smarter with real-time stats, advanced analytics, and beautiful visualizations.

## Features

### ✨ Voice-Activated Tracking
- **Hands-free operation** via Bluetooth headphones
- **Expanded voice commands**:
  - Say "hit", "make", "made", "good", or "in" to record a make
  - Say "miss", "missed", "no good", or "brick" to record a miss
  - Say "undo", "cancel", or "take back" to undo last shot
  - Specify shot types: "three pointer hit", "free throw miss", etc.
- **Background operation** - runs without interrupting your music
- **Haptic feedback** - feel confirmation vibrations on each shot
- **Voice feedback** (optional) - hear "Hit" or "Miss" confirmations

### 🎯 Shot Type Tracking
Track different shot types separately:
- General shots
- 3-Pointers
- Mid-Range shots
- Layups
- Free Throws

View detailed statistics for each shot type!

### 📊 Comprehensive Statistics
- **Real-time shooting percentage** with circular progress indicator
- **Current streak tracking** - see your hot/cold streaks with visual indicators
- **Time-based stats**: Today, Week, Month, Year
- **Beautiful bar charts** showing percentage trends over time
- **Session tracking** - organize practice sessions with notes
- **Shot history** - complete log of all shots with swipe-to-delete

### 🎯 Daily Goals
- Set custom daily shot goals
- Track progress toward percentage targets
- Visual progress indicators
- Configurable targets in settings

### 📱 Smart Features
- **Undo functionality** - made a mistake? Undo the last shot
- **Manual entry buttons** - quick tap entry when voice isn't available
- **CSV export** - share your data with coaches or analyze in spreadsheets
- **Settings screen** - customize feedback, sessions, and goals
- **Auto-session start** - automatically create practice sessions
- **Persistent notification** with live stats and quick actions
- **Foreground service** keeps tracking active in background
- **Room database** for reliable local data storage

### 🎨 Modern Design
- Material Design 3 UI with smooth animations
- Clean, intuitive interface optimized for one-handed use
- Circular progress indicators
- Interactive charts with MPAndroidChart
- Beautiful orange/blue color scheme

## Requirements

- Android 8.0 (API 26) or higher
- Microphone permission for voice tracking
- Bluetooth (optional, for headphones)
- Google Speech Recognition

## How to Use

### 1. Grant Permissions
   - Allow microphone access when prompted
   - Allow Bluetooth and notification permissions (if applicable)

### 2. Configure Settings (Optional)
   - Tap the settings icon to customize:
     - Enable/disable haptic feedback
     - Enable/disable voice feedback
     - Set daily shot goals
     - Configure auto-session start

### 3. Start Tracking
   - Select your shot type (General, 3PT, Mid-Range, Layup, Free Throw)
   - Tap "Start Voice Tracking"
   - Connect your Bluetooth headphones (optional)
   - Start shooting!

### 4. Record Shots
   **Voice Commands:**
   - "hit" / "make" / "made" / "good" / "in" → Record a make
   - "miss" / "missed" / "no good" / "brick" → Record a miss
   - "three hit" or "three pointer make" → 3PT make
   - "free throw miss" → Free throw miss
   - "undo" / "cancel" / "take back" → Undo last shot

   **Manual Entry:**
   - Tap ✓ Hit or ✗ Miss buttons for quick entry
   - Use ↶ Undo button to remove last shot

### 5. View Stats & Export
   - Switch between Today/Week/Month/Year views
   - Check your current streak
   - View shooting percentage over time in charts
   - Tap History icon to see all shots
   - Tap Export CSV to share your data

## Technical Details

### Architecture
- **MVVM Pattern** with ViewModels and LiveData
- **Repository Pattern** for data access
- **Room Database** with 3 entities (Shots, Sessions, Goals)
- **Kotlin Coroutines** for async operations
- **Foreground Service** for continuous voice tracking

### Key Components

**Data Layer:**
- `Shot`, `Session`, `Goal` entities
- `ShotDao`, `SessionDao`, `GoalDao` for database access
- `ShotRepository` with comprehensive statistics methods
- `Preferences` for user settings

**Service Layer:**
- `ShotTrackingService` - Foreground service with:
  - Continuous speech recognition
  - Text-to-speech feedback
  - Haptic vibration support
  - Intelligent error recovery with exponential backoff
  - Session management integration

**UI Layer:**
- `MainActivity` - Main dashboard with stats, charts, and controls
- `ShotHistoryActivity` - Scrollable shot log with swipe-to-delete
- `SettingsActivity` - Customizable preferences
- `MainViewModel` - Business logic and state management

### Voice Recognition
- Uses Android SpeechRecognizer API
- Continuous listening mode with automatic restart
- Multi-result processing for better accuracy
- Context-aware shot type detection
- Music-friendly (doesn't pause playback)
- Robust error handling

### Statistics Calculations
- Shot percentage by time period
- Streak tracking (current hot/cold streak)
- Daily/weekly/monthly/yearly aggregations
- Shot type breakdowns
- Goal progress tracking
- Session statistics

## Building the App

1. Clone the repository
```bash
git clone https://github.com/DerMoha/HoopTracker.git
cd HoopTracker
```

2. Open in Android Studio
   - Android Studio Hedgehog or newer recommended
   - Kotlin 1.9.20+
   - Gradle 8.2.0+

3. Sync Gradle and build
```bash
./gradlew build
```

4. Run on device or emulator
```bash
./gradlew installDebug
```

## Permissions

The app requires:

- `RECORD_AUDIO`: For voice recognition
- `BLUETOOTH` / `BLUETOOTH_CONNECT`: For Bluetooth headphone support
- `VIBRATE`: For haptic feedback
- `FOREGROUND_SERVICE`: To keep tracking active in background
- `POST_NOTIFICATIONS`: For persistent tracking notification
- `WRITE/READ_EXTERNAL_STORAGE`: For CSV export (API < 29)

## Privacy

- **100% local** - all data stored on your device
- **No internet required** - works completely offline
- **No data collection** - we don't track or collect anything
- **No analytics** - your shooting stats stay private
- **Open source** - inspect the code yourself

## Feature Highlights

### 🔥 Streak Tracking
See your current hot streak (consecutive makes) or cold streak (consecutive misses) with visual indicators. Motivates you to keep the streak alive!

### 📈 Advanced Analytics
- Compare performance across different shot types
- Track improvement over time with trend charts
- Export data to CSV for deeper analysis
- Session-based organization for structured practice

### ⚡ Quick Actions
- Undo button right in the notification
- One-tap manual entry
- Quick shot type switching
- Fast access to history and settings

### 🎯 Smart Sessions
- Auto-start sessions when tracking begins
- Add notes to practice sessions
- Review session statistics
- Historical session tracking

## Future Enhancements

- [ ] Shot charts with court visualization
- [ ] Multiple user profiles
- [ ] Advanced drills and challenges
- [ ] Social features (share achievements)
- [ ] Integration with fitness apps
- [ ] Dark mode
- [ ] Home screen widget
- [ ] Wear OS companion app

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- MPAndroidChart for beautiful chart visualizations
- Material Design 3 for modern UI components
- Android Jetpack for robust architecture components

---

Made with ❤️ for basketball enthusiasts. Keep shooting, keep improving! 🏀
