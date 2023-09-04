package com.example.mysequencer01ui.ui

import com.example.mysequencer01ui.Note
import com.example.mysequencer01ui.PadsMode
import com.example.mysequencer01ui.PressedNote

data class ChannelState(
    val channelStateNumber: Int,
    val notes: Array<Note> = emptyArray(),

    val isMuted: Boolean = false,
    val isSoloed: Boolean = false,
    val channelIsPlayingNotes: Int = 0,
    var pressedNotes: Array<PressedNote> = Array(128){ PressedNote(false, Int.MAX_VALUE, Long.MIN_VALUE) }, // manually pressed notes that are muting same ones played by sequencer
    val onPressedMode: PadsMode = PadsMode.DEFAULT,
//    var noteId: Int = Int.MIN_VALUE,

//    var draggedNoteOnIndex: Int = -1,
//    var draggedNoteOffIndex: Int = -1,

    val stepViewYScroll: Int = 3300,
    val stepViewRefresh: Boolean = false,
    val pianoViewOctaveHigh: Int = 4,
    val pianoViewOctaveLow: Int = 2,

//    var bpmDelta: Double = 0.0,
    val seqLength: Int = 4, // future feature
    val totalTime: Int = BARTIME, // TODO how are totalTime & seqLength correlated? Replace totalTime with relative one?
    val deltaTime: Double = 0.0,

    val deltaTimeRepeat: Double = 0.0,
    val repeatStartTime: Double = 0.0,
    val repeatEndTime: Double = 0.0,
)