package com.example.mysequencer01ui.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.PadsMode
import com.example.mysequencer01ui.SeqView
import com.example.mysequencer01ui.Sequence

data class SeqUiState (
    val seqView: SeqView = SeqView.LIVE,
    val stepViewNoteHeight: Dp = 20.dp,
    val bpm: Float = 120f,
    val factorBpm: Double = 1.0,
    val timingClock: Double = 500.0 / 24,

    val transmitClock: Boolean = true,
    val lazyKeyboard: Boolean = false,
    val visualDebugger: Boolean = false,
    val debuggerViewSetting: Int = 1,

    val seqIsPlaying: Boolean = false,
    val seqIsRecording: Boolean = false,
    val padsMode: PadsMode = PadsMode.DEFAULT,
    val visualArrayRefresh: Boolean = false,
    val selectedChannel: Int = 0,
    val isQuantizing: Boolean = true,
    val quantizationValue: Int = 16,
    val quantizeModeTimer: Int = 0,
    val isRepeating: Boolean = false,
    val divisorState: Int = 0,
    val repeatLength: Double = 0.0,
    val muteIsOn: Boolean = false,
    val soloIsOn: Boolean = false,
    val repeatStartFlag: Boolean = false,
    val interactionSources: Array<Array<Pair<MutableInteractionSource, PressInteraction.Press>>> = Array(16) {
        Array(128) {
            Pair(
                MutableInteractionSource(),
                PressInteraction.Press( Offset(0f,0f) )
            )
        }
    },
    val sequences: MutableList<Sequence> = MutableList(16){ Sequence(it) },
    )