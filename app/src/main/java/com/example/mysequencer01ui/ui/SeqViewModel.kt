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


class SeqViewModel(private val kmmk: KmmkComponentContext, private val dao: SeqDao) : ViewModel() {

    private val _uiState = MutableStateFlow(SeqUiState())
    val uiState: StateFlow<SeqUiState> = _uiState.asStateFlow()

    var toggleTime = 150
    var staticNoteOffTime = 1
    private var allChannelsMuted = false
    private var previousPadsMode: PadsMode = DEFAULT


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

        CoroutineScope(Dispatchers.Main).launch {
            while (uiState.value.seqIsPlaying) {
                for(c in 0..15) {
                    with(uiState.value.sequences[c]){

                        // UPDATE DELTATIME
                        deltaTime = (System.currentTimeMillis() - startTimeStamp) * uiState.value.factorBpm

                        // NORMAL ERASING or PLAYING
                        while(notes.size > indexToPlay && notes[indexToPlay].time <= deltaTime) {
                            if(!uiState.value.isRepeating && (isErasing || (uiState.value.seqIsRecording && pressedNotes[notes[indexToPlay].pitch]))) {
                                if (!erasing(false, indexToPlay)) break
                            } else {
                                if(!uiState.value.isRepeating) playing(kmmk, indexToPlay)
                                indexToPlay++
                            }
                        }

                        // END OF SEQUENCE
                        if (deltaTime >= totalTime) {
                            startTimeStamp = System.currentTimeMillis() - (deltaTime - totalTime).toLong()
                            indexToPlay = 0
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
                                    if(pressedNotes[p]) {
                                        kmmk.noteOn(c, p, 100)
                                        if(uiState.value.seqIsRecording) recordNote(
                                            p,
                                            100,
                                            staticNoteOffTime,
                                            true,
                                            true,
                                            (repeatEndTime - uiState.value.repeatLength)  // TODO wrapAround additional var if Result_of_subtracting < 0 ??
                                        )
                                    }
                                }
                                savedRepeatsCount = repeatsCount
                            }

                            if(c == 0) Log.d("ryjtyj", "wrapIndex = $wrapIndex, deltaTimeRepeat = $deltaTimeRepeat, wrapDelta = $wrapDelta")
                            // erasing or playing
                            while(
                                notes.size > (indexToRepeat - wrapIndex)
                                && (notes[indexToRepeat - wrapIndex].time <= (deltaTimeRepeat + wrapDelta + wrapTime))
                                && (notes[indexToRepeat - wrapIndex].time >= repeatEndTime - uiState.value.repeatLength + wrapTime)
                            ) {
                                if(isErasing || (uiState.value.seqIsRecording && pressedNotes[notes[indexToRepeat - wrapIndex].pitch])
                                ) {
                                    if (!erasing(true, indexToRepeat - wrapIndex)) break
                                } else {
                                    playing(kmmk, indexToRepeat - wrapIndex)
                                    indexToRepeat++
                                }
                            }
                        }
                    }
                }
                _uiState.update { a -> a.copy(
                    visualArrayRefresh = !uiState.value.visualArrayRefresh,
                ) }
                delay(1L)
            }
        }
    }


    fun stopSeq() {
        stopNotes(STOPSEQ)
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
                        if (sequences[c].pressedNotes[p]) {
                            kmmk.noteOn(c, p, 0)
                            sequences[c].recordNote(
                                p,
                                0,
                                staticNoteOffTime,
                                seqIsPlaying,
                                isRepeating,
                                repeatLength,
                            )
                            sequences[c].pressedNotes[p] = false
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
                SAVING -> { if(velocity > 0) savePattern(pattern = channel, sequence = uiState.value.sequences) }
                LOADING -> { if(!allButton && velocity > 0) loadPattern(pattern = channel) }
                SELECTING -> { if(!allButton && velocity > 0) _uiState.update { a -> a.copy(selectedChannel = channel) } }
                else -> {
                    if (playingNotes[pitch] && velocity > 0) {
                        kmmk.noteOn(channel, pitch, 0)  // allows to retrigger already playing notes
                    }
                    kmmk.noteOn(channel, pitch, velocity)
                    pressedNotes[pitch] = velocity > 0

                    if (uiState.value.seqIsRecording) {
                        recordNote(
                            pitch,
                            velocity,
                            staticNoteOffTime,
                            uiState.value.seqIsPlaying,
                            uiState.value.isRepeating,
                            uiState.value.repeatLength
                        )
                    }
                    _uiState.update { a -> a.copy(selectedChannel = channel) }
                }
            }
        }.also { if(!allButton) recomposeVisualArray() } // allButton recomposes by itself // TODO is this function still needed at all?
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
            if (sequences[c].playingNotes[p]) {
                sequences[c].playingNotes[p] = false
                kmmk.noteOn(c, p, 0)
            }
            if (seqIsRecording && sequences[c].pressedNotes[p]) {
                sequences[c].recordNote(
                    p,
                    0,
                    staticNoteOffTime,
                    seqIsPlaying,
                    isRepeating,
                    repeatLength,
                    if (mode == END_OF_REPEAT) sequences[c].repeatEndTime.toInt() else -1
                )
                //sequences[c].pressedNotes[p] = false
            }
        }
        sequences[c].channelIsPlayingNotes = false
    }


    private fun savePattern(pattern: Int, sequence: MutableList<Sequence>) {
        CoroutineScope(Dispatchers.Default).launch {
            dao.deletePattern(pattern)
            for(c in sequence.indices) {
                sequence[c].notes.forEach {
                    dao.savePattern(
                        Patterns(
                            pattern = pattern,
                            channel = c,
                            time = it.time,
                            pitch = it.pitch,
                            velocity = it.velocity,
                            length = it.length
                        )
                    )
                }
            }
        }
    }

    private fun loadPattern(pattern: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            for(c in 0..15) {
                val size = dao.countNotes(pattern, c)
                _uiState.value.sequences[c].notes = Array(size) { Note(0, 0, 0, 0) }
                var advanceTime = -1
                for(i in 0 until size) {
                    val note = dao.loadPattern(pattern = pattern, channel = c, advanceTime = advanceTime)
                    _uiState.value.sequences[c].notes[i].apply {
                        time = note.time
                        pitch = note.pitch
                        velocity = note.velocity
                        length = note.length
                    }
                    advanceTime = uiState.value.sequences[c].notes[i].time
                }
            }
            _uiState.update { a -> a.copy(
                sequences = uiState.value.sequences
            ) }
        }
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

    fun recomposeVisualArray() {
        _uiState.update { a -> a.copy(
            sequences = uiState.value.sequences,
            visualArrayRefresh = !uiState.value.visualArrayRefresh
        ) }
    }

    fun showSettings() {
        _uiState.update { a -> a.copy( showSettings = !uiState.value.showSettings ) }
    }

    fun changeSeqViewState(seqView: SeqView) {
        _uiState.update { a -> a.copy(
            seqView = seqView
        ) }
    }

    fun updateNotesGridState() {
        _uiState.update { a -> a.copy(
            sequences = uiState.value.sequences
        ) }
    }

    fun changePianoRollNoteHeight(noteHeight: Dp) {
        _uiState.update { a -> a.copy( stepViewNoteHeight = noteHeight ) }
    }

    fun changeBPM(bpm: Float) {
        val bpmPointOne = (bpm * 10 + 0.5).toInt() / 10f
        _uiState.update { a -> a.copy (
            bpm = bpmPointOne,
            factorBpm = bpmPointOne / 120.0
        ) }

        Log.d("emptyTag", " ") // to hold in imports
    }

}