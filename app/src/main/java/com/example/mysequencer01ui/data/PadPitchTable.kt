package com.example.mysequencer01ui.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PadPitches(
    @PrimaryKey(autoGenerate = false)
    val id: Int = 0,
//    val channel: Int = 0,
    val pitch: Int = 60,
)
