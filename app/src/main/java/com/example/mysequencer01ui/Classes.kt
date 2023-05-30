package com.example.mysequencer01ui

data class Note (
    var time: Int,
    val pitch: Int = 60,
    val velocity: Int = 100,
    //val length: Int = 0,
)

enum class SeqMode{
    DEFAULT, MUTING, ERASING, CLEARING
}