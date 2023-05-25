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

    var modeTime = 150L

    private var coroutineScope = CoroutineScope(Dispatchers.Main)

    private var notes = MutableList(16){ emptyArray<Note>() }

    private var indexToPlayOnChannel = Array(16){ 0 }

    private var currentMode = mutableListOf<SeqModes>()


    private fun editCurrentMode(buttonState: Boolean, mode: SeqModes){
        if(buttonState) currentMode.remove(mode) else currentMode.add(mode)
        _uiState.update { a -> a.copy(
            seqMode = if(currentMode.isNotEmpty()) currentMode.last() else DEFAULT
        ) }
    }


    fun startSeq() {
        _uiState.update { a ->
            a.copy(
                seqStartTime = Array(16){ System.currentTimeMillis() },
                deltaTime = Array(16){ 0L }
            )
        }
        if (uiState.value.seqIsPlaying) return

        indexToPlayOnChannel = Array(16){ 0 }
        //editCurrentMode(uiState.value.seqIsPlaying, PLAYING)
        _uiState.update { a -> a.copy(seqIsPlaying = true) }

        coroutineScope.launch {
            while (uiState.value.seqIsPlaying) {
                for(c in 0..15) {

                    _uiState.value.deltaTime[c] = System.currentTimeMillis() - uiState.value.seqStartTime[c]

                    if (notes[c].size > indexToPlayOnChannel[c]) {
                        for (i in indexToPlayOnChannel[c]..notes[c].lastIndex) {
                            if (notes[c][i].time <= uiState.value.deltaTime[c]) {
                                // play note
                                // update UI
                                // if (next_note.time > deltaTime) OR no other notes exist  = remember index, exit from For()

                                kmmk.noteOn( notes[c][i].channel, notes[c][i].pitch, notes[c][i].velocity )
                                updatePadState(c, notes[c][i].velocity)
                                if (((notes[c].size > i + 1) && (notes[c][i + 1].time > uiState.value.deltaTime[c])) || (notes[c].size <= i + 1)) {
                                    indexToPlayOnChannel[c] = i + 1
                                    break
                                }
                            }
                        }
                    }

                    if (uiState.value.deltaTime[c] >= uiState.value.seqTotalTime[c]) {
                        _uiState.value.seqStartTime[c] = System.currentTimeMillis()
                        indexToPlayOnChannel[c] = 0
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
        //editCurrentMode(!uiState.value.seqIsPlaying, STOPPED)
        _uiState.update { a ->
            a.copy(
                deltaTime = Array(16){ 0L },
                seqIsPlaying = false
            )
        }
    }


    fun recSeq(){
        //editCurrentMode(uiState.value.seqIsRecording, RECORDING)
        _uiState.update { a -> a.copy (seqIsRecording = !uiState.value.seqIsRecording) }
    }


    fun muteChannel(){
        editCurrentMode(uiState.value.muteButtonState, MUTING)
        _uiState.update { a -> a.copy( muteButtonState = !uiState.value.muteButtonState ) }
    }


    fun eraseNotes(){
        editCurrentMode(uiState.value.eraseButtonState, ERASING)
        _uiState.update { a -> a.copy( eraseButtonState = !uiState.value.eraseButtonState ) }
    }


    fun clearSeq(){
        editCurrentMode(uiState.value.clearButtonState, CLEARING)
        _uiState.update { a -> a.copy( clearButtonState = !uiState.value.clearButtonState ) }
    }


    private fun recordNote(channel: Int, pitch: Int, velocity: Int = 100) {

        if(uiState.value.seqIsPlaying){
            val deltaTime = System.currentTimeMillis() - uiState.value.seqStartTime[channel]
            when {
                // end of array
                indexToPlayOnChannel[channel] == notes[channel].size -> {
                    notes[channel] = notes[channel] + Note(deltaTime, channel, pitch, velocity)
                }
                // beginning of array
                notes[channel].isNotEmpty() && indexToPlayOnChannel[channel] == 0 -> {
                    val tempNotes = notes[channel]
                    notes[channel] = Array(1){Note(deltaTime, channel, pitch, velocity)} + tempNotes
                }
                // middle of array
                else -> {
                    val tempNotes1 = notes[channel].copyOfRange(0, indexToPlayOnChannel[channel])
                    val tempNotes2 = notes[channel].copyOfRange(indexToPlayOnChannel[channel], notes[channel].size)
                    notes[channel] = tempNotes1 + Note(deltaTime, channel, pitch, velocity) + tempNotes2
                }
            }
        } else {
            if(velocity > 0) {
                val tempNotes = notes[channel]
                notes[channel] = Array(1){Note(0L, channel, pitch, velocity)} + tempNotes
            }
            else notes[channel] = notes[channel] + Note(1L, channel, pitch, velocity)
        }



        _uiState.update { a -> a.copy(
            visualArray = notes
        ) }
    }


    fun pressPad(channel: Int, pitch: Int, velocity: Int = 100){
        when(uiState.value.seqMode){
            //STOPPED -> kmmk.noteOn(channel, pitch, velocity)
            //PLAYING -> kmmk.noteOn(channel, pitch, velocity)
            //RECORDING -> recordNote(channel, pitch, velocity)
            MUTING -> TODO()
            ERASING -> TODO()
            CLEARING -> {
                indexToPlayOnChannel[channel] = 0
                notes[channel] = emptyArray()
            }
            else -> if(uiState.value.seqIsRecording) recordNote(channel, pitch, velocity) else kmmk.noteOn(channel, pitch, velocity)
        }.also {
            updatePadState(channel, velocity)
            _uiState.update { a -> a.copy(
                visualArray = notes,
                visualArrayRefresh = !uiState.value.visualArrayRefresh
            ) }
        }
    }


    private fun updatePadState(channel: Int, velocity: Int){
        _uiState.value.channelIsActive[channel] = velocity > 0
    }

}