package com.example.mysequencer01ui

data class Note (
    val time: Long,
    val channel: Int,
    val pitch: Int = 60,
    val velocity: Int = 100,
        )

enum class SeqModes{
    DEFAULT, MUTING, ERASING, CLEARING
}