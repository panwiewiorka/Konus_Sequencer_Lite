package com.example.mysequencer01ui

data class Note (
    var time: Int,
    var pitch: Int = 60,
    var velocity: Int = 100,
    var length: Int = 0,
)

enum class PadsMode{
    DEFAULT, MUTING, ERASING, CLEARING, SELECTING, SAVING, LOADING
}

enum class StopNotesMode{
    STOPSEQ, RECOFF_OR_MUTE, END_OF_REPEAT
}
//
//class Converters {
//    @TypeConverter
//    fun listToJson(value: Array<Note>?) = Gson().toJson(value)
//
//    @TypeConverter
//    fun jsonToList(value: String) = Gson().fromJson(value, Array<Note>::class.java)
//}