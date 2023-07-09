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

    var stepViewYScroll: Int = 0,
    var pianoViewLowPianoScroll: Int = 7,
    var pianoViewHighPianoScroll: Int = 21,

    var indexToPlay: Int = 0,
    var startTimeStamp: Long = 0,
    var seqLength: Int = 4,
    var totalTime: Int = 2000, // TODO how are totalTime & seqLength correlated? Replace totalTime with relative one?
    var deltaTime: Double = 0.0,

    var indexToStartRepeating: Int = 0,
    var indexToRepeat: Int = 0,
    var deltaTimeRepeat: Double = 0.0,
    var repeatEndTime: Double = 0.0,
    var repeatsCount: Int = 0,
    var savedRepeatsCount: Int = 0,
    var wrapDelta: Int = 0,
    ) {

    fun recordNote(
        pitch: Int,
        velocity: Int,
        staticNoteOffTime: Int,
        seqIsPlaying: Boolean,
//        isQuantizing: Boolean,
//        quantization: Int,
        isRepeating: Boolean,
        repeatLength: Double,
        customTime: Int = -1,
        stepRecord: Boolean = false,
        noteHeight: Float = 60f
    ) {
        val customRecTime = if(customTime >= totalTime) customTime - totalTime else customTime
        val recordTime: Int
        if(customRecTime > -1) {
//            recordTime = if(isQuantizing) {
//                val remainder = customRecTime % (totalTime / quantization)
//                if(remainder > quantization / 2)
//                    customRecTime - remainder + quantization
//                else
//                    customRecTime - remainder
//            } else customRecTime
            recordTime = customRecTime
        } else {
             if(seqIsPlaying){
                 recordTime = if(isRepeating) {
                     if(deltaTimeRepeat + wrapDelta < 0)   // wrap-around
                         (deltaTimeRepeat + wrapDelta + totalTime).toInt()
                     else
                         (deltaTimeRepeat + wrapDelta).toInt()
                 } else deltaTime.toInt()
             } else {            // Recording to beginning if Seq is stopped
                 if(velocity > 0) {
                     indexToPlay = 0 // TODO add indexToRepeat?
                     recordTime = 0
                 }
                 else {
                     indexToPlay = notes.indexOfFirst { it.time > 0 }
                     if(indexToPlay < 0) indexToPlay = notes.size
                     recordTime = staticNoteOffTime
                 }
             }
        }

        var index = if(stepRecord) {
            val searchedIndex = notes.indexOfLast { it.time <= customRecTime } + 1
            if(searchedIndex != 0 && velocity == 0 && notes[searchedIndex - 1].time == customRecTime) searchedIndex - 1 else searchedIndex
        } else if(isRepeating) indexToRepeat else indexToPlay

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
        if (!stepRecord && playingNotes[pitch] && velocity > 0) {
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
            if(isRepeating && pairedNoteOffIndex in 0 until indexToRepeat) indexToRepeat--
            if(pairedNoteOffIndex in 0 until indexToPlay) indexToPlay--
//            if (pairedNoteOffIndex < index && pairedNoteOffIndex > -1) {
//                if(isRepeating) indexToRepeat--
//                indexToPlay--
//            }

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
                if(deltaTime >= repeatEndTime - repeatLength) indexToPlay++
            } else indexToPlay++
            channelIsPlayingNotes = velocity > 0
//        }

        if(!stepRecord) stepViewYScroll = (noteHeight * pitch
                //- notesGridHeight / 2
                ).toInt()
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


    fun erasing(kmmk: KmmkComponentContext, isRepeating: Boolean, index: Int): Boolean {
        if (notes[index].velocity > 0) {
            var breakFlag = false

            // searching for paired noteOFF
            val pairedNoteOff = returnPairedNoteOffIndexAndTime(index)
            var pairedNoteOffIndex = pairedNoteOff.first
            val pairedNoteOffTime = pairedNoteOff.second

            // stop note_to_be_erased if playing (StepView condition)
            val dt = if(isRepeating) deltaTimeRepeat else deltaTime
            val deltaTimeInRange = if(index < pairedNoteOffIndex) {
                dt in notes[index].time.toDouble()..notes[pairedNoteOffIndex].time.toDouble()
            } else {
                dt in notes[index].time.toDouble()..totalTime.toDouble() || dt in 0.0..notes[pairedNoteOffIndex].time.toDouble()
            }
            if(isRepeating) {
                if(playingNotes[notes[index].pitch] && deltaTimeInRange) {
                    kmmk.noteOn(channel, notes[index].pitch, 0)
                    playingNotes[notes[index].pitch] = false
                    channelIsPlayingNotes = false // TODO !!!
                }
            } else {
                if(playingNotes[notes[index].pitch] && deltaTimeInRange) {
                    kmmk.noteOn(channel, notes[index].pitch, 0)
                    playingNotes[notes[index].pitch] = false
                    channelIsPlayingNotes = false // TODO !!!
                }
            }
            if(pairedNoteOff.first > index) pairedNoteOffIndex -= 1

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
                        Log.d("ryjtyj", "erasing noteOff = emptyArray. index = $index, pairedNoteOffIndex = $pairedNoteOffIndex")
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
                if(isRepeating) if(indexToRepeat > 0) indexToRepeat-- else if(indexToPlay > 0) indexToPlay--
            }
            if (breakFlag) return false // break out of while() when erased last note in array

        } else if(isRepeating) indexToRepeat++ else indexToPlay++  // skipping noteOFFs
        return true
    }


    fun clearChannel(channel: Int, kmmk: KmmkComponentContext) {
        indexToPlay = 0
        indexToRepeat = 0
        indexToStartRepeating = 0
        notes = emptyArray()
        for(p in 0..127){
            if(playingNotes[p]) {
                kmmk.noteOn(channel, p, 0)
                playingNotes[p] = false
            }
        }
        Log.d("emptyTag", " ") // to hold in imports
    }


    fun updateStepViewYScroll(y: Int) {
        stepViewYScroll = y
    }

    fun updatePianoViewXScroll(x: Int, lowerPiano: Boolean = false) {
        if(lowerPiano) pianoViewLowPianoScroll = x else pianoViewHighPianoScroll = x
    }

    fun returnPairedNoteOffIndexAndTime(index: Int): Pair<Int, Int> {
        val searchedPitch = notes[index].pitch
        var pairedNoteOffIndex = -1
        var searchedIndex: Int
        // searching forward from indexToPlay
        if (index < notes.lastIndex) {
            searchedIndex = notes
                .copyOfRange(index + 1, notes.size)
                .indexOfFirst { it.pitch == searchedPitch && it.velocity == 0 }
            pairedNoteOffIndex = if (searchedIndex == -1) -1 else index + 1 + searchedIndex
        }
        // searching forward from 0
        if (pairedNoteOffIndex == -1) {
            searchedIndex = notes
                .copyOfRange(0, index)
                .indexOfFirst { it.pitch == searchedPitch && it.velocity == 0 }
            pairedNoteOffIndex = if (searchedIndex == -1) -1 else searchedIndex
        }
        return if(pairedNoteOffIndex > -1) Pair(pairedNoteOffIndex, notes[pairedNoteOffIndex].time) else Pair(-1, -2)
    }

    fun changeNotePitch(index: Int, pitch: Int) {
        notes[index].pitch = pitch
    }

    fun changeNoteTime(index: Int, time: Int, isDeltaTime: Boolean = false) {
        notes[index].time = if(isDeltaTime) {
            notes[index].time + time
        } else {
            time
        }
        if(notes[index].time >= 2000) {
            notes[index].time -= 2000
        } else if(notes[index].time < 0) {
            notes[index].time += 2000
        }
    }

    fun sortNotesByTime(){
        notes.sortBy { it.time }
    }
}