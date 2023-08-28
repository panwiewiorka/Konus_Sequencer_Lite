package com.example.mysequencer01ui.ui

import android.util.Log
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import com.example.mysequencer01ui.KmmkComponentContext
import com.example.mysequencer01ui.Note
import com.example.mysequencer01ui.PadsMode
import com.example.mysequencer01ui.PadsMode.*
import com.example.mysequencer01ui.PressedNote
import com.example.mysequencer01ui.RememberedPressInteraction
import com.example.mysequencer01ui.SeqView
import com.example.mysequencer01ui.Sequence
import com.example.mysequencer01ui.StopNotesMode
import com.example.mysequencer01ui.StopNotesMode.*
import com.example.mysequencer01ui.data.Patterns
import com.example.mysequencer01ui.data.SeqDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.milliseconds

const val BARTIME = 2000

class SeqViewModel(private val kmmk: KmmkComponentContext, private val dao: SeqDao) : ViewModel() {

    private val _uiState = MutableStateFlow(SeqUiState(kmmk))
    val uiState: StateFlow<SeqUiState> = _uiState.asStateFlow()

    var toggleTime = 150
    private var anyChannelIsMuted = false
    private var anyChannelIsSoloed = false
    private var previousPadsMode: PadsMode = DEFAULT
    private var listOfMutedChannels = emptyList<Int>()
    private var listOfSoloedChannels = emptyList<Int>()
    private var previousDivisorValue = 0

    val interactionSources =
        Array(16) {
            Array(128) {
                RememberedPressInteraction(
                    MutableInteractionSource(),
                    PressInteraction.Press( Offset(0f,0f) )
                )
            }
        }

    private var patterns: Array<Array<Array<Note>>> = Array(16){ Array(16) { emptyArray() } }
    private var job = CoroutineScope(EmptyCoroutineContext).launch {  }

    init {
        CoroutineScope(Dispatchers.Default).launch {
            for(p in 0..15) {
                for (c in 0..15) {
                    val lastIndex = dao.getLastIndex(p, c) ?: -1
                    patterns[p][c] = Array(lastIndex + 1) { Note(0.0, 0, 0, 0) }
                    for (i in 0..lastIndex) {
                        val note = dao.loadNoteFromPattern(pattern = p, channel = c, index = i)
                        patterns[p][c][i].apply {
                            time = note.time
                            pitch = note.pitch
                            velocity = note.velocity
                            id = note.noteId
                        }
//                        Log.d("ryjtyj", "index = ${note.noteIndex}, id = ${note.noteId}")
//                        _uiState.value.sequences[c].recordNote(
//                            note.pitch,
//                            note.velocity,
//                            staticNoteOffTime,
//                            uiState.value.seqIsPlaying,
//                            uiState.value.isRepeating,
//                            uiState.value.repeatLength,
//                            note.time,
//                            true,
//                        )
                    }
                }
            }
            _uiState.update { a -> a.copy(
                sequences = uiState.value.sequences
            ) }
        }
    }


    fun startSeq() {
        if(
            uiState.value.seqIsPlaying
            // && !reStartMode
        ) {
            stopSeq()
            return
        }

        for(c in uiState.value.sequences.indices) {
            uiState.value.sequences[c].apply {
                startTimeStamp = System.currentTimeMillis()
                deltaTime = 0.0
                indexToPlay = 0
                indexToRepeat = 0
            }
        }
        _uiState.update { a -> a.copy( sequences = uiState.value.sequences ) }

        if (uiState.value.seqIsPlaying) return // needed for future implementing of reStart()


        _uiState.update { a -> a.copy( seqIsPlaying = true ) }
        var clockTicks = 0
        if (uiState.value.transmitClock) kmmk.startClock()

        // ------====== MAIN CYCLE
        CoroutineScope(Dispatchers.Default).launch {
            while (uiState.value.seqIsPlaying) {
                for (c in 0..15) {
                    with (uiState.value.sequences[c]) {

                        // UPDATE DELTATIME
                        deltaTime = (System.currentTimeMillis() - startTimeStamp) * uiState.value.factorBpm + bpmDelta

                        // NORMAL ERASING or PLAYING
                        while (notes.size > indexToPlay && notes[indexToPlay].time <= deltaTime) {
                            val overdubbing = uiState.value.seqIsRecording && pressedNotes[notes[indexToPlay].pitch].isPressed
                            val noteShouldBeTiedToItself = System.currentTimeMillis() - pressedNotes[notes[indexToPlay].pitch].noteOnTimestamp > uiState.value.quantizationTime / uiState.value.factorBpm * 1.5
                            if (!uiState.value.isRepeating && (isErasing || overdubbing)) {
                                if (overdubbing && pressedNotes[notes[indexToPlay].pitch].id == notes[indexToPlay].id && noteShouldBeTiedToItself) {
                                    pressedNotes[notes[indexToPlay].pitch].isPressed = false
                                    recIntoArray(
                                        index = indexToPlay,
                                        recordTime = notes[indexToPlay].time,
                                        pitch = notes[indexToPlay].pitch,
                                        velocity = 0,
                                        id = pressedNotes[notes[indexToPlay].pitch].id
                                    )
                                    cancelPadInteraction()
                                    updateSequencesUiState()
                                    indexToPlay++
                                } else if (!erasing(false, indexToPlay, false, ::updateSequencesUiState, ::updatePadsUiState)) break
                            } else {
                                if(!uiState.value.isRepeating && indexToPlay != draggedNoteOffIndex) {
                                    playing(
                                        indexToPlay,
                                        !uiState.value.soloIsOn,
                                        uiState.value.seqView == SeqView.STEP,
                                        ::updatePadsUiState
                                    )
                                    // StepView case:
                                    if (indexToPlay == draggedNoteOnIndex) {
                                        playingDraggedNoteOffInTime(uiState.value.factorBpm, ::updatePadsUiState)
                                    }
                                }
                                indexToPlay++
                            }
                        }

                        /** IF REPEATING **/
                        if (uiState.value.isRepeating) {

                            // update deltaTimeRepeat
                            if (deltaTime < previousDeltaTime) {
                                previousDeltaTime -= totalTime
                            }
                            deltaTimeRepeat += (deltaTime - previousDeltaTime)
//                            if(c == 0) Log.d("ryjtyj", "UPDATE: deltaTimeRepeat = $deltaTimeRepeat, deltaTime = $deltaTime")
                            previousDeltaTime = deltaTime

                            // erasing or playing
                            while(
                                // make sure element exists in array
                                notes.size > indexToRepeat
                                // time to play note
                                && (notes[indexToRepeat].time in repeatStartTime .. deltaTimeRepeat
                                    || (repeatStartTime > repeatEndTime && (notes[indexToRepeat].time in 0.0 .. deltaTimeRepeat)))
                                // note is in repeat bounds
                                && (notes[indexToRepeat].time in repeatStartTime .. repeatEndTime     // [...]
                                    || (repeatStartTime > repeatEndTime && (notes[indexToRepeat].time >= repeatStartTime || notes[indexToRepeat].time <= repeatEndTime))) // ..] && [..
                                // no noteON present in repeatEndTime
                                && !(notes[indexToRepeat].time == repeatEndTime && notes[indexToRepeat].velocity > 0)
                            ) {
                                val overdubbing = uiState.value.seqIsRecording && pressedNotes[notes[indexToRepeat].pitch].isPressed
                                if (isErasing || overdubbing) {
                                    // tie note
                                    if (overdubbing && (getNotePairedIndexAndTime(indexToRepeat).time == repeatEndTime
                                        && notes[notes.indexOfLast { it.id == pressedNotes[notes[indexToRepeat].pitch].id }].time == repeatStartTime )) {
                                        pressedNotes[notes[indexToRepeat].pitch].isPressed = false
                                        recIntoArray(
                                            index = indexToRepeat,
                                            recordTime = notes[indexToRepeat].time,
                                            pitch = notes[indexToRepeat].pitch,
                                            velocity = 0,
                                            id = pressedNotes[notes[indexToRepeat].pitch].id
                                        )
                                        updateSequencesUiState()
                                        cancelPadInteraction()
                                        indexToRepeat++
                                        Log.d("ryjtyj", "tied note ${notes[indexToRepeat].pitch}")
                                    } else
                                        if (!erasing(true, indexToRepeat, false, ::updateSequencesUiState, ::updatePadsUiState)) break
                                } else {
                                    if (indexToRepeat != draggedNoteOffIndex) {
                                        playing(
                                            indexToRepeat,
                                            !uiState.value.soloIsOn,
                                            uiState.value.seqView == SeqView.STEP,
                                            ::updatePadsUiState
                                        )
                                        // StepView case:
                                        if (indexToRepeat == draggedNoteOnIndex) {
                                            playingDraggedNoteOffInTime(uiState.value.factorBpm, ::updatePadsUiState)
                                        }
                                    }
                                    indexToRepeat++
                                }
                            }

                            // end of repeating loop
                            if ((repeatStartTime < repeatEndTime && deltaTimeRepeat > repeatEndTime) || deltaTimeRepeat in repeatEndTime .. repeatStartTime) {
                                uiState.value.stopNotesOnChannel(c, END_OF_REPEAT)   // notesOFF on the end of loop // TODO activate only if channelIsPlayingNotes?

                                indexToStartRepeating = searchIndexInTimeBoundaries()
                                indexToRepeat = indexToStartRepeating

                                deltaTimeRepeat -= (repeatEndTime - repeatStartTime)
//                                if(c == 0) Log.d("ryjtyj", "end of REPEAT (> repeatEnd): deltaTimeRepeat = $deltaTimeRepeat, deltaTime = $deltaTime")
                                idOfNotesToIgnore.clear()

                                for (p in 0..127) { // TODO change pressedNotes[] to mutableList?
                                    // ignore noteOff if we start repeatLoop in the middle of the note
                                    var hangingNoteOffIndex = notes.indexOfFirst {
                                        it.pitch == p && (it.time in repeatStartTime .. repeatEndTime || (repeatStartTime > repeatEndTime && it.time >= repeatStartTime))
                                    }
                                    if (hangingNoteOffIndex == -1 && repeatStartTime > repeatEndTime) hangingNoteOffIndex = notes.indexOfFirst {
                                        it.pitch == p && it.time in 0.0 .. repeatEndTime
                                    }
                                    if (hangingNoteOffIndex != -1 && notes[hangingNoteOffIndex].velocity == 0
                                        && (getNotePairedIndexAndTime(hangingNoteOffIndex).time !in repeatStartTime .. deltaTimeRepeat
                                            || (repeatStartTime > repeatEndTime && getNotePairedIndexAndTime(hangingNoteOffIndex).time in 0.0 .. deltaTimeRepeat))
                                    ) {
                                        idOfNotesToIgnore.add(notes[hangingNoteOffIndex].id)
                                        Log.d("ryjtyj", "hanging NoteOff INDEX on the start of the loop = $hangingNoteOffIndex")
                                    }
                                    // notesON on the start of the loop if notes are being held
                                    if (pressedNotes[p].isPressed) {
                                        kmmk.noteOn(c, p, 100)
                                        Log.d("ryjtyj", "noteON on the start of the loop (note $p is being pressed")
                                        if (uiState.value.seqIsRecording) recordNote(
                                            pitch = p,
                                            velocity = 100,
                                            id = pressedNotes[p].id,
                                            quantizationTime = uiState.value.quantizationTime,
                                            factorBpm = uiState.value.factorBpm,
                                            seqIsPlaying = true,
                                            isRepeating = true,
                                            quantizeTime = ::quantizeTime,
                                            isStepView = uiState.value.seqView == SeqView.STEP,
                                            isStepRecord = false,
                                            customTime = repeatStartTime
                                        )
                                        changePlayingNotes(p, 100, ::updatePadsUiState)
                                        increaseNoteId()
                                    }
                                }
                                updateSequencesUiState()
                            }
                            if (deltaTimeRepeat > totalTime) {
                                deltaTimeRepeat -= totalTime
//                                if(c == 0) Log.d("ryjtyj", "end of CYCLE (> totalTime): deltaTimeRepeat = $deltaTimeRepeat, deltaTime = $deltaTime")
                                indexToRepeat = 0
                                updateSequencesUiState()
                            }
                        }

                        // END OF SEQUENCE (non-repeating)
                        if (deltaTime >= totalTime) {
                            startTimeStamp = System.currentTimeMillis() - (deltaTime - totalTime).toLong()
                            bpmDelta = 0.0
                            indexToPlay = 0
                            idOfNotesToIgnore.clear()
                            if(c == 0 && uiState.value.transmitClock) clockTicks = 0 // TODO move lower vv to CLOCK
                            updateSequencesUiState()
                        }

                        // RECORD NOTES (if any)
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
                                val dt = if(uiState.value.isRepeating) deltaTimeRepeat else deltaTime
                                if (velocity > 0 && uiState.value.seqIsPlaying && recordTime > dt && !isStepRecord) {
                                    idOfNotesToIgnore.add(id)
                                }
                            }
                            listOfNotesToRecord.removeAt(0)
                            updateSequencesUiState()
                        }
                    }
                }

                // CLOCK
                if (uiState.value.transmitClock) {
                    if (uiState.value.sequences[0].deltaTime >= uiState.value.timingClock * clockTicks) {
                        kmmk.sendTimingClock()
                        clockTicks++
                    }
                }

                // ENGAGE REPEAT()
                if (uiState.value.divisorState != previousDivisorValue && quantizeTime(uiState.value.sequences[0].deltaTime) <= uiState.value.sequences[0].deltaTime) { // FIXME repeatOFF quantizes only "forward"
//                    Log.d("ryjtyj", "ENGAGE REPEAT(): deltaTimeRepeat = ${uiState.value.sequences[0].deltaTimeRepeat}, deltaTime = ${uiState.value.sequences[0].deltaTime}, quantizeTime(deltaTime) = ${quantizeTime(uiState.value.sequences[0].deltaTime)}")
                    repeat(uiState.value.divisorState)
                    updateSequencesUiState()
                }

                _uiState.update { it.copy(
                    visualArrayRefresh = !uiState.value.visualArrayRefresh,
//                    sequences = uiState.value.sequences
                ) }

                delay(3.milliseconds)
            }
        }
    }


    fun stopSeq() {
        _uiState.update { it.copy(seqIsPlaying = false) }
        stopChannels(STOP_SEQ)
        kmmk.stopClock()
        updateSequencesUiState()
    }

    private fun stopChannels(mode: StopNotesMode) {
        for (c in 0..15) {
            with(uiState.value.sequences[c]) {
                uiState.value.stopNotesOnChannel(c, mode)
                idOfNotesToIgnore.clear()
                notesDragEndOnRepeat = Array(128){ false }
                if(mode == STOP_SEQ) {
                    deltaTime = 0.0
                    bpmDelta = 0.0
                    deltaTimeRepeat = 0.0
                    if(draggedNoteOffJob.isActive) {
                        draggedNoteOffJob.cancel()
                        Log.d("ryjtyj", "stopChannel $c: draggedNoteOffJob.cancel()")
                        kmmk.noteOn(c, pitchTempSavedForCancel, 0)
                        changePlayingNotes(pitchTempSavedForCancel, 0, ::updatePadsUiState)
                    }
                }
            }
        }
    }

    private fun SeqUiState.stopNotesOnChannel(c: Int, mode: StopNotesMode) {
        for (p in 0..127) {
            while (sequences[c].playingNotes[p] > 0) {
                kmmk.noteOn(c, p, 0)
                Log.d("ryjtyj", "stopNotesOnChannel(): Stopping note on channel $c, pitch = $p")
                sequences[c].changePlayingNotes(p, 0, ::updatePadsUiState)
            }
            if (seqIsRecording && sequences[c].pressedNotes[p].isPressed) {
                sequences[c].recordNote(
                    pitch = p,
                    velocity = 0,
                    id = sequences[c].pressedNotes[p].id,
                    quantizationTime = quantizationTime,
                    factorBpm = uiState.value.factorBpm,
                    seqIsPlaying = seqIsPlaying,
                    isRepeating = isRepeating,
                    quantizeTime = ::quantizeTime,
                    isStepView = uiState.value.seqView == SeqView.STEP,
                    isStepRecord = false,
                    customTime = if (mode == END_OF_REPEAT) sequences[c].repeatEndTime else -1.0
                )
                if (mode == END_OF_REPEAT) {
                    sequences[c].pressedNotes[p] = PressedNote(true, sequences[c].noteId, System.currentTimeMillis()) // updating noteID for new note in beatRepeat (wrapAround case)
                }
            }
        }
        _uiState.update { it.copy(
            sequences = uiState.value.sequences,
            padsState = uiState.value.padsState
        ) }
    }

    fun stopAllNotes() {
        for (c in 0..15) {
            for (p in 0..127) {
                kmmk.noteOn(c, p, 0)
            }
        }
        updateSequencesUiState()
    }


    fun changeRecState() {
        if(uiState.value.seqIsRecording) {
            with(uiState.value) {
                for (c in 0..15) {
                    for (p in 0..127) {
                        if (sequences[c].pressedNotes[p].isPressed) {
                            kmmk.noteOn(c, p, 0)
                            sequences[c].recordNote(
                                pitch = p,
                                velocity = 0,
                                id = sequences[c].pressedNotes[p].id,
                                quantizationTime = quantizationTime,
                                factorBpm = uiState.value.factorBpm,
                                seqIsPlaying = seqIsPlaying,
                                isRepeating = isRepeating,
                                quantizeTime = ::quantizeTime,
                                isStepView = uiState.value.seqView == SeqView.STEP,
                                isStepRecord = false,
                            )
                            sequences[c].pressedNotes[p] = PressedNote(false, Int.MAX_VALUE, Long.MIN_VALUE)
                        }
                    }
                }
            }
        }
        _uiState.update { a -> a.copy( seqIsRecording = !uiState.value.seqIsRecording ) }
    }


    private fun soloChannel(channel: Int, velocity: Int, elapsedTime: Long, allButton: Boolean) {
        with(uiState.value.sequences[channel]) {
            val pressOrLongRelease = velocity > 0 || (System.currentTimeMillis() - elapsedTime) > toggleTime

            if(pressOrLongRelease) {
                if(allButton) {
                    if(velocity > 0) {
                        if(channel == 0) {
                            listOfSoloedChannels = uiState.value.sequences.filter { it.isSoloed }.map { it.channel }
                            anyChannelIsSoloed = listOfSoloedChannels.isNotEmpty()
                        }
                        isSoloed = !anyChannelIsSoloed
                    } else {
                        if(!anyChannelIsSoloed) {
                            isSoloed = false
                        } else if(listOfSoloedChannels.contains(channel)) {
                            isSoloed = true
                        }
                    }
                    if(channel == 15) _uiState.update { it.copy(soloIsOn = uiState.value.sequences.any{ sequence -> sequence.isSoloed }) }
                } else {   // single Pad
                    isSoloed = !isSoloed
                    if(isSoloed) {
                        _uiState.update { it.copy(soloIsOn = true) }
                        for (c in 0..15) {
                            if (!uiState.value.sequences[c].isSoloed) uiState.value.stopNotesOnChannel(c, STOP_NOTES)
                        }
                    } else _uiState.update { it.copy(soloIsOn = uiState.value.sequences.any{ sequence -> sequence.isSoloed }) }
                }
            }
        }
    }


    private fun muteChannel(channel: Int, velocity: Int, elapsedTime: Long, allButton: Boolean) {
        with(uiState.value.sequences[channel]) {
            val pressOrLongRelease = velocity > 0 || (System.currentTimeMillis() - elapsedTime) > toggleTime

            if(pressOrLongRelease) {
                if(allButton) {
                    if(velocity > 0) {
                        if(channel == 0) {
                            listOfMutedChannels = uiState.value.sequences.filter { it.isMuted }.map { it.channel }
                            anyChannelIsMuted = listOfMutedChannels.isNotEmpty()
                        }
                        isMuted = !anyChannelIsMuted
                    } else {
                        if(!anyChannelIsMuted) {
                            isMuted = false
                        } else if(listOfMutedChannels.contains(channel)) {
                            isMuted = true
                        }
                    }
                    if(channel == 15) _uiState.update { it.copy(muteIsOn = uiState.value.sequences.any{ sequence -> sequence.isMuted }) }
                } else {   // single Pad
                    isMuted = !isMuted
                    if (isMuted) {
                        _uiState.update { it.copy(muteIsOn = true) }
                        uiState.value.stopNotesOnChannel(channel, STOP_NOTES) // TODO when allButton?
                    } else _uiState.update { it.copy(muteIsOn = uiState.value.sequences.any{ sequence -> sequence.isMuted }) }
                }
            }
        }
    }


    private fun enableEraseOnChannel(channel: Int, velocity: Int) {
        uiState.value.sequences[channel].isErasing = velocity > 0
    }


    /** REPEAT */
    private fun repeat(divisor: Int) {
        if (divisor == 0) {
            stopChannels(STOP_NOTES)
        } else {
            val repeatLength = when(divisor) {
                2 -> 8.0
                3 -> 6.0
                4 -> 4.0
                6 -> 3.0
                8 -> 2.0
                12 -> 1.5
                16 -> 1.0
                24 -> 0.75
                else -> 0.5
            } * 125
            for(c in 0..15) {
                with(uiState.value.sequences[c]) {
                    if (!uiState.value.isRepeating) {
                        previousDeltaTime = deltaTime
                        deltaTimeRepeat = deltaTime
                        if (deltaTimeRepeat >= totalTime) deltaTimeRepeat -= totalTime
                        if(c == 0) Log.d("ryjtyj", "INIT repeat: deltaTimeRepeat = $deltaTimeRepeat, deltaTime = $deltaTime")
                        repeatStartTime = quantizeTime(deltaTimeRepeat)
//                        if (repeatStartTime == totalTime.toDouble()) repeatStartTime = 0.0
                    }
                    repeatEndTime = repeatStartTime + repeatLength
                    if(repeatEndTime > totalTime) repeatEndTime -= totalTime

                    if (!uiState.value.isRepeating) {
                        indexToStartRepeating = searchIndexInTimeBoundaries()
                        indexToRepeat = indexToPlay
                    } else {
                        uiState.value.stopNotesOnChannel(c, STOP_NOTES)

                        val deltaTimeRepeatNotInBounds =
                            (repeatStartTime < repeatEndTime && deltaTimeRepeat !in repeatStartTime .. repeatEndTime)
                                || (deltaTimeRepeat > repeatEndTime && deltaTimeRepeat < repeatStartTime)

                        if (deltaTimeRepeatNotInBounds) {
                            var multiplier = 1
                            while (repeatEndTime + repeatLength * multiplier < deltaTimeRepeat) {
                                multiplier++
                            }
                            deltaTimeRepeat -= repeatLength * multiplier
                            if (deltaTimeRepeat < 0) deltaTimeRepeat += totalTime
                            if(c == 0) Log.d("ryjtyj", "not_init / change MULTIPLIER: deltaTimeRepeat = $deltaTimeRepeat, deltaTime = $deltaTime")
                            indexToRepeat = notes.indexOfLast { it.time <= deltaTimeRepeat } + 1
                        }
                    }
                    if(c == 0) Log.d("ryjtyj", "indexToStartRepeating = $indexToStartRepeating, repeatStartTime = $repeatStartTime, repeatEndTime = $repeatEndTime")
                }
            }
            _uiState.update { it.copy( repeatLength = repeatLength ) }
        }
        for(c in 0..15) uiState.value.sequences[c].idOfNotesToIgnore.clear()
        _uiState.update { it.copy(
            isRepeating = divisor > 0,
            sequences = uiState.value.sequences
        ) }
        previousDivisorValue = divisor
    }

    fun changeRepeatDivisor(divisor: Int) {
        _uiState.update { it.copy(divisorState = divisor) }
        if (!uiState.value.seqIsPlaying) repeat(divisor)
    }


    /** PRESS PAD */
    fun pressPad(channel: Int, pitch: Int, velocity: Int, elapsedTime: Long = 0, allButton: Boolean = false) {
        with(uiState.value.sequences[channel]) {
            val padsModeOnPress: PadsMode
            if(velocity > 0) {
                padsModeOnPress = uiState.value.padsMode
                onPressedMode = uiState.value.padsMode
            } else padsModeOnPress = onPressedMode

            when(padsModeOnPress) {
                SELECTING -> {
                    if(!allButton && velocity > 0) {
                        _uiState.update { a -> a.copy(selectedChannel = channel) }
                    }
                }
                QUANTIZING -> {
                    if(velocity > 0) {
                        val tempIsQuantizing = uiState.value.isQuantizing
                        _uiState.update { a -> a.copy(isQuantizing = true) }
                        quantizeChannel(channel)
                        _uiState.update { a -> a.copy(isQuantizing = tempIsQuantizing) }
                    }
                }
                SAVING -> {
                    if(velocity > 0) savePattern(pattern = channel, sequences = uiState.value.sequences)
                }
                LOADING -> {
                    if(!allButton && velocity > 0) {
                        for(c in 0..15) {
                            uiState.value.sequences[c].clearChannel(c, ::updatePadsUiState)
                        }
                        loadPattern(pattern = channel)
                    }
                }
                SOLOING -> soloChannel(channel, velocity, elapsedTime, allButton)
                MUTING -> muteChannel(channel, velocity, elapsedTime, allButton)
                ERASING -> enableEraseOnChannel(channel, velocity)
                CLEARING -> {
                    if(velocity > 0) clearChannel(channel, ::updatePadsUiState) // TODO implement UNDO CLEAR
                }
                // PLAYING & RECORDING
                else -> {
                    val retrigger = playingNotes[pitch] > 0 && velocity > 0
                    if (retrigger) {
                        kmmk.noteOn(channel, pitch, 0)    // allows to retrigger already playing notes
                    }
                    kmmk.noteOn(channel, pitch, velocity)
                    pressedNotes[pitch] = if (velocity > 0) {
                        PressedNote(true, noteId, System.currentTimeMillis())
                    } else {
                        PressedNote(false, pressedNotes[pitch].id, pressedNotes[pitch].noteOnTimestamp)
                    }

                    if (uiState.value.seqIsRecording) {
                        if(velocity > 0) {
                            recordNote(
                                pitch = pitch,
                                velocity = velocity,
                                id = noteId,
                                quantizationTime = uiState.value.quantizationTime,
                                factorBpm = uiState.value.factorBpm,
                                seqIsPlaying = uiState.value.seqIsPlaying,
                                isRepeating = uiState.value.isRepeating,
                                quantizeTime = ::quantizeTime,
                                isStepView = uiState.value.seqView == SeqView.STEP,
                                isStepRecord = false,
                            )
                            increaseNoteId()
                        } else {
                            if(!notesDragEndOnRepeat[pitch]) {
                                recordNote(
                                    pitch = pitch,
                                    velocity = 0,
                                    id = pressedNotes[pitch].id,
                                    quantizationTime = uiState.value.quantizationTime,
                                    factorBpm = uiState.value.factorBpm,
                                    seqIsPlaying = uiState.value.seqIsPlaying,
                                    isRepeating = uiState.value.isRepeating,
                                    quantizeTime = ::quantizeTime,
                                    isStepView = uiState.value.seqView == SeqView.STEP,
                                    isStepRecord = false,
                                )
                            } else {
                                notesDragEndOnRepeat[pitch] = false
                            }
                        }
                    }
                    if(!retrigger) changePlayingNotes(pitch, velocity, ::updatePadsUiState)
                    _uiState.update { it.copy(selectedChannel = channel) }
                }
            }
        }.also { if(!allButton) updateSequencesUiState() } // allButton recomposes by itself // TODO is it still needed here?
    }


    private fun savePattern(pattern: Int, sequences: MutableList<Sequence>) {
        CoroutineScope(Dispatchers.Default).launch {
            dao.deletePattern(pattern)
            for(c in sequences.indices) {
                patterns[pattern][c] = sequences[c].notes
                for(i in sequences[c].notes.indices) {
                    dao.saveNoteToPattern(
                        Patterns(
                            pattern = pattern,
                            channel = c,
                            noteIndex = i,
                            time = sequences[c].notes[i].time,
                            pitch = sequences[c].notes[i].pitch,
                            velocity = sequences[c].notes[i].velocity,
                            noteId = sequences[c].notes[i].id,
//                            length = sequences[c].notes[i].length
                        )
                    )
                }
            }
        }
    }

    private fun loadPattern(pattern: Int) {
        CoroutineScope(Dispatchers.Default).launch {
            for(c in 0..15) {
//                val lastIndex = patterns[pattern][c].lastIndex
//                for(i in 0 ..lastIndex) {
//                    val note = patterns[pattern][c][i]
//                    _uiState.value.sequences[c].recordNote(
//                        note.pitch,
//                        note.velocity,
//                        staticNoteOffTime,
//                        uiState.value.seqIsPlaying,
//                        uiState.value.isRepeating,
//                        uiState.value.repeatLength,
//                        note.time,
//                        note.noteId,
//                        true,
//                    )
//                }
                with(_uiState.value.sequences[c]) {
                    idOfNotesToIgnore.clear()
                    notes = patterns[pattern][c]
                    indexToPlay = notes.indexOfLast { it.time < deltaTime } + 1
                    if(uiState.value.isRepeating) {
                        indexToRepeat = notes.indexOfLast { it.time < deltaTimeRepeat } + 1
                    }
                    noteId = if(notes.isNotEmpty()) notes.maxOf { it.id } + 1 else Int.MIN_VALUE
                }
            }
            _uiState.update { a -> a.copy(
                sequences = uiState.value.sequences
            ) }
        }
    }


    fun quantizeTime(time: Double): Double {
        return if(uiState.value.isQuantizing) {
            val remainder = (time + uiState.value.quantizationTime / 2) % uiState.value.quantizationTime
            time + uiState.value.quantizationTime / 2 - remainder
        } else time
    }

    private fun quantizeChannel(channel: Int) {
        with(uiState.value.sequences[channel]) {
            for(i in notes.indices) {
                if(notes[i].velocity > 0) {
                    val tempTime = notes[i].time
                    val time = quantizeTime(tempTime)
                    changeNoteTime(i, time)
                    changeNoteTime(getNotePairedIndexAndTime(i).index, time - tempTime, true)
                    sortNotesByTime()
                }
            }
        }
    }

    fun switchQuantization() {
        _uiState.update { it.copy(
            isQuantizing = !uiState.value.isQuantizing
        ) }
    }

    fun switchPadsToQuantizingMode(switchOn: Boolean) {
        if(switchOn) {
            if(uiState.value.seqView != SeqView.LIVE) {
                job = CoroutineScope(Dispatchers.Default).launch {
                    val time = System.currentTimeMillis()
                    while (uiState.value.quantizeModeTimer < toggleTime) {
                        delay(5)
                        _uiState.update { it.copy(quantizeModeTimer = uiState.value.quantizeModeTimer + (System.currentTimeMillis() - time).toInt()) }
                    }
                    _uiState.update { it.copy(quantizeModeTimer = 0) }
                    editCurrentPadsMode(QUANTIZING, true)
                }
            } else editCurrentPadsMode(QUANTIZING, true)
        } else {
            if(job.isActive) job.cancel()
            if(uiState.value.quantizeModeTimer == 0) {
                editCurrentPadsMode(QUANTIZING, false)
            }
            _uiState.update { it.copy(quantizeModeTimer = 0) }
        }
    }


    fun editCurrentPadsMode(mode: PadsMode, switchOn: Boolean, momentary: Boolean = false){ // TODO simplify with when?
        if(switchOn) {
            if(mode != uiState.value.padsMode) {
                previousPadsMode = uiState.value.padsMode
                _uiState.update { it.copy(padsMode = mode) }
            } else _uiState.update { it.copy(padsMode = DEFAULT) }
        } else if(momentary) {
            if(previousPadsMode == mode) {
                previousPadsMode = uiState.value.padsMode
            }
            if(mode != uiState.value.padsMode) return else {
                _uiState.update { it.copy(padsMode = if(previousPadsMode != uiState.value.padsMode && previousPadsMode != mode) previousPadsMode else DEFAULT) }
            }
        } else {
            _uiState.update { it.copy(padsMode = DEFAULT) }
        }
    }

    fun changeSeqViewState(seqView: SeqView) {
        _uiState.update { it.copy(
            seqView = seqView
        ) }
    }

    fun updateSequencesUiState() {
        _uiState.update { it.copy(
            sequences = uiState.value.sequences, // FIXME this doesn't recompose (at least when seq is stopped)
            visualArrayRefresh = !uiState.value.visualArrayRefresh // that's why it's here TODO (get rid of)
        ) }
//        Log.d("ryjtyj", "updateSequencesUiState()")
    }

    fun updatePadsUiState(gg: Pair<Int, Boolean> = Pair(0, uiState.value.padsState[0])) {
        val mm = uiState.value.padsState.toMutableList()
        mm[gg.first] = gg.second
//        Log.d("ryjtyj", "GGGGGGGGGGG ${mm[0]}, ${mm.toTypedArray().contentEquals(uiState.value.padsState)}")

        _uiState.update { a -> a.copy(
            padsState = mm.toTypedArray(),
        ) }
    }

    fun changeStepViewNoteHeight(noteHeight: Dp) {
        _uiState.update { a -> a.copy( stepViewNoteHeight = noteHeight ) }
    }

    fun changeBPM(bpm: Float) {
        val bpmPointOne = (bpm * 10 + 0.5).toInt() / 10f

        if(uiState.value.seqIsPlaying) {
            for(c in 0..15) {
                with(uiState.value.sequences[c]) {
                    bpmDelta += (System.currentTimeMillis() - startTimeStamp) * (uiState.value.factorBpm - bpmPointOne / 120.0)
                }
            }
        }

        _uiState.update { a -> a.copy (
            bpm = bpmPointOne,
            factorBpm = bpmPointOne / 120.0,
            timingClock = 500.0 / 24.0 //* bpmPointOne / 120.0,
        ) }

        Log.d("emptyTag", " ") // to hold in imports
    }


    fun rememberInteraction(channel: Int, pitch: Int, interaction: PressInteraction.Press) {
        interactionSources[channel][pitch].pressInteraction = interaction
    }

    fun cancelPadInteraction() {
//        CoroutineScope(Dispatchers.Main).launch {
        runBlocking {
            for(c in 0..15) {
                for (p in 0..127) {
                    if(uiState.value.sequences[c].pressedNotes[p].isPressed) {
                        interactionSources[c][p].interactionSource.emit (PressInteraction.Cancel (interactionSources[c][p].pressInteraction))
                    }
                }
            }
        }
    }

    fun switchClockTransmitting() {
        _uiState.update { it.copy(
            transmitClock = !uiState.value.transmitClock
        ) }
    }

    fun switchVisualDebugger() {
        _uiState.update { it.copy(
            visualDebugger = !uiState.value.visualDebugger
        ) }
    }

    fun selectDebuggerSetting(setting: Int) {
        _uiState.update { it.copy(
            debuggerViewSetting = setting
        ) }
    }

}