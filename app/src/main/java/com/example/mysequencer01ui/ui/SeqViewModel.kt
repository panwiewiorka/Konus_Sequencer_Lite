package com.example.mysequencer01ui.ui

import androidx.lifecycle.ViewModel
import com.example.mysequencer01ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class SeqViewModel(private val kmmk: KmmkComponentContext) : ViewModel() {

    /** UI state exposed to the UI **/
    private val _uiState = MutableStateFlow(SeqUiState())
    // Backing property to avoid state updates from other classes
    val uiState: StateFlow<SeqUiState> = _uiState.asStateFlow()
    // The asStateFlow() makes this mutable state flow a read-only state flow

    private var coroutineScope = CoroutineScope(Dispatchers.Main)

    private var notes = Array(0){Note(0, 0, 0)}

    var modeTime = 150L

    private var indexToPlayFrom = 0


    fun startSeq() {
        _uiState.update { a ->
            a.copy(
                seqStartTime = System.currentTimeMillis(),
                deltaTime = 0L,
            )
        }
        if (uiState.value.seqIsPlaying) return

        _uiState.update { a -> a.copy(seqIsPlaying = true) }

        coroutineScope.launch {
            while (uiState.value.seqIsPlaying) {

                _uiState.update { a ->
                    a.copy( deltaTime = System.currentTimeMillis() - uiState.value.seqStartTime )
                }

                if(notes.size > indexToPlayFrom) {
                    for(i in indexToPlayFrom .. notes.lastIndex) {
                        if(notes[i].time <= uiState.value.deltaTime) {
                            // play note
                            // update UI
                            // if (next_note.time > deltaTime) OR no other notes exist  = remember index, exit from For()

                            kmmk.noteOn(notes[i].channel, notes[i].pitch, notes[i].velocity)
                            updatePadStatus(notes[i].channel, notes[i].velocity)
                            if(((notes.size > i + 1) && (notes[i + 1].time > uiState.value.deltaTime)) || (notes.size <= i + 1)) {
                                indexToPlayFrom = i + 1
                                break
                            }
                        }
                    }
                }

                if (uiState.value.deltaTime >= uiState.value.seqTotalTime) {
                    _uiState.update { a ->
                        a.copy( seqStartTime = System.currentTimeMillis() )
                    }
                    indexToPlayFrom = 0
                }

                delay(1L)
            }
        }
    }


    fun stopSeq(){
        _uiState.update { a -> a.copy (seqIsPlaying = false) }
    }


    fun recSeq(){
        _uiState.update { a -> a.copy (seqIsRecording = !uiState.value.seqIsRecording) }
    }


    fun muteChannel(){
        _uiState.update { a -> a.copy( muteMode = !uiState.value.muteMode ) }
    }


    fun eraseNotes(){
        _uiState.update { a -> a.copy( eraseMode = !uiState.value.eraseMode ) }
    }


    fun clearSeq(){
        _uiState.update { a -> a.copy( clearMode = !uiState.value.clearMode ) }
        indexToPlayFrom = 0
        notes = emptyArray()
        _uiState.update { a -> a.copy(
            stepSequencer = notes
        ) }
    }


    private fun recordNote(channel: Int, pitch: Int, velocity: Int = 100) {

        val deltaTime = System.currentTimeMillis() - uiState.value.seqStartTime
        val ind = indexToPlayFrom

        when {
            // end of array
            ind == notes.size -> {
                notes += Note(deltaTime, channel, pitch, velocity)
            }
            // beginning of array
            notes.isNotEmpty() && ind == 0 -> {
                val tempNotes = notes
                notes = Array(1){Note(deltaTime, channel, pitch, velocity)} + tempNotes
            }
            // middle of array
            else -> {
                val tempNotes1 = notes.copyOfRange(0, ind)
                val tempNotes2 = notes.copyOfRange(ind, notes.size)
                notes = tempNotes1 + Note(deltaTime, channel, pitch, velocity) + tempNotes2
            }
        }

        _uiState.update { a -> a.copy(
            stepSequencer = notes
        ) }
    }


    fun pressPad(channel: Int, pitch: Int, velocity: Int = 100){
        kmmk.noteOn(channel, pitch, velocity)
        if(uiState.value.seqIsPlaying && uiState.value.seqIsRecording) recordNote(channel, pitch, velocity)
        updatePadStatus(channel, velocity)
    }


    private fun updatePadStatus(channel: Int, velocity: Int){
        _uiState.value.channelIsPlaying[channel] = velocity > 0
    }

}