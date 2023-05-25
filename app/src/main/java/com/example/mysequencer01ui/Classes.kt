package com.example.mysequencer01ui

data class Note (
    val time: Long,
    val channel: Int = 0,
    val pitch: Int = 60,
    val velocity: Int = 100,
        )

data class Sequence (
    val channel: Int,
    val notes: Array<Note>,
    val indexToPlay: Int,
    val startTimeStamp: Long,
    val totalTime: Long,
    val deltaTime: Long,
        )

enum class SeqModes{
    DEFAULT, MUTING, ERASING, CLEARING
}