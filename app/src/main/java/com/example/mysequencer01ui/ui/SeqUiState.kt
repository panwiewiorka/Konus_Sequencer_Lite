package com.example.mysequencer01ui.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.KmmkComponentContext
import com.example.mysequencer01ui.PadsMode
import com.example.mysequencer01ui.SeqView
import com.example.mysequencer01ui.Sequence

data class SeqUiState (
    val kmmk: KmmkComponentContext,
    val seqView: SeqView = SeqView.LIVE,
    val bpm: Float = 120f,
    val factorBpm: Double = 1.0,
    val timingClock: Double = 500.0 / 24,

    val seqIsPlaying: Boolean = false,
    val seqIsRecording: Boolean = false,
    val padsMode: PadsMode = PadsMode.DEFAULT,
    val muteIsOn: Boolean = false,
    val soloIsOn: Boolean = false,
    val selectedChannel: Int = 0,

    val isQuantizing: Boolean = true,
    val quantizationValue: Int = 16,
    val quantizationTime: Double = BARTIME.toDouble() / quantizationValue,
    val quantizeModeTimer: Int = 0,

    val isRepeating: Boolean = false,
    val divisorState: Int = 0,
    val repeatLength: Double = 0.0,

    val transmitClock: Boolean = false,
    val visualDebugger: Boolean = false,
    val debuggerViewSetting: Int = 0,
    val stepViewNoteHeight: Dp = 20.dp,
    val visualArrayRefresh: Boolean = false,

    val padsState: Array<Boolean> = Array(16){false},
    val sequences: MutableList<Sequence> = MutableList(16){ Sequence(it, kmmk) },
    )