package com.example.mysequencer01ui.ui

import androidx.lifecycle.ViewModel
import com.example.mysequencer01ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.mysequencer01ui.PadsMode.*


class SeqViewModel(private val kmmk: KmmkComponentContext) : ViewModel() {

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

                        deltaTime = (System.currentTimeMillis() - startTimeStamp).toInt()
                        factoredDeltaTime = deltaTime * uiState.value.factorBpm

                        while(notes.size > indexToPlay && notes[indexToPlay].time <= factoredDeltaTime) {
                            if(isErasing) {
                                if (!erasing()) break
                            } else {
                                playing(kmmk)
                                indexToPlay++
                            }
                        }

                        if (factoredDeltaTime >= totalTime) {
                            startTimeStamp = System.currentTimeMillis()
                            indexToPlay = 0
                        }
                    }
                }
                _uiState.update { a -> a.copy(
                    visualArrayRefresh = !uiState.value.visualArrayRefresh,
                    //sequences = uiState.value.sequences
                ) }
                delay(1L)
            }
        }
    }


    fun stopSeq() {
        for(c in 0..15){
            for(p in 0..127){
                if(uiState.value.sequences[c].noteOnStates[p]) {
                    kmmk.noteOn(c, p, 0) // TODO try to remove this line and see whether note still shuts OFF
                    // TODO Properly record noteOFF for held note, don't record noteOFF onRelease // doesn't work now bc of pressedNotes
                    if(uiState.value.seqIsRecording && uiState.value.sequences[c].pressedNotes[p]) uiState.value.sequences[c].recordNote(
                        p,
                        0,
                        staticNoteOffTime,
                        uiState.value.seqIsPlaying,
                        uiState.value.factorBpm
                    )
                    uiState.value.sequences[c].noteOnStates[p] = false
                }
            }
            uiState.value.sequences[c].channelIsPlayingNotes = false
            uiState.value.sequences[c].deltaTime = 0
            uiState.value.sequences[c].factoredDeltaTime = 0.0
        }

        _uiState.update { a ->
            a.copy(
                sequences = uiState.value.sequences,
                seqIsPlaying = false
            )
        }
    }


    fun changeRecState() {
        _uiState.update { a -> a.copy(seqIsRecording = !uiState.value.seqIsRecording) }
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
                        pressOrLongRelease -> true
                        else -> return
                    }
                }
            } else   // single Pad
                if(pressOrLongRelease) {
                    isMuted = !isMuted
                } else return
        }
    }


    private fun enableEraseOnChannel(channel: Int, velocity: Int) {
        uiState.value.sequences[channel].isErasing = velocity > 0
        uiState.value.sequences[channel].channelIsPlayingNotes = velocity > 0
    }


    fun repeat(time: Int) {
        // totalTime, factorBpm
        _uiState.update { a -> a.copy(
            isRepeating = time > 0,
            repeatTime = time,
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
                    else channelIsPlayingNotes = false // TODO implement UNDO CLEAR
                }
                else -> {
                    if (uiState.value.seqIsRecording) {
                        recordNote(
                            pitch,
                            velocity,
                            staticNoteOffTime,
                            uiState.value.seqIsPlaying,
                            uiState.value.factorBpm
                        )
                        channelIsPlayingNotes = velocity > 0
                    } else {
                        if (noteOnStates[pitch] && velocity > 0) {
                            kmmk.noteOn(channel, pitch, 0)  // allows to retrigger already playing notes
                        }
                        kmmk.noteOn(channel, pitch, velocity)
                        pressedNotes[pitch] = velocity > 0
                        noteOnStates[pitch] = velocity > 0
                    }
                    _uiState.update { a -> a.copy(selectedChannel = channel) }
                }
            }
        }.also { if(!allButton) recomposeVisualArray() }
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

        /*
        val factor: Double = uiState.value.bpm.toDouble() / bpmPointOne
        for(c in 0..15) {
            with(uiState.value.sequences[c]) {
                deltaTime = ((deltaTime * factor) + 0.5).toInt()
                totalTime = ((totalTime * factor) + 0.5).toInt()
                for(i in notes.indices) {
                    notes[i].time = ((notes[i].time * factor) + 0.5).toInt()
                }
            }
        }
        _uiState.update { a -> a.copy( bpm = bpmPointOne, sequences = uiState.value.sequences ) }
         */
    }

}