package com.example.mysequencer01ui

data class Note (
    val time: Long,
    val channel: Int,
    val pitch: Int = 60,
    val velocity: Int = 100,
        )

class Sequence (
    bpm: Int,
    //val channel: Int, // is it needed?
    var notes: Array<Note> = emptyArray(),
    var indexToPlay: Int = 0,
    var startTimeStamp: Long = 0,
    var seqLength: Int = 4,
    var totalTime: Long = (60f / bpm * seqLength * 1000).toLong(),
    var deltaTime: Long = 0L,
    var isMuted: Boolean = false,
    var isErasing: Boolean = false,
    var noteOnStates: Array<Boolean> = Array(128){false},
        ){

}

enum class SeqModes{
    DEFAULT, MUTING, ERASING, CLEARING
}