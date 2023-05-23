package com.example.mysequencer01ui.ui

import com.example.mysequencer01ui.Note

data class SeqUiState (
    val bpm: Int = 120,
    val seqLength: Int = 4,
    val seqIsPlaying: Boolean = false,
    val seqIsRecording: Boolean = false,
    val muteMode: Boolean = false,
    val eraseMode: Boolean = false,
    val clearMode: Boolean = false,
    val seqStartTime: Long = 0L,
    val seqTotalTime: Long = (60f / bpm * seqLength * 1000).toLong(),
    val deltaTime: Long = 0L,
    val channelIsPlaying: Array<Boolean> = Array(16) { false },
    val stepSequencer: Array<Note> = emptyArray(),
    )