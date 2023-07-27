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
import kotlin.time.Duration.Companion.nanoseconds


class SeqViewModel(private val kmmk: KmmkComponentContext, private val dao: SeqDao) : ViewModel() {

    private val _uiState = MutableStateFlow(SeqUiState())
    val uiState: StateFlow<SeqUiState> = _uiState.asStateFlow()

    var toggleTime = 150
    var staticNoteOffTime = 100 // TODO replace with quantization time?
    var quantizationTime = 240000 / uiState.value.bpm / uiState.value.quantizationValue * uiState.value.factorBpm
    private var anyChannelIsMuted = false
    private var anyChannelIsSoloed = false
    private var previousPadsMode: PadsMode = DEFAULT

    private var patterns: Array<Array<Array<Note>>> = Array(16){ Array(16) { emptyArray() } }
    private var job = CoroutineScope(EmptyCoroutineContext).launch {  }

    val interactionSources = Array(16) {
        Array(128) {
            Pair(
                MutableInteractionSource(),
                PressInteraction.Press( Offset(0f,0f) )
            )
        }
    }

    init {
        CoroutineScope(Dispatchers.Main).launch {
            for(p in 0..15) {
                for (c in 0..15) {
                    val lastIndex = dao.getLastIndex(p, c) ?: -1
                    patterns[p][c] = Array(lastIndex + 1){Note(0,0,0, 0)}
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
                wrapDelta = 0
                tempNotesSize = 0
            }
        }
        _uiState.update { a -> a.copy( sequences = uiState.value.sequences ) }

        if (uiState.value.seqIsPlaying) return // needed for implementing ReStart()


        _uiState.update { a -> a.copy( seqIsPlaying = true ) }
        var clockTicks = 0
        kmmk.startClock()

        // ------====== MAIN CYCLE
        CoroutineScope(Dispatchers.Main).launch {
            while (uiState.value.seqIsPlaying) {
                for(c in 0..15) {
                    with(uiState.value.sequences[c]){

                        // UPDATE DELTATIME
                        deltaTime = (System.currentTimeMillis() - startTimeStamp) * uiState.value.factorBpm + bpmDelta

                        // NORMAL ERASING or PLAYING
                        while(notes.size > indexToPlay && notes[indexToPlay].time <= deltaTime) {
                            if(!uiState.value.isRepeating && (isErasing || (uiState.value.seqIsRecording && pressedNotes[notes[indexToPlay].pitch].first))) {
                                if (!erasing(kmmk,false, indexToPlay, false)) break
                            } else {
                                if(!uiState.value.isRepeating && indexToPlay != draggedNoteOffIndex) {
                                    playing(kmmk, indexToPlay, !anyChannelIsSoloed)
                                    // StepView case:
                                    if (indexToPlay == draggedNoteOnIndex) {
                                        playingDraggedNoteOffInTime(uiState.value.factorBpm, kmmk)
                                    }
                                }
                                indexToPlay++
                            }
                        }

                        // CLOCK
                        if(c == 0 && uiState.value.transmitClock) {
                            if(deltaTime >= uiState.value.timingClock * clockTicks) {
                                kmmk.sendTimingClock()
                                clockTicks++
                            }
                        }

                        if(c == 0)Log.d("ryjtyj", "$deltaTime, $bpmDelta")

                        // END OF SEQUENCE
                        if (deltaTime >= totalTime) {
                            if(c == 0)Log.d("ryjtyj", "END OF SEQUENCE")
                            startTimeStamp = System.currentTimeMillis() - (deltaTime - totalTime).toLong()
                            bpmDelta = 0.0
                            indexToPlay = 0
                            if(c == 0 && uiState.value.transmitClock) clockTicks = 0
                        }


                        /** IF REPEATING **/
                        if(uiState.value.isRepeating) {

                            // update deltaTimeRepeat
                            deltaTimeRepeat = deltaTime - uiState.value.repeatLength * repeatsCount

                            // end of repeating loop
                            if (deltaTimeRepeat + wrapDelta >= repeatEndTime) {
                                uiState.value.stopNotesOnChannel(c, END_OF_REPEAT)   // notesOFF on the end of loop // TODO activate only if channelIsPlayingNotes?

                                indexToStartRepeating = searchIndexInTimeBoundaries()

                                indexToRepeat = indexToStartRepeating

                                if(repeatsCount == uiState.value.divisorState) repeatsCount = 0
                                repeatsCount++
                                deltaTimeRepeat = deltaTime - uiState.value.repeatLength * repeatsCount
                            }

                            wrapDelta = if(deltaTime < repeatEndTime) totalTime else 0

                            if(deltaTimeRepeat + wrapDelta < 0) {
                                wrapTime = totalTime
                                wrapIndex = 0
                                tempNotesSize = notes.size
                            } else {
                                wrapTime = 0
                                wrapIndex = if(repeatStartTime > repeatEndTime) tempNotesSize else 0
                            }

                            fullDeltaTimeRepeat = deltaTimeRepeat + wrapDelta + wrapTime

                            // start of the loop
                            if(repeatsCount != savedRepeatsCount) {
                                for (p in 0..127) {
                                    // notesON if we start repeatLoop in the middle of the note
                                    val hangingNoteOffIndex = notes.indexOfFirst { it.pitch == p && it.time > fullDeltaTimeRepeat }
                                    if(hangingNoteOffIndex != -1 && notes[hangingNoteOffIndex].velocity == 0
                                        && returnPairedNoteOnIndexAndTime(hangingNoteOffIndex).second.toDouble() !in repeatStartTime..fullDeltaTimeRepeat
                                    ) {
                                        kmmk.noteOn(c, p, 100)
                                        Log.d("ryjtyj", "hangingNoteOffIndex on the start of the loop = $hangingNoteOffIndex")
                                        changePlayingNotes(p, 100)
                                    }
                                    // notesON on the start of the loop if notes are being held // TODO move higher than wrapAround? ^^
                                    if(pressedNotes[p].first) {
                                        kmmk.noteOn(c, p, 100)
                                        Log.d("ryjtyj", "pressedNotes[p].first")
                                        if(uiState.value.seqIsRecording) recordNote(
                                            pitch = p,
                                            velocity = 100,
                                            id = pressedNotes[p].second,
                                            staticNoteOffTime = staticNoteOffTime,
                                            seqIsPlaying = true,
                                            isRepeating = true,
                                            customTime = repeatStartTime.toInt()
                                        )
                                        changePlayingNotes(p, 100)
                                        increaseNoteId()
                                    }
                                }
                                savedRepeatsCount = repeatsCount
                            }

                            if(indexToRepeat - wrapIndex < 0) Log.d("ryjtyj", "indexToRepeat = $indexToRepeat, wrapIndex = $wrapIndex: beatRepeat before erasing or playing. ")

                            // erasing or playing
                            while(
                                notes.size > (indexToRepeat - wrapIndex)
                                && (
                                    (notes[indexToRepeat - wrapIndex].time.toDouble() in repeatStartTime .. fullDeltaTimeRepeat)   // normal case, not-wrapAround: [---]
                                        || (repeatStartTime > repeatEndTime && (
                                        wrapIndex > 0 && (notes[indexToRepeat - wrapIndex].time.toDouble() in 0.0 .. fullDeltaTimeRepeat)
                                        )
                                    )
                                )
                            ) {
                                if(isErasing || (uiState.value.seqIsRecording && pressedNotes[notes[indexToRepeat - wrapIndex].pitch].first)
                                ) {
                                    if (!erasing(kmmk,true, indexToRepeat - wrapIndex, false)) break
                                } else {
                                    if(indexToRepeat != draggedNoteOffIndex) {
                                        playing(kmmk, indexToRepeat - wrapIndex, !anyChannelIsSoloed)
                                        // StepView case:
                                        if (indexToRepeat == draggedNoteOnIndex) {
                                            playingDraggedNoteOffInTime(uiState.value.factorBpm, kmmk)
                                        }
                                    }
                                    indexToRepeat++
                                }
                            }
                        }
                    }
                }
                _uiState.update { a -> a.copy(
                    visualArrayRefresh = !uiState.value.visualArrayRefresh,
                ) }
                delay(3000.nanoseconds)
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

    private fun SeqUiState.stopNotesOnChannel(c: Int, mode: StopNotesMode) {
        for (p in 0..127) {
            while (sequences[c].playingNotes[p] > 0) {
                kmmk.noteOn(c, p, 0)
//                Log.d("ryjtyj", "stopNotesOnChannel")
                sequences[c].changePlayingNotes(p, 0)
            }
            if (seqIsRecording && sequences[c].pressedNotes[p].first) {
                sequences[c].recordNote(
                    pitch = p,
                    velocity = 0,
                    id = sequences[c].pressedNotes[p].second,
                    staticNoteOffTime = staticNoteOffTime,
                    seqIsPlaying = seqIsPlaying,
                    isRepeating = isRepeating,
                    customTime = if (mode == END_OF_REPEAT) sequences[c].repeatEndTime.toInt() else -1
                )
                if (mode == END_OF_REPEAT) sequences[c].pressedNotes[p] = Pair(true, sequences[c].noteId) // updating noteID for new note in beatRepeat (wrapAround case)
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
                        if (sequences[c].pressedNotes[p].first) {
                            kmmk.noteOn(c, p, 0)
                            sequences[c].recordNote(
                                pitch = p,
                                velocity = 0,
                                id = sequences[c].pressedNotes[p].second,
                                staticNoteOffTime = staticNoteOffTime,
                                seqIsPlaying = seqIsPlaying,
                                isRepeating = isRepeating,
                            )
                            sequences[c].pressedNotes[p] = Pair(false, Int.MAX_VALUE)
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
                    if(channel == 0) {
                        anyChannelIsSoloed = (uiState.value.sequences.any{ it.isSoloed })
                    }
                    isSoloed = !anyChannelIsSoloed
                    if(channel == 15) anyChannelIsSoloed = !anyChannelIsSoloed
                } else {   // single Pad
                    isSoloed = !isSoloed
                    if(isSoloed) {
                        anyChannelIsSoloed = true
                        for (c in 0..15) {
                            if (!uiState.value.sequences[c].isSoloed) uiState.value.stopNotesOnChannel(c, MUTE)
                        }
                    } else anyChannelIsSoloed = (uiState.value.sequences.any{ it.isSoloed })
                }
            }
        }
    }


    private fun muteChannel(channel: Int, velocity: Int, elapsedTime: Long, allButton: Boolean) {
        with(uiState.value.sequences[channel]) {
            val pressOrLongRelease = velocity > 0 || (System.currentTimeMillis() - elapsedTime) > toggleTime

            if(pressOrLongRelease) {
                if(allButton) {
                    if(channel == 0) {
                        anyChannelIsMuted = (uiState.value.sequences.find{ it.isMuted } != null)
                    }
                    isMuted = !anyChannelIsMuted
                } else {   // single Pad
                    isMuted = !isMuted
                    if (isMuted) uiState.value.stopNotesOnChannel(channel, MUTE)
                }
            }
        }
    }


    private fun enableEraseOnChannel(channel: Int, velocity: Int) {
        uiState.value.sequences[channel].isErasing = velocity > 0
    }


    fun repeat(divisor: Int) {
        stopChannels(MUTE)
        if(divisor != uiState.value.divisorState && divisor != 0) {
            val repeatLength = 2000.0 / divisor
            for(c in 0..15) {
                with(uiState.value.sequences[c]) {
                    repeatEndTime = deltaTime
                    deltaTimeRepeat = deltaTime - repeatLength
                    repeatStartTime = if(deltaTimeRepeat < 0) deltaTimeRepeat + totalTime else deltaTimeRepeat

                    indexToStartRepeating = searchIndexInTimeBoundaries()
                    indexToRepeat = indexToStartRepeating
                    repeatsCount = 1
                    savedRepeatsCount = repeatsCount
                    wrapDelta = 0

                    for (p in 0..127) {
                        val hangingNoteOffIndex = notes.indexOfFirst { it.pitch == p && it.time > if(deltaTimeRepeat < 0) deltaTimeRepeat + totalTime else deltaTimeRepeat }
                        if (hangingNoteOffIndex != -1 && notes[hangingNoteOffIndex].velocity == 0) {
                            if(c == 0) Log.d("ryjtyj", "hangingNoteOffIndex in repeat() = $hangingNoteOffIndex")
                            kmmk.noteOn(c, p, 100)
                            changePlayingNotes(p, 100)
                        }
                    }
                    if(c == 0) Log.d("ryjtyj", "indexToStartRepeating = $indexToStartRepeating")
                }
            }
            _uiState.update { a -> a.copy( repeatLength = repeatLength ) }
        }
        _uiState.update { a -> a.copy(
            isRepeating = divisor > 0,
            divisorState = divisor,
            sequences = uiState.value.sequences
        ) }
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
                            uiState.value.sequences[c].clearChannel(c, kmmk)
                        }
                        loadPattern(pattern = channel)
                    }
                }
                SOLOING -> soloChannel(channel, velocity, elapsedTime, allButton)
                MUTING -> muteChannel(channel, velocity, elapsedTime, allButton)
                ERASING -> enableEraseOnChannel(channel, velocity)
                CLEARING -> {
                    if(velocity > 0) clearChannel(channel, kmmk) // TODO implement UNDO CLEAR
                }
                // PLAYING & RECORDING
                else -> {
                    val retrigger = playingNotes[pitch] > 0 && velocity > 0
                    if (retrigger) {
                        kmmk.noteOn(channel, pitch, 0)    // allows to retrigger already playing notes
                    }
                    kmmk.noteOn(channel, pitch, velocity)
                    pressedNotes[pitch] = Pair(velocity > 0, if(velocity > 0) noteId else pressedNotes[pitch].second)

                    if (uiState.value.seqIsRecording) {
                        if(velocity > 0) {
                            recordNote(
                                pitch = pitch,
                                velocity = velocity,
                                id = noteId,
                                staticNoteOffTime = staticNoteOffTime,
                                seqIsPlaying = uiState.value.seqIsPlaying,
                                isRepeating = uiState.value.isRepeating,
                            )
                            increaseNoteId()
                        } else {
                            recordNote(
                                pitch = pitch,
                                velocity = velocity,
                                id = pressedNotes[pitch].second,
                                staticNoteOffTime = staticNoteOffTime,
                                seqIsPlaying = uiState.value.seqIsPlaying,
                                isRepeating = uiState.value.isRepeating,
                            )
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


    fun quantizeTime(time: Int): Int {
        return if(uiState.value.isQuantizing) {
            val remainder = time % quantizationTime
//            if(remainder > quantizationTime / 2)
//                (time - remainder + quantizationTime).toInt()
//            else
//                (time - remainder).toInt()
            (time - remainder).toInt()
        } else time
    }

    private fun quantizeChannel(channel: Int) {
        with(uiState.value.sequences[channel]) {
            for(i in notes.indices) {
                if(notes[i].velocity > 0) {
                    val tempTime = notes[i].time
                    val time = quantizeTime((notes[i].time + quantizationTime / 2).toInt())
                    changeNoteTime(i, time)
                    changeNoteTime(returnPairedNoteOffIndexAndTime(i).first, time - tempTime, true)
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
//        Log.d("ryjtyj", "rememberInteraction: channel = $channel, pitch = $pitch, interaction = $interaction")
        interactionSources[channel][pitch] = Pair(interactionSources[channel][pitch].first, interaction)
    }

    fun cancelInteractionWhenSwitchingViews() {
//        CoroutineScope(Dispatchers.Main).launch {
        runBlocking {
            for(c in 0..15) {
                for (p in 0..127) {
                    if(uiState.value.sequences[c].pressedNotes[p].first) {
//                        Log.d("ryjtyj", "cancelInteraction: channel = $c, pitch = $p, interaction = ${interactionSources[c][p].second}")
                        interactionSources[c][p].first.emit(PressInteraction.Cancel (interactionSources[c][p].second))
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