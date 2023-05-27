package com.example.mysequencer01ui.ui

import com.example.mysequencer01ui.Note
import com.example.mysequencer01ui.SeqModes
import com.example.mysequencer01ui.Sequence

data class SeqUiState (
    val bpm: Int = 120,
    val seqLength: Int = 4, // TODO per channel
    val seqIsPlaying: Boolean = false,
    val seqIsRecording: Boolean = false,
    val seqMode: SeqModes = SeqModes.DEFAULT,
    val muteButtonState: Boolean = false,
    val eraseButtonState: Boolean = false,
    val clearButtonState: Boolean = false,
    val visualArrayRefresh: Boolean = false,
    val seqStartTime: Array<Long> = Array(16){0L},
    val seqTotalTime: Array<Long> = Array(16){(60f / bpm * seqLength * 1000).toLong()},
    val deltaTime: Array<Long> = Array(16){0L},
    val channelIsActive: Array<Boolean> = Array(16) { false },
    val visualArray: MutableList<Sequence> = MutableList(16){Sequence(bpm)},
    )