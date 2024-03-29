package com.example.mysequencer01ui.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Patterns(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val pattern: Int,
    val channel: Int,
    val noteIndex: Int,
    val time: Double,
    val pitch: Int,
    val velocity: Int,
    val noteId: Int,
)
