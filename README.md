# 🏀 HoopTracker

A sleek Android app for tracking basketball shots using voice commands. Practice smarter with real-time stats and beautiful visualizations.

## Features

✨ **Voice-Activated Tracking**
- Hands-free shot tracking via Bluetooth headphones
- Just say "hit" or "miss" after each shot
- Runs in background without interrupting music

📊 **Comprehensive Statistics**
- Real-time shooting percentage
- Track hits, misses, and total shots
- View stats by: Today, Week, Month, Year
- Beautiful bar charts showing progress over time

🎨 **Modern Design**
- Material Design 3 UI
- Clean, intuitive interface
- Circular progress indicators
- Easy-to-read charts

📱 **Smart Features**
- Foreground service keeps tracking active
- Persistent notification with live stats
- Manual entry for offline tracking
- Data persistence with Room database

## Requirements

- Android 8.0 (API 26) or higher
- Microphone permission for voice tracking
- Bluetooth (optional, for headphones)
- Google Speech Recognition

## How to Use

1. **Grant Permissions**
   - Allow microphone access when prompted
   - Allow Bluetooth and notification permissions

2. **Start Tracking**
   - Tap "Start Voice Tracking"
   - Connect your Bluetooth headphones
   - Start shooting!

3. **Record Shots**
   - Say "hit" or "make" when you score
   - Say "miss" or "missed" when you miss
   - Or use manual buttons for quick entry

4. **View Stats**
   - Check your shooting percentage
   - Switch between time periods (Today/Week/Month/Year)
   - Analyze trends with the chart

## Technical Details

### Architecture
- **MVVM Pattern** with ViewModels and LiveData
- **Repository Pattern** for data access
- **Room Database** for local storage
- **Coroutines** for async operations

### Key Components
- `ShotTrackingService`: Foreground service with speech recognition
- `ShotRepository`: Data layer with statistics calculations
- `MainViewModel`: Business logic and state management
- `MainActivity`: UI with Material Design components

### Voice Recognition
- Uses Android SpeechRecognizer API
- Continuous listening mode
- Low-latency recognition
- Music-friendly (doesn't pause playback)

## Building the App

1. Clone the repository
```bash
git clone https://github.com/yourusername/HoopTracker.git
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

The app requires the following permissions:

- `RECORD_AUDIO`: For voice recognition
- `BLUETOOTH` / `BLUETOOTH_CONNECT`: For Bluetooth headphone support
- `FOREGROUND_SERVICE`: To keep tracking active in background
- `POST_NOTIFICATIONS`: For persistent tracking notification

## Privacy

- All data is stored locally on your device
- No internet connection required
- No data collection or analytics
- Your shooting stats stay private

## Future Enhancements

- [ ] Export stats to CSV
- [ ] Session tracking with notes
- [ ] Multiple user profiles
- [ ] Shot location tracking
- [ ] Integration with fitness apps
- [ ] Dark mode
- [ ] Widget support

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

Made with ❤️ for basketball enthusiasts
