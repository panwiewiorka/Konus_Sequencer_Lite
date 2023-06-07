package com.example.mysequencer01ui

import android.util.Log

class Sequence (
    val channel: Int,
    var notes: Array<Note> = emptyArray(),

    var isMuted: Boolean = false,
    var isErasing: Boolean = false,
    var channelIsPlayingNotes: Boolean = false,
    var noteOnStates: Array<Boolean> = Array(128){false},
    var pressedNotes: Array<Boolean> = Array(128){false}, // manually pressed notes that are muting same ones played by sequencer
    var onPressedMode: PadsMode = PadsMode.DEFAULT,

    var indexToPlay: Int = 0,
    var startTimeStamp: Long = 0,
    var seqLength: Int = 4,
    var totalTime: Int = 2000, // TODO how are totalTime & seqLength correlated? Replace totalTime with relative one?
    var deltaTime: Int = 0,
    var factoredDeltaTime: Double = 0.0,

    var indexToStartRepeating: Int = 0,
    var indexToRepeat: Int = 0,
    var startTimeStampRepeat: Double = 0.0,
    //var totalTimeRepeat: Int = 2000,
    var deltaTimeRepeat: Double = 0.0,
    var factoredDeltaTimeRepeat: Double = 0.0,
    var timeOfRepeatStart: Double = 0.0,
    var repeatsCount: Int = 0,
    ) {
    fun recordNote(pitch: Int, velocity: Int, staticNoteOffTime: Int, seqIsPlaying: Boolean, isRepeating: Boolean, repeatTime: Double, factorBpm: Double) {

        // Recording to beginning if Seq is stopped
        val recordTime: Int
        if(seqIsPlaying){
            if(isRepeating) {
                recordTime = ((System.currentTimeMillis() - startTimeStampRepeat - repeatTime) * factorBpm).toInt()
            } else recordTime = ((System.currentTimeMillis() - startTimeStamp) * factorBpm).toInt()
        } else {
            if(velocity > 0) {
                indexToPlay = 0
                recordTime = 0
            }
            else {
                indexToPlay = notes.indexOfFirst { it.time > 0 }
                if(indexToPlay < 0) indexToPlay = notes.size
                recordTime = staticNoteOffTime
            }
        }

        // NOTE LENGTH (for StepSeq)
        var pairedNoteOnIndex = -1
        if(velocity == 0) {
            // searching for paired NoteON
            var searchedIndex: Int
            // searching backward from indexToPlay
            if (indexToPlay > 0) {
                searchedIndex = notes
                    .copyOfRange(0, indexToPlay)
                    .indexOfLast { it.pitch == pitch && it.velocity > 0 }
                pairedNoteOnIndex = if (searchedIndex == -1) -1 else searchedIndex
            }
            // searching backward from end of array
            if (pairedNoteOnIndex == -1) {
                searchedIndex = notes
                    .copyOfRange(indexToPlay, notes.size)
                    .indexOfLast { it.pitch == pitch && it.velocity > 0 }
                pairedNoteOnIndex = if (searchedIndex == -1) -1 else searchedIndex + indexToPlay
            }
        }

        // RECORDING
        if(!(!noteOnStates[pitch] && velocity == 0)) {  // to disable writing second NoteOFF when note is already off
            if(velocity == 0 && pairedNoteOnIndex != -1) {
                notes[pairedNoteOnIndex].length = recordTime - notes[pairedNoteOnIndex].time
            }
            when {
                // end of array
                indexToPlay == notes.size -> {
                    notes += Note(recordTime, pitch, velocity)
                }
                // beginning of array
                notes.isNotEmpty() && indexToPlay == 0 -> {
                    val tempNotes = notes
                    notes = Array(1){ Note(recordTime, pitch, velocity) } + tempNotes
                }
                // middle of array
                else -> {
                    val tempNotes1 = notes.copyOfRange(0, indexToPlay)
                    val tempNotes2 = notes.copyOfRange(indexToPlay, notes.size)
                    notes = tempNotes1 + Note(recordTime, pitch, velocity) + tempNotes2
                }
            }
            channelIsPlayingNotes = velocity > 0
        }
    }


    fun playing(kmmk: KmmkComponentContext, index: Int) {
        // play note (if it's not being manually played, or if channel isn't muted)
        // remember which notes are playing
        // update channelIsPlayingNotes for visual info

        if (!pressedNotes[notes[index].pitch] && (!isMuted || notes[index].velocity == 0)) {
            kmmk.noteOn(
                channel,
                notes[index].pitch,
                notes[index].velocity
            )
            noteOnStates[notes[index].pitch] = notes[index].velocity > 0
        }
        channelIsPlayingNotes = notes[index].velocity > 0
    }


    fun erasing(isRepeating: Boolean, index: Int): Boolean {
        if (notes[index].velocity > 0) {
            val searchedPitch = notes[index].pitch
            var breakFlag = false

            // erasing noteON
            when {
                // end of array
                index == notes.lastIndex -> {
                    if (notes.size > 1) {
                        notes = notes.copyOfRange(0, index)
                    } else {
                        notes = emptyArray()
                    }
                    breakFlag = true
                }
                // beginning of array
                index == 0 -> {
                    notes = notes.copyOfRange(1, notes.size)
                }
                // middle of array
                else -> {
                    val tempNotes = notes.copyOfRange(index + 1, notes.size)
                    notes = notes.copyOfRange(0, index) + tempNotes
                }
            }

            // searching for paired noteOFF
            var pairedNoteOffIndex = -1
            var searchedIndex: Int
            // searching forward from indexToPlay
            if (index <= notes.lastIndex) {
                searchedIndex = notes
                    .copyOfRange(index, notes.size)
                    .indexOfFirst { it.pitch == searchedPitch && it.velocity == 0 }
                pairedNoteOffIndex = if (searchedIndex == -1) -1 else searchedIndex + index
            }
            // searching forward from 0
            if (pairedNoteOffIndex == -1) {
                searchedIndex = notes
                    .copyOfRange(0, index)
                    .indexOfFirst { it.pitch == searchedPitch && it.velocity == 0 }
                pairedNoteOffIndex = if (searchedIndex == -1) -1 else searchedIndex
            }

            // erasing paired noteOFF
            when {
                // no paired noteOFF found
                pairedNoteOffIndex == -1 -> {  // do nothing
                }
                // end of array
                pairedNoteOffIndex == notes.lastIndex -> {
                    if (notes.size > 1) {
                        notes = notes.copyOfRange(0, pairedNoteOffIndex)
                    } else {
                        notes = emptyArray()
                    }
                    breakFlag = true
                }
                // beginning of array
                pairedNoteOffIndex == 0 -> {
                    notes = notes.copyOfRange(1, notes.size)
                }
                // middle of array
                else -> {
                    val tempNotes = notes.copyOfRange(pairedNoteOffIndex + 1, notes.size)
                    notes = notes.copyOfRange(0, pairedNoteOffIndex) + tempNotes
                }
            }

            // updating index when erasing note before it
            if (pairedNoteOffIndex < index && pairedNoteOffIndex > -1) {
                if(isRepeating) indexToRepeat-- else indexToPlay--
            }
            if (breakFlag) return false // break out of while() when erased last note in array

        } else if(isRepeating) indexToRepeat++ else indexToPlay++  // skipping noteOFFs
        return true
    }


    fun clearChannel(channel: Int, kmmk: KmmkComponentContext) {
        indexToPlay = 0
        notes = emptyArray()
        for(p in 0..127){
            if(noteOnStates[p]) {
                kmmk.noteOn(channel, p, 0)
                noteOnStates[p] = false
            }
        }
        Log.d("emptyTag", " ") // to hold in imports
    }
}