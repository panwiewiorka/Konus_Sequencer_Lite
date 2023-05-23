package com.example.mysequencer01ui

data class Note (
    val time: Long,
    val channel: Int = 0,
    val pitch: Int = 60,
    val velocity: Int = 100,
        )
