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

//    private val notes = mutableMapOf<Time, NotesList>()
    private var notes = mutableListOf<Note>()
    //private var notes = List(16){mutableListOf<Note>()}

    var modeTime = 200L

    fun startSeq() {
        _uiState.update { a ->
            a.copy(
                seqStartTime = System.currentTimeMillis(),
                deltaTime = 0L,
            )
        }
//        var currentNotes = notes
        if (uiState.value.seqIsPlaying) return

        _uiState.update { a -> a.copy(seqIsPlaying = true) }
        var m = 0

        coroutineScope.launch {
            while (uiState.value.seqIsPlaying) {

                _uiState.update { a ->
                    a.copy(
                        deltaTime = System.currentTimeMillis() - uiState.value.seqStartTime
                    )
                }

                if(notes.size > 0 && notes.size > m){
                    for(i in m until notes.size){
                        if(notes[i].time <= uiState.value.deltaTime){
                            // play note
                            // update UI
                            // if (next_note.time > deltaTime) OR no other notes exist  = remember index, exit from For()

                            kmmk.noteOn(notes[i].channel, notes[i].pitch, notes[i].velocity)
                            updatePadStatus(notes[i].channel, notes[i].velocity)
                            if(((notes.size > i + 1) && (notes[i + 1].time > uiState.value.deltaTime)) || (notes.size <= i + 1)) {
                                m = i + 1
                                break
                            }
                        }
                    }
                }

                if (uiState.value.deltaTime >= uiState.value.seqTotalTime) {
                    _uiState.update { a ->
                        a.copy(
                            seqStartTime = System.currentTimeMillis(),
                        )
                    }
//                    currentNotes = notes
                    m = 0
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


    fun clearNotes(){
        _uiState.update { a -> a.copy( clearMode = !uiState.value.clearMode ) }
    }


    fun delSeq(){
        _uiState.update { a -> a.copy( delMode = !uiState.value.delMode ) }
        notes.clear()
    }


    private fun recordNote(channel: Int, pitch: Int, velocity: Int = 100) {
        if(uiState.value.seqIsPlaying){
            val deltaTime = System.currentTimeMillis() - uiState.value.seqStartTime

            var ind = notes.binarySearch { (it.time - deltaTime).toInt() }
            if(ind < 0) ind = ind * (-1) - 1

            notes.add(ind, Note(deltaTime, channel, pitch, velocity))
        }
    }


    private fun updatePadStatus(channel: Int, velocity: Int){
        val channelStatus = uiState.value.channelIsPlaying.toMutableList()
        channelStatus[channel] = velocity > 0
        _uiState.update { a -> a.copy(
            channelIsPlaying = channelStatus
        ) }
    }


    fun pressPad(channel: Int, pitch: Int, velocity: Int = 100){
        kmmk.noteOn(channel, pitch, velocity)
        if(uiState.value.seqIsRecording) recordNote(channel, pitch, velocity)
        updatePadStatus(channel, velocity)
    }



}