package com.hooptracker.app

import android.app.Application
import com.hooptracker.app.data.Preferences
import com.hooptracker.app.data.ShotDatabase
import com.hooptracker.app.data.ShotRepository

class HoopTrackerApplication : Application() {
    private val database by lazy { ShotDatabase.getDatabase(this) }
    val repository by lazy {
        ShotRepository(
            database.shotDao(),
            database.sessionDao(),
            database.goalDao(),
            this
        )
    }
    val preferences by lazy { Preferences(this) }
}
