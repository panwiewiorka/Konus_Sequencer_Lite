package com.example.mysequencer01ui.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [
    Patterns::class,
    Settings::class
], version = 1, exportSchema = false)
abstract class SeqDatabase: RoomDatabase() {
    abstract val dao: SeqDao
    //abstract fun dao(): SettingsDao
/*
    companion object {
        @Volatile
        private var Instance: SettingsDatabase? = null

        fun getDatabase(context: Context): SettingsDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, SettingsDatabase::class.java, "settingsTable")
                    // Setting this option in your app's database builder means that Room
                    // permanently deletes all data from the tables in your database when it
                    // attempts to perform a migration with no defined migration path.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }

 */
}