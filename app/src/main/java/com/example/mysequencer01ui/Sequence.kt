package com.example.mysequencer01ui

import android.util.Log
import com.example.mysequencer01ui.ui.BARTIME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

class Sequence(
    val channel: Int,
    val kmmk: KmmkComponentContext,
    var notes: Array<Note> = emptyArray(),

    var isMuted: Boolean = false,
    var isSoloed: Boolean = false,
    var isErasing: Boolean = false,
    var channelIsPlayingNotes: Int = 0,
    var playingNotes: Array<Int> = Array(128){ 0 },
    var pressedNotes: Array<PressedNote> = Array(128){ PressedNote(false, Int.MAX_VALUE, Long.MIN_VALUE) }, // manually pressed notes that are muting same ones played by sequencer
    var dePressedNotesOnRepeat: Array<Boolean> = Array(128){ false }, // remembering notes that shouldn't play on release/cancel // TODO rename (notesToIgnore?)
    private var idOfQuantizedNotesToIgnore: MutableList<Int> = mutableListOf(),
    var onPressedMode: PadsMode = PadsMode.DEFAULT,
    var noteId: Int = Int.MIN_VALUE,

    var draggedNoteOnIndex: Int = -1,
    var draggedNoteOffIndex: Int = -1,

    var stepViewYScroll: Int = 0,
    var pianoViewOctaveHigh: Int = 4,
    var pianoViewOctaveLow: Int = 2,

    var indexToPlay: Int = 0,
    var startTimeStamp: Long = 0,
    var bpmDelta: Double = 0.0,
    var seqLength: Int = 4, // future feature
    var totalTime: Int = BARTIME, // TODO how are totalTime & seqLength correlated? Replace totalTime with relative one?
    var deltaTime: Double = 0.0,

    var indexToStartRepeating: Int = 0,
    var indexToRepeat: Int = 0,
    var deltaTimeRepeat: Double = 0.0,
    var previousDeltaTime: Double = 0.0,
    var repeatStartTime: Double = 0.0,
    var repeatEndTime: Double = 0.0,
    var repeatsCount: Int = 0,
    var savedRepeatsCount: Int = 0,
    var wrapIndex: Int = 0,
    var tempNotesSize: Int = 0,
    ) {
    fun recordNote(
        pitch: Int,
        velocity: Int,
        id: Int,
        quantizationTime: Double,
        factorBpm: Double,
        staticNoteOffTime: Int,
        seqIsPlaying: Boolean,
        isRepeating: Boolean,
        quantizeTime: (Double) -> Double,
        customTime: Double = -1.0,
        stepRecord: Boolean = false,
        noteHeight: Float = 60f
    ) {
        val customRecTime = if (customTime >= totalTime) customTime - totalTime else customTime

        val recordTime = getRecordTime(
            quantizeTime,
            id,
            quantizationTime,
            factorBpm,
            pitch,
            customRecTime,
            seqIsPlaying,
            isRepeating,
            velocity,
            staticNoteOffTime
        )
        if (recordTime == repeatEndTime && isRepeating && velocity > 0) return

        var index = notes.indexOfLast { it.time <= recordTime } + 1

        Log.d("ryjtyj", "index to record: $index, record time = $recordTime")

        index = overdubPlayingNotes(pitch, recordTime, id, velocity, stepRecord, index)

        // if noteOff has the same time as some other note(ON) -> place it before:
        val indexOfNoteOnAtTheSameTime = notes.indexOfLast { it.time == recordTime && it.pitch == pitch && it.velocity > 0 && velocity == 0 }
        if (indexOfNoteOnAtTheSameTime != -1) {
            index = indexOfNoteOnAtTheSameTime
        }

        // RECORDING
        Log.d("ryjtyj", "RECORDING index = $index, id = ${id - Int.MIN_VALUE}")
        recIntoArray(index, recordTime, pitch, velocity, id)

        indexToPlay = notes.indexOfLast { it.time < deltaTime } + 1
        indexToRepeat = notes.indexOfLast { it.time < deltaTimeRepeat } + 1

        if(!stepRecord) {
            stepViewYScroll = (noteHeight * (pitch - 5)).toInt().coerceAtLeast(0)
        }
    }

    private fun getRecordTime(
        quantizeTime: (Double) -> Double,
        id: Int,
        quantizationTime: Double,
        factorBpm: Double,
        pitch: Int,
        customRecTime: Double,
        seqIsPlaying: Boolean,
        isRepeating: Boolean,
        velocity: Int,
        staticNoteOffTime: Int
    ): Double {
        var time = when {
            (customRecTime > -1) -> customRecTime
            !seqIsPlaying -> if (velocity > 0) 0.0 else staticNoteOffTime.toDouble()
            !isRepeating -> deltaTime
            else -> deltaTimeRepeat
        }

        if (velocity > 0 && seqIsPlaying) {
            time = quantizeTime(time)
            idOfQuantizedNotesToIgnore.add(id)
        } else {
            val indexOfQuantizedNoteOn = notes.indexOfFirst { it.id ==  pressedNotes[pitch].id }
            val noteIsShorterThanQuantization = (indexOfQuantizedNoteOn != -1) && (abs(time - notes[indexOfQuantizedNoteOn].time) < quantizationTime)
            val notBigWrapAround = System.currentTimeMillis() - pressedNotes[pitch].noteOnTimestamp <= quantizationTime / factorBpm * 1.5  // rare case of almost self-tied note  ...][...

            if (noteIsShorterThanQuantization && notBigWrapAround) {
                time = notes[indexOfQuantizedNoteOn].time + quantizationTime
            }
        }

        return when {
            (customRecTime > -1) -> time
            !seqIsPlaying -> time
            !isRepeating -> {
                time = time.coerceAtMost(totalTime.toDouble())
                if (time >= totalTime.toDouble() && velocity > 0) 0.0 else time
            }
            (repeatStartTime < repeatEndTime) -> {
                time.coerceIn(repeatStartTime..repeatEndTime)   // [...]
            }
            (deltaTimeRepeat >= 0) -> {
                time.coerceAtMost(repeatEndTime)    // ..]
            }
            else -> {
                time.coerceIn(repeatStartTime..totalTime.toDouble())    // [..
            }
        }
    }

    private fun overdubPlayingNotes(
        pitch: Int,
        recordTime: Double,
        id: Int,
        velocity: Int,
        stepRecord: Boolean,
        theIndex: Int
    ): Int {
        // We are searching for note instead of relying on playingNotes[] state because of quantizing issues (note could possibly play now but not in quantized time)
        var index = theIndex
        var noteOnBeforeRec = notes.indexOfLast { it.pitch == pitch && it.time <= recordTime && it.id != id }
        var noteOffAfterRec: Int

        if (noteOnBeforeRec == -1 || notes[noteOnBeforeRec].velocity == 0) {
            noteOnBeforeRec = -1
            noteOffAfterRec = notes.indexOfFirst { it.pitch == pitch && it.time > recordTime && it.id != id }
            if (noteOffAfterRec == -1 || notes[noteOffAfterRec].velocity > 0) {
                noteOffAfterRec = -1
            } else noteOnBeforeRec = getPairedNoteOnIndexAndTime(noteOffAfterRec).index
        } else {
            noteOffAfterRec = getPairedNoteOffIndexAndTime(noteOnBeforeRec).index
        }

        Log.d("ryjtyj", "noteOnBeforeRec = $noteOnBeforeRec, noteOffAfterRec = $noteOffAfterRec")

        if (noteOnBeforeRec != -1 && (notes[noteOnBeforeRec].time == recordTime && notes[noteOnBeforeRec].velocity > 0 && velocity == 0)) {
            noteOnBeforeRec = -1
            noteOffAfterRec = -1
        }
        Log.d("ryjtyj", "noteOnBeforeRec = $noteOnBeforeRec, noteOffAfterRec = $noteOffAfterRec")

        if (noteOnBeforeRec != -1 || noteOffAfterRec != -1) {
            if (velocity > 0) {
                if (noteOnBeforeRec != -1 && notes[noteOnBeforeRec].time == recordTime) {
                    eraseOverdubbingNote(noteOnBeforeRec)
                } else if (noteOffAfterRec != -1) {
                    changeNoteTime(noteOffAfterRec, recordTime)
                    sortNotesByTime()
                } else {
                    recIntoArray(index, recordTime, pitch, 0, notes[noteOnBeforeRec].id)
                }
            } else if (noteOnBeforeRec != -1) {
                eraseOverdubbingNote(noteOnBeforeRec)
            } else {
                eraseFromArray(noteOffAfterRec, false)
            }
            index = notes.indexOfLast { it.time <= recordTime } + 1
        }
        return index
    }

    private fun eraseOverdubbingNote(noteOnBeforeRec: Int) {
        val noteOffId = notes[noteOnBeforeRec].id
        eraseFromArray(noteOnBeforeRec, false)
        val noteOffIndex = notes.indexOfFirst { it.id == noteOffId }
        if (noteOffIndex != -1) eraseFromArray(noteOffIndex, false)
    }

    private fun recIntoArray(
        index: Int,
        recordTime: Double,
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


    fun playing(index: Int, soloIsOff: Boolean, stepView: Boolean) {
        // if note isn't being manually played   AND   channel is soloed  OR  no soloing occurs and channel isn't muted
        if (!pressedNotes[notes[index].pitch].isPressed && (isSoloed || (soloIsOff && !isMuted))) {

            if(dePressedNotesOnRepeat[notes[index].pitch] && stepView && notes[index].velocity == 0) {  // a fix for playingNotes == -1 when releasing dragged note that was playing
                Log.d("ryjtyj", "noteOn ind11111ex = $index")
                changePlayingNotes(notes[index].pitch, 1)
                dePressedNotesOnRepeat[notes[index].pitch] = false
            }
              else if (idOfQuantizedNotesToIgnore.indexOfFirst{ it == notes[index].id } != -1) {  // skipping recording notes that are quantized forward
                Log.d("ryjtyj", "noteOn ind2222ex = $index")
                if(notes[index].velocity == 0) {
                    idOfQuantizedNotesToIgnore.removeAt(idOfQuantizedNotesToIgnore.indexOfFirst{ it == notes[index].id })
                }
            }
        else {
                kmmk.noteOn(
                    channel,
                    notes[index].pitch,
                    notes[index].velocity
                )
            }
        }
        changePlayingNotes(notes[index].pitch, notes[index].velocity)
    }

    private var draggedNoteOffJob = CoroutineScope(Dispatchers.IO).launch { }

    fun playingDraggedNoteOffInTime(factorBpm: Double) {
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
        }
    }


    fun erasing(isRepeating: Boolean, index: Int, stepViewCase: Boolean): Boolean {
        if (notes[index].velocity > 0 && notes[index].id != pressedNotes[notes[index].pitch].id
        ) {
            Log.d("ryjtyj", "erasing index $index")

            var pairedNoteOffIndex = getPairedNoteOffIndexAndTime(index).index

            // stop note_to_be_erased if playing (StepView condition)
            if(stepViewCase) stopNoteIfPlaying(isRepeating, index, pairedNoteOffIndex)

            if(pairedNoteOffIndex > index) pairedNoteOffIndex--

            // erasing noteON
            var breakFlag = eraseFromArray(index, false)

            Log.d("ryjtyj", "erasing paired noteOff at $pairedNoteOffIndex")
            // erasing paired noteOFF
            breakFlag = eraseFromArray(pairedNoteOffIndex, breakFlag)

            // updating index when erasing note before it
            if (pairedNoteOffIndex < index && pairedNoteOffIndex > -1) {
                if(isRepeating) if(indexToRepeat > 0) indexToRepeat-- else if(indexToPlay > 0) indexToPlay-- // TODO why we don't indexToPlay-- in other conditions, if else if..
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


    fun clearChannel(channel: Int) {
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
        noteOffIndex: Int
    ) {
        if(noteOnIndex == -1 || noteOffIndex == -1) {
            Log.d("ryjtyj", "stopNoteIfPlaying() error: ${if(noteOnIndex == -1) "noteOnIndex" else "noteOffIndex"} out of bounds")
        }
        val dt = if (isRepeating) deltaTimeRepeat else deltaTime
        val deltaTimeInRange = if (noteOnIndex < noteOffIndex) {
            dt in notes[noteOnIndex].time..notes[noteOffIndex].time
        } else {
            dt in notes[noteOnIndex].time..totalTime.toDouble() || dt in 0.0..notes[noteOffIndex].time
        }
        while (!pressedNotes[notes[noteOnIndex].pitch].isPressed && playingNotes[notes[noteOnIndex].pitch] > 0 && deltaTimeInRange) {
            kmmk.noteOn(channel, notes[noteOnIndex].pitch, 0)
            changePlayingNotes(notes[noteOnIndex].pitch, 0)
        }
    }

    fun changeStepViewYScroll(y: Int) {
        stepViewYScroll = y
    }

    fun changeKeyboardOctave(lowKeyboard: Boolean, increment: Int) {
        var newValue = (if(lowKeyboard) pianoViewOctaveLow else pianoViewOctaveHigh) + increment
        if(newValue < -1) newValue = -1
        if(newValue > 7) newValue = 7
        if(lowKeyboard) pianoViewOctaveLow = newValue else pianoViewOctaveHigh = newValue
    }

    fun getPairedNoteOffIndexAndTime(index: Int): NoteIndexAndTime {
        val pairedNoteOffIndex = notes.indexOfFirst { it.id == notes[index].id && it.velocity == 0 }
        return if(pairedNoteOffIndex > -1) NoteIndexAndTime(pairedNoteOffIndex, notes[pairedNoteOffIndex].time) else NoteIndexAndTime(-1, Double.MIN_VALUE)
    }

    // TODO merge with ^^ (assume that no duplicate IDs exist, search for same ID but different index)
    fun getPairedNoteOnIndexAndTime(index: Int): NoteIndexAndTime {
        val pairedNoteOnIndex = notes.indexOfFirst { it.id == notes[index].id && it.velocity > 0 }
        return if(pairedNoteOnIndex > -1) NoteIndexAndTime(pairedNoteOnIndex, notes[pairedNoteOnIndex].time) else NoteIndexAndTime(-1, Double.MIN_VALUE)
    }

    fun changeNotePitch(index: Int, pitch: Int) {
        notes[index].pitch = pitch
    }

    fun changeNoteTime(index: Int, time: Double, isDeltaTime: Boolean = false) {
        notes[index].time = if(isDeltaTime) {
            notes[index].time + time
        } else {
            time
        }
        if(notes[index].time >= BARTIME) {
            notes[index].time -= BARTIME
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
        if(velocity > 0) {
            playingNotes[pitch]++
            channelIsPlayingNotes++
        } else {
            playingNotes[pitch]--
            channelIsPlayingNotes--
        }
//        Log.d("ryjtyj", "playingNotes[$pitch] = ${playingNotes[pitch]}")
        if(playingNotes[pitch] < 0) {
            Log.d("ryjtyj", "playingNotes[$pitch] = ${playingNotes[pitch]}, fixing to 0")
            playingNotes[pitch] = 0
        }
        if(channelIsPlayingNotes < 0) {
            Log.d("ryjtyj", "channelIsPlayingNotes = $channelIsPlayingNotes, fixing to 0")
            channelIsPlayingNotes = 0
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