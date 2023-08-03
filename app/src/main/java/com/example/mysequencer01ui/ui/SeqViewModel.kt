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
import com.example.mysequencer01ui.PadsMode.CLEARING
import com.example.mysequencer01ui.PadsMode.DEFAULT
import com.example.mysequencer01ui.PadsMode.ERASING
import com.example.mysequencer01ui.PadsMode.LOADING
import com.example.mysequencer01ui.PadsMode.MUTING
import com.example.mysequencer01ui.PadsMode.QUANTIZING
import com.example.mysequencer01ui.PadsMode.SAVING
import com.example.mysequencer01ui.PadsMode.SELECTING
import com.example.mysequencer01ui.PadsMode.SOLOING
import com.example.mysequencer01ui.PressedNote
import com.example.mysequencer01ui.RememberedPressInteraction
import com.example.mysequencer01ui.SeqView
import com.example.mysequencer01ui.Sequence
import com.example.mysequencer01ui.StopNotesMode
import com.example.mysequencer01ui.StopNotesMode.END_OF_REPEAT
import com.example.mysequencer01ui.StopNotesMode.MUTE
import com.example.mysequencer01ui.StopNotesMode.STOPSEQ
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


class SeqViewModel(private val kmmk: KmmkComponentContext, private val dao: SeqDao) : ViewModel() {

    private val _uiState = MutableStateFlow(SeqUiState(kmmk))
    val uiState: StateFlow<SeqUiState> = _uiState.asStateFlow()

    var toggleTime = 150
    var staticNoteOffTime = 100 // TODO replace with quantization time?
    var quantizationTime = 240000 / uiState.value.bpm / uiState.value.quantizationValue * uiState.value.factorBpm
    private var anyChannelIsMuted = false
    private var anyChannelIsSoloed = false
    private var previousPadsMode: PadsMode = DEFAULT
    private var listOfMutedChannels = emptyList<Int>()
    private var listOfSoloedChannels = emptyList<Int>()

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
        CoroutineScope(Dispatchers.Main).launch {
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
//                deltaTimeRepeat = 0.0
                indexToRepeat = 0
                tempNotesSize = 0
            }
        }
        _uiState.update { a -> a.copy( sequences = uiState.value.sequences ) }

        if (uiState.value.seqIsPlaying) return // needed for future implementing of reStart()


        _uiState.update { a -> a.copy( seqIsPlaying = true ) }
        var clockTicks = 0
        kmmk.startClock()

        // ------====== MAIN CYCLE
        CoroutineScope(Dispatchers.Main).launch {
            while (uiState.value.seqIsPlaying) {
                for (c in 0..15) {
                    with (uiState.value.sequences[c]) {

                        // UPDATE DELTATIME
                        deltaTime = (System.currentTimeMillis() - startTimeStamp) * uiState.value.factorBpm + bpmDelta

                        // CLOCK
                        if (c == 0 && uiState.value.transmitClock) {
                            if (deltaTime >= uiState.value.timingClock * clockTicks) {
                                kmmk.sendTimingClock()
                                clockTicks++
                            }
                        }

                        // ENGAGE REPEAT()
                        if (uiState.value.divisorState > 0 != uiState.value.isRepeating && quantizeTime(deltaTime) <= deltaTime) {
                            repeat(uiState.value.divisorState)
                        }

                        // NORMAL ERASING or PLAYING
                        while (notes.size > indexToPlay && notes[indexToPlay].time <= deltaTime) {
                            if (!uiState.value.isRepeating && (isErasing || (uiState.value.seqIsRecording && pressedNotes[notes[indexToPlay].pitch].isPressed))) {
                                if (!erasing(false, indexToPlay, false)) break
                            } else {
                                if(!uiState.value.isRepeating && indexToPlay != draggedNoteOffIndex) {
                                    playing(
                                        indexToPlay,
                                        !uiState.value.soloIsOn,
                                        uiState.value.seqView == SeqView.STEP
                                    )
                                    // StepView case:
                                    if (indexToPlay == draggedNoteOnIndex) {
                                        playingDraggedNoteOffInTime(uiState.value.factorBpm)
                                    }
                                }
                                indexToPlay++
                            }
                        }

                        // END OF SEQUENCE
                        if (deltaTime >= totalTime) {
                            startTimeStamp = System.currentTimeMillis() - (deltaTime - totalTime).toLong()
                            bpmDelta = 0.0
                            indexToPlay = 0
                            if(c == 0 && uiState.value.transmitClock) clockTicks = 0
                        }

                        /** IF REPEATING **/
                        if (uiState.value.isRepeating) {

                            // update deltaTimeRepeat
                            if(deltaTime < previousDeltaTime) {
                                previousDeltaTime -= totalTime
                            }
                            deltaTimeRepeat = deltaTimeRepeat + deltaTime - previousDeltaTime
                            previousDeltaTime = deltaTime

                            // end of repeating loop
                            if ((repeatEndTime > repeatStartTime && deltaTimeRepeat > repeatEndTime) || deltaTimeRepeat in repeatEndTime..repeatStartTime) {
                                uiState.value.stopNotesOnChannel(c, END_OF_REPEAT)   // notesOFF on the end of loop // TODO activate only if channelIsPlayingNotes?

                                indexToStartRepeating = searchIndexInTimeBoundaries()
                                indexToRepeat = indexToStartRepeating

                                repeatsCount++ // TODO change to Boolean?

                                deltaTimeRepeat = deltaTimeRepeat - repeatEndTime + repeatStartTime
                                if(c == 0) Log.d("ryjtyj", "deltaTimeRepeat = $deltaTimeRepeat")
                            }

//                            if(deltaTimeRepeat + wrapDelta < 0) {
//                                wrapTime = totalTime
//                                wrapIndex = 0
//                                tempNotesSize = notes.size
//                            } else {
//                                wrapTime = 0
//                                wrapIndex = if(repeatStartTime > repeatEndTime) tempNotesSize else 0
//                            }

                            if (deltaTimeRepeat > totalTime) deltaTimeRepeat -= totalTime
//                            if (deltaTimeRepeat < repeatStartTime)
                            wrapIndex = if(deltaTimeRepeat < repeatStartTime) tempNotesSize else 0

                            fullDeltaTimeRepeat = deltaTimeRepeat

                            // start of the loop
                            if (repeatsCount != savedRepeatsCount) {
//                                if(c == 0)Log.d("ryjtyj", "start of the loop")
                                for (p in 0..127) {
                                    // notesON if we start repeatLoop in the middle of the note
                                    val hangingNoteOffIndex = notes.indexOfFirst { it.pitch == p && it.time > fullDeltaTimeRepeat }
                                    if (hangingNoteOffIndex != -1 && notes[hangingNoteOffIndex].velocity == 0
                                        && getPairedNoteOnIndexAndTime(hangingNoteOffIndex).time !in repeatStartTime..fullDeltaTimeRepeat
                                    ) {
                                        kmmk.noteOn(c, p, 100)
                                        Log.d("ryjtyj", "hangingNoteOffIndex on the start of the loop = $hangingNoteOffIndex")
                                        changePlayingNotes(p, 100)
                                    }
                                    // notesON on the start of the loop if notes are being held // TODO move higher than wrapAround? ^^
                                    if (pressedNotes[p].isPressed) {
                                        kmmk.noteOn(c, p, 100)
                                        Log.d("ryjtyj", "pressedNotes[p].first")
                                        if (uiState.value.seqIsRecording) recordNote(
                                            pitch = p,
                                            velocity = 100,
                                            id = pressedNotes[p].id,
                                            quantizationTime = quantizationTime,
                                            staticNoteOffTime = staticNoteOffTime,
                                            seqIsPlaying = true,
                                            isRepeating = true,
                                            quantizeTime = ::quantizeTime,
                                            customTime = repeatStartTime
                                        )
                                        changePlayingNotes(p, 100)
                                        increaseNoteId()
                                    }
                                }
                                savedRepeatsCount = repeatsCount
                            }

                            if (indexToRepeat - wrapIndex < 0) Log.d("ryjtyj", "indexToRepeat = $indexToRepeat, wrapIndex = $wrapIndex: beatRepeat before erasing or playing. ")

                            // erasing or playing
                            while(
                                notes.size > (indexToRepeat - wrapIndex)
                                && (
                                    (notes[indexToRepeat - wrapIndex].time in repeatStartTime .. fullDeltaTimeRepeat)   // normal case, not-wrapAround: [---]
                                        || (repeatStartTime > repeatEndTime && (
                                        wrapIndex > 0 && (notes[indexToRepeat - wrapIndex].time in 0.0 .. fullDeltaTimeRepeat)
                                        )
                                    )
                                )
                            ) {
                                if(isErasing || (uiState.value.seqIsRecording && pressedNotes[notes[indexToRepeat - wrapIndex].pitch].isPressed)
                                ) {
//                                    Log.d("ryjtyj", "erasing or playing")
                                    if (!erasing(true, indexToRepeat - wrapIndex, false)) break
                                } else {
                                    if(indexToRepeat != draggedNoteOffIndex) {
                                        playing(
                                            indexToRepeat - wrapIndex,
                                            !uiState.value.soloIsOn,
                                            uiState.value.seqView == SeqView.STEP
                                        )
                                        // StepView case:
                                        if (indexToRepeat == draggedNoteOnIndex) {
                                            playingDraggedNoteOffInTime(uiState.value.factorBpm)
                                        }
                                    }
                                    indexToRepeat++
                                }
                            }
                        }
                    }
                }
                _uiState.update { a -> a.copy(
                    visualArrayRefresh = !uiState.value.visualArrayRefresh, // TODO needed?
                ) }
                delay(3.milliseconds)
            }
        }
    }


    fun stopSeq() {
        stopChannels(STOPSEQ)
        kmmk.stopClock()
        _uiState.update { it.copy(
                sequences = uiState.value.sequences, // TODO needed?
                seqIsPlaying = false
            )
        }
    }

    private fun stopChannels(mode: StopNotesMode) {
        for (c in 0..15) {
            uiState.value.stopNotesOnChannel(c, mode)
            if(mode == STOPSEQ) {
                uiState.value.sequences[c].deltaTime = 0.0
                uiState.value.sequences[c].bpmDelta = 0.0
                uiState.value.sequences[c].fullDeltaTimeRepeat = 0.0
            }
        }
    }

    // TODO merge ^^vv ?

    private fun SeqUiState.stopNotesOnChannel(c: Int, mode: StopNotesMode) {
        for (p in 0..127) {
            while (sequences[c].playingNotes[p] > 0) {
                kmmk.noteOn(c, p, 0)
//                Log.d("ryjtyj", "stopNotesOnChannel")
                sequences[c].changePlayingNotes(p, 0)
            }
            if (seqIsRecording && sequences[c].pressedNotes[p].isPressed) {
                sequences[c].recordNote(
                    pitch = p,
                    velocity = 0,
                    id = sequences[c].pressedNotes[p].id,
                    quantizationTime = quantizationTime,
                    staticNoteOffTime = staticNoteOffTime,
                    seqIsPlaying = seqIsPlaying,
                    isRepeating = isRepeating,
                    quantizeTime = ::quantizeTime,
                    customTime = if (mode == END_OF_REPEAT) sequences[c].repeatEndTime else -1.0
                )
                if (mode == END_OF_REPEAT) sequences[c].pressedNotes[p] = PressedNote(true, sequences[c].noteId) // updating noteID for new note in beatRepeat (wrapAround case)
            }
        }
    }

    fun stopAllNotes() {
        for (c in 0..15) {
            for (p in 0..127) {
                kmmk.noteOn(c, p, 0)
            }
        }
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
                                staticNoteOffTime = staticNoteOffTime,
                                seqIsPlaying = seqIsPlaying,
                                isRepeating = isRepeating,
                                quantizeTime = ::quantizeTime,
                            )
                            sequences[c].pressedNotes[p] = PressedNote(false, Int.MAX_VALUE)
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
                            if (!uiState.value.sequences[c].isSoloed) uiState.value.stopNotesOnChannel(c, MUTE)
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
                        uiState.value.stopNotesOnChannel(channel, MUTE) // TODO when allButton?
                    } else _uiState.update { it.copy(muteIsOn = uiState.value.sequences.any{ sequence -> sequence.isMuted }) }
                }
            }
        }
    }


    private fun enableEraseOnChannel(channel: Int, velocity: Int) {
        uiState.value.sequences[channel].isErasing = velocity > 0
    }


    private fun repeat(divisor: Int) {
        stopChannels(MUTE)
        if(
//            divisor != uiState.value.divisorState &&
            divisor != 0) {
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
                    repeatEndTime = quantizeTime(deltaTime)
                    deltaTimeRepeat = quantizeTime(deltaTime) - repeatLength
                    if(deltaTimeRepeat < 0) deltaTimeRepeat += totalTime
                    repeatStartTime = deltaTimeRepeat
                    previousDeltaTime = repeatEndTime

                    indexToStartRepeating = searchIndexInTimeBoundaries()
                    indexToRepeat = indexToStartRepeating
                    repeatsCount = 1
                    savedRepeatsCount = repeatsCount

                    for (p in 0..127) {
                        cancelPressedNotes(c, p)

                        val hangingNoteOffIndex = notes.indexOfFirst { it.pitch == p && it.time > deltaTimeRepeat }
                        if (hangingNoteOffIndex != -1 && notes[hangingNoteOffIndex].velocity == 0) {
                            if(c == 0) Log.d("ryjtyj", "hangingNoteOffIndex in repeat() = $hangingNoteOffIndex")
                            kmmk.noteOn(c, p, 100)
                            changePlayingNotes(p, 100)
                        }
                    }
                    if(c == 0) Log.d("ryjtyj", "indexToStartRepeating = $indexToStartRepeating, repeatStartTime = $repeatStartTime, repeatEndTime = $repeatEndTime")
                }
            }
            _uiState.update { it.copy( repeatLength = repeatLength ) }
        }
        _uiState.update { it.copy(
            isRepeating = divisor > 0,
//            divisorState = divisor,
            sequences = uiState.value.sequences
        ) }
    }

    fun changeRepeatDivisor(divisor: Int) {
        _uiState.update { it.copy(divisorState = divisor) }
    }


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
                            uiState.value.sequences[c].clearChannel(c)
                        }
                        loadPattern(pattern = channel)
                    }
                }
                SOLOING -> soloChannel(channel, velocity, elapsedTime, allButton)
                MUTING -> muteChannel(channel, velocity, elapsedTime, allButton)
                ERASING -> enableEraseOnChannel(channel, velocity)
                CLEARING -> {
                    if(velocity > 0) clearChannel(channel) // TODO implement UNDO CLEAR
                }
                // PLAYING & RECORDING
                else -> {
                    val retrigger = playingNotes[pitch] > 0 && velocity > 0
                    if (retrigger) {
                        kmmk.noteOn(channel, pitch, 0)    // allows to retrigger already playing notes
                    }
                    kmmk.noteOn(channel, pitch, velocity)
                    pressedNotes[pitch] = PressedNote(velocity > 0, if(velocity > 0) noteId else pressedNotes[pitch].id)

                    if (uiState.value.seqIsRecording) {
                        if(velocity > 0) {
                            recordNote(
                                pitch = pitch,
                                velocity = velocity,
                                id = noteId,
                                quantizationTime = quantizationTime,
                                staticNoteOffTime = staticNoteOffTime,
                                seqIsPlaying = uiState.value.seqIsPlaying,
                                isRepeating = uiState.value.isRepeating,
                                quantizeTime = ::quantizeTime,
                            )
                            increaseNoteId()
                        } else {
                            if(!dePressedNotesOnRepeat[pitch]) {
                                recordNote(
                                    pitch = pitch,
                                    velocity = velocity,
                                    id = pressedNotes[pitch].id,
                                    quantizationTime = quantizationTime,
                                    staticNoteOffTime = staticNoteOffTime,
                                    seqIsPlaying = uiState.value.seqIsPlaying,
                                    isRepeating = uiState.value.isRepeating,
                                    quantizeTime = ::quantizeTime,
                                )
                            } else {
                                dePressedNotesOnRepeat[pitch] = false
                            }
                        }
                    }
                    if(!retrigger) changePlayingNotes(pitch, velocity)
                    _uiState.update { a -> a.copy(selectedChannel = channel) }
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
        CoroutineScope(Dispatchers.Main).launch {
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
                    notes = patterns[pattern][c]
                    indexToPlay = notes.indexOfLast { it.time < deltaTime / uiState.value.factorBpm } + 1
                    if(uiState.value.isRepeating) {
                        indexToRepeat = notes.indexOfLast { it.time < fullDeltaTimeRepeat / uiState.value.factorBpm } + 1
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
            val remainder = (time + quantizationTime / 2) % quantizationTime
            time + quantizationTime / 2 - remainder
        } else time
    }

    private fun quantizeChannel(channel: Int) {
        with(uiState.value.sequences[channel]) {
            for(i in notes.indices) {
                if(notes[i].velocity > 0) {
                    val tempTime = notes[i].time
                    val time = quantizeTime(tempTime)
                    changeNoteTime(i, time)
                    changeNoteTime(getPairedNoteOffIndexAndTime(i).index, time - tempTime, true)
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
                job = CoroutineScope(Dispatchers.Main).launch {
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


    fun editCurrentPadsMode(mode: PadsMode, switchOn: Boolean, momentary: Boolean = false){
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
        _uiState.update { a -> a.copy(
            seqView = seqView
        ) }
    }

    fun updateSequencesUiState() {
        _uiState.update { a -> a.copy(
            sequences = uiState.value.sequences,
            visualArrayRefresh = !uiState.value.visualArrayRefresh
        ) }
//        Log.d("ryjtyj", "updateSequencesUiState()")
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
            timingClock = 500.0 / 24.0 * bpmPointOne / 120.0,
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


    private fun cancelPressedNotes(c: Int, p: Int) {
        with(uiState.value.sequences[c]) {
//            Log.d("ryjtyj", "cancelPressedNotes() on channel $c, pitch $p")
            if(pressedNotes[p].isPressed) {
                pressedNotes[p].isPressed = false
                dePressedNotesOnRepeat[p] = true
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