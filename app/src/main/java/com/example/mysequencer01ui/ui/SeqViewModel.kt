package com.example.mysequencer01ui.ui

import androidx.lifecycle.ViewModel
import com.example.mysequencer01ui.KmmkComponentContext
import com.example.mysequencer01ui.Note
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.coroutines.EmptyCoroutineContext


class SeqViewModel(private val kmmk: KmmkComponentContext) : ViewModel() {

    /** UI state exposed to the UI **/
    private val _uiState = MutableStateFlow(SeqUiState())
    // Backing property to avoid state updates from other classes
    val uiState: StateFlow<SeqUiState> = _uiState.asStateFlow()
    // The asStateFlow() makes this mutable state flow a read-only state flow

    private var coroutineScope = CoroutineScope(
        //EmptyCoroutineContext
        Dispatchers.Main
            //Dispatchers.Default
    )
    private var note1 = Note(4, 100, 0L)


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

                if(uiState.value.seqStartTime == System.currentTimeMillis()){
                    launch(Dispatchers.Main) {
                        delay(note1.startTime)
                        kmmk.noteOn(note1.pitch, 0)
                        _uiState.update { a -> a.copy(note1IsPlaying = true) }
                        delay(note1.length)
                        kmmk.noteOff(note1.pitch, 0)
                        _uiState.update { a -> a.copy(note1IsPlaying = false) }
                    }
                }

                delay(1L)
                _uiState.update { a ->
                    a.copy(
                        deltaTime = System.currentTimeMillis() - uiState.value.seqStartTime
                    )
                }

                if (uiState.value.deltaTime >= uiState.value.seqTotalTime) {
                    _uiState.update { a ->
                        a.copy(
                            seqStartTime = System.currentTimeMillis()
                        )
                    }
                }
            }
        }
    }

    fun stopSeq(){
        _uiState.update { a -> a.copy (seqIsPlaying = false) }
    }

    fun recordNoteOn(){
        note1.startTime = System.currentTimeMillis() - uiState.value.seqStartTime
        _uiState.update { a -> a.copy (noteStartTime = note1.startTime) }
    }

    fun recordNoteOff(){
        note1.length = System.currentTimeMillis() - note1.startTime - uiState.value.seqStartTime
        _uiState.update { a -> a.copy (noteLegnth = note1.length) }
    }

}