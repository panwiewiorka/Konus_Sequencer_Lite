package com.example.mysequencer01ui.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import com.example.mysequencer01ui.Note
import com.example.mysequencer01ui.PadsMode
import com.example.mysequencer01ui.PressedNote
import com.example.mysequencer01ui.RememberedPressInteraction

data class ChannelState(
    val channelStateNumber: Int,
    val notes: Array<Note> = emptyArray(),

    val isMuted: Boolean = false,
    val isSoloed: Boolean = false,
    val channelIsPlayingNotes: Int = 0,
    var playingNotes: Array<Int> = Array(128){ 0 },
    val pressedNotes: Array<PressedNote> = Array(128){ PressedNote(false, Int.MAX_VALUE, 0) },
    val onPressedMode: PadsMode = PadsMode.DEFAULT,

    val stepViewYScroll: Int = 3300,
    val stepViewRefresh: Boolean = false,
    val pianoViewOctaveHigh: Int = 4,
    val pianoViewOctaveLow: Int = 2,

    val seqLength: Int = 4, // future feature
    val totalTime: Int = BARTIME,
    val deltaTime: Double = 0.0,

    val deltaTimeRepeat: Double = 0.0,
    val repeatStartTime: Double = 0.0,
    val repeatEndTime: Double = 0.0,
)