package com.example.mysequencer01ui

data class Note (
    var time: Int,
    var pitch: Int,
    var velocity: Int,
    var id: Int,
//    var length: Int = 0,
)

enum class PadsMode{
    DEFAULT, SELECTING, QUANTIZING, SAVING, LOADING, SOLOING, MUTING, ERASING, CLEARING
}

enum class StopNotesMode{
    STOPSEQ, MUTE, END_OF_REPEAT
}

enum class SeqView{
    LIVE, STEP, PIANO, AUTOMATION, SETTINGS
}

enum class PianoKeysType(val keyIsWhite: Boolean){
    WHITE(true),
    BLACK(false)
}