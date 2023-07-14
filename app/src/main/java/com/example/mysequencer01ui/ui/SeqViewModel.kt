package com.example.mysequencer01ui.ui

import android.util.Log
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModel
import com.example.mysequencer01ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.mysequencer01ui.PadsMode.*
import com.example.mysequencer01ui.StopNotesMode.*
import com.example.mysequencer01ui.data.Patterns
import com.example.mysequencer01ui.data.SeqDao
import kotlin.time.Duration.Companion.nanoseconds


class SeqViewModel(private val kmmk: KmmkComponentContext, private val dao: SeqDao) : ViewModel() {

    private val _uiState = MutableStateFlow(SeqUiState())
    val uiState: StateFlow<SeqUiState> = _uiState.asStateFlow()

    var toggleTime = 150
    var staticNoteOffTime = 100 // TODO replace with quantization time?
    var quantizationTime = 240000 / uiState.value.bpm / uiState.value.quantizationValue * uiState.value.factorBpm
    private var allChannelsMuted = false
    private var previousPadsMode: PadsMode = DEFAULT

    private var patterns: Array<Array<Array<Note>>> = Array(16){ Array(16) { emptyArray() } }

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
//                            length = note.length
                        }
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
            uiState.value.sequences[c].apply { startTimeStamp = System.currentTimeMillis(); deltaTime = 0.0; indexToPlay = 0 }
        }
        _uiState.update { a -> a.copy( sequences = uiState.value.sequences ) }

        if (uiState.value.seqIsPlaying) return // needed for implementing ReStart()


        _uiState.update { a -> a.copy( seqIsPlaying = true ) }
        var clockTicks = 0
        kmmk.startClock()

        CoroutineScope(Dispatchers.Main).launch {
            while (uiState.value.seqIsPlaying) {
                for(c in 0..15) {
                    with(uiState.value.sequences[c]){

                        // UPDATE DELTATIME
                        deltaTime = (System.currentTimeMillis() - startTimeStamp) * uiState.value.factorBpm

                        // NORMAL ERASING or PLAYING
                        while(notes.size > indexToPlay && notes[indexToPlay].time <= deltaTime) {
                            if(!uiState.value.isRepeating && (isErasing || (uiState.value.seqIsRecording && pressedNotes[notes[indexToPlay].pitch].first))) {
                                if (!erasing(kmmk,false, indexToPlay)) break
                            } else {
                                if(!uiState.value.isRepeating && indexToPlay != draggedNoteOffIndex) {
                                    playing(kmmk, indexToPlay)
                                    // StepView case:
                                    playingDraggedNoteOffInTime(indexToPlay)
                                }
                                indexToPlay++
                            }
                        }

                        // CLOCK
                        if(uiState.value.transmitClock && c == 0) {
                            if(deltaTime >= uiState.value.timingClock * clockTicks) {
                                kmmk.sendTimingClock()
                                clockTicks++
                            }
                        }

                        // END OF SEQUENCE
                        if (deltaTime >= totalTime) {
                            startTimeStamp = System.currentTimeMillis() - (deltaTime - totalTime).toLong()
                            indexToPlay = 0
                            clockTicks = 0
                        }

                        /** IF REPEATING **/
                        if(uiState.value.isRepeating) {

                            // update deltaTimeRepeat
                            deltaTimeRepeat = deltaTime - uiState.value.repeatLength * repeatsCount

                            // end of repeating loop
                            if (deltaTimeRepeat + wrapDelta >= repeatEndTime) {
                                uiState.value.stopNotesOnChannel(c, END_OF_REPEAT)   // notesOFF on the end of loop
                                indexToRepeat = indexToStartRepeating

                                if(repeatsCount == uiState.value.divisorState) repeatsCount = 0
                                repeatsCount++
                                deltaTimeRepeat = deltaTime - uiState.value.repeatLength * repeatsCount
                            }
                            wrapDelta = if(deltaTime < repeatEndTime) totalTime else 0

                            val wrapTime: Int
                            val wrapIndex: Int
                            if(deltaTimeRepeat + wrapDelta < 0) {
                                wrapTime = totalTime
                                wrapIndex = if(indexToStartRepeating > 0) notes.size else 0
                            } else {
                                wrapTime = 0
                                wrapIndex = 0
                            }

                            // notesON on the start of loop if notes are being held // TODO move higher than wrapAround? ^^
                            if(repeatsCount != savedRepeatsCount) {
                                for (p in 0..127) {
                                    if(pressedNotes[p].first) { // TODO check for possible conflict of two notes with same id
                                        kmmk.noteOn(c, p, 100)
                                        if(uiState.value.seqIsRecording) recordNote(
                                            pitch = p,
                                            velocity = 100,
                                            id = pressedNotes[p].second,
                                            staticNoteOffTime = staticNoteOffTime,
                                            seqIsPlaying = true,
                                            isRepeating = true,
                                            repeatLength = (repeatEndTime - uiState.value.repeatLength)  // TODO wrapAround additional var if Result_of_subtracting < 0 ??
                                        )
                                    }
                                }
                                savedRepeatsCount = repeatsCount
                            }

                            // erasing or playing
                            while(
                                notes.size > (indexToRepeat - wrapIndex)
                                && (notes[indexToRepeat - wrapIndex].time <= (deltaTimeRepeat + wrapDelta + wrapTime))
                                && (notes[indexToRepeat - wrapIndex].time >= repeatEndTime - uiState.value.repeatLength + wrapTime)
                            ) {
                                if(isErasing || (uiState.value.seqIsRecording && pressedNotes[notes[indexToRepeat - wrapIndex].pitch].first)
                                ) {
                                    if (!erasing(kmmk,true, indexToRepeat - wrapIndex)) break
                                } else {
                                    if(indexToRepeat != draggedNoteOffIndex) {
                                        playing(kmmk, indexToRepeat - wrapIndex)
                                        // StepView case:
                                        playingDraggedNoteOffInTime(indexToRepeat)
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

    private fun Sequence.playingDraggedNoteOffInTime(indexToPlayOrRepeat: Int) {
        if (indexToPlayOrRepeat == draggedNoteOnIndex) {
            val delayTime = if (draggedNoteOnIndex < draggedNoteOffIndex) {
                notes[draggedNoteOffIndex].time - notes[draggedNoteOnIndex].time
            } else {
                notes[draggedNoteOffIndex].time - notes[draggedNoteOnIndex].time + totalTime
            }
            val pitchTemp = notes[draggedNoteOffIndex].pitch
            CoroutineScope(Dispatchers.Default).launch {
                delay((delayTime / uiState.value.factorBpm).toLong())
                kmmk.noteOn(channel, pitchTemp, 0)
                changePlayingNotes(pitchTemp, 0)
                channelIsPlayingNotes = false // TODO !!!
            }
        }
    }


    fun stopSeq() {
        stopNotes(STOPSEQ)
        kmmk.stopClock()
        _uiState.update { a ->
            a.copy(
                sequences = uiState.value.sequences, // TODO needed?
                seqIsPlaying = false
            )
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
                                repeatLength = repeatLength,
                            )
                            sequences[c].pressedNotes[p] = Pair(false, Int.MAX_VALUE)
                        }
                    }
                }
            }
        }
        _uiState.update { a -> a.copy( seqIsRecording = !uiState.value.seqIsRecording ) }
    }


    private fun muteChannel(channel: Int, velocity: Int, elapsedTime: Long, allButton: Boolean) {
        with(uiState.value.sequences[channel]) {
            val pressOrLongRelease = velocity > 0 || (System.currentTimeMillis() - elapsedTime) > toggleTime
            if(allButton) {
                if(channel == 0) {
                    allChannelsMuted = (uiState.value.sequences.find{ !it.isMuted } == null)
                }
                isMuted = if(allChannelsMuted){
                    when{
                        pressOrLongRelease -> false
                        else -> return
                    }
                } else {
                    when {
                        pressOrLongRelease -> {stopNotes(MUTE); true}
                        else -> return
                    }
                }
            } else   // single Pad
                if(pressOrLongRelease) {
                    if(!isMuted) uiState.value.stopNotesOnChannel(channel, MUTE)
                    isMuted = !isMuted
                } else return
        }
    }


    private fun enableEraseOnChannel(channel: Int, velocity: Int) {
        uiState.value.sequences[channel].isErasing = velocity > 0
        uiState.value.sequences[channel].channelIsPlayingNotes = velocity > 0
    }


    fun repeat(divisor: Int) {
        stopNotes(MUTE)
        if(divisor != uiState.value.divisorState && divisor != 0) {
            val repeatLength = 2000.0 / divisor
            for(c in 0..15) {   // TODO move into Sequence class?
                with(uiState.value.sequences[c]) {
                    repeatEndTime = deltaTime
                    deltaTimeRepeat = deltaTime - repeatLength
                    val repeatStartTime = if(deltaTimeRepeat < 0) deltaTimeRepeat + totalTime else deltaTimeRepeat

                    var index = notes.indexOfFirst { (it.time >= repeatStartTime && it.time < repeatEndTime && it.velocity > 0) }
                    // TODO instead of ^^ (notes.indexOfFirst) ->    index = notes.copyRange(0, indexToPlay).indexOfLast(it.time < timeOfRepeatStart) + 1
                    if(index == -1 && repeatStartTime < 0) {
                        index = notes.indexOfFirst { (it.time >= repeatStartTime && it.time < totalTime && it.velocity > 0) }
                    }
                    if(index == -1 && repeatStartTime < 0) {
                        index = notes.indexOfFirst { (it.time < (totalTime - repeatStartTime) && it.velocity > 0) }
                    }
                    indexToStartRepeating = if(index > -1) index else indexToPlay
                    indexToRepeat = indexToStartRepeating
                    repeatsCount = 1
                    savedRepeatsCount = repeatsCount
                    wrapDelta = 0
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
                MUTING -> muteChannel(channel, velocity, elapsedTime, allButton)
                ERASING -> enableEraseOnChannel(channel, velocity)
                CLEARING -> {
                    if(velocity > 0) clearChannel(channel, kmmk)
                    else channelIsPlayingNotes = false // TODO implement UNDO CLEAR, move {channelIsPlayingNotes = false} into IF?
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
                QUANTIZING -> {
                    if(velocity > 0) {
                        val tempIsQuantizing = uiState.value.isQuantizing
                        _uiState.update { a -> a.copy(isQuantizing = true) }
                        quantizeChannel(channel)
                        _uiState.update { a -> a.copy(isQuantizing = tempIsQuantizing) }
                    }
                }
                SELECTING -> {
                    if(!allButton && velocity > 0) {
                        _uiState.update { a -> a.copy(selectedChannel = channel) }
                    }
                }
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
                                repeatLength = uiState.value.repeatLength
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
                                repeatLength = uiState.value.repeatLength
                            )
                        }
                    }
                    if(!retrigger) changePlayingNotes(pitch, velocity)
                    _uiState.update { a -> a.copy(selectedChannel = channel) }
                }
            }
        }.also { if(!allButton) updateSequencesUiState() } // allButton recomposes by itself // TODO is it still needed here?
    }


    private fun stopNotes(mode: StopNotesMode) {
        for (c in 0..15) {
            uiState.value.stopNotesOnChannel(c, mode)
            if(mode == STOPSEQ) {
                uiState.value.sequences[c].deltaTime = 0.0
            }
        }
    }

    private fun SeqUiState.stopNotesOnChannel(c: Int, mode: StopNotesMode) {
        for (p in 0..127) {
            while (sequences[c].playingNotes[p] > 0) {
                kmmk.noteOn(c, p, 0)
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
                    repeatLength = repeatLength,
                    customTime = if (mode == END_OF_REPEAT) sequences[c].repeatEndTime.toInt() else -1
                )
                //sequences[c].pressedNotes[p] = false
            }
        }
        sequences[c].channelIsPlayingNotes = false // TODO
    }

    fun stopAllNotes() {
        for (c in 0..15) {
            for (p in 0..127) {
                kmmk.noteOn(c, p, 0)
            }
        }
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
                        indexToRepeat = notes.indexOfLast { it.time < deltaTimeRepeat / uiState.value.factorBpm } + 1
                    }
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


    fun editCurrentPadsMode(mode: PadsMode, momentary: Boolean = false){
        if(mode != uiState.value.padsMode) {
            previousPadsMode = uiState.value.padsMode
            _uiState.update { a -> a.copy(padsMode = mode) }
        } else if(momentary) {
            _uiState.update { a -> a.copy(padsMode = previousPadsMode) }
        } else {
            _uiState.update { a -> a.copy(padsMode = DEFAULT) }
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
        _uiState.update { a -> a.copy (
            bpm = bpmPointOne,
            factorBpm = bpmPointOne / 120.0,
            timingClock = 500.0 / 24.0 * bpmPointOne / 120.0
        ) }

        Log.d("emptyTag", " ") // to hold in imports
    }

    fun switchClockTransmitting() {
        _uiState.update { it.copy(
            transmitClock = !uiState.value.transmitClock
        ) }
    }

    fun switchLazyKeyboard() {
        _uiState.update { it.copy(
            lazyKeyboard = !uiState.value.lazyKeyboard
        ) }
    }

}