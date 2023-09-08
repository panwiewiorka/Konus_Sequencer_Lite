package com.example.mysequencer01ui

import android.util.Log
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import com.example.mysequencer01ui.ui.BARTIME
import com.example.mysequencer01ui.ui.ChannelState
import com.example.mysequencer01ui.StopNotesMode.*
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
    var notesDragEndOnRepeat: Array<Boolean> = Array(128){ false } // remembering noteOFFs that shouldn't play on release/cancel
    var idsOfNotesToIgnore: MutableList<Int> = mutableListOf()
    private var listOfNotesToRecord: MutableList<RecordingPackage> = mutableListOf()
    var pitchTempSavedForCancel: Int = 0
    var onPressedMode: PadsMode = PadsMode.DEFAULT
    var noteId: Int = Int.MIN_VALUE

    var draggedNoteOnIndex: Int = -1
    var draggedNoteOffIndex: Int = -1

    var indexToPlay: Int = 0
    var startTimeStamp: Long = 0
    var bpmDelta: Double = 0.0

    var indexToStartRepeating: Int = 0
    var indexToRepeat: Int = 0
    private var previousDeltaTime: Double = 0.0

    val interactionSources: Array<RememberedPressInteraction> = Array(128) {
        RememberedPressInteraction(
            MutableInteractionSource(),
            PressInteraction.Press( Offset(0f,0f) )
        )
    }


    var notes = channelState.value.notes

    var pressedNotes = channelState.value.pressedNotes
    var playingNotes: Array<Int> = channelState.value.playingNotes
    private var channelIsPlayingNotes = channelState.value.channelIsPlayingNotes

    var totalTime = channelState.value.totalTime
    var deltaTime = channelState.value.deltaTime

    private var repeatStartTime = channelState.value.repeatStartTime
    var repeatEndTime = channelState.value.repeatEndTime
    var deltaTimeRepeat = channelState.value.deltaTimeRepeat



/** MAIN CYCLE **/
    fun advanceChannelSequence(
        seqIsPlaying: Boolean,
        seqIsRecording: Boolean,
        factorBpm: Double,
        soloIsOn: Boolean,
        quantizationTime: Double,
        quantizeTime: (Double) -> Double,
        isRepeating: Boolean,
        isStepView: Boolean,
        stopNotesOnChannel: (Int, StopNotesMode) -> Unit,
    ) {
        deltaTime = (System.currentTimeMillis() - startTimeStamp) * factorBpm + bpmDelta

        while (notes.size > indexToPlay && notes[indexToPlay].time <= deltaTime) {
            if (eraseOrPlay(seqIsRecording, quantizationTime, factorBpm, isRepeating, soloIsOn, isStepView)) break
        }

        if (isRepeating) {
            advanceRepeating(seqIsRecording, soloIsOn, isStepView, factorBpm, stopNotesOnChannel, quantizationTime, quantizeTime)
        }

        // end of sequence
        if (deltaTime >= totalTime) {
            startTimeStamp = System.currentTimeMillis() - (deltaTime - totalTime).toLong()
            bpmDelta = 0.0
            indexToPlay = 0
            idsOfNotesToIgnore.clear()
        }

        // record notes (if any)
        while (listOfNotesToRecord.isNotEmpty()) {
            with(listOfNotesToRecord[0]) {
                recording(
                    recordTime = recordTime,
                    pitch = pitch,
                    id = id,
                    velocity = velocity,
                    isStepView = isStepView,
                    noteHeight = noteHeight
                )
                val dt = if(isRepeating) deltaTimeRepeat else deltaTime
                if (velocity > 0 && seqIsPlaying && recordTime > dt && !isStepRecord) {
                    idsOfNotesToIgnore.add(id)
                }
            }
            listOfNotesToRecord.removeAt(0)
        }

        _channelState.update { it.copy(
            notes = notes,
            deltaTime = deltaTime,
            deltaTimeRepeat = deltaTimeRepeat,
            playingNotes = playingNotes,
            channelIsPlayingNotes = channelIsPlayingNotes
        ) }
    }

    private fun eraseOrPlay(
        seqIsRecording: Boolean,
        quantizationTime: Double,
        factorBpm: Double,
        isRepeating: Boolean,
        soloIsOn: Boolean,
        isStepView: Boolean,
    ): Boolean {
        val noteToPlay = notes[indexToPlay]
        val pressedNote = pressedNotes[noteToPlay.pitch]
        val overdubbing = seqIsRecording && pressedNote.isPressed
        val noteShouldBeTiedToItself = System.currentTimeMillis() - pressedNote.noteOnTimestamp > quantizationTime / factorBpm * 1.5

        if (!isRepeating && (isErasing || overdubbing)) {
            if (overdubbing && pressedNote.id == noteToPlay.id && noteShouldBeTiedToItself) { // tie note
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
                    isStepView
                )
                // StepView case:
                if (indexToPlay == draggedNoteOnIndex) {
                    playingDraggedNoteOffInTime(factorBpm)
                }
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
        quantizationTime: Double,
        quantizeTime: (Double) -> Double,
    ) {
        if (deltaTime < previousDeltaTime) {
            previousDeltaTime -= totalTime
        }
        deltaTimeRepeat += deltaTime - previousDeltaTime
        previousDeltaTime = deltaTime

        // erasing or playing

        while (
        // make sure element exists in array
            notes.size > indexToRepeat
            // time to play note
            && (notes[indexToRepeat].time in repeatStartTime..deltaTimeRepeat
                || (repeatStartTime > repeatEndTime && (notes[indexToRepeat].time in 0.0..deltaTimeRepeat)))
            // note is in repeat bounds
            && (notes[indexToRepeat].time in repeatStartTime..repeatEndTime     // [...]
                || (repeatStartTime > repeatEndTime && (notes[indexToRepeat].time >= repeatStartTime || notes[indexToRepeat].time <= repeatEndTime))) // ..] && [..
            // no noteON present in repeatEndTime
            && !(notes[indexToRepeat].time == repeatEndTime && notes[indexToRepeat].velocity > 0)
        ) {
            if (eraseOrPlayInRepeat(seqIsRecording, repeatEndTime, repeatStartTime, soloIsOn, isStepView, factorBpm)) break
        }

        // end of repeating loop
        if ((repeatStartTime < repeatEndTime && deltaTimeRepeat > repeatEndTime) || deltaTimeRepeat in repeatEndTime..repeatStartTime) {
            restartRepeatingLoop(stopNotesOnChannel, seqIsRecording, quantizationTime, quantizeTime, factorBpm, isStepView)
        }
        // wrap-around
        if (deltaTimeRepeat > totalTime) {
            deltaTimeRepeat -= totalTime
            indexToRepeat = 0
        }
    }

    private fun restartRepeatingLoop(
        stopNotesOnChannel: (Int, StopNotesMode) -> Unit,
        seqIsRecording: Boolean,
        quantizationTime: Double,
        quantizeTime: (Double) -> Double,
        factorBpm: Double,
        isStepView: Boolean,
    ) {
        stopNotesOnChannel(channelNumber, END_OF_REPEAT)  // notesOFF on the end of loop // TODO activate only if channelIsPlayingNotes?

        indexToStartRepeating = searchIndexInTimeBoundaries()
        indexToRepeat = indexToStartRepeating

        deltaTimeRepeat -= (repeatEndTime - repeatStartTime)
        idsOfNotesToIgnore.clear()

        for (p in 0..127) { // TODO change pressedNotes[] to mutableList?
            // ignore noteOff if we start repeatLoop in the middle of the note
            var hangingNoteOffIndex = notes.indexOfFirst {
                it.pitch == p && (it.time in repeatStartTime..repeatEndTime || (repeatStartTime > repeatEndTime && it.time >= repeatStartTime))
            }
            if (hangingNoteOffIndex == -1 && repeatStartTime > repeatEndTime) hangingNoteOffIndex = notes.indexOfFirst {
                it.pitch == p && it.time in 0.0..repeatEndTime
            }
            if (hangingNoteOffIndex != -1 && notes[hangingNoteOffIndex].velocity == 0
                && (getNotePairedIndexAndTime(hangingNoteOffIndex).time !in repeatStartTime..deltaTimeRepeat
                    || (repeatStartTime > repeatEndTime && getNotePairedIndexAndTime(hangingNoteOffIndex).time in 0.0..deltaTimeRepeat))
            ) {
                idsOfNotesToIgnore.add(notes[hangingNoteOffIndex].id)
                Log.d("ryjtyj", "hanging NoteOff INDEX on the start of the loop = $hangingNoteOffIndex")
            }
            // notesON on the start of the loop if notes are being held
            if (pressedNotes[p].isPressed) {
                kmmk.noteOn(channelNumber, p, 100)
                Log.d("ryjtyj", "noteON on the start of the loop (note $p is being pressed")
                if (seqIsRecording) recordNote(
                    pitch = p,
                    velocity = 100,
                    id = pressedNotes[p].id,
                    quantizationTime = quantizationTime,
                    quantizeTime = quantizeTime,
                    factorBpm = factorBpm,
                    seqIsPlaying = true,
                    isRepeating = true,
                    isStepView = isStepView,
                    isStepRecord = false,
                    customTime = repeatStartTime
                )
                changePlayingNotes(p, 100)
                increaseNoteId()
            }
        }
    }

    private fun eraseOrPlayInRepeat(
        seqIsRecording: Boolean,
        repeatEndTime: Double,
        repeatStartTime: Double,
        soloIsOn: Boolean,
        isStepView: Boolean,
        factorBpm: Double,
    ): Boolean {
        val noteToRepeat = notes[indexToRepeat]
        val pressedNote = pressedNotes[noteToRepeat.pitch]
        val overdubbing = seqIsRecording && pressedNote.isPressed
        if (isErasing || overdubbing) {
            // tie note
            if (overdubbing && (getNotePairedIndexAndTime(indexToRepeat).time == repeatEndTime
                    && notes[notes.indexOfLast { it.id == pressedNote.id }].time == repeatStartTime)
            ) {
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
                Log.d("ryjtyj", "tied note ${noteToRepeat.pitch}")
            } else
                if (!erasing(true, indexToRepeat, false)) return true
        } else {
            if (indexToRepeat != draggedNoteOffIndex) {
                playing(
                    indexToRepeat,
                    !soloIsOn,
                    isStepView
                )
                // StepView case:
                if (indexToRepeat == draggedNoteOnIndex) {
                    playingDraggedNoteOffInTime(factorBpm)
                }
            }
            indexToRepeat++
        }
        return false
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
        isStepView: Boolean,
        isStepRecord: Boolean,
        customTime: Double = -1.0,
        noteHeight: Float = 60f
    ) {
        Log.d("ryjtyj", "recording() at channel $channelNumber, id = ${id - Int.MIN_VALUE}, velocity = $velocity")

        if(velocity == 0 && notes.indexOfFirst { it.id == id && it.velocity > 0 } == -1
            && !isStepRecord // added because of delayed recording in main cycle
        ) {}//return // TODO uncomment 'return' or delete whole condition?

        val customRecTime = if (customTime > totalTime) customTime - totalTime else customTime

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
        )
        Log.d("ryjtyj", "recordTime = $recordTime")

        if (recordTime == repeatEndTime && isRepeating && velocity > 0) return // TODO change to recordTime = repeatStartTime instead of ignoring record?

        if(seqIsPlaying) {
            listOfNotesToRecord.add(RecordingPackage(recordTime, pitch, id, velocity, isStepView, isStepRecord, noteHeight))
        } else {
            recording(recordTime, pitch, id, velocity, isStepView, noteHeight)
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
    ): Double {
        var time = when {
            (customRecTime > -1) -> if (customRecTime == totalTime.toDouble() && velocity > 0) 0.0 else customRecTime
            !seqIsPlaying -> if (velocity > 0) 0.0 else quantizationTime
            !isRepeating -> deltaTime
            else -> deltaTimeRepeat
        }
        Log.d("ryjtyj", "time = $time")

        if (velocity > 0
//                && seqIsPlaying
            ) {
            time = quantizeTime(time)
//            idOfNotesToIgnore.add(id)
        } else {
            val indexOfQuantizedNoteOn =
                notes.indexOfFirst { it.id == pressedNotes[pitch].id }
            val notBigWrapAround =
                System.currentTimeMillis() - pressedNotes[pitch].noteOnTimestamp <= quantizationTime / factorBpm  // catching rare case of almost self-tied note  ..]_[..

            if (notBigWrapAround && indexOfQuantizedNoteOn != -1 && notes[indexOfQuantizedNoteOn].time == 0.0) { // TODO needed?
                time = quantizationTime
            }

            val pairedNoteOn = listOfNotesToRecord.indexOfFirst { it.id == id }
            val noteOnTime = when {
                indexOfQuantizedNoteOn != -1 -> notes[indexOfQuantizedNoteOn].time
                pairedNoteOn != -1 -> listOfNotesToRecord[pairedNoteOn].recordTime
                else -> Double.MIN_VALUE
            }
            val noteIsShorterThanQuantization = (abs(time - noteOnTime) < quantizationTime)

            if (noteIsShorterThanQuantization && notBigWrapAround) {
                time = noteOnTime + quantizationTime
            }
        }

        return when {
            (customRecTime > -1) || !seqIsPlaying -> {
                time
            }
            !isRepeating -> {
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
        Log.d("ryjtyj", "index to record: $index, record time = $recordTime")

        index = overdubPlayingNotes(pitch, recordTime, id, velocity, index)

        // if noteOff has the same time as some other note(ON) -> place it before:
        val indexOfNoteOnAtTheSameTime =
            notes.indexOfLast { it.time == recordTime && it.pitch == pitch && it.velocity > 0 && velocity == 0 }
        if (indexOfNoteOnAtTheSameTime != -1) {
            Log.d("ryjtyj", "indexOfNoteOnAtTheSameTime = $indexOfNoteOnAtTheSameTime")
            index = indexOfNoteOnAtTheSameTime
        }

        // RECORDING
        Log.d("ryjtyj", "RECORDING index = $index, id = ${id - Int.MIN_VALUE}")
        recIntoArray(index, recordTime, pitch, velocity, id)

        refreshIndices()

        if (!isStepView) {
            updateStepViewYScroll((noteHeight * (pitch - 5)).toInt().coerceAtLeast(0))
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

        if (noteOnBeforeRec != -1 && (notes[noteOnBeforeRec].time == recordTime && notes[noteOnBeforeRec].velocity > 0 && velocity == 0)) {
            noteOnBeforeRec = -1
            noteOffAfterRec = -1
        }

        if (noteOnBeforeRec != -1 || noteOffAfterRec != -1) {
            Log.d("ryjtyj", "OVERDUB: noteOnBeforeRec = $noteOnBeforeRec, noteOffAfterRec = $noteOffAfterRec")

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
    private fun playing(index: Int, soloIsOff: Boolean, stepView: Boolean) {

        if(notes.size <= index) Log.e("ryjtyj", "start of playing(): channel $channelNumber, index = $index, indexToPlay = $indexToPlay, notes.size = ${notes.size}")

        if(notesDragEndOnRepeat[notes[index].pitch] && stepView && notes[index].velocity == 0) {  // a fix for playingNotes == -1 when releasing dragged note that was playing
            Log.d("ryjtyj", "notesDragEndOnRepeat INDEX = $index, PITCH = ${notes[index].pitch}")
            notesDragEndOnRepeat[notes[index].pitch] = false
        } //else if (notes[index].time - pressedNotes[notes[index].pitch].noteOnTimestamp <= quantizationTime / 2 / factorBpm)

        else if (idsOfNotesToIgnore.indexOfFirst{ it == notes[index].id } != -1) {  // fix for skipping recording notes that are quantized forward
            Log.d("ryjtyj", "SKIP idOfNotesToIgnore = ${idsOfNotesToIgnore.indexOfFirst{ it == notes[index].id }}")
            if(notes[index].velocity == 0) {
                Log.d("ryjtyj", "REMOVE idOfNotesToIgnore = ${idsOfNotesToIgnore.indexOfFirst{ it == notes[index].id }}")
                idsOfNotesToIgnore.removeAt(idsOfNotesToIgnore.indexOfFirst{ it == notes[index].id })
            }
        }
        else {
            if (!pressedNotes[notes[index].pitch].isPressed && (channelState.value.isSoloed || (soloIsOff && !channelState.value.isMuted))) {
                Log.d("ryjtyj", "playing() at channel $channelNumber: deltaTimeRepeat = $deltaTimeRepeat, index = $index, pitch = ${notes[index].pitch}")
                kmmk.noteOn(
                    channelNumber,
                    notes[index].pitch,
                    notes[index].velocity
                )
            }
            changePlayingNotes(notes[index].pitch, notes[index].velocity)
        }
    }

    var draggedNoteOffJob = CoroutineScope(Dispatchers.Default).launch { }

    private fun playingDraggedNoteOffInTime(factorBpm: Double) {
        Log.d("ryjtyj", "playingDraggedNoteOffInTime()")
        val delayTime = if (draggedNoteOnIndex < draggedNoteOffIndex) {
            notes[draggedNoteOffIndex].time - notes[draggedNoteOnIndex].time
        } else {
            notes[draggedNoteOffIndex].time - notes[draggedNoteOnIndex].time + totalTime
        }
        val pitchTemp = notes[draggedNoteOnIndex].pitch

        if(draggedNoteOffJob.isActive) {
            draggedNoteOffJob.cancel()
            Log.d("ryjtyj", "draggedNoteOffJob.cancel()")
            kmmk.noteOn(channelNumber, pitchTempSavedForCancel, 0)
            changePlayingNotes(pitchTempSavedForCancel, 0)
        }
        pitchTempSavedForCancel = pitchTemp // TODO do we need a list of those pitches (could there be more than one?)

        draggedNoteOffJob = CoroutineScope(Dispatchers.Default).launch {
            delay((delayTime / factorBpm).toLong())
            kmmk.noteOn(channelNumber, pitchTemp, 0)
            changePlayingNotes(pitchTemp, 0)
        }
    }


/** ERASE **/
    fun erasing(isRepeating: Boolean, index: Int, stepViewCase: Boolean): Boolean {
        val noteToErase = notes[index]
        val pressedNote = pressedNotes[noteToErase.pitch]
        if (noteToErase.velocity > 0 && (noteToErase.id != pressedNote.id || !pressedNote.isPressed)
        ) {
            Log.d("ryjtyj", "erasing index $index")

            var pairedNoteOffIndex = getNotePairedIndexAndTime(index).index

            // stop note_to_be_erased if playing (StepView condition)
            if(stepViewCase) stopNoteIfPlaying(isRepeating, index, pairedNoteOffIndex)

            if(pairedNoteOffIndex > index) pairedNoteOffIndex--

            // erasing noteON
            var breakFlag = eraseFromArray(index, false) // FIXME inside there's an updateNotes(), which is necessary only lower (when erasing noteOff) vv

            Log.d("ryjtyj", "erasing paired noteOff at $pairedNoteOffIndex")
            // erasing paired noteOFF
            breakFlag = eraseFromArray(pairedNoteOffIndex, breakFlag)

            // updating index when erasing note before it
            if (pairedNoteOffIndex < index && pairedNoteOffIndex > -1) {
                if(isRepeating) if(indexToRepeat > 0) indexToRepeat-- else if(indexToPlay > 0) indexToPlay-- // TODO why we don't indexToPlay-- in other conditions, if else if..
            }
            if (breakFlag) return false // break out of while() when erased last note in array

        } else {  // skipping noteOFFs
            if(isRepeating) indexToRepeat++ else indexToPlay++
            changePlayingNotes(notes[index].pitch, 0)
        }
        return true
    }

    fun eraseFromArray(index: Int, breakFlag: Boolean): Boolean {
        var breakFlag1 = breakFlag
        when {
            // no paired noteOFF found
            index == -1 -> {  // do nothing
            }
            // end of array
            index == notes.lastIndex -> {
                notes = if (notes.size > 1) notes.copyOfRange(0, index) else emptyArray()
                breakFlag1 = true
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
            Log.d("ryjtyj", "stopNoteIfPlaying() error: ${if(noteOnIndex == -1) "noteOnIndex" else "noteOffIndex"} out of bounds")
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

            if(channelNumber == 0) Log.d("ryjtyj", "INIT repeat: deltaTimeRepeat = $deltaTimeRepeat, deltaTime = $deltaTime")
            repeatStartTime = quantizeTime(deltaTimeRepeat)
        }

        val tempRepeatEnd = repeatStartTime + repeatLength
        repeatEndTime = if(tempRepeatEnd > totalTime) tempRepeatEnd - totalTime else tempRepeatEnd

        if (!isRepeating) {
            indexToStartRepeating = searchIndexInTimeBoundaries()
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

//                    deltaTimeRepeat -= repeatLength * multiplier
//                    if (deltaTimeRepeat < 0) {
//                        deltaTimeRepeat += totalTime
//                    }

                if(channelNumber == 0) Log.d("ryjtyj", "not_init / change MULTIPLIER: deltaTimeRepeat = ${deltaTimeRepeat}, deltaTime = ${deltaTime}")
                indexToRepeat = notes.indexOfLast { it.time <= deltaTimeRepeat } + 1
            }
        }
        _channelState.update { it.copy(
            repeatStartTime = repeatStartTime,
            repeatEndTime = repeatEndTime
        ) }
        if(channelNumber == 0) Log.d("ryjtyj", "indexToStartRepeating = $indexToStartRepeating, repeatStartTime = ${repeatStartTime}, repeatEndTime = ${repeatEndTime}")
    }



/** STEP VIEW **/
// on Drag Start:
    fun getNoteOnAndNoteOffIndices(pitch: Int, time: Double): Pair<Int, Int> {
        var noteOnIndex = notes.indexOfLast { it.pitch == pitch && it.time <= time }
        var noteOffIndex: Int

        if (noteOnIndex == -1 || notes[noteOnIndex].velocity == 0) {
//            noteOffIndex = -1 // FIXME why it's here?
            noteOffIndex = notes.indexOfFirst { it.pitch == pitch && it.time > time }

            if (noteOffIndex != -1 && notes[noteOffIndex].velocity == 0) {
                noteOnIndex =
                    getNotePairedIndexAndTime(noteOffIndex).index
            } else noteOffIndex = -1
        } else {
            noteOffIndex = getNotePairedIndexAndTime(noteOnIndex).index
        }
    //                                Log.d("ryjtyj", "noteOnIndex = $noteOnIndex, noteOffIndex = $noteOffIndex")

        if (noteOnIndex == -1) noteOffIndex = -1
        if (noteOffIndex == -1) noteOnIndex = -1

        return Pair(noteOnIndex, noteOffIndex)
    }

    private fun getChangeLengthArea(noteOnIndex: Int, noteOffIndex: Int): Triple<Double, Double, Double> {
//        var widthOfChangeLengthArea =
//            if (noteOnIndex < noteOffIndex) {   // normal case (not wrap-around)
//                (notes[noteOffIndex].time - notes[noteOnIndex].time) / 2.5
//            } else if (noteOnIndex > noteOffIndex) {   // wrap-around case
//                (notes[noteOffIndex].time - notes[noteOnIndex].time + totalTime) / 2.5
//            } else 0.0
//        if (widthOfChangeLengthArea !in 0.0..totalTime / 12.0) {
//            widthOfChangeLengthArea = totalTime / 12.0
//        }

//                                        val rightmostAreaOfChangeLength =
//                                            if (noteOffIndex == -1) 0.0 else {
//                                                (channelState.totalTime.toDouble() - widthOfChangeLengthArea + notes[noteOffIndex].time)
//                                                    .coerceAtMost(channelState.totalTime.toDouble())
//                                            }
//                                        val leftmostAreaOfChangeLength =
//                                            if (noteOffIndex == -1) 0.0 else {
//                                                (notes[noteOffIndex].time - widthOfChangeLengthArea).coerceAtLeast(0.0)
//                                            }

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

    fun fireNoteOffOnTime(seqIsPlaying: Boolean, isRepeating: Boolean, factorBpm: Double, noteOnIndex: Int, noteOffIndex: Int, pitch: Int) {
        val currentIndex =
            if (isRepeating) indexToRepeat else indexToPlay

        if (seqIsPlaying &&
            (currentIndex in noteOnIndex + 1..noteOffIndex // normal case (not wrap-around)
                || noteOnIndex > noteOffIndex && (   // wrap-around case
                    currentIndex in noteOnIndex + 1 .. notes.size || currentIndex in 0..noteOffIndex
                )
            )
        ) {
            CoroutineScope(Dispatchers.Default).launch {
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
        }
    }

// on Drag:
    fun updateIndices(changeLengthAreaDetected: Boolean, noteOnIndex: Int, noteOffIndex: Int, draggedNoteId: Int): Pair<Int, Int> {
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
            refreshIndices()
            Pair(noteOnIndexAfterSort, noteOffIndexAfterSort)
        } else Pair(noteOnIndex, noteOffIndex)
    }

// on Drag End:
    fun eraseOverlappingNotes(noteOnIndex: Int, noteOffIndex: Int, pitch: Int, isRepeating: Boolean) {
        draggedNoteOnIndex = -1
        draggedNoteOffIndex = -1

        val noteOnTime = notes[noteOnIndex].time
        val noteOffTime = notes[noteOffIndex].time
        val noteId = notes[noteOnIndex].id

        while (true) {
            val indexToErase = if (noteOnIndex < noteOffIndex) {  // not wrap-around [..]
                notes.indexOfFirst {
                    it.pitch == pitch && it.id != noteId && (
                        (it.time > noteOnTime && it.time < noteOffTime)
                            || (it.time == noteOnTime && it.velocity > 0)
                            || (it.time == noteOffTime && it.velocity == 0)
                        )
                }
            } else {                                            // wrap-around
                notes.indexOfFirst {
                    it.pitch == pitch && it.id != noteId && (
                        (it.time > noteOnTime && it.time <= totalTime)
                            || (it.time >= 0 && it.time < noteOffTime)
                            || (it.time == noteOnTime && it.velocity > 0)
                            || (it.time == noteOffTime && it.velocity == 0)
                        )
                }
            }

            if (indexToErase == -1) break else {
                erasing(
                    isRepeating,
                    if (notes[indexToErase].velocity > 0) indexToErase else getNotePairedIndexAndTime(indexToErase).index,
                    false
                )
            }
        }
    }



/** MISC **/
    fun changeKeyboardOctave(lowKeyboard: Boolean, increment: Int) {
        val newValue = ((if(lowKeyboard) channelState.value.pianoViewOctaveLow else channelState.value.pianoViewOctaveHigh) + increment).coerceIn(-1..7)

        if(lowKeyboard) {
            _channelState.update { it.copy( pianoViewOctaveLow = newValue ) }
        } else {
            _channelState.update { it.copy( pianoViewOctaveHigh = newValue ) }
        }
    }

    fun getNotePairedIndexAndTime(index: Int): NoteIndexAndTime {
        if(notes.lastIndex < index) return NoteIndexAndTime(-1, Double.MIN_VALUE) // FIXME that was introduces because app was occasionally crashing when erasing/overdubbing notes. Now visuals glitch for a moment bc of note with only noteON (of noteOFF).
        val notePairedIndex = notes.indexOfFirst { it.id == notes[index].id && it.velocity != notes[index].velocity}
        return if(notePairedIndex > -1) NoteIndexAndTime(notePairedIndex, notes[notePairedIndex].time) else NoteIndexAndTime(-1, Double.MIN_VALUE)
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
        Log.d("ryjtyj", "channel $channelNumber: playingNotes[$pitch] = ${playingNotes[pitch]}, channelIsPlayingNotes = $channelIsPlayingNotes")

        if(playingNotes[pitch] < 0) {
            Log.d("ryjtyj", "playingNotes[$pitch] = ${playingNotes[pitch]}, fixing to 0")
            playingNotes[pitch] = 0
        }
        if(channelIsPlayingNotes < 0) {
            Log.d("ryjtyj", "channelIsPlayingNotes = $channelIsPlayingNotes, fixing to 0")
            channelIsPlayingNotes = 0
        }
    }

    private fun searchIndexInTimeBoundaries(): Int {
        var index =
            notes.indexOfFirst { it.time >= repeatStartTime && it.time < repeatEndTime } // normal search  [...]
        if (index == -1 && repeatStartTime > repeatEndTime) {
            index = notes.indexOfFirst { it.time >= repeatStartTime } // wrap-around  [..   (we don't search in 0..repeatEnd)
        }
        if (index == -1) {
            index = if(repeatStartTime > repeatEndTime) notes.size else indexToPlay // TODO check whether notes.size could be changed to indexToPlay
        }
        return index
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

    fun updateNotes() {
        _channelState.update { it.copy(notes = notes) }
//        if (channelNumber == 0) Log.d("ryjtyj", "updateNotes()")
    }

    fun updateStepView() {
        _channelState.update { it.copy(stepViewRefresh = !channelState.value.stepViewRefresh) }
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
}