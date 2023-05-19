package com.example.mysequencer01ui.ui

data class SeqUiState (
    val bpm: Int = 120,
    val seqLength: Int = 4,
    val seqIsPlaying: Boolean = false,
    val seqIsRecording: Boolean = false,
    val muteMode: Boolean = false,
    val clearMode: Boolean = false,
    val delMode: Boolean = false,
    val seqStartTime: Long = 0L,
    val seqTotalTime: Long = (60f / bpm * seqLength * 1000).toLong(),
    val deltaTime: Long = 0L,
    val channelIsPlaying: List<Boolean> = List(16) { false },
    )