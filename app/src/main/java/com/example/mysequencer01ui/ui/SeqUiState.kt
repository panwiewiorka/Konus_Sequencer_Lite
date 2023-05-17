package com.example.mysequencer01ui.ui

data class SeqUiState (
    val bpm: Int = 120,
    val seqLength: Int = 4,
    val seqIsPlaying: Boolean = false,
    val seqStartTime: Long = 0L,
    val seqTotalTime: Long = (60f / bpm * seqLength * 1000).toLong(),
    val deltaTime: Long = 0L,
    val note1IsPlaying: Boolean = false,
    val note2IsPlaying: Boolean = false,
    val note3IsPlaying: Boolean = false,
    val note4IsPlaying: Boolean = false,
    val noteStartTime: Long = 0L,
    val noteLegnth: Long = 0L,
    )