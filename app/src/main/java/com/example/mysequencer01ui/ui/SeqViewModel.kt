package com.example.mysequencer01ui.ui

import android.util.Log
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
            uiState.value.sequences[c].apply { startTimeStamp = System.currentTimeMillis(); deltaTime = 0; indexToPlay = 0 }
        }
        _uiState.update { a -> a.copy( sequences = uiState.value.sequences ) }

        if (uiState.value.seqIsPlaying) return // needed for implementing ReStart()


        _uiState.update { a -> a.copy( seqIsPlaying = true ) }

        CoroutineScope(Dispatchers.Main).launch {
            while (uiState.value.seqIsPlaying) {
                for(c in 0..15) {
                    with(uiState.value.sequences[c]){

                        // UPDATE DELTATIME
                        deltaTime = (System.currentTimeMillis() - startTimeStamp).toInt()  // TODO remove deltaTime, rename factoredDT & fDTRepeat
                        factoredDeltaTime = deltaTime * uiState.value.factorBpm

                        // NORMAL ERASING or PLAYING
                        while(notes.size > indexToPlay && notes[indexToPlay].time <= factoredDeltaTime) {
                            if(isErasing || (uiState.value.seqIsRecording && pressedNotes[notes[indexToPlay].pitch])) {
                                if(!uiState.value.isRepeating) {
                                    if (!erasing(false, indexToPlay)) break
                                }
                            } else {
                                if(!uiState.value.isRepeating) playing(kmmk, indexToPlay)
                                indexToPlay++
                            }
                        }

                        // END OF SEQUENCE
                        if (factoredDeltaTime >= totalTime) {
                            startTimeStamp = System.currentTimeMillis() - (factoredDeltaTime - totalTime).toLong()
                            indexToPlay = 0
                        }

                        /** IF REPEATING **/
                        if(uiState.value.isRepeating) {

                            // update deltaTimeRepeat
                            factoredDeltaTimeRepeat = factoredDeltaTime - uiState.value.repeatLength * repeatsCount

                            // end of repeating loop
                            if (factoredDeltaTimeRepeat + wrapDelta >= repeatEndTime) {
                                stopNotes(END_OF_REPEAT)   // notesOFF on the end of loop
                                indexToRepeat = indexToStartRepeating

                                if(repeatsCount == uiState.value.divisorState) repeatsCount = 0
                                repeatsCount++
                                factoredDeltaTimeRepeat = factoredDeltaTime - uiState.value.repeatLength * repeatsCount
                            }
                            wrapDelta = if(factoredDeltaTime < repeatEndTime) totalTime else 0

                            val wrapTime: Int
                            val wrapIndex: Int
                            if(factoredDeltaTimeRepeat + wrapDelta < 0) {
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

                            // erasing or playing
                            while(
                                notes.size > (indexToRepeat - wrapIndex)
                                && (notes[indexToRepeat - wrapIndex].time <= (factoredDeltaTimeRepeat + wrapDelta + wrapTime))
                                && (notes[indexToRepeat - wrapIndex].time >= repeatEndTime - uiState.value.repeatLength + wrapTime)
                            ) {
                                if(isErasing
//                                    || (uiState.value.seqIsRecording && pressedNotes[notes[indexToRepeat - wrapIndex].pitch])  // TODO
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
            stopNotes(RECOFF_OR_MUTE)
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
                        pressOrLongRelease -> {stopNotes(RECOFF_OR_MUTE); true}
                        else -> return
                    }
                }
            } else   // single Pad
                if(pressOrLongRelease) {
                    if(!isMuted) stopNotes(RECOFF_OR_MUTE)
                    isMuted = !isMuted
                } else return
        }
    }


    private fun enableEraseOnChannel(channel: Int, velocity: Int) {
        uiState.value.sequences[channel].isErasing = velocity > 0
        uiState.value.sequences[channel].channelIsPlayingNotes = velocity > 0
    }


    fun repeat(divisor: Int) {
        stopNotes(RECOFF_OR_MUTE)
        val repeatLength = if(divisor > 0) { (2000.0 / divisor) } else 0.0
        if(repeatLength > 0.0) {
            for(c in 0..15) {   // TODO move into Sequence class?
                with(uiState.value.sequences[c]) {
                    repeatEndTime = factoredDeltaTime
                    var repeatStartTime = factoredDeltaTime - repeatLength
                    if (repeatStartTime < 0) repeatStartTime += totalTime

                    var index = notes.indexOfFirst { (it.time >= repeatStartTime && it.velocity > 0) }
                    // TODO instead of ^^ (notes.indexOfFirst) ->    index = notes.copyRange(0, indexToPlay).indexOfLast(it.time < timeOfRepeatStart) + 1
                    // TODO use indexToStartRepeating instead of additional var index?
                    if(index == -1 && repeatStartTime < 0) index = notes.indexOfFirst { (
                            it.time < (totalTime - repeatStartTime) && // TODO unnecessary? Just grab the first noteON because if it is out of repeating range it just won't play?
                                it.velocity > 0) }
                    indexToStartRepeating = if(index > -1) index else indexToPlay
                    indexToRepeat = indexToStartRepeating
                    repeatsCount = 1
                }
            }
        } else {
            for(c in uiState.value.sequences.indices) {
                uiState.value.sequences[c].wrapDelta = 0
                uiState.value.sequences[c].savedRepeatsCount = 0
            }
        }
        _uiState.update { a -> a.copy(
            isRepeating = repeatLength > 0,
            divisorState = divisor,
            repeatLength = repeatLength,
            sequences = uiState.value.sequences // TODO unnecessary?
        ) }
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
                LOADING -> { if(velocity > 0) loadPattern(pattern = channel) }
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
        with(uiState.value) {
            for (c in 0..15) {
                for (p in 0..127) {
                    if (sequences[c].playingNotes[p]) {
                        sequences[c].playingNotes[p] = false
                        kmmk.noteOn(c, p, 0)
                    }
                    if (seqIsRecording && sequences[c].pressedNotes[p]) sequences[c].recordNote(
                        p,
                        0,
                        staticNoteOffTime,
                        seqIsPlaying,
                        isRepeating,
                        uiState.value.repeatLength,
                        if(mode == END_OF_REPEAT) sequences[c].repeatEndTime.toInt() else -1
                    )
                }
                sequences[c].channelIsPlayingNotes = false
                if(mode == STOPSEQ) {
                    sequences[c].deltaTime = 0
                    sequences[c].factoredDeltaTime = 0.0
                }
            }
        }
    }

    fun editCurrentMode(mode: PadsMode, momentary: Boolean = false){
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

    fun changeBPM(bpm: Float) {
        val bpmPointOne = (bpm * 10 + 0.5).toInt() / 10f
        _uiState.update { a -> a.copy (
            bpm = bpmPointOne,
            factorBpm = bpmPointOne / 120.0
        ) }

        Log.d("emptyTag", " ") // to hold in imports
    }

}