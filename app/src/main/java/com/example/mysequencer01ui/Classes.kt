package com.example.mysequencer01ui

data class Note (
    var time: Long,
    var channel: Int = 0,
    var pitch: Int = 60,
    var velocity: Int = 100,
        )
