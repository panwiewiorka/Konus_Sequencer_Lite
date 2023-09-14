package com.example.mysequencer01ui.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Settings(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val transmitClock: Boolean = false,
    val bpm: Float = 120f,
    val factorBpm: Double = 1.0,
    val isQuantizing: Boolean = true,
    val keepScreenOn: Boolean = false,
    val showChannelNumberOnPads: Boolean = false,
    val allowRecordShortNotes: Boolean = false,
    val fullScreen: Boolean = true,
    val toggleTime: Int = 300,
    val uiRefreshRate: Int = 3,
    val dataRefreshRate: Int = 3,
    val showVisualDebugger: Boolean = false,
    val debuggerViewSetting: Int = 0,
)
