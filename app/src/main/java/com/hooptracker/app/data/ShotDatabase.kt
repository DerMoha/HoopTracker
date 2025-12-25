package com.hooptracker.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Shot::class, Session::class, Goal::class],
    version = 2,
    exportSchema = false
)
abstract class ShotDatabase : RoomDatabase() {
    abstract fun shotDao(): ShotDao
    abstract fun sessionDao(): SessionDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: ShotDatabase? = null

        fun getDatabase(context: Context): ShotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShotDatabase::class.java,
                    "shot_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
