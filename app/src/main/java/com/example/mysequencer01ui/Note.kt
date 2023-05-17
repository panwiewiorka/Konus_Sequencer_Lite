package com.example.mysequencer01ui

data class Note (
    var pitch: Int = 60,
    var velocity: Int = 100,
    var startTime: Long = 0L,
    var length: Long = 100L,
        )