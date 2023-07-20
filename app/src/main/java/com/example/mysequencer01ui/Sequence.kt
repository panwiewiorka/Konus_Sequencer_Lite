package com.example.mysequencer01ui

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Sequence (
    val channel: Int,
    var notes: Array<Note> = emptyArray(),

    var isMuted: Boolean = false,
    var isErasing: Boolean = false,
    var channelIsPlayingNotes: Boolean = false,  // TODO will not work in polyphony, change
    var playingNotes: Array<Int> = Array(128){ 0 },
    var pressedNotes: Array<Pair<Boolean, Int>> = Array(128){Pair(false, Int.MAX_VALUE)}, // manually pressed notes (and IDs) that are muting same ones played by sequencer
    var onPressedMode: PadsMode = PadsMode.DEFAULT,
    var noteId: Int = Int.MIN_VALUE,

    var draggedNoteOnIndex: Int = -1,
    var draggedNoteOffIndex: Int = -1,

    var stepViewYScroll: Int = 0, // TODO float?
    var pianoViewOctaveHigh: Int = 4,
    var pianoViewOctaveLow: Int = 2,

    var indexToPlay: Int = 0,
    var startTimeStamp: Long = 0,
    var seqLength: Int = 4,
    var totalTime: Int = 2000, // TODO how are totalTime & seqLength correlated? Replace totalTime with relative one?
    var deltaTime: Double = 0.0,

    var indexToStartRepeating: Int = 0,
    var indexToRepeat: Int = 0,
    var deltaTimeRepeat: Double = 0.0,
    var repeatStartTime: Double = 0.0,
    var repeatEndTime: Double = 0.0,
    var repeatsCount: Int = 0,
    var savedRepeatsCount: Int = 0,
    var wrapDelta: Int = 0,
    var wrapTime: Int = 0,
    var wrapIndex: Int = 0,
    var fullDeltaTimeRepeat: Double = 0.0,
    var tempNotesSize: Int = 0,
    ) {

    fun recordNote(
        pitch: Int,
        velocity: Int,
        id: Int,
        staticNoteOffTime: Int,
        seqIsPlaying: Boolean,
        isRepeating: Boolean,
        customTime: Int = -1,
        stepRecord: Boolean = false,
        noteHeight: Float = 60f
    ) {
        val customRecTime = if(customTime >= totalTime) customTime - totalTime else customTime
        var localId = id
        var recordTime: Int
        if(customRecTime > -1) {
            recordTime = customRecTime
        } else {
             if(seqIsPlaying){
                 recordTime = if(isRepeating) {
                     if((deltaTimeRepeat + wrapDelta >= 0 && repeatStartTime > repeatEndTime) || repeatStartTime < repeatEndTime) {
                         if(fullDeltaTimeRepeat > repeatEndTime) repeatEndTime.toInt() else fullDeltaTimeRepeat.toInt()
                     } else if((deltaTimeRepeat + wrapDelta < 0 && repeatStartTime > repeatEndTime) || repeatStartTime < repeatEndTime) {
                         if(fullDeltaTimeRepeat < repeatStartTime) repeatStartTime.toInt() else fullDeltaTimeRepeat.toInt()
                     } else fullDeltaTimeRepeat.toInt()
                 } else deltaTime.toInt()
                 if(recordTime > totalTime) recordTime = totalTime
                 if(recordTime < 0) recordTime = 0
             } else {     // Recording to beginning if Seq is stopped
                 recordTime = if(velocity > 0) 0 else staticNoteOffTime
             }
        }

        var index = notes.indexOfLast { it.time < recordTime } + 1
        // if noteOff has the same time as some other noteON -> place it before:
        if(index != 0 && velocity == 0 && notes[index - 1].time == recordTime) {
            index--
        }
        Log.d("ryjtyj", "index to record: $index")

        // if same note is already playing -> find and erase it's paired noteOFF, and record noteOFF in current recordTime
        if (!stepRecord && playingNotes[pitch] > 0 && velocity > 0) {
            val noteOnIndex = notes.indexOfLast { it.pitch == pitch && it.velocity > 0 && it.time <= recordTime }
            val pairedNoteOffIndex = if(noteOnIndex == -1) {
                Log.d("ryjtyj", "no noteON, finded noteOFF")
                notes.indexOfFirst { it.pitch == pitch && it.velocity == 0 && it.time > recordTime }
            } else returnPairedNoteOffIndexAndTime(noteOnIndex).first

            val retrigId = if(pairedNoteOffIndex == -1) {
                increaseNoteId()
                localId++
                Log.d("ryjtyj", "pairedNoteOffIndex = $pairedNoteOffIndex, rare case of localId++")
                noteId - 1
            } else notes[pairedNoteOffIndex].id
            Log.d("ryjtyj", "here pairedNoteOffIndex = $pairedNoteOffIndex")

            // erasing paired noteOFF
            eraseFromArray(pairedNoteOffIndex, false)

            // updating index when erasing note before it
            if(isRepeating && pairedNoteOffIndex in 0 until indexToRepeat - wrapIndex) {
                Log.d("ryjtyj", "updating index when erasing note before it")
                index--
                indexToRepeat--
                // TODO indexToStartRepeating?
            }
            if(pairedNoteOffIndex in 0 until indexToPlay) indexToPlay--

            // then record noteOFF to retrigger afterwards
            recIntoArray(index, recordTime, pitch, 0, retrigId)
            if(isRepeating) indexToRepeat++ else indexToPlay++
            index++
            Log.d("ryjtyj", "finished rec")
        }

        // RECORDING
//        if(!(!playingNotes[pitch] && velocity == 0)) {  // to disable writing second NoteOFF when note is already off // TODO  redo (uncomment)?
        recIntoArray(index, recordTime, pitch, velocity, localId)

        // updating indices
        if(isRepeating) {
            if(!stepRecord || (customRecTime < fullDeltaTimeRepeat)) {
                indexToRepeat++
            }
            if(deltaTime >= recordTime && (!stepRecord || (customRecTime < deltaTime))) {
                indexToPlay++
            }
//            if(recordTime < repeatStartTime) {
//                indexToStartRepeating++
//            }
        } else if(!stepRecord || (customRecTime < deltaTime)) {
            indexToPlay++
        }
        channelIsPlayingNotes = velocity > 0  // TODO
//        }

        if(!stepRecord) {
            stepViewYScroll = (noteHeight * (pitch - 5)).toInt()
            if(stepViewYScroll < 0) stepViewYScroll = 0
        }
    }

    private fun recIntoArray(
        index: Int,
        recordTime: Int,
        pitch: Int,
        velocity: Int,
        id: Int,
    ) {
        when {
            // end of array
            index >= notes.size -> {
                notes += Note(recordTime, pitch, velocity, id)
            }
            // beginning of array
            notes.isNotEmpty() && index == 0 -> {
                val tempNotes = notes
                notes = Array(1) { Note(recordTime, pitch, velocity, id) } + tempNotes
            }
            // middle of array
            else -> {
                val tempNotes1 = notes.copyOfRange(0, index)
                val tempNotes2 = notes.copyOfRange(index, notes.size)
                notes = tempNotes1 + Note(recordTime, pitch, velocity, id) + tempNotes2
            }
        }
    }


    fun playing(kmmk: KmmkComponentContext, index: Int) {
        // play note (if it's not being manually played, or if channel isn't muted)
        // remember which notes are playing
        // update channelIsPlayingNotes for visual info

        if (!pressedNotes[notes[index].pitch].first && (!isMuted || notes[index].velocity == 0)) {
            kmmk.noteOn(
                channel,
                notes[index].pitch,
                notes[index].velocity
            )
            changePlayingNotes(notes[index].pitch, notes[index].velocity)
        }
        channelIsPlayingNotes = notes[index].velocity > 0
        // TODO polyphony: channelIsPlayingNotes += if(notes[index].velocity > 0) 1 else -1; if(channelIsPlayingNotes < 0) channelIsPlayingNotes = 0; update when stop() etc..
    }


    var draggedNoteOffJob = CoroutineScope(Dispatchers.IO).launch { }

    fun playingDraggedNoteOffInTime(factorBpm: Double, kmmk: KmmkComponentContext) {
        Log.d("ryjtyj", "playingDraggedNoteOffInTime()")
        val delayTime = if (draggedNoteOnIndex < draggedNoteOffIndex) {
            notes[draggedNoteOffIndex].time - notes[draggedNoteOnIndex].time
        } else {
            notes[draggedNoteOffIndex].time - notes[draggedNoteOnIndex].time + totalTime
        }
        val pitchTemp = notes[draggedNoteOffIndex].pitch
        if(draggedNoteOffJob.isActive) draggedNoteOffJob.cancel()
        draggedNoteOffJob = CoroutineScope(Dispatchers.IO).launch {
            delay((delayTime / factorBpm).toLong())
            kmmk.noteOn(channel, pitchTemp, 0)
            changePlayingNotes(pitchTemp, 0)
            channelIsPlayingNotes = false // TODO !!!
        }
    }


    fun erasing(kmmk: KmmkComponentContext, isRepeating: Boolean, index: Int, stepViewCase: Boolean): Boolean {
        if (notes[index].velocity > 0) {
            Log.d("ryjtyj", "erasing index $index")

            var pairedNoteOffIndex = returnPairedNoteOffIndexAndTime(index).first

            // stop note_to_be_erased if playing (StepView condition)
            if(stepViewCase) stopNoteIfPlaying(isRepeating, index, pairedNoteOffIndex, kmmk)

            if(pairedNoteOffIndex > index) pairedNoteOffIndex -= 1

            // erasing noteON
            var breakFlag = eraseFromArray(index, false)

            if(pairedNoteOffIndex != -1) Log.d("ryjtyj", "erasing paired noteOff at $pairedNoteOffIndex")
            // erasing paired noteOFF
            breakFlag = eraseFromArray(pairedNoteOffIndex, breakFlag)

            // updating index when erasing note before it
            if (pairedNoteOffIndex < index && pairedNoteOffIndex > -1) {
                if(isRepeating) if(indexToRepeat > 0) indexToRepeat-- else if(indexToPlay > 0) indexToPlay--
            }
            if (breakFlag) return false // break out of while() when erased last note in array

        } else if(isRepeating) indexToRepeat++ else indexToPlay++  // skipping noteOFFs
        return true
    }

    private fun eraseFromArray(index: Int, breakFlag: Boolean): Boolean {
        var breakFlag1 = breakFlag
        when {
            // no paired noteOFF found
            index == -1 -> {  // do nothing
            }
            // end of array
            index == notes.lastIndex -> {
                if (notes.size > 1) {
                    notes = notes.copyOfRange(0, index)
                } else {
                    notes = emptyArray()
                }
                breakFlag1 = true
            }
            // beginning of array
            index == 0 -> {
                Log.d("ryjtyj", "erasing from beginning of array")
                notes = notes.copyOfRange(1, notes.size)
            }
            // middle of array
            else -> {
                val tempNotes = notes.copyOfRange(index + 1, notes.size)
                notes = notes.copyOfRange(0, index) + tempNotes
            }
        }
        return breakFlag1
    }


    fun clearChannel(channel: Int, kmmk: KmmkComponentContext) {
        indexToPlay = 0
        indexToRepeat = 0
        indexToStartRepeating = 0
        tempNotesSize = 0
        notes = emptyArray()
        for(p in 0..127){
            while(playingNotes[p] > 0) {
                kmmk.noteOn(channel, p, 0)
                changePlayingNotes(p, 0)
            }
        }
        Log.d("emptyTag", " ") // to hold in imports
    }


    private fun stopNoteIfPlaying(
        isRepeating: Boolean,
        noteOnIndex: Int,
        noteOffIndex: Int,
        kmmk: KmmkComponentContext
    ) {
        if(noteOnIndex == -1 || noteOffIndex == -1) {
            Log.d("ryjtyj", "stopNoteIfPlaying() error: ${if(noteOnIndex == -1) "noteOnIndex" else "noteOffIndex"} out of bounds")
        }
        val dt = if (isRepeating) fullDeltaTimeRepeat else deltaTime
        val deltaTimeInRange = if (noteOnIndex < noteOffIndex) {
            dt in notes[noteOnIndex].time.toDouble()..notes[noteOffIndex].time.toDouble()
        } else {
            dt in notes[noteOnIndex].time.toDouble()..totalTime.toDouble() || dt in 0.0..notes[noteOffIndex].time.toDouble()
        }
        while (!pressedNotes[notes[noteOnIndex].pitch].first && playingNotes[notes[noteOnIndex].pitch] > 0 && deltaTimeInRange) {
            kmmk.noteOn(channel, notes[noteOnIndex].pitch, 0)
            changePlayingNotes(notes[noteOnIndex].pitch, 0)
            channelIsPlayingNotes = false // TODO !!!
        }
    }

    fun changeStepViewYScroll(y: Int) {
        stepViewYScroll = y
    }

    fun changeKeyboardOctave(lowKeyboard: Boolean, increment: Int) {
        if(lowKeyboard) pianoViewOctaveLow += increment
        else pianoViewOctaveHigh += increment
    }

    fun returnPairedNoteOffIndexAndTime(index: Int): Pair<Int, Int> {
        val pairedNoteOffIndex = notes.indexOfFirst { it.id == notes[index].id && it.velocity == 0 }
        return if(pairedNoteOffIndex > -1) Pair(pairedNoteOffIndex, notes[pairedNoteOffIndex].time) else Pair(-1, Int.MIN_VALUE)
    }

    fun returnPairedNoteOnIndexAndTime(index: Int): Pair<Int, Int> {
        val pairedNoteOnIndex = notes.indexOfFirst { it.id == notes[index].id && it.velocity > 0 }
        return if(pairedNoteOnIndex > -1) Pair(pairedNoteOnIndex, notes[pairedNoteOnIndex].time) else Pair(-1, Int.MIN_VALUE)
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

    fun increaseNoteId() {
        noteId++
    }

    fun changePlayingNotes(pitch: Int, velocity: Int) {
        if(velocity > 0) playingNotes[pitch]++ else playingNotes[pitch]--
//        Log.d("ryjtyj", "playingNotes[$pitch] = ${playingNotes[pitch]}")
        if(playingNotes[pitch] < 0) {
            Log.d("ryjtyj", "playingNotes[$pitch] = ${playingNotes[pitch]}, fixing to 0")
            playingNotes[pitch] = 0
        }
    }

    fun searchIndexInTimeBoundaries(): Int {
        var index =
            notes.indexOfFirst { it.time >= repeatStartTime && it.time < repeatEndTime } // normal search
        if (index == -1 && repeatStartTime > repeatEndTime) {
            index =
                notes.indexOfFirst { it.time >= repeatStartTime } // wrap-around search 1
        }
        if (index == -1) {
            index = if(repeatStartTime > repeatEndTime) notes.size else indexToPlay
        }
        return index
    }
}