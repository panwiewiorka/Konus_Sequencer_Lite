package com.example.mysequencer01ui

import android.util.Log

class Sequence (
    val channel: Int,
    var notes: Array<Note> = emptyArray(),

    var isMuted: Boolean = false,
    var isErasing: Boolean = false,
    var channelIsPlayingNotes: Boolean = false,  // TODO will not work in polyphony, change
    var playingNotes: Array<Boolean> = Array(128){false},
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
    var factoredDeltaTimeRepeat: Double = 0.0,
    var repeatEndTime: Double = 0.0,
    var repeatsCount: Int = 0,
    var savedRepeatsCount: Int = 0,
    var wrapDelta: Int = 0,
    ) {

    fun recordNote(pitch: Int, velocity: Int, staticNoteOffTime: Int, seqIsPlaying: Boolean, isRepeating: Boolean, repeatLength: Double, customRecTime: Int = -1) {

        val recordTime: Int
        if(seqIsPlaying){
            recordTime = if(customRecTime > -1) {
                customRecTime
            } else
                if(isRepeating) {
                    if(factoredDeltaTimeRepeat + wrapDelta < 0)   // wrap-around
                        (factoredDeltaTimeRepeat + wrapDelta + totalTime).toInt()
                    else
                        (factoredDeltaTimeRepeat + wrapDelta).toInt()
                } else factoredDeltaTime.toInt()
        } else {            // Recording to beginning if Seq is stopped
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

        var index = if(isRepeating) indexToRepeat else indexToPlay

        // NOTE LENGTH (for StepSeq)
        var pairedNoteOnIndex = -1
        if(velocity == 0 || (playingNotes[pitch] && velocity > 0)) {
            // searching for paired NoteON
            // searching backwards from index
            if (index > 0) {
                pairedNoteOnIndex = notes
                    .copyOfRange(0, index)
                    .indexOfLast { it.pitch == pitch && it.velocity > 0 }
            }
            // searching backwards from end of array
            if (pairedNoteOnIndex == -1) {
                pairedNoteOnIndex = notes
                    .copyOfRange(index, notes.size)
                    .indexOfLast { it.pitch == pitch && it.velocity > 0 }
                if (pairedNoteOnIndex != -1) pairedNoteOnIndex += index
            }
        }
        // updating previous noteON length
        if((velocity == 0 || (playingNotes[pitch] && velocity > 0)) && pairedNoteOnIndex != -1) {
            notes[pairedNoteOnIndex].length = recordTime - notes[pairedNoteOnIndex].time
        }

        // if same note is already playing -> find and erase it's paired noteOFF, and record noteOFF in current recordTime
        if (playingNotes[pitch] && velocity > 0) {
            var pairedNoteOffIndex = -1
            // searching for paired NoteOFF forward from index
            if (index < notes.size) {
                pairedNoteOffIndex = notes
                    .copyOfRange(index, notes.size)
                    .indexOfFirst { it.pitch == pitch && it.velocity == 0 }
                if (pairedNoteOffIndex != -1) pairedNoteOffIndex += index
            }
            // searching forward from start of array
            if (pairedNoteOffIndex == -1) {
                pairedNoteOffIndex = notes
                    .copyOfRange(0, index)
                    .indexOfFirst { it.pitch == pitch && it.velocity == 0 }
            }
            // erasing paired noteOFF // SAME AS PART 3 of fun erasing()
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
//                if(isRepeating) indexToRepeat-- else indexToPlay--
                if(isRepeating) indexToRepeat--
                indexToPlay--
            }

            // then record noteOFF to retrigger afterwards
            recIntoArray(index, recordTime, pitch, 0)
            if(isRepeating) indexToRepeat++ else indexToPlay++
            index++
        }

        // RECORDING
//        if(!(!playingNotes[pitch] && velocity == 0)) {  // to disable writing second NoteOFF when note is already off // TODO  redo (uncomment)
            recIntoArray(index, recordTime, pitch, velocity)
            if(isRepeating) {
                indexToRepeat++
                if(factoredDeltaTime >= repeatEndTime - repeatLength) indexToPlay++
            } else indexToPlay++
            channelIsPlayingNotes = velocity > 0
//        }


    }

    private fun recIntoArray(
        index: Int,
        recordTime: Int,
        pitch: Int,
        velocity: Int
    ) {
        when {
            // end of array
            index >= notes.size -> {
                notes += Note(recordTime, pitch, velocity)
            }
            // beginning of array
            notes.isNotEmpty() && index == 0 -> {
                val tempNotes = notes
                notes = Array(1) { Note(recordTime, pitch, velocity) } + tempNotes
            }
            // middle of array
            else -> {
                val tempNotes1 = notes.copyOfRange(0, index)
                val tempNotes2 = notes.copyOfRange(index, notes.size)
                notes = tempNotes1 + Note(recordTime, pitch, velocity) + tempNotes2
            }
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
            playingNotes[notes[index].pitch] = notes[index].velocity > 0
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
            if(playingNotes[p]) {
                kmmk.noteOn(channel, p, 0)
                playingNotes[p] = false
            }
        }
        Log.d("emptyTag", " ") // to hold in imports
    }
}