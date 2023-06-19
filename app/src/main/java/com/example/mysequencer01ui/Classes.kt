package com.example.mysequencer01ui

data class Note (
    var time: Int,
    var pitch: Int = 60,
    var velocity: Int = 100,
    var length: Int = 0,
)

enum class PadsMode{
    DEFAULT, SELECTING, QUANTIZING, SAVING, LOADING, SOLOING, MUTING, ERASING, CLEARING
}

enum class StopNotesMode{
    STOPSEQ, MUTE, END_OF_REPEAT
}

enum class SeqView{
    LIVE, STEP, AUTOMATION, SONG, SETTINGS
}


//
//class Converters {
//    @TypeConverter
//    fun listToJson(value: Array<Note>?) = Gson().toJson(value)
//
//    @TypeConverter
//    fun jsonToList(value: String) = Gson().fromJson(value, Array<Note>::class.java)
//}