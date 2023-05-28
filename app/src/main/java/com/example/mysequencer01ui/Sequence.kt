package com.example.mysequencer01ui

class Sequence (
    bpm: Int,
    // channel: Int, ?
    var notes: Array<Note> = emptyArray(),
    var indexToPlay: Int = 0,
    var startTimeStamp: Long = 0,
    var seqLength: Int = 4,
    var totalTime: Long = (60f / bpm * seqLength * 1000).toLong(),
    var deltaTime: Long = 0L,
    var isMuted: Boolean = false,
    var isErasing: Boolean = false,
    var noteOnStates: Array<Boolean> = Array(128){false},
    var pressedNotes: Array<Boolean> = Array(128){false},
){
    fun recordNote(channel: Int, pitch: Int, velocity: Int, staticNoteOffTime: Long, seqIsPlaying: Boolean) {

        val recordTime: Long

        if(seqIsPlaying){
            recordTime = System.currentTimeMillis() - startTimeStamp
        } else {
            if(velocity > 0) {
                indexToPlay = 0
                recordTime = 0L
            }
            else {
                indexToPlay = notes.indexOfFirst { it.time > 0 }
                if(indexToPlay < 0) indexToPlay = notes.size
                recordTime = staticNoteOffTime
            }
        }

        when {
            // end of array
            indexToPlay == notes.size -> {
                notes += Note(recordTime, channel, pitch, velocity)
            }
            // beginning of array
            notes.isNotEmpty() && indexToPlay == 0 -> {
                val tempNotes = notes
                notes = Array(1){ Note(recordTime, channel, pitch, velocity) } + tempNotes
            }
            // middle of array
            else -> {
                val tempNotes1 = notes.copyOfRange(0, indexToPlay)
                val tempNotes2 = notes.copyOfRange(indexToPlay, notes.size)
                notes = tempNotes1 + Note(recordTime, channel, pitch, velocity) + tempNotes2
            }
        }
    }


    fun playing(kmmk: KmmkComponentContext) {
        // play note (if it's not being manually played, or if channel isn't muted)
        // remember which notes are playing
        // update UI
        // if (next_note.time > deltaTime) OR no other notes exist -> increase index, exit from while()

        if (!pressedNotes[notes[indexToPlay].pitch] && (!isMuted || notes[indexToPlay].velocity == 0)) {
            kmmk.noteOn(
                notes[indexToPlay].channel,
                notes[indexToPlay].pitch,
                notes[indexToPlay].velocity
            )
            noteOnStates[notes[indexToPlay].pitch] = notes[indexToPlay].velocity > 0
        }
    }


    fun erasing(): Boolean {
        if (notes[indexToPlay].velocity > 0) {
            val searchedPitch = notes[indexToPlay].pitch
            var breakFlag = false

            // erasing noteON
            when {
                // end of array
                indexToPlay == notes.lastIndex -> {
                    if (notes.size > 1) {
                        notes = notes.copyOfRange(0, indexToPlay)
                    } else {
                        notes = emptyArray()
                    }
                    breakFlag = true
                }
                // beginning of array
                indexToPlay == 0 -> {
                    notes = notes.copyOfRange(1, notes.size)
                }
                // middle of array
                else -> {
                    val tempNotes = notes.copyOfRange(indexToPlay + 1, notes.size)
                    notes = notes.copyOfRange(0, indexToPlay) + tempNotes
                }
            }

            // searching for paired noteOFF
            var pairedNoteOffIndex = -1
            var searchedIndex: Int
            if (indexToPlay <= notes.lastIndex) {
                searchedIndex = notes
                    .copyOfRange(indexToPlay, notes.size)
                    .indexOfFirst { it.pitch == searchedPitch && it.velocity == 0 }
                pairedNoteOffIndex = if (searchedIndex == -1) -1 else searchedIndex + indexToPlay
            }
            if (pairedNoteOffIndex == -1) {
                searchedIndex = notes
                    .copyOfRange(0, indexToPlay)
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

            // updating indexToPlay when erasing note before it
            if (pairedNoteOffIndex < indexToPlay && pairedNoteOffIndex > -1) {
                indexToPlay--
            }
            if (breakFlag) return false // break out of while() when erased last note in array

        } else indexToPlay++  // skipping noteOFFs
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
    }
}