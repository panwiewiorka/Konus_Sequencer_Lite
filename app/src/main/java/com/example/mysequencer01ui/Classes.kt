package com.example.mysequencer01ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction

data class Note (
    var time: Double,
    var pitch: Int,
    var velocity: Int,
    var id: Int,
)

data class NoteIndexAndTime (
    var index: Int,
    var time: Double,
)

data class NoteIdAndVelocity (
    var id: Int,
    val velocity: Int
)

data class PressedNote (
    var isPressed: Boolean,
    var id: Int,
    var noteOnTimestamp: Long
)

data class RememberedPressInteraction (
    var interactionSource: MutableInteractionSource,
    var pressInteraction: PressInteraction.Press,
)

data class KeyColorAndNumber (
    var isBlack: Boolean,
    var number: Int,
)

data class RecordingPackage (
    val recordTime: Double,
    val pitch: Int,
    val id: Int,
    val velocity: Int,
    val isStepView: Boolean,
    val isStepRecord: Boolean,
    val noteHeight: Float
)

data class PressPadPackage(
    val channel: Int,
    val pitch: Int,
    val velocity: Int,
    val elapsedTime: Long,
    val allButton: Boolean,
)

//-------------------------------------

enum class PadsMode{
    DEFAULT, SELECTING, QUANTIZING, SAVING, LOADING, SOLOING, MUTING, ERASING, CLEARING
}

enum class StopNotesMode{
    STOP_SEQ, STOP_NOTES, END_OF_REPEAT
}

enum class SeqView{
    LIVE, STEP, PIANO, AUTOMATION, SETTINGS
}

enum class PianoKeysType(val keyIsWhite: Boolean){
    WHITE(true),
    BLACK(false)
}