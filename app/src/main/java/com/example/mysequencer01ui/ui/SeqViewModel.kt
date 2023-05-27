package com.example.mysequencer01ui.ui

import androidx.lifecycle.ViewModel
import com.example.mysequencer01ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.mysequencer01ui.SeqModes.*


class SeqViewModel(private val kmmk: KmmkComponentContext) : ViewModel() {

    /** UI state exposed to the UI **/
    private val _uiState = MutableStateFlow(SeqUiState())
    // Backing property to avoid state updates from other classes
    val uiState: StateFlow<SeqUiState> = _uiState.asStateFlow()
    // The asStateFlow() makes this mutable state flow a read-only state flow

    var toggleTime = 150L
    var staticNoteOffTime = 1L
    private var allChannelsMuted = false
    private var coroutineScope = CoroutineScope(Dispatchers.Main)
    var sequences = MutableList(16){ Sequence(uiState.value.bpm) }
    private var currentMode = mutableListOf<SeqModes>()


    fun startSeq() {
        _uiState.update { a ->
            a.copy(
                seqStartTime = Array(16){ System.currentTimeMillis() },
                deltaTime = Array(16){ 0L }
            )
        }

        for(c in sequences.indices) {
            sequences[c].apply { startTimeStamp = System.currentTimeMillis(); deltaTime = 0L; indexToPlay = 0 }
        }

        if (uiState.value.seqIsPlaying) return

        _uiState.update { a -> a.copy(seqIsPlaying = true) }

        coroutineScope.launch {
            while (uiState.value.seqIsPlaying) {
                for(c in 0..15) {
                    with(sequences[c]){

                        deltaTime = System.currentTimeMillis() - startTimeStamp

                        while(notes.size > indexToPlay && notes[indexToPlay].time <= deltaTime) {
                            if(isErasing) {
                                if (!erasing()) break
                            } else {
                                playing(kmmk)
                                _uiState.value.channelIsActive[c] = notes[indexToPlay].velocity > 0
                                indexToPlay++
                            }
                        }

                        if (deltaTime >= totalTime) {
                            startTimeStamp = System.currentTimeMillis()
                            indexToPlay = 0
                        }
                        _uiState.value.deltaTime[c] = deltaTime
                    }
                }
                _uiState.update { a -> a.copy(
                    deltaTime = uiState.value.deltaTime,
                    visualArrayRefresh = !uiState.value.visualArrayRefresh
                ) }
                delay(1L)
            }
        }
    }


    fun stopSeq(){
        for(c in 0..15){
            for(p in 0..127){
                if(sequences[c].noteOnStates[p]) {
                    kmmk.noteOn(c, p, 0)
                    sequences[c].noteOnStates[p] = false
                }
            }
            _uiState.value.channelIsActive[c] = false
        }

        _uiState.update { a ->
            a.copy(
                deltaTime = Array(16){ 0L },
                seqIsPlaying = false
            )
        }
    }

    fun recMode(){
        //editCurrentMode(uiState.value.seqIsRecording, RECORDING)
        _uiState.update { a -> a.copy (seqIsRecording = !uiState.value.seqIsRecording) }
    }

    fun muteMode(){
        editCurrentMode(uiState.value.muteButtonState, MUTING)
        _uiState.update { a -> a.copy( muteButtonState = !uiState.value.muteButtonState ) }
    }

    fun eraseMode(){
        editCurrentMode(uiState.value.eraseButtonState, ERASING)
        _uiState.update { a -> a.copy( eraseButtonState = !uiState.value.eraseButtonState ) }
    }

    fun clearMode(){
        editCurrentMode(uiState.value.clearButtonState, CLEARING)
        _uiState.update { a -> a.copy( clearButtonState = !uiState.value.clearButtonState ) }
    }

    private fun editCurrentMode(buttonState: Boolean, mode: SeqModes){
        if(buttonState) currentMode.remove(mode) else currentMode.add(mode)
        _uiState.update { a -> a.copy(
            seqMode = if(currentMode.isNotEmpty()) currentMode.last() else DEFAULT
        ) }
    }


    private fun muteChannel(channel: Int, velocity: Int, elapsedTime: Long, allButton: Boolean) {
        with(sequences[channel]) {
            if(allButton) {
                if(channel == 0) {
                    allChannelsMuted = (sequences.find{ !it.isMuted } == null)
                }
                isMuted = if(allChannelsMuted){
                    when{
                        velocity > 0 || (System.currentTimeMillis() - elapsedTime) > toggleTime -> false
                        else -> return
                    }
                } else {
                    when {
                        velocity > 0 || (System.currentTimeMillis() - elapsedTime) > toggleTime -> true
                        else -> return
                    }
                }
            } else
                if(velocity > 0 || (System.currentTimeMillis() - elapsedTime > toggleTime)) {
                    isMuted = !isMuted
                } else return
        }
    }


    private fun enableEraseOnChannel(channel: Int, velocity: Int) {
        sequences[channel].isErasing = velocity > 0
        _uiState.value.channelIsActive[channel] = velocity > 0
    }


    fun pressPad(channel: Int, pitch: Int, velocity: Int, elapsedTime: Long = 0, allButton: Boolean = false) {
        when(uiState.value.seqMode) {
            MUTING -> muteChannel(channel, velocity, elapsedTime, allButton)
            ERASING -> enableEraseOnChannel(channel, velocity)
            CLEARING -> {
                if(velocity > 0) sequences[channel].clearChannel(channel, kmmk)
                else _uiState.value.channelIsActive[channel] = false // TODO implement UNDO CLEAR
            }
            else -> if(uiState.value.seqIsRecording) {
                sequences[channel].recordNote(channel, pitch, velocity, staticNoteOffTime, uiState.value.seqIsPlaying)
                _uiState.value.channelIsActive[channel] = velocity > 0
            } else kmmk.noteOn(channel, pitch, velocity)
        }.also {
            _uiState.update { a -> a.copy(
                visualArray = sequences,
                visualArrayRefresh = !uiState.value.visualArrayRefresh
            ) }
        }
    }

}