package com.example.mysequencer01ui.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.View
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import com.example.mysequencer01ui.ChannelSequence
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
import com.example.mysequencer01ui.SeqView
import com.example.mysequencer01ui.StopNotesMode
import com.example.mysequencer01ui.StopNotesMode.END_OF_REPEAT
import com.example.mysequencer01ui.StopNotesMode.STOP_NOTES
import com.example.mysequencer01ui.StopNotesMode.STOP_SEQ
import com.example.mysequencer01ui.data.Patterns
import com.example.mysequencer01ui.data.SeqDao
import com.example.mysequencer01ui.data.Settings
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
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.milliseconds

const val TAG = "myTag"
const val BARTIME = 2000

class SeqViewModel(private val kmmk: KmmkComponentContext, private val dao: SeqDao) : ViewModel() {

    private val _uiState = MutableStateFlow(SeqUiState(kmmk))
    val uiState: StateFlow<SeqUiState> = _uiState.asStateFlow()

    val channelSequences = MutableList(16){ ChannelSequence(it, kmmk,) }

    private var previousPadsMode: PadsMode = DEFAULT
    private var listOfMutedChannels = emptyList<Int>()
    private var listOfSoloedChannels = emptyList<Int>()
    private var previousDivisorValue = 0

    private var patterns: Array<Array<Array<Note>>> = Array(16){ Array(16) { emptyArray() } }
    private var jobQuantizeSwitch = CoroutineScope(EmptyCoroutineContext).launch { }

    init {
        CoroutineScope(Dispatchers.Default).launch {
            val settingsTable = Settings()
            dao.populateSettings(settingsTable)
            _uiState.update {
                it.copy(
                    transmitClock = dao.loadSettings().transmitClock,
                    bpm = dao.loadSettings().bpm,
                    factorBpm = dao.loadSettings().factorBpm,
                    isQuantizing = dao.loadSettings().isQuantizing,
                    keepScreenOn = dao.loadSettings().keepScreenOn,
                    showChannelNumberOnPads = dao.loadSettings().showChannelNumberOnPads,
                    allowRecordShortNotes = dao.loadSettings().allowRecordShortNotes,
                    fullScreen = dao.loadSettings().fullScreen,
                    toggleTime = dao.loadSettings().toggleTime,
                    uiRefreshRate = dao.loadSettings().uiRefreshRate,
                    dataRefreshRate = dao.loadSettings().dataRefreshRate,
                    showVisualDebugger = dao.loadSettings().showVisualDebugger,
                    debuggerViewSetting = dao.loadSettings().debuggerViewSetting,
                )
            }

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


/** MAIN CYCLE **/
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
                        isQuantizing = uiState.value.isQuantizing,
                        quantizationTime = uiState.value.quantizationTime,
                        quantizeTime = ::quantizeTime,
                        isRepeating = uiState.value.isRepeating,
                        isStepView = uiState.value.seqView == SeqView.STEP,
                        stopNotesOnChannel = ::stopNotesOnChannel,
                        allowRecordShortNotes = uiState.value.allowRecordShortNotes,
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

                delay(uiState.value.dataRefreshRate.milliseconds)
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            while (uiState.value.seqIsPlaying) {
                for (c in channelSequences.indices) {
                    channelSequences[c].updateChannelState()
                }
                delay(uiState.value.uiRefreshRate.milliseconds)
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
                    val isNotMuted = (uiState.value.soloIsOn && channelState.value.isSoloed) || (!uiState.value.soloIsOn && !channelState.value.isMuted)
                    if (retrigger && isNotMuted) {
                        kmmk.noteOn(channel, pitch, 0)    // allows to retrigger already playing notes
                    }

                    if (isNotMuted) kmmk.noteOn(channel, pitch, velocity)

                    if (velocity > 0) {
                        updatePressedNote(pitch, true, noteId, System.currentTimeMillis())
                    } else {
                        updatePressedNote(pitch, false, pressedNotes[pitch].id, pressedNotes[pitch].noteOnTimestamp)
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
                                isQuantizing = uiState.value.isQuantizing,
                                isStepView = uiState.value.seqView == SeqView.STEP,
                                isStepRecord = false,
                                allowRecordShortNotes = uiState.value.allowRecordShortNotes,
                            )
                            increaseNoteId()
                        } else {
                            recordNote(
                                pitch = pitch,
                                velocity = 0,
                                id = pressedNotes[pitch].id,
                                quantizationTime = uiState.value.quantizationTime,
                                quantizeTime = ::quantizeTime,
                                factorBpm = uiState.value.factorBpm,
                                seqIsPlaying = uiState.value.seqIsPlaying,
                                isRepeating = uiState.value.isRepeating,
                                isQuantizing = uiState.value.isQuantizing,
                                isStepView = uiState.value.seqView == SeqView.STEP,
                                isStepRecord = false,
                                allowRecordShortNotes = uiState.value.allowRecordShortNotes,
                            )
                        }
                    }
                    if(!retrigger) changePlayingNotes(pitch, velocity)
                    _uiState.update { it.copy(selectedChannel = channel) }
                    if(!uiState.value.seqIsPlaying) updateNotes()
                }
            }
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


/** MODE BUTTONS **/
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

    fun switchQuantization() {
        _uiState.update { it.copy(
            isQuantizing = !uiState.value.isQuantizing
        ) }
    }

    fun switchPadsToQuantizingMode(switchOn: Boolean) {
        if(switchOn) {
            if(uiState.value.seqView != SeqView.LIVE) {
                jobQuantizeSwitch = CoroutineScope(Dispatchers.Main).launch {
                    val time = System.currentTimeMillis()
                    while (uiState.value.quantizeModeTimer < uiState.value.toggleTime) {
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


    private fun soloChannel(channel: Int, velocity: Int, elapsedTime: Long, allButton: Boolean) {
        with(channelSequences[channel]) {
            val pressOrLongRelease = velocity > 0 || (System.currentTimeMillis() - elapsedTime) > uiState.value.toggleTime

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
            val pressOrLongRelease = velocity > 0 || (System.currentTimeMillis() - elapsedTime) > uiState.value.toggleTime

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
                            quantizeTime = ::quantizeTime,
                            factorBpm = uiState.value.factorBpm,
                            seqIsPlaying = uiState.value.seqIsPlaying,
                            isRepeating = uiState.value.isRepeating,
                            isQuantizing = uiState.value.isQuantizing,
                            isStepView = uiState.value.seqView == SeqView.STEP,
                            isStepRecord = false,
                            allowRecordShortNotes = uiState.value.allowRecordShortNotes,
                        )
                        channelSequences[c].updatePressedNote(p, false, Int.MAX_VALUE, Long.MIN_VALUE)
                    }
                }
            }
        }
        _uiState.update { a -> a.copy( seqIsRecording = !uiState.value.seqIsRecording ) }
        changePlayHeadsColor()
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
//                notesOffToIgnoreOnDragEnd = Array(128){ false }
                if(mode == STOP_SEQ) {
                    deltaTime = uiState.value.quantizationTime // for static recording
                    deltaTimeRepeat = 0.0
                    bpmDelta = 0.0
                    if(draggedNoteOffJob.isActive) {
                        draggedNoteOffJob.cancel()
                        Log.d(TAG, "stopChannel $c: draggedNoteOffJob.cancel()")
                        kmmk.noteOn(c, pitchTempSavedForCancel, 0)
                        changePlayingNotes(pitchTempSavedForCancel, 0)
                    }
                    updateChannelState()
                }
            }
        }
    }

    private fun stopNotesOnChannel(c: Int, mode: StopNotesMode) {
        for (p in 0..127) {
            while (channelSequences[c].playingNotes[p] > 0) {
                kmmk.noteOn(c, p, 0)
                Log.d(TAG, "stopNotesOnChannel(): Stopping note on channel $c, pitch = $p")
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
                        isQuantizing = uiState.value.isQuantizing,
                        isStepView = uiState.value.seqView == SeqView.STEP,
                        isStepRecord = false,
                        allowRecordShortNotes = uiState.value.allowRecordShortNotes,
                        customTime = if (mode == END_OF_REPEAT) channelSequences[c].repeatEndTime else -1.0
                    )
                    if (mode == END_OF_REPEAT) {
                        with (channelSequences[c]) {
                            val noteOnIndex = notes.indexOfFirst { it.id == pressedNotes[p].id && it.velocity > 0 }
                            val fasterThanHalfOfQuantize = System.currentTimeMillis() - pressedNotes[p].noteOnTimestamp <= quantizationTime / factorBpm / 2

                            if (notes[noteOnIndex].time == repeatStartTime && !fasterThanHalfOfQuantize) { // tied note [startRepeat..endRepeat]
                                updatePressedNote(p, false)
                                cancelPadInteraction()
                                Log.d(TAG, "tied note $p")
                            } else {
                                updatePressedNote(p, true, noteId, System.currentTimeMillis()) // updating noteID for new note in beatRepeat (wrapAround case)
                            }
                        }
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


/** MISC **/
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


    fun rememberInteraction(channel: Int, pitch: Int, interaction: PressInteraction.Press) {
        channelSequences[channel].interactionSources[pitch].pressInteraction = interaction
    }

    fun cancelAllPadsInteraction() {
        for (c in channelSequences.indices) channelSequences[c].cancelPadInteraction()
    }


/** SETTINGS **/
    fun saveSettingsScrollPosition(y: Int) {
        _uiState.update { it.copy(settingsScrollPosition = y) }
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

    fun switchClockTransmitting() {
        _uiState.update { it.copy(
            transmitClock = !uiState.value.transmitClock
        ) }
        saveSettingsToDatabase()
    }

    fun switchFullScreenState() {
        _uiState.update { it.copy(
            fullScreen = !uiState.value.fullScreen
        ) }
        saveSettingsToDatabase()
    }

    fun goFullScreen(view: View) {
        // !! should be safe here since the view is part of an Activity
        val window = view.context.getActivity()!!.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, view).hide(
//        WindowInsetsCompat.Type.statusBars() or
            WindowInsetsCompat.Type.navigationBars()
        )
        if (uiState.value.seqView == SeqView.SETTINGS) saveSettingsToDatabase()
    }

    fun goOutOfFullScreen(view: View) {
        // !! should be safe here since the view is part of an Activity
        val window = view.context.getActivity()!!.window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, view).show(
//        WindowInsetsCompat.Type.statusBars() or
            WindowInsetsCompat.Type.navigationBars()
        )
        saveSettingsToDatabase()
    }

    private fun Context.getActivity(): Activity? = when (this) {
        is Activity -> this
        // this recursion should be okay since we call getActivity on a view context
        // that should have an Activity as its baseContext at some point
        is ContextWrapper -> baseContext.getActivity()
        else -> null
    }

    fun switchKeepScreenOn() {
        _uiState.update { it.copy(
            keepScreenOn = !uiState.value.keepScreenOn
        ) }
        saveSettingsToDatabase()
    }

    fun switchRecordShortNotes() {
        _uiState.update { it.copy(
            allowRecordShortNotes = !uiState.value.allowRecordShortNotes
        ) }
        saveSettingsToDatabase()
    }

    fun switchShowChannelNumberOnPads() {
        _uiState.update { it.copy(
            showChannelNumberOnPads = !uiState.value.showChannelNumberOnPads
        ) }
        saveSettingsToDatabase()
    }

    fun changeToggleTime(toggleTime: Int) {
        _uiState.update { it.copy(toggleTime = toggleTime) }
        saveSettingsToDatabase()
    }

    fun changeUiRefreshRate(uiRefreshRate: Int) {
        _uiState.update { it.copy(uiRefreshRate = uiRefreshRate) }
        saveSettingsToDatabase()
    }

    fun changeDataRefreshRate(dataRefreshRate: Int) {
        _uiState.update { it.copy(dataRefreshRate = dataRefreshRate) }
        saveSettingsToDatabase()
    }

    fun switchVisualDebugger() {
        _uiState.update { it.copy(
            showVisualDebugger = !uiState.value.showVisualDebugger
        ) }
        saveSettingsToDatabase()
    }

    fun selectDebuggerSetting(setting: Int) {
        _uiState.update { it.copy(
            debuggerViewSetting = setting
        ) }
        saveSettingsToDatabase()
    }



    fun saveSettingsToDatabase(){
        runBlocking {
            dao.saveSettings(
                Settings(
                    id = 1,
                    transmitClock = uiState.value.transmitClock,
                    bpm = uiState.value.bpm,
                    factorBpm = uiState.value.factorBpm,
                    isQuantizing = uiState.value.isQuantizing,
                    keepScreenOn = uiState.value.keepScreenOn,
                    showChannelNumberOnPads = uiState.value.showChannelNumberOnPads,
                    allowRecordShortNotes = uiState.value.allowRecordShortNotes,
                    fullScreen = uiState.value.fullScreen,
                    toggleTime = uiState.value.toggleTime,
                    uiRefreshRate = uiState.value.uiRefreshRate,
                    dataRefreshRate = uiState.value.dataRefreshRate,
                    showVisualDebugger = uiState.value.showVisualDebugger,
                    debuggerViewSetting = uiState.value.debuggerViewSetting,
                )
            )
        }
    }
}