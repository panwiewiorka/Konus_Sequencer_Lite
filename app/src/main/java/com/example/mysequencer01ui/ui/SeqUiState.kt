package com.example.mysequencer01ui.ui

import com.example.mysequencer01ui.SeqMode
import com.example.mysequencer01ui.Sequence

data class SeqUiState (
    val showSettings: Boolean = false,
    val bpm: Float = 120f,
    val factorBpm: Double = 1.0,
    val seqIsPlaying: Boolean = false,
    val seqIsRecording: Boolean = false,
    val seqMode: SeqMode = SeqMode.DEFAULT,
    val visualArrayRefresh: Boolean = false,
    val selectedChannel: Int = 0,
    val sequences: MutableList<Sequence> = MutableList(16){ Sequence(it)},
    )