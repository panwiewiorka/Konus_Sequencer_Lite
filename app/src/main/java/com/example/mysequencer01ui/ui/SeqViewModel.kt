package com.example.mysequencer01ui.ui

import android.util.Log
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import com.example.mysequencer01ui.KmmkComponentContext
import com.example.mysequencer01ui.Note
import com.example.mysequencer01ui.PadsMode
import com.example.mysequencer01ui.PadsMode.*
import com.example.mysequencer01ui.SeqView
import com.example.mysequencer01ui.ChannelSequence
import com.example.mysequencer01ui.StopNotesMode
import com.example.mysequencer01ui.StopNotesMode.*
import com.example.mysequencer01ui.data.Patterns
import com.example.mysequencer01ui.data.SeqDao
import com.example.mysequencer01ui.ui.theme.dusk
import com.example.mysequencer01ui.ui.theme.notWhite
import com.example.mysequencer01ui.ui.theme.playGreen
import com.example.mysequencer01ui.ui.theme.violet
import com.example.mysequencer01ui.ui.theme.warmRed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.milliseconds

const val BARTIME = 2000

class SeqViewModel(private val kmmk: KmmkComponentContext, private val dao: SeqDao) : ViewModel() {

    private val _uiState = MutableStateFlow(SeqUiState(kmmk))
    val uiState: StateFlow<SeqUiState> = _uiState.asStateFlow()

    val channelSequences = MutableList(16){ ChannelSequence(it, kmmk,) }

    var toggleTime = 150
    private var previousPadsMode: PadsMode = DEFAULT
    private var listOfMutedChannels = emptyList<Int>()
    private var listOfSoloedChannels = emptyList<Int>()
    private var previousDivisorValue = 0

    private var patterns: Array<Array<Array<Note>>> = Array(16){ Array(16) { emptyArray() } }
    private var jobQuantizeSwitch = CoroutineScope(EmptyCoroutineContext).launch { }
    private var jobUpdateChannelsState = CoroutineScope(EmptyCoroutineContext).launch { }

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
                    }
                }
            }
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

        for(c in channelSequences.indices) {
            channelSequences[c].resetChannelSequencerValues()
        }

        if (uiState.value.seqIsPlaying) return // needed for future implementing of reStart()


        _uiState.update { it.copy( seqIsPlaying = true ) }
        var clockTicks = 0
        if (uiState.value.transmitClock) kmmk.startClock()

        // ------====== MAIN CYCLE
        CoroutineScope(Dispatchers.Default).launch {
            while (uiState.value.seqIsPlaying) {
                for (c in channelSequences.indices) {
                    channelSequences[c].advanceChannelSequence(
                        seqIsPlaying = uiState.value.seqIsPlaying,
                        seqIsRecording = uiState.value.seqIsRecording,
                        factorBpm = uiState.value.factorBpm,
                        soloIsOn = uiState.value.soloIsOn,
                        quantizationTime = uiState.value.quantizationTime,
                        quantizeTime = ::quantizeTime,
                        isRepeating = uiState.value.isRepeating,
                        isStepView = uiState.value.seqView == SeqView.STEP,
                        stopNotesOnChannel = ::stopNotesOnChannel,
                    )
                }

                with(channelSequences[0]) {
                    // CLOCK
                    if (uiState.value.transmitClock) {
                        if (deltaTime >= totalTime) {
                            clockTicks = 0
                        } else if (deltaTime >= uiState.value.timingClock * clockTicks) {
                            kmmk.sendTimingClock()
                            clockTicks++
                        }
                    }

                    // ENGAGE REPEAT()
                    if (uiState.value.divisorState != previousDivisorValue && quantizeTime(deltaTime) <= deltaTime) {
                        engageRepeat(uiState.value.divisorState)
                    }
                }

                delay(3.milliseconds)
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            while (uiState.value.seqIsPlaying) {
                for (c in channelSequences.indices) {
                    channelSequences[c].updateChannelState()
                }
                delay(3.milliseconds)
            }
        }
    }



    fun stopSeq() {
        _uiState.update { it.copy(seqIsPlaying = false) }
        stopChannels(STOP_SEQ)
        kmmk.stopClock()
    }

    private fun stopChannels(mode: StopNotesMode) {
        for (c in 0..15) {
            with(channelSequences[c]) {
                stopNotesOnChannel(c, mode)
                idsOfNotesToIgnore.clear()
                notesDragEndOnRepeat = Array(128){ false }
                if(mode == STOP_SEQ) {
                    deltaTime = 0.0
                    deltaTimeRepeat = 0.0
                    bpmDelta = 0.0
                    if(draggedNoteOffJob.isActive) {
                        draggedNoteOffJob.cancel()
                        Log.d("ryjtyj", "stopChannel $c: draggedNoteOffJob.cancel()")
                        kmmk.noteOn(c, pitchTempSavedForCancel, 0)
                        changePlayingNotes(pitchTempSavedForCancel, 0)
                    }
                }
            }
        }
    }

    private fun stopNotesOnChannel(c: Int, mode: StopNotesMode) {
        for (p in 0..127) {
            while (channelSequences[c].playingNotes[p] > 0) {
                kmmk.noteOn(c, p, 0)
                Log.d("ryjtyj", "stopNotesOnChannel(): Stopping note on channel $c, pitch = $p")
                channelSequences[c].changePlayingNotes(p, 0)
            }
            with(uiState.value) {
                if (seqIsRecording && channelSequences[c].pressedNotes[p].isPressed) {
                    channelSequences[c].recordNote(
                        pitch = p,
                        velocity = 0,
                        id = channelSequences[c].pressedNotes[p].id,
                        quantizationTime = quantizationTime,
                        quantizeTime = ::quantizeTime,
                        factorBpm = uiState.value.factorBpm,
                        seqIsPlaying = seqIsPlaying,
                        isRepeating = isRepeating,
                        isStepView = uiState.value.seqView == SeqView.STEP,
                        isStepRecord = false,
                        customTime = if (mode == END_OF_REPEAT) channelSequences[c].repeatEndTime else -1.0
                    )
                    if (mode == END_OF_REPEAT) {
                        channelSequences[c].updatePressedNote(p, true, channelSequences[c].noteId, System.currentTimeMillis()) // updating noteID for new note in beatRepeat (wrapAround case)
                    }
                }
            }
        }
        channelSequences[c].updatePadState()
    }

    fun stopAllNotes() {
        CoroutineScope(Dispatchers.Default).launch {
            for (c in channelSequences.indices) {
                launch {stopAllNotesOnChannel(c)}
            }
        }
    }

    private suspend fun stopAllNotesOnChannel(c: Int) {
        for (p in 0..127) {
            kmmk.noteOn(c, p, 0)
            delay(1) // sometimes weird behaviour in audio channel occurs without delay (some stuck note)
        }
    }


    fun changeRecState() {
        if(uiState.value.seqIsRecording) {
            for (c in channelSequences.indices) {
                for (p in 0..127) {
                    if (channelSequences[c].pressedNotes[p].isPressed) {
                        kmmk.noteOn(c, p, 0)
                        channelSequences[c].recordNote(
                            pitch = p,
                            velocity = 0,
                            id = channelSequences[c].pressedNotes[p].id,
                            quantizationTime = uiState.value.quantizationTime,
                            factorBpm = uiState.value.factorBpm,
                            seqIsPlaying = uiState.value.seqIsPlaying,
                            isRepeating = uiState.value.isRepeating,
                            quantizeTime = ::quantizeTime,
                            isStepView = uiState.value.seqView == SeqView.STEP,
                            isStepRecord = false,
                        )
                        channelSequences[c].updatePressedNote(p, false, Int.MAX_VALUE, Long.MIN_VALUE)
                    }
                }
            }
        }
        _uiState.update { a -> a.copy( seqIsRecording = !uiState.value.seqIsRecording ) }
        changePlayHeadsColor()
    }


    private fun soloChannel(channel: Int, velocity: Int, elapsedTime: Long, allButton: Boolean) {
        with(channelSequences[channel]) {
            val pressOrLongRelease = velocity > 0 || (System.currentTimeMillis() - elapsedTime) > toggleTime

            if(pressOrLongRelease) {
                when {
                    !allButton && channelState.value.isSoloed -> {
                        updateIsSoloed(!channelState.value.isSoloed)
                        _uiState.update { it.copy(soloIsOn = channelSequences.any{ sequence -> sequence.channelState.value.isSoloed }) }
                    }
                    !allButton -> {
                        updateIsSoloed(!channelState.value.isSoloed)
                        _uiState.update { it.copy(soloIsOn = true) }
                        for (c in channelSequences.indices) if (!channelSequences[c].channelState.value.isSoloed) stopNotesOnChannel(c, STOP_NOTES)
                    }
                    velocity > 0 -> {
                        if(channel == 0) {
                            listOfSoloedChannels = channelSequences.filter { it.channelState.value.isSoloed }.map { it.channelNumber }
                            _uiState.update { it.copy(soloIsOn = listOfSoloedChannels.isEmpty()) }
                        }
                        updateIsSoloed(uiState.value.soloIsOn)
                    }
                    listOfSoloedChannels.contains(channel) -> {
                        updateIsSoloed(true)
                    }
                    listOfSoloedChannels.isEmpty() -> {
                        updateIsSoloed(false)
                        if(channel == 15) _uiState.update { it.copy(soloIsOn = false) }
                    }
                }
            }
        }
    }


    private fun muteChannel(channel: Int, velocity: Int, elapsedTime: Long, allButton: Boolean) {
        with(channelSequences[channel]) {
            val pressOrLongRelease = velocity > 0 || (System.currentTimeMillis() - elapsedTime) > toggleTime

            if(pressOrLongRelease) {
                when {
                    !allButton && channelState.value.isMuted -> {
                        updateIsMuted(!channelState.value.isMuted)
                        _uiState.update { it.copy(muteIsOn = channelSequences.any{ sequence -> sequence.channelState.value.isMuted }) }
                    }
                    !allButton -> {
                        updateIsMuted(!channelState.value.isMuted)
                        _uiState.update { it.copy(muteIsOn = true) }
                        stopNotesOnChannel(channel, STOP_NOTES)
                    }
                    velocity > 0 -> {
                        if(channel == 0) {
                            listOfMutedChannels = channelSequences.filter { it.channelState.value.isMuted }.map { it.channelNumber }
                            _uiState.update { it.copy(muteIsOn = listOfMutedChannels.isEmpty()) }
                        }
                        updateIsMuted(uiState.value.muteIsOn)
                        if(uiState.value.muteIsOn) stopNotesOnChannel(channel, STOP_NOTES)
                    }
                    listOfMutedChannels.contains(channel) -> {
                        updateIsMuted(true)
                        stopNotesOnChannel(channel, STOP_NOTES)
                    }
                    listOfMutedChannels.isEmpty() -> {
                        updateIsMuted(false)
                        if (channel == 15) _uiState.update { it.copy(muteIsOn = false) }
                    }
                }
            }
        }
    }



    fun quantizeTime(time: Double, ): Double {
        return if(uiState.value.isQuantizing) {
            val remainder = (time + uiState.value.quantizationTime / 2) % uiState.value.quantizationTime
            time + uiState.value.quantizationTime / 2 - remainder
        } else time
    }

    private fun quantizeChannel(channel: Int) {
        with(channelSequences[channel]) {
            for (i in notes.indices) {
                if (notes[i].velocity > 0) {
                    val tempTime = notes[i].time
                    val time = quantizeTime(tempTime)
                    changeNoteTime(i, time)
                    changeNoteTime(getNotePairedIndexAndTime(i).index, time - tempTime, true)
                    sortNotesByTime()
                }
            }
        }
    }


/** REPEAT **/
    private fun engageRepeat(divisor: Int) {
        if (divisor == 0) {
            stopChannels(STOP_NOTES)
            for(c in channelSequences.indices) channelSequences[c].refreshIndices()
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
            for(c in channelSequences.indices) {
                channelSequences[c].engageRepeatOnChannel(
                    repeatLength = repeatLength,
                    isRepeating = uiState.value.isRepeating,
                    stopNotesOnChannel = ::stopNotesOnChannel,
                    quantizeTime = ::quantizeTime
                )
            }
            _uiState.update { it.copy( repeatLength = repeatLength ) }
        }
        for(c in channelSequences.indices) channelSequences[c].idsOfNotesToIgnore.clear()
        _uiState.update { it.copy(
            isRepeating = divisor > 0,
        ) }
        previousDivisorValue = divisor
    }

    fun changeRepeatDivisor(divisor: Int) {
        _uiState.update { it.copy(divisorState = divisor) }
        if (!uiState.value.seqIsPlaying) engageRepeat(divisor)
    }


/** PRESS PAD */
    fun pressPad(channel: Int, pitch: Int, velocity: Int, elapsedTime: Long, allButton: Boolean) {
        with(channelSequences[channel]) {
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
                    if(velocity > 0) savePattern(pattern = channel, channelSequences = channelSequences)
                }
                LOADING -> {
                    if(!allButton && velocity > 0) {
                        for(c in channelSequences.indices) {
                            channelSequences[c].clearChannel(c)
                        }
                        loadPattern(pattern = channel)
                    }
                }
                SOLOING -> soloChannel(channel, velocity, elapsedTime, allButton)
                MUTING -> muteChannel(channel, velocity, elapsedTime, allButton)
                ERASING -> isErasing = velocity > 0
                CLEARING -> {
                    if(velocity > 0) clearChannel(channel)
                }
                // PLAYING & RECORDING
                else -> {
                    val retrigger = playingNotes[pitch] > 0 && velocity > 0
                    if (retrigger) {
                        kmmk.noteOn(channel, pitch, 0)    // allows to retrigger already playing notes
                    }
                    kmmk.noteOn(channel, pitch, velocity)
                    if (velocity > 0) {
                        updatePressedNote(
                            pitch = pitch,
                            isPressed = true,
                            id = noteId,
                            noteOnTimestamp = System.currentTimeMillis()
                        )
                    } else {
                        updatePressedNote(
                            pitch = pitch,
                            isPressed = false,
                            id = pressedNotes[pitch].id,
                            noteOnTimestamp = pressedNotes[pitch].noteOnTimestamp
                        )
                    }

                    if (uiState.value.seqIsRecording) {
                        if(velocity > 0) {
                            recordNote(
                                pitch = pitch,
                                velocity = velocity,
                                id = noteId,
                                quantizationTime = uiState.value.quantizationTime,
                                quantizeTime = ::quantizeTime,
                                factorBpm = uiState.value.factorBpm,
                                seqIsPlaying = uiState.value.seqIsPlaying,
                                isRepeating = uiState.value.isRepeating,
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
                                    quantizeTime = ::quantizeTime,
                                    factorBpm = uiState.value.factorBpm,
                                    seqIsPlaying = uiState.value.seqIsPlaying,
                                    isRepeating = uiState.value.isRepeating,
                                    isStepView = uiState.value.seqView == SeqView.STEP,
                                    isStepRecord = false,
                                )
                            } else {
                                notesDragEndOnRepeat[pitch] = false
                            }
                        }
                    }
                    if(!retrigger) changePlayingNotes(pitch, velocity)
                    _uiState.update { it.copy(selectedChannel = channel) }
                    if(!uiState.value.seqIsPlaying) updateNotes()
                }
            }
        }
    }


    private fun savePattern(pattern: Int, channelSequences: MutableList<ChannelSequence>) {
        CoroutineScope(Dispatchers.Default).launch {
            dao.deletePattern(pattern)
            for(c in channelSequences.indices) {
                with(channelSequences[c]) {
                    patterns[pattern][c] = notes
                    for(i in notes.indices) {
                        dao.saveNoteToPattern(
                            Patterns(
                                pattern = pattern,
                                channel = c,
                                noteIndex = i,
                                time = notes[i].time,
                                pitch = notes[i].pitch,
                                velocity = notes[i].velocity,
                                noteId = notes[i].id,
                            )
                        )
                    }
                }
            }
        }
    }

    private fun loadPattern(pattern: Int) {
        CoroutineScope(Dispatchers.Default).launch {
            for(c in channelSequences.indices) {
                with(channelSequences[c]) {
                    idsOfNotesToIgnore.clear()
                    notes = patterns[pattern][c]
                    if (!uiState.value.seqIsPlaying) updateNotes()
                    refreshIndices()
                    noteId = if(notes.isNotEmpty()) notes.maxOf { it.id } + 1 else Int.MIN_VALUE
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
                jobQuantizeSwitch = CoroutineScope(Dispatchers.Default).launch {
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
            if(jobQuantizeSwitch.isActive) jobQuantizeSwitch.cancel()
            if(uiState.value.quantizeModeTimer == 0) {
                editCurrentPadsMode(QUANTIZING, false)
            }
            _uiState.update { it.copy(quantizeModeTimer = 0) }
        }
    }


    fun editCurrentPadsMode(mode: PadsMode, switchOn: Boolean, momentary: Boolean = false) {
        when {
            (!switchOn && !momentary) || (switchOn && mode == uiState.value.padsMode) -> {
                _uiState.update { it.copy(padsMode = DEFAULT) }
            }
            momentary -> {
                if(previousPadsMode == mode) {
                    previousPadsMode = uiState.value.padsMode
                }
                if(mode != uiState.value.padsMode) return else {
                    _uiState.update { it.copy(padsMode = if(previousPadsMode != uiState.value.padsMode && previousPadsMode != mode) previousPadsMode else DEFAULT) }
                }
            }
            else -> {
                previousPadsMode = uiState.value.padsMode
                _uiState.update { it.copy(padsMode = mode) }
            }
        }
        changePlayHeadsColor()
    }

    private fun changePlayHeadsColor() {
        val playHeadsColor = when (uiState.value.padsMode) {
            QUANTIZING -> dusk
            LOADING -> dusk
            SAVING -> dusk
            SOLOING -> violet
            MUTING -> violet
            ERASING -> notWhite
            CLEARING -> notWhite
            else -> {
                if (uiState.value.seqIsRecording) warmRed
                else playGreen
            }
        }
        _uiState.update { it.copy(playHeadsColor = playHeadsColor) }
    }

    fun changeSeqViewState(seqView: SeqView) {
        _uiState.update { it.copy(
            seqView = seqView
        ) }
    }

    fun changeStepViewNoteHeight(noteHeight: Dp) { // future feature
        _uiState.update { a -> a.copy( stepViewNoteHeight = noteHeight ) }
    }

    fun changeBPM(bpm: Float) {
        val bpmPointOne = (bpm * 10 + 0.5).toInt() / 10f

        if(uiState.value.seqIsPlaying) {
            for(c in channelSequences.indices) {
                channelSequences[c].bpmDelta += (System.currentTimeMillis() - channelSequences[c].startTimeStamp) * (uiState.value.factorBpm - bpmPointOne / 120.0)
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
        channelSequences[channel].interactionSources[pitch].pressInteraction = interaction
    }

    fun cancelAllPadsInteraction() {
        for (c in channelSequences.indices) channelSequences[c].cancelPadInteraction()
    }

    fun switchClockTransmitting() {
        _uiState.update { it.copy(
            transmitClock = !uiState.value.transmitClock
        ) }
    }

    fun switchKeepScreenOn() {
        _uiState.update { it.copy(
            keepScreenOn = !uiState.value.keepScreenOn
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