package com.example.mysequencer01ui

data class Note (
    var time: Int,
    val pitch: Int = 60,
    val velocity: Int = 100,
    var length: Int = 0,
)

enum class PadsMode{
    DEFAULT, MUTING, ERASING, CLEARING, SELECTING
}

enum class OnOff{ ON, OFF }