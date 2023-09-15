package com.example.mysequencer01ui

import android.util.Log
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import com.example.mysequencer01ui.StopNotesMode.END_OF_REPEAT
import com.example.mysequencer01ui.StopNotesMode.STOP_NOTES
import com.example.mysequencer01ui.ui.BARTIME
import com.example.mysequencer01ui.ui.ChannelState
import com.example.mysequencer01ui.ui.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.abs

class ChannelSequence(val channelNumber: Int, val kmmk: KmmkComponentContext) {

    private val _channelState = MutableStateFlow(ChannelState(channelNumber))
    val channelState: StateFlow<ChannelState> = _channelState.asStateFlow()

    var isErasing: Boolean = false
    var idsOfNotesToIgnore: MutableList<NoteIdAndVelocity> = mutableListOf()
    var pitchTempSavedForCancel: Int = 0
    var onPressedMode: PadsMode = PadsMode.DEFAULT
    var noteId: Int = Int.MIN_VALUE

    var draggedNoteOnIndex: Int = -1
    var draggedNoteOffIndex: Int = -1
    var draggedNoteOffJob = CoroutineScope(Dispatchers.Default).launch { }

    var indexToPlay: Int = 0
    var startTimeStamp: Long = 0
    var bpmDelta: Double = 0.0

    var indexToStartRepeating: Int = 0
    var indexToRepeat: Int = 0
    private var previousDeltaTime: Double = 0.0

    val interactionSources = Array(128) {
        RememberedPressInteraction(
            MutableInteractionSource(),
            PressInteraction.Press( Offset(0f,0f) )
        )
    }


    var notes = channelState.value.notes

    var pressedNotes = channelState.value.pressedNotes
    var playingNotes = channelState.value.playingNotes
    private var channelIsPlayingNotes = channelState.value.channelIsPlayingNotes

    var totalTime = channelState.value.totalTime
    var deltaTime = channelState.value.deltaTime

    var repeatStartTime = channelState.value.repeatStartTime
    var repeatEndTime = channelState.value.repeatEndTime
    var deltaTimeRepeat = channelState.value.deltaTimeRepeat



    /** MAIN channel CYCLE **/
    fun advanceChannelSequence(
        seqIsRecording: Boolean,
        factorBpm: Double,
        soloIsOn: Boolean,
        isQuantizing: Boolean,
        quantizationTime: Double,
        quantizeTime: (Double) -> Double,
        isRepeating: Boolean,
        isStepView: Boolean,
        stopNotesOnChannel: (Int, StopNotesMode) -> Unit,
        allowRecordShortNotes: Boolean
    ) {
        deltaTime = (System.currentTimeMillis() - startTimeStamp) * factorBpm + bpmDelta

        while (notes.size > indexToPlay && notes[indexToPlay].time <= deltaTime) {
            if (eraseOrPlay(seqIsRecording, quantizationTime, factorBpm, isRepeating, soloIsOn)) break
        }

        if (isRepeating) {
            advanceRepeating(seqIsRecording, soloIsOn, isStepView, factorBpm, stopNotesOnChannel, isQuantizing, quantizationTime, quantizeTime, allowRecordShortNotes)
        }

        // end of sequence
        if (deltaTime >= totalTime) {
            startTimeStamp = System.currentTimeMillis() - (deltaTime - totalTime).toLong()
            bpmDelta = 0.0
            indexToPlay = 0
            idsOfNotesToIgnore.clear()
        }

        // record notes (if any)
//        while (listOfNotesToRecord.isNotEmpty()) {
//            with(listOfNotesToRecord[0]) {
//                recording(
//                    recordTime = recordTime,
//                    pitch = pitch,
//                    id = id,
//                    velocity = velocity,
//                    isStepView = isStepView,
//                    noteHeight = noteHeight
//                )
//                val dt = if(isRepeating) deltaTimeRepeat else deltaTime
//                if (velocity > 0 && seqIsPlaying && recordTime > dt && !isStepRecord) {
//                    idsOfNotesToIgnore.add(id)
//                }
//            }
//            listOfNotesToRecord.removeAt(0)
//        }
    }

    private fun eraseOrPlay(
        seqIsRecording: Boolean,
        quantizationTime: Double,
        factorBpm: Double,
        isRepeating: Boolean,
        soloIsOn: Boolean,
    ): Boolean {
        val noteToPlay = notes[indexToPlay]
        val pressedNote = pressedNotes[noteToPlay.pitch]
        val overdubbing = seqIsRecording && pressedNote.isPressed
        val noteShouldBeTiedToItself = System.currentTimeMillis() - pressedNote.noteOnTimestamp > quantizationTime / factorBpm * 1.5
        val sameNoteId = noteToPlay.id == pressedNote.id
        val isQuantizedForward = sameNoteId && (noteToPlay.velocity > 0) && !noteShouldBeTiedToItself

        if (!isRepeating && (isErasing || (overdubbing && !isQuantizedForward))) {
            if (overdubbing && sameNoteId && noteShouldBeTiedToItself) { // tie note
                updatePressedNote(noteToPlay.pitch, false)
                recIntoArray(
                    index = indexToPlay,
                    recordTime = noteToPlay.time,
                    pitch = noteToPlay.pitch,
                    velocity = 0,
                    id = pressedNote.id
                )
                cancelPadInteraction()
                indexToPlay++
            } else {
                if (!erasing(false, indexToPlay, false)) return true
            }
        } else {
            if (!isRepeating && indexToPlay != draggedNoteOffIndex) {
                playing(
                    indexToPlay,
                    !soloIsOn,
                    factorBpm
                )
            }
            indexToPlay++
        }
        return false
    }

    private fun advanceRepeating(
        seqIsRecording: Boolean,
        soloIsOn: Boolean,
        isStepView: Boolean,
        factorBpm: Double,
        stopNotesOnChannel: (Int, StopNotesMode) -> Unit,
        isQuantizing: Boolean,
        quantizationTime: Double,
        quantizeTime: (Double) -> Double,
        allowRecordShortNotes: Boolean
    ) {
        if (deltaTime < previousDeltaTime) {
            previousDeltaTime -= totalTime
        }
        deltaTimeRepeat += deltaTime - previousDeltaTime
        previousDeltaTime = deltaTime

        // erasing or playing
        while (notes.size > indexToRepeat && checkIfNoteShouldPlay()) {
            if (eraseOrPlayInRepeat(seqIsRecording, repeatEndTime, repeatStartTime, soloIsOn, factorBpm)) break
        }

        // end of repeating loop
        val repeatLoopEnded = deltaTimeRepeat > repeatEndTime && (repeatStartTime < repeatEndTime || deltaTimeRepeat < repeatStartTime)
        if (repeatLoopEnded) {
            restartRepeatingLoop(stopNotesOnChannel, seqIsRecording, isQuantizing, quantizationTime, quantizeTime, factorBpm, isStepView, allowRecordShortNotes)
        }

        // wrap-around
        if (deltaTimeRepeat > totalTime) {
            deltaTimeRepeat -= totalTime
            indexToRepeat = 0
        }
    }

    private fun checkIfNoteShouldPlay(): Boolean {
        val timeToPlayNote = notes[indexToRepeat].time in repeatStartTime..deltaTimeRepeat
            || (repeatStartTime > repeatEndTime && (notes[indexToRepeat].time in 0.0..deltaTimeRepeat))

        val noteInRepeatBounds = notes[indexToRepeat].time in repeatStartTime..repeatEndTime     // [...]
            || (repeatStartTime > repeatEndTime && (notes[indexToRepeat].time >= repeatStartTime || notes[indexToRepeat].time <= repeatEndTime))  // ..] && [..

        val noteOnInRepeatEnd = notes[indexToRepeat].time == repeatEndTime && notes[indexToRepeat].velocity > 0

        return timeToPlayNote && noteInRepeatBounds && !noteOnInRepeatEnd
    }

    private fun eraseOrPlayInRepeat(
        seqIsRecording: Boolean,
        repeatEndTime: Double,
        repeatStartTime: Double,
        soloIsOn: Boolean,
        factorBpm: Double,
    ): Boolean {
        val noteToRepeat = notes[indexToRepeat]
        val pressedNote = pressedNotes[noteToRepeat.pitch]
        val overdubbing = seqIsRecording && pressedNote.isPressed
        val sameNote = (noteToRepeat.velocity > 0) && (noteToRepeat.id == pressedNote.id)
        val notesShouldBeTied = overdubbing && getNotePairedIndexAndTime(indexToRepeat).time == repeatEndTime
            && notes[notes.indexOfLast { it.id == pressedNote.id && it.velocity > 0 }].time == repeatStartTime

        if (isErasing || (overdubbing && !sameNote)) {
            // tie note
            if (notesShouldBeTied) {
                updatePressedNote(noteToRepeat.pitch, false)
                recIntoArray(
                    index = indexToRepeat,
                    recordTime = noteToRepeat.time,
                    pitch = noteToRepeat.pitch,
                    velocity = 0,
                    id = pressedNote.id
                )
                cancelPadInteraction()
                indexToRepeat++
                Log.d(TAG, "tied note ${noteToRepeat.pitch}")
            } else
                if (!erasing(true, indexToRepeat, false)) return true
        } else {
            if (indexToRepeat != draggedNoteOffIndex) {
                playing(
                    indexToRepeat,
                    !soloIsOn,
                    factorBpm
                )
            }
            indexToRepeat++
        }
        return false
    }

    private fun restartRepeatingLoop(
        stopNotesOnChannel: (Int, StopNotesMode) -> Unit,
        seqIsRecording: Boolean,
        isQuantizing: Boolean,
        quantizationTime: Double,
        quantizeTime: (Double) -> Double,
        factorBpm: Double,
        isStepView: Boolean,
        allowRecordShortNotes: Boolean,
    ) {
        stopNotesOnChannel(channelNumber, END_OF_REPEAT)  // notesOFF on the end of loop

        indexToStartRepeating = notes.indexOfLast { it.time < repeatStartTime } + 1
        indexToRepeat = indexToStartRepeating

        deltaTimeRepeat -= (repeatEndTime - repeatStartTime)
        idsOfNotesToIgnore.clear()

        for (p in 0..127) {
            // ignore noteOff if we start repeatLoop in the middle of the note
            addHangingNoteToIgnore(p)

            // notesON on the start of the loop if notes are being held
            if (pressedNotes[p].isPressed) {
                kmmk.noteOn(channelNumber, p, 100)
                Log.d(TAG, "noteON on the start of the loop (note $p is being pressed)")
                if (seqIsRecording) recordNote(
                    pitch = p,
                    velocity = 100,
                    id = pressedNotes[p].id,
                    quantizationTime = quantizationTime,
                    quantizeTime = quantizeTime,
                    factorBpm = factorBpm,
                    seqIsPlaying = true,
                    isRepeating = true,
                    isQuantizing = isQuantizing,
                    isStepView = isStepView,
                    isStepRecord = false,
                    allowRecordShortNotes = allowRecordShortNotes,
                    customTime = repeatStartTime
                )
                changePlayingNotes(p, 100)
                increaseNoteId()
            }
        }
    }

    private fun addHangingNoteToIgnore(p: Int) {
        var hangingNoteOffIndex = notes.indexOfFirst {
            it.pitch == p && (it.time in repeatStartTime..repeatEndTime || (repeatStartTime > repeatEndTime && it.time >= repeatStartTime))
        }
        if (hangingNoteOffIndex == -1 && repeatStartTime > repeatEndTime) hangingNoteOffIndex = notes.indexOfFirst {
            it.pitch == p && it.time in 0.0..repeatEndTime
        }

        val noteOffInRepeatButNoteOnIsNot = hangingNoteOffIndex != -1 && notes[hangingNoteOffIndex].velocity == 0
            && (getNotePairedIndexAndTime(hangingNoteOffIndex).time !in repeatStartTime..deltaTimeRepeat
            || (repeatStartTime > repeatEndTime && getNotePairedIndexAndTime(hangingNoteOffIndex).time in 0.0..deltaTimeRepeat))

        if (noteOffInRepeatButNoteOnIsNot) {
            idsOfNotesToIgnore.add(NoteIdAndVelocity(notes[hangingNoteOffIndex].id, 0))
            Log.d(TAG, "hanging NoteOff INDEX on the start of the loop = $hangingNoteOffIndex")
        }
    }


    /** RECORD **/
    fun recordNote(
        pitch: Int,
        velocity: Int,
        id: Int,
        quantizationTime: Double,
        quantizeTime: (Double) -> Double,
        factorBpm: Double,
        seqIsPlaying: Boolean,
        isRepeating: Boolean,
        isQuantizing: Boolean,
        isStepView: Boolean,
        isStepRecord: Boolean,
        allowRecordShortNotes: Boolean,
        customTime: Double = -1.0,
        noteHeight: Float = 60f
    ) {
        Log.d(TAG, "recording() at channel $channelNumber, id = ${id - Int.MIN_VALUE}, velocity = $velocity")

        val noNoteOn = notes.indexOfFirst { it.id == id && it.velocity > 0 } == -1

        if(velocity == 0 && noNoteOn
//            && !isStepRecord // added because of delayed recording in main cycle
        ) return

        if (velocity > 0 && !isStepRecord) idsOfNotesToIgnore.add(NoteIdAndVelocity(id, velocity))

        val customRecTime = if (customTime > totalTime) customTime - totalTime else customTime

        var recordTime = getRecordTime(
            isQuantizing,
            quantizeTime,
            quantizationTime,
            factorBpm,
            pitch,
            customRecTime,
            seqIsPlaying,
            isRepeating,
            velocity,
            allowRecordShortNotes,
        )
        Log.d(TAG, "recordTime = $recordTime")

        if (recordTime == repeatEndTime && isRepeating && velocity > 0) recordTime = repeatStartTime

//        if(seqIsPlaying) {
//            listOfNotesToRecord.add(RecordingPackage(recordTime, pitch, id, velocity, isStepView, isStepRecord, noteHeight))
//        } else {
        recording(recordTime, pitch, id, velocity, isStepView, noteHeight)
//        }
    }

    private fun getRecordTime(
        isQuantizing: Boolean,
        quantizeTime: (Double) -> Double,
        quantizationTime: Double,
        factorBpm: Double,
        pitch: Int,
        customRecTime: Double,
        seqIsPlaying: Boolean,
        isRepeating: Boolean,
        velocity: Int,
        allowRecordShortNotes: Boolean
    ): Double {
        var time = when {
            (customRecTime > -1) -> if (customRecTime == totalTime.toDouble() && velocity > 0) 0.0 else customRecTime
            !seqIsPlaying -> if (velocity > 0) 0.0 else quantizationTime
            !isRepeating -> deltaTime
            else -> deltaTimeRepeat
        }
        Log.d(TAG, "time = $time")

        if (velocity > 0) {
            time = quantizeTime(time)
        } else {
            val indexOfQuantizedNoteOn = notes.indexOfFirst { it.id == pressedNotes[pitch].id }
            val notBigWrapAround =
                System.currentTimeMillis() - pressedNotes[pitch].noteOnTimestamp <= quantizationTime / factorBpm  // catching rare case of almost self-tied note  ..]_[..

//            val pairedNoteOn = listOfNotesToRecord.indexOfFirst { it.id == id }
            val noteOnTime = when {
                indexOfQuantizedNoteOn != -1 -> notes[indexOfQuantizedNoteOn].time
//                pairedNoteOn != -1 -> listOfNotesToRecord[pairedNoteOn].recordTime
                else -> Double.MIN_VALUE
            }
            val noteIsShorterThanQuantization = (abs(time - noteOnTime) < quantizationTime)
            val noteOffInRepeatEnd = isRepeating && velocity == 0 && time == repeatEndTime

            if (notBigWrapAround && (noteIsShorterThanQuantization && !noteOffInRepeatEnd && !(allowRecordShortNotes && !isQuantizing)
                || (!noteIsShorterThanQuantization && (isQuantizing || !allowRecordShortNotes)))) {  // fix for rec near end of the loop
                time = noteOnTime + quantizationTime
            }
        }

        return when {
            (customRecTime > -1) || !seqIsPlaying -> {
                time
            }
            !isRepeating -> {
                if (!isQuantizing && !allowRecordShortNotes && time > totalTime && velocity == 0) time -= totalTime
                time = time.coerceAtMost(totalTime.toDouble())
                if (time >= totalTime.toDouble() && velocity > 0) 0.0 else time
            }
            (repeatStartTime < repeatEndTime) -> {
                time.coerceIn(repeatStartTime..repeatEndTime)   // [...]
            }
            (deltaTimeRepeat >= repeatStartTime) -> {
                time.coerceAtMost(totalTime.toDouble())    // [..
            }
            else -> {
                time.coerceAtMost(repeatEndTime)    // ..]
            }
        }
    }

    private fun recording(
        recordTime: Double,
        pitch: Int,
        id: Int,
        velocity: Int,
        isStepView: Boolean,
        noteHeight: Float
    ) {
        var index = notes.indexOfLast { it.time <= recordTime } + 1
        Log.d(TAG, "index to record: $index, record time = $recordTime")

        index = overdubPlayingNotes(pitch, recordTime, id, velocity, index)

        // if noteOff has the same time as some other note(ON) -> place it before:
        val indexOfNoteOnAtTheSameTime =
            notes.indexOfLast { it.time == recordTime && it.pitch == pitch && it.velocity > 0 && velocity == 0 }
        if (indexOfNoteOnAtTheSameTime != -1) {
            Log.d(TAG, "indexOfNoteOnAtTheSameTime = $indexOfNoteOnAtTheSameTime")
            index = indexOfNoteOnAtTheSameTime
        }

        // RECORDING
        Log.d(TAG, "RECORDING index = $index, id = ${id - Int.MIN_VALUE}")
        recIntoArray(index, recordTime, pitch, velocity, id)

        refreshIndices()

        if (!isStepView) {
            updateStepViewYScroll((noteHeight * (pitch - 8)).toInt().coerceAtLeast(0))
        }
    }

    private fun overdubPlayingNotes(
        pitch: Int,
        recordTime: Double,
        id: Int,
        velocity: Int,
        theIndex: Int
    ): Int {
        // We are searching for note instead of relying on playingNotes[] state because of quantizing issues (note could possibly play now but not in quantized time)
        var index = theIndex
        var (noteOnBeforeRec, noteOffAfterRec: Int) = findNoteIndicesAroundGivenTime(pitch, recordTime, id)

        // no overdub of other noteON if we record noteOFF in the same record time
        if (noteOnBeforeRec != -1 && (notes[noteOnBeforeRec].time == recordTime && notes[noteOnBeforeRec].velocity > 0 && velocity == 0)) {
            noteOnBeforeRec = -1
            noteOffAfterRec = -1
        }

        if (noteOnBeforeRec != -1 || noteOffAfterRec != -1) {
            Log.d(TAG, "OVERDUB: noteOnBeforeRec = $noteOnBeforeRec, noteOffAfterRec = $noteOffAfterRec")

            when {
                velocity == 0 && noteOnBeforeRec == -1 -> {
                    eraseFromArray(noteOffAfterRec, false)
                }
                velocity == 0 || (noteOnBeforeRec != -1 && notes[noteOnBeforeRec].time == recordTime) -> {
                    eraseOverdubbingNote(noteOnBeforeRec)
                }
                noteOffAfterRec != -1 -> {
                    changeNoteTime(noteOffAfterRec, recordTime)
                    sortNotesByTime()
                }
                else -> {
                    recIntoArray(index, recordTime, pitch, 0, notes[noteOnBeforeRec].id)
                }
            }
            index = notes.indexOfLast { it.time <= recordTime } + 1
        }
        return index
    }

    fun findNoteIndicesAroundGivenTime(
        pitch: Int,
        recordTime: Double,
        id: Int
    ): Pair<Int, Int> {
        var noteOnBeforeRec = notes.indexOfLast { it.pitch == pitch && it.time <= recordTime && it.id != id }
        var noteOffAfterRec: Int

        if (noteOnBeforeRec == -1 || notes[noteOnBeforeRec].velocity == 0) {
            noteOnBeforeRec = -1
            noteOffAfterRec =
                notes.indexOfFirst { it.pitch == pitch && it.time > recordTime && it.id != id }
            if (noteOffAfterRec == -1 || notes[noteOffAfterRec].velocity > 0) {
                noteOffAfterRec = -1
            } else noteOnBeforeRec = getNotePairedIndexAndTime(noteOffAfterRec).index
        } else {
            noteOffAfterRec = getNotePairedIndexAndTime(noteOnBeforeRec).index
        }
        return Pair(noteOnBeforeRec, noteOffAfterRec)
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


    /** PLAY **/
    private fun playing(index: Int, soloIsOff: Boolean, factorBpm: Double) {
        if(notes.size <= index) Log.e(TAG, "start of playing(): channel $channelNumber, index = $index, indexToPlay = $indexToPlay, indexToRepeat = $indexToRepeat, notes.size = ${notes.size}")

        val indexOfIgnoredNote = idsOfNotesToIgnore.indexOfFirst{ it.id == notes[index].id && it.velocity == notes[index].velocity }

        if (indexOfIgnoredNote != -1) {
            Log.d(TAG, "SKIP idOfNotesToIgnore = $indexOfIgnoredNote")
            if(notes[index].velocity == 0) {
                Log.d(TAG, "REMOVE idOfNotesToIgnore = $indexOfIgnoredNote")
                idsOfNotesToIgnore.removeAt(indexOfIgnoredNote)
            }
        } else {
            val isNotPressed = !pressedNotes[notes[index].pitch].isPressed
            val isNotMuted = channelState.value.isSoloed || (soloIsOff && !channelState.value.isMuted)

            if (isNotPressed && isNotMuted) {
//                Log.d(TAG, "playing() at channel $channelNumber: deltaTimeRepeat = $deltaTimeRepeat, index = $index, pitch = ${notes[index].pitch}")
                if (indexToPlay == draggedNoteOnIndex) {
                    playingDraggedNoteOffInTime(factorBpm)
                }
                kmmk.noteOn(
                    channelNumber,
                    notes[index].pitch,
                    notes[index].velocity
                )
            }
            changePlayingNotes(notes[index].pitch, notes[index].velocity)
        }
    }


    /** ERASE **/
    fun erasing(isRepeating: Boolean, index: Int, stepViewCase: Boolean): Boolean {
        val noteToErase = notes[index]
        val pressedNote = pressedNotes[noteToErase.pitch]
        if (noteToErase.velocity > 0 && (!pressedNote.isPressed || noteToErase.id != pressedNote.id)
        ) {
            Log.d(TAG, "erasing index $index")

            var pairedNoteOffIndex = getNotePairedIndexAndTime(index).index

            // stop note_to_be_erased if playing (StepView condition)
            if(stepViewCase) stopNoteIfPlaying(isRepeating, index, pairedNoteOffIndex)

            if(pairedNoteOffIndex > index) pairedNoteOffIndex--

            // erasing noteON
            var breakFlag = eraseFromArray(index, false)

            Log.d(TAG, "erasing paired noteOff at $pairedNoteOffIndex")
            // erasing paired noteOFF
            breakFlag = eraseFromArray(pairedNoteOffIndex, breakFlag)

            // updating index when erasing note before it
            if (pairedNoteOffIndex in 0 until index) {
                if(isRepeating && indexToRepeat > 0) indexToRepeat--
                if(indexToPlay > 0) indexToPlay--
            }
            if (breakFlag) return false // break out of while() when erased last note in array

        } else {  // skipping noteOFFs or pressed notes
            if(isRepeating) indexToRepeat++
            indexToPlay++
            changePlayingNotes(notes[index].pitch, 0)
        }
        return true
    }

    private fun eraseFromArray(index: Int, breakFlag: Boolean): Boolean {
        var breakFlag1 = breakFlag
        when (index) {
            // no paired noteOFF found (do nothing)
            -1 -> { }
            // end of array
            notes.lastIndex -> {
                notes = if (notes.size > 1) notes.copyOfRange(0, index) else emptyArray()
                breakFlag1 = true
            }
            // beginning of array
            0 -> {
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
        notes = emptyArray()
        noteId = Int.MIN_VALUE
        idsOfNotesToIgnore.clear()
        for(p in 0..127){
            while(playingNotes[p] > 0) {
                kmmk.noteOn(channel, p, 0)
                changePlayingNotes(p, 0)
            }
        }
        updateNotes()
        Log.d("emptyTag", " ") // to hold in imports
    }


    private fun stopNoteIfPlaying(
        isRepeating: Boolean,
        noteOnIndex: Int,
        noteOffIndex: Int,
    ) {
        if(noteOnIndex == -1 || noteOffIndex == -1) {
            Log.d(TAG, "stopNoteIfPlaying() error: ${if(noteOnIndex == -1) "noteOnIndex" else "noteOffIndex"} out of bounds")
        }
        val dt = if (isRepeating) deltaTimeRepeat else deltaTime
        val deltaTimeInRange = if (noteOnIndex < noteOffIndex) {
            dt in notes[noteOnIndex].time..notes[noteOffIndex].time
        } else {
            dt in notes[noteOnIndex].time..totalTime.toDouble() || dt in 0.0..notes[noteOffIndex].time
        }
        while (!pressedNotes[notes[noteOnIndex].pitch].isPressed && playingNotes[notes[noteOnIndex].pitch] > 0 && deltaTimeInRange) {
            kmmk.noteOn(channelNumber, notes[noteOnIndex].pitch, 0)
            changePlayingNotes(notes[noteOnIndex].pitch, 0)
        }
    }


    /** REPEAT **/
    fun engageRepeatOnChannel(
        repeatLength: Double,
        isRepeating: Boolean,
        stopNotesOnChannel: (Int, StopNotesMode) -> Unit,
        quantizeTime: (Double) -> Double,
    ) {
        if (!isRepeating) {
            previousDeltaTime = deltaTime
            deltaTimeRepeat = if (deltaTime >= totalTime) deltaTime - totalTime else deltaTime

            if(channelNumber == 0) Log.d(TAG, "INIT repeat: deltaTimeRepeat = $deltaTimeRepeat, deltaTime = $deltaTime")
            repeatStartTime = quantizeTime(deltaTimeRepeat)
        }

        val tempRepeatEnd = repeatStartTime + repeatLength
        repeatEndTime = if(tempRepeatEnd > totalTime) tempRepeatEnd - totalTime else tempRepeatEnd

        if (!isRepeating) {
            indexToStartRepeating = notes.indexOfLast { it.time < repeatStartTime } + 1
            indexToRepeat = indexToPlay
        } else {
            stopNotesOnChannel(channelNumber, STOP_NOTES)

            val deltaTimeRepeatNotInBounds =
                (repeatStartTime < repeatEndTime && deltaTimeRepeat !in repeatStartTime .. repeatEndTime)
                    || (deltaTimeRepeat > repeatEndTime && deltaTimeRepeat < repeatStartTime)

            if (deltaTimeRepeatNotInBounds) {
                var multiplier = 1
                while (repeatEndTime + repeatLength * multiplier < deltaTimeRepeat) {
                    multiplier++
                }

                val tempDelta = deltaTimeRepeat - (repeatLength * multiplier)
                deltaTimeRepeat = if(tempDelta < 0) tempDelta + totalTime else tempDelta

                if(channelNumber == 0) Log.d(TAG, "not_init / change MULTIPLIER: deltaTimeRepeat = $deltaTimeRepeat, deltaTime = $deltaTime")
                indexToRepeat = notes.indexOfLast { it.time <= deltaTimeRepeat } + 1
            }
        }
        _channelState.update { it.copy(
            repeatStartTime = repeatStartTime,
            repeatEndTime = repeatEndTime
        ) }
        if(channelNumber == 0) Log.d(TAG, "indexToStartRepeating = $indexToStartRepeating, repeatStartTime = $repeatStartTime, repeatEndTime = $repeatEndTime")
    }


    /** STEP VIEW **/
    // on Drag Start:
    fun getNoteOnAndNoteOffIndices(pitch: Int, time: Double): Pair<Int, Int> {
        var noteOnIndex = notes.indexOfLast { it.pitch == pitch && it.time <= time }
        var noteOffIndex: Int

        if (noteOnIndex == -1 || notes[noteOnIndex].velocity == 0) {
            noteOffIndex = notes.indexOfFirst { it.pitch == pitch && it.time > time }

            if (noteOffIndex != -1 && notes[noteOffIndex].velocity == 0) {
                noteOnIndex =
                    getNotePairedIndexAndTime(noteOffIndex).index
            } else noteOffIndex = -1
        } else {
            noteOffIndex = getNotePairedIndexAndTime(noteOnIndex).index
        }
    //                                Log.d(TAG, "noteOnIndex = $noteOnIndex, noteOffIndex = $noteOffIndex")

        if (noteOnIndex == -1) noteOffIndex = -1
        if (noteOffIndex == -1) noteOnIndex = -1

        return Pair(noteOnIndex, noteOffIndex)
    }

    private fun getChangeLengthArea(noteOnIndex: Int, noteOffIndex: Int): Triple<Double, Double, Double> {
        val widthOfChangeLengthArea =
            (if (noteOnIndex < noteOffIndex) {
                (notes[noteOffIndex].time - notes[noteOnIndex].time) / 2.5
            } else {
                (notes[noteOffIndex].time - notes[noteOnIndex].time + totalTime) / 2.5
            }).coerceAtMost(totalTime / 12.0)

        // wrap-around case
        val rightmostArea = (totalTime.toDouble() - widthOfChangeLengthArea + notes[noteOffIndex].time).coerceAtMost(totalTime.toDouble())
        val leftmostArea = (notes[noteOffIndex].time - widthOfChangeLengthArea).coerceAtLeast(0.0)

        return Triple(widthOfChangeLengthArea, rightmostArea, leftmostArea)
    }

    fun isNoteDetected(noteOnIndex: Int, noteOffIndex: Int, time: Double): Boolean {
        return (noteOnIndex > -1 && noteOffIndex > -1) && (
                (time in notes[noteOnIndex].time..notes[noteOffIndex].time)  // normal case (not wrap-around)
                    || ((noteOnIndex > noteOffIndex) &&                            // wrap-around case
                    (time in notes[noteOnIndex].time..totalTime.toDouble()) || (time in 0.0..notes[noteOffIndex].time))
                )
    }

    fun isChangeLengthAreaDetected(noteOnIndex: Int, noteOffIndex: Int, time: Double): Boolean {
        val (widthOfChangeLengthArea, rightmostArea, leftmostArea) = getChangeLengthArea(noteOnIndex, noteOffIndex)

        return (noteOnIndex > -1 && noteOffIndex > -1) && (
                (time in (notes[noteOffIndex].time - widthOfChangeLengthArea)..notes[noteOffIndex].time)  // normal case (not wrap-around)
                    || ((noteOnIndex > noteOffIndex) &&                            // wrap-around case
                    (time in rightmostArea..totalTime.toDouble()) || (time in leftmostArea..notes[noteOffIndex].time))
                )
    }

    fun getNoteDeltaTime(changeLengthAreaDetected: Boolean, noteOnIndex: Int, noteOffIndex: Int, quantizeTime: (Double) -> Double): Double {
        return if (!changeLengthAreaDetected) {
            val tempTime = notes[noteOnIndex].time
            changeNoteTime(noteOnIndex, quantizeTime(tempTime))
            val noteDeltaTime = notes[noteOnIndex].time - tempTime
            changeNoteTime(noteOffIndex, noteDeltaTime, true)
            noteDeltaTime
        } else {
            val tempTime = notes[noteOffIndex].time
            changeNoteTime(noteOffIndex, quantizeTime(tempTime))
            notes[noteOffIndex].time - tempTime
        }
    }

    fun fireNoteOffInTime(seqIsPlaying: Boolean, isRepeating: Boolean, factorBpm: Double, noteOnIndex: Int, noteOffIndex: Int, pitch: Int) {
        val currentIndex = if (isRepeating) indexToRepeat else indexToPlay
        val noteIsPlaying = seqIsPlaying &&
            (currentIndex in noteOnIndex + 1..noteOffIndex // normal case (not wrap-around)
                || (noteOnIndex > noteOffIndex    // wrap-around case
                && (currentIndex in noteOnIndex + 1 .. notes.size || currentIndex in 0..noteOffIndex)
                    )
                )

        if (noteIsPlaying) {
            Log.d(TAG, "onDragStart: fireNoteOffOnTime() at pitch $pitch")
            idsOfNotesToIgnore.add(NoteIdAndVelocity(notes[noteOnIndex].id, 0) )

            draggedNoteOffJob = CoroutineScope(Dispatchers.Default).launch {
                val dt =
                    if (isRepeating) deltaTimeRepeat else deltaTime
                val delayTime =
                    if (dt > notes[noteOffIndex].time) {
                        notes[noteOffIndex].time - dt + totalTime
                    } else {
                        notes[noteOffIndex].time - dt
                    }
                delay((delayTime / factorBpm).toLong())
                kmmk.noteOn(channelNumber, pitch, 0)
                changePlayingNotes(pitch, 0)
            }
            pitchTempSavedForCancel = pitch
        }
    }

    // on Drag:
    private fun playingDraggedNoteOffInTime(factorBpm: Double) {
        Log.d(TAG, "onDrag: playingDraggedNoteOffInTime() at pitch ${notes[draggedNoteOnIndex].pitch}")

        val delayTime = if (draggedNoteOnIndex < draggedNoteOffIndex) {
            notes[draggedNoteOffIndex].time - notes[draggedNoteOnIndex].time
        } else {
            notes[draggedNoteOffIndex].time - notes[draggedNoteOnIndex].time + totalTime
        }
        val pitchTemp = notes[draggedNoteOnIndex].pitch

        if(draggedNoteOffJob.isActive) {
            draggedNoteOffJob.cancel()
            Log.d(TAG, "draggedNoteOffJob.cancel()")
            kmmk.noteOn(channelNumber, pitchTempSavedForCancel, 0)
            changePlayingNotes(pitchTempSavedForCancel, 0)
            idsOfNotesToIgnore.removeIf { it.id == notes[draggedNoteOnIndex].id }
        }
        pitchTempSavedForCancel = pitchTemp
        idsOfNotesToIgnore.add(NoteIdAndVelocity(notes[draggedNoteOnIndex].id, 0))

        draggedNoteOffJob = CoroutineScope(Dispatchers.Default).launch {
            delay((delayTime / factorBpm).toLong())
            kmmk.noteOn(channelNumber, pitchTemp, 0)
            changePlayingNotes(pitchTemp, 0)
        }
    }

    fun swapNoteOffAndNoteOn(changeLengthAreaDetected: Boolean, noteOnIndex: Int, noteOffIndex: Int, draggedNoteId: Int): Pair<Int, Int> {
        if (changeLengthAreaDetected && notes[noteOffIndex].time == notes[noteOnIndex].time && noteOffIndex > noteOnIndex) {
            notes[noteOffIndex] = notes[noteOnIndex].also {
                notes[noteOnIndex] = notes[noteOffIndex]
            } // swapping two elements for wrap-around note when length = 0
        }

        sortNotesByTime()
        updateNotes()

        val noteOnIndexAfterSort = notes.indexOfFirst { it.id == draggedNoteId && it.velocity > 0 }
        val noteOffIndexAfterSort = notes.indexOfFirst { it.id == draggedNoteId && it.velocity == 0 }

        return if (noteOnIndexAfterSort != noteOnIndex || noteOffIndexAfterSort != noteOffIndex) {
            draggedNoteOnIndex = noteOnIndexAfterSort
            draggedNoteOffIndex = noteOffIndexAfterSort
            Pair(noteOnIndexAfterSort, noteOffIndexAfterSort)
        } else Pair(noteOnIndex, noteOffIndex)
    }

    // on Drag End:
    fun removeNoteOffFromIgnore (isRepeating: Boolean) {
        val index = if (isRepeating) indexToRepeat else indexToPlay

        val noteIsPlaying = (index in draggedNoteOnIndex + 1..draggedNoteOffIndex   // [..]
            || (draggedNoteOnIndex > draggedNoteOffIndex   // ..] or [..
            && (index in draggedNoteOnIndex + 1..notes.size || index in 0..draggedNoteOffIndex)))

        if (!noteIsPlaying) idsOfNotesToIgnore.removeIf { it.id == notes[draggedNoteOnIndex].id }
    }

    fun eraseOverlappingNotes(noteOnIndex: Int, noteOffIndex: Int, pitch: Int, isRepeating: Boolean) {
        val noteOnTime = notes[noteOnIndex].time
        val noteOffTime = notes[noteOffIndex].time
        val notePitch = notes[noteOnIndex].pitch
        val noteId = notes[noteOnIndex].id
        var indexOfNearestNote = -1
        var nearestNoteOverlapsFully = false

        while (true) {
            val indexToErase = if (noteOnIndex < noteOffIndex) {  // not wrap-around [..]
                notes.indexOfFirst {
                    it.pitch == pitch && it.id != noteId && (
                        (it.time > noteOnTime && it.time < noteOffTime)
                            || (it.time == noteOnTime && it.velocity > 0)
                            || (it.time == noteOffTime && it.velocity == 0)
                        )
                }
            } else {                                            // wrap-around ..] or [..
                notes.indexOfFirst {
                    it.pitch == pitch && it.id != noteId && (
                        (it.time > noteOnTime && it.time <= totalTime)
                            || (it.time >= 0 && it.time < noteOffTime)
                            || (it.time == noteOnTime && it.velocity > 0)
                            || (it.time == noteOffTime && it.velocity == 0)
                        )
                }
            }

            if(indexToErase == -1) {
                indexOfNearestNote = notes.indexOfLast { it.pitch == notePitch && it.time < noteOnTime}
                nearestNoteOverlapsFully = indexOfNearestNote != -1 && notes[indexOfNearestNote].velocity > 0
            }

            when {
                indexToErase != -1 -> {
                    erasing(
                        isRepeating,
                        if (notes[indexToErase].velocity > 0) indexToErase else getNotePairedIndexAndTime(indexToErase).index,
                        false
                    )
                }
                nearestNoteOverlapsFully -> {
                    erasing(
                        isRepeating,
                        indexOfNearestNote,
                        false
                    )
                }
                else -> break
            }
        }
    }



    /** MISC **/
    fun changeKeyboardOctave(lowKeyboard: Boolean, increment: Int) {
        val newValue = ((if (lowKeyboard) channelState.value.pianoViewOctaveLow else channelState.value.pianoViewOctaveHigh) + increment).coerceIn(-1..7)

        if(lowKeyboard) {
            _channelState.update { it.copy( pianoViewOctaveLow = newValue ) }
        } else {
            _channelState.update { it.copy( pianoViewOctaveHigh = newValue ) }
        }
    }

    fun getNotePairedIndexAndTime(index: Int, uiUpdate: Boolean = false): NoteIndexAndTime {
        val localNotes = if (uiUpdate) channelState.value.notes else notes
        if(localNotes.lastIndex < index) return NoteIndexAndTime(-1, Double.MIN_VALUE)
        val notePairedIndex = localNotes.indexOfFirst { it.id == localNotes[index].id && it.velocity != localNotes[index].velocity}
        return if(notePairedIndex > -1) NoteIndexAndTime(notePairedIndex, localNotes[notePairedIndex].time) else NoteIndexAndTime(-1, Double.MIN_VALUE)
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

        when {
            notes[index].time > BARTIME -> notes[index].time -= BARTIME
            notes[index].time == BARTIME.toDouble() && notes[index].velocity > 0 -> notes[index].time = 0.0
            notes[index].time < 0 -> notes[index].time += BARTIME
            else -> {}
        }
    }

    fun sortNotesByTime(){
        notes.sortBy { it.time }
    }

    fun refreshIndices() {
        indexToPlay = notes.indexOfLast { it.time < deltaTime } + 1
        indexToRepeat = notes.indexOfLast { it.time < deltaTimeRepeat } + 1
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
        Log.d(TAG, "channel $channelNumber: playingNotes[$pitch] = ${playingNotes[pitch]}, channelIsPlayingNotes = $channelIsPlayingNotes")

        if(playingNotes[pitch] < 0) {
            Log.d(TAG, "playingNotes[$pitch] = ${playingNotes[pitch]}, fixing to 0")
            playingNotes[pitch] = 0
        }
        if(channelIsPlayingNotes < 0) {
            Log.d(TAG, "channelIsPlayingNotes = $channelIsPlayingNotes, fixing to 0")
            channelIsPlayingNotes = 0
        }
    }

    fun cancelPadInteraction() {
        runBlocking {
            for (p in 0..127) {
                if(pressedNotes[p].isPressed) {
                    interactionSources[p].interactionSource.emit (PressInteraction.Cancel (interactionSources[p].pressInteraction))
                }
            }
        }
    }

    fun resetChannelSequencerValues() {
        startTimeStamp = System.currentTimeMillis()
        indexToPlay = 0
        indexToRepeat = 0
        deltaTime = 0.0
    }

    fun updateChannelState() {
        _channelState.update { it.copy(
            notes = notes,
            deltaTime = deltaTime,
            deltaTimeRepeat = deltaTimeRepeat,
            playingNotes = playingNotes,
            channelIsPlayingNotes = channelIsPlayingNotes
        ) }
    }

    fun updateNotes() {
        _channelState.update { it.copy(notes = notes) }
//        if (channelNumber == 0) Log.d(TAG, "updateNotes()")
    }

    fun updatePressedNote(
        pitch: Int,
        isPressed: Boolean,
        id: Int = pressedNotes[pitch].id,
        noteOnTimestamp: Long = pressedNotes[pitch].noteOnTimestamp,
    ) {
        pressedNotes[pitch] = PressedNote(isPressed, id, noteOnTimestamp)
        _channelState.update { it.copy(
            pressedNotes = pressedNotes.toMutableList().toTypedArray()  // this double transformation is needed to fail array equality test and force recomposition
        ) }
    }

    fun updatePadState() {
        _channelState.update { it.copy(
            playingNotes = playingNotes,
            channelIsPlayingNotes = channelIsPlayingNotes
        ) }
    }

    fun updateIsSoloed(isSoloed: Boolean) {
        _channelState.update { it.copy( isSoloed = isSoloed ) }
    }

    fun updateIsMuted(isMuted: Boolean) {
        _channelState.update { it.copy( isMuted = isMuted ) }
    }

    fun updateStepViewYScroll(stepViewYScroll: Int) {
        _channelState.update { it.copy( stepViewYScroll = stepViewYScroll ) }
    }

    fun updateStepViewNoteGrid() {
        _channelState.update { it.copy(stepViewRefresh = !channelState.value.stepViewRefresh) }
    }
}