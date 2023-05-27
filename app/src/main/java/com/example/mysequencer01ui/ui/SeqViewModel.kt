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
    var sequence = MutableList(16){ Sequence(uiState.value.bpm) }
    private var currentMode = mutableListOf<SeqModes>()


    fun startSeq() {
        _uiState.update { a ->
            a.copy(
                seqStartTime = Array(16){ System.currentTimeMillis() },
                deltaTime = Array(16){ 0L }
            )
        }

        for(c in sequence.indices) {
            sequence[c].apply { startTimeStamp = System.currentTimeMillis(); deltaTime = 0L; indexToPlay = 0 }
        }

        if (uiState.value.seqIsPlaying) return

        _uiState.update { a -> a.copy(seqIsPlaying = true) }

        coroutineScope.launch {
            while (uiState.value.seqIsPlaying) {
                for(c in 0..15) {
                    with(sequence[c]){

                        deltaTime = System.currentTimeMillis() - startTimeStamp

                        if (notes.size > indexToPlay) {

                            if(isErasing) {

                                while(notes.size > indexToPlay && notes[indexToPlay].time <= deltaTime) {

                                    if(notes[indexToPlay].velocity > 0) {
                                        val searchedPitch = notes[indexToPlay].pitch
                                        var breakFlag = false

                                        // erasing noteON
                                        when {
                                            // end of array
                                            indexToPlay == notes.lastIndex -> {
                                                if(notes.size > 1) {
                                                    notes = notes.copyOfRange(0, indexToPlay)
                                                } else {
                                                    notes = emptyArray()
                                                }
                                                breakFlag = true
                                            }
                                            // beginning of array
                                            indexToPlay == 0 -> {
                                                notes = notes.copyOfRange(1, notes.size)
                                            }
                                            // middle of array
                                            else -> {
                                                val tempNotes = notes.copyOfRange(indexToPlay + 1, notes.size)
                                                notes = notes.copyOfRange(0, indexToPlay) + tempNotes
                                            }
                                        }

                                        // searching for paired noteOFF
                                        var pairedNoteOffIndex = -1
                                        var searchedIndex: Int
                                        if(indexToPlay <= notes.lastIndex) {
                                            searchedIndex = notes
                                                .copyOfRange(indexToPlay, notes.size)
                                                .indexOfFirst { it.pitch == searchedPitch && it.velocity == 0 }
                                            pairedNoteOffIndex = if(searchedIndex == -1) -1 else searchedIndex + indexToPlay
                                        }
                                        if(pairedNoteOffIndex == -1) {
                                            searchedIndex = notes
                                                .copyOfRange(0, indexToPlay)
                                                .indexOfFirst { it.pitch == searchedPitch && it.velocity == 0 }
                                            pairedNoteOffIndex = if(searchedIndex == -1) -1 else searchedIndex
                                        }

                                        // erasing paired noteOFF
                                        when {
                                            // no paired noteOFF found
                                            pairedNoteOffIndex == -1 -> {  // do nothing
                                            }
                                            // end of array
                                            pairedNoteOffIndex == notes.lastIndex -> {
                                                if(notes.size > 1) {
                                                    notes = notes.copyOfRange(0, pairedNoteOffIndex)
                                                } else {
                                                    notes = emptyArray()
                                                }
                                                breakFlag = true
                                            }
                                            // beginning of array
                                            pairedNoteOffIndex == 0 -> {
                                                notes = notes.copyOfRange(1, notes.size)
                                            }
                                            // middle of array
                                            else -> {
                                                val tempNotes = notes.copyOfRange(pairedNoteOffIndex + 1, notes.size)
                                                notes = notes.copyOfRange(0, pairedNoteOffIndex) + tempNotes
                                            }
                                        }

                                        // updating indexToPlay when erasing note before it
                                        if(pairedNoteOffIndex < indexToPlay  &&  pairedNoteOffIndex > -1) {
                                            indexToPlay--
                                        }
                                        if(breakFlag) break // out of while() when erased last note in array

                                    } else indexToPlay++ // skipping noteOFFs
                                }

                            } else {


                                for (i in indexToPlay..notes.lastIndex) {  // searching for chords (besides usual notes)
                                    if (notes[i].time <= deltaTime) {

    //                                    if(isErasing) {
    //
    //                                        // TODO lift Erase up from For()?
    //
    //                                        if(notes[i].velocity > 0) {
    //                                            val searchedPitch = notes[i].pitch
    //
    //                                            // erasing noteON(s)
    //                                            when {
    //                                                // end of array
    //                                                indexToPlay == notes.lastIndex -> {
    //                                                    notes = if(notes.size > 1) notes.copyOfRange(0, indexToPlay) else emptyArray()
    //                                                }
    //                                                // beginning of array
    //                                                indexToPlay == 0 -> {
    //                                                    while (notes[i].time <= deltaTime) {notes = notes.copyOfRange(1, notes.size)}
    //                                                }
    //                                                // middle of array
    //                                                else -> {
    //                                                    while (notes[i].time <= deltaTime) {
    //                                                        val tempNotes = notes.copyOfRange(indexToPlay + 1, notes.size)
    //                                                        notes = notes.copyOfRange(0, indexToPlay) + tempNotes
    //                                                    }
    //                                                }
    //                                            }
    //
    //                                            // searching for paired noteOFF(s)
    //                                            val pairedNoteIndex: Int
    //                                            val searchingNoteOff: Array<Note>
    //                                            val indexOfNoteOff: Int
    //                                            if(indexToPlay <= notes.lastIndex) {
    //                                                searchingNoteOff = notes.copyOfRange(indexToPlay, notes.size)
    //                                                indexOfNoteOff = searchingNoteOff.indexOfFirst { it.velocity == 0 && it.pitch == searchedPitch }
    //                                                pairedNoteIndex = if(indexOfNoteOff == -1) -1 else indexOfNoteOff + indexToPlay
    //                                            } else {
    //                                                searchingNoteOff = notes.copyOfRange(0, indexToPlay)
    //                                                indexOfNoteOff = searchingNoteOff.indexOfFirst { it.velocity == 0 && it.pitch == searchedPitch }
    //                                                pairedNoteIndex = if(indexOfNoteOff == -1) -1 else indexOfNoteOff
    //                                            }
    //
    //                                            // erasing paired noteOFF(s)
    //                                            when {
    //                                                // no paired noteOFF found
    //                                                pairedNoteIndex == -1 -> {  // do nothing
    //                                                }
    //                                                // end of array
    //                                                pairedNoteIndex == notes.lastIndex -> {
    //                                                    notes = if(notes.size > 1) notes.copyOfRange(0, pairedNoteIndex) else emptyArray()
    //                                                }
    //                                                // beginning of array
    //                                                pairedNoteIndex == 0 -> {
    //                                                    notes = notes.copyOfRange(1, notes.size)
    //                                                }
    //                                                // middle of array
    //                                                else -> {
    //                                                    val tempNotes = notes.copyOfRange(pairedNoteIndex + 1, notes.size)
    //                                                    notes = notes.copyOfRange(0, pairedNoteIndex) + tempNotes
    //                                                }
    //                                            }
    //                                            break
    //                                        }
    //
    //                                    } else {  // TODO watchOut mode conflicts & priorities

                                            // play note (if channel isn't muted)
                                            // remember which notes are playing
                                            // update UI
                                            // if (next_note.time > deltaTime) OR no other notes exist  = remember index, exit from For()

                                            if(!isMuted || notes[i].velocity == 0){
                                                kmmk.noteOn( notes[i].channel, notes[i].pitch, notes[i].velocity )
                                                noteOnStates[notes[i].pitch] = notes[i].velocity > 0
                                            }
                                            _uiState.value.channelIsActive[c] = notes[i].velocity > 0
                                            if ((notes.size <= i + 1) || (notes[i + 1].time > deltaTime)) {
                                                indexToPlay = i + 1  // TODO chords will have BUG! get rid of i, replace with indexToPlay
                                                break
                                            }
    //                                    }
                                    }
                                }
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
                if(sequence[c].noteOnStates[p]) {
                    kmmk.noteOn(c, p, 0)
                    sequence[c].noteOnStates[p] = false
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


    private fun recordNote(channel: Int, pitch: Int, velocity: Int) {

        with(sequence[channel]){

            val recordTime: Long

            if(uiState.value.seqIsPlaying){
                recordTime = System.currentTimeMillis() - startTimeStamp
            } else {
                if(velocity > 0) {
                    indexToPlay = 0
                    recordTime = 0L
                }
                else {
                    indexToPlay = notes.indexOfFirst { it.time > 0 }
                    if(indexToPlay < 0) indexToPlay = notes.size
                    recordTime = staticNoteOffTime
                }
            }

            when {
                // end of array
                indexToPlay == notes.size -> {
                    notes += Note(recordTime, channel, pitch, velocity)
                }
                // beginning of array
                notes.isNotEmpty() && indexToPlay == 0 -> {
                    val tempNotes = notes
                    notes = Array(1){Note(recordTime, channel, pitch, velocity)} + tempNotes
                }
                // middle of array
                else -> {
                    val tempNotes1 = notes.copyOfRange(0, indexToPlay)
                    val tempNotes2 = notes.copyOfRange(indexToPlay, notes.size)
                    notes = tempNotes1 + Note(recordTime, channel, pitch, velocity) + tempNotes2
                }
            }
        }

        _uiState.update { a -> a.copy(
            visualArray = sequence
        ) }
    }


    private fun muteChannel(channel: Int, velocity: Int, elapsedTime: Long, allButton: Boolean) {
        with(sequence[channel]) {
            if(allButton) {
                if(channel == 0) {
                    allChannelsMuted = (sequence.find{ !it.isMuted } == null)
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


    private fun eraseNotes(channel: Int, velocity: Int) {
        sequence[channel].isErasing = velocity > 0
        _uiState.value.channelIsActive[channel] = velocity > 0
    }


    private fun clearChannel(channel: Int, velocity: Int) {
        with(sequence[channel]) {
            if(velocity > 0) {
                indexToPlay = 0
                notes = emptyArray()
                for(p in 0..127){
                    if(noteOnStates[p]) {
                        kmmk.noteOn(channel, p, 0)
                        noteOnStates[p] = false
                    }
                }
            } else {
                _uiState.value.channelIsActive[channel] = false
                /*TODO Text "UNDO" */
            }
        }
    }


    fun pressPad(channel: Int, pitch: Int, velocity: Int, elapsedTime: Long = 0, allButton: Boolean = false) {
        when(uiState.value.seqMode) {
            MUTING -> muteChannel(channel, velocity, elapsedTime, allButton)
            ERASING -> eraseNotes(channel, velocity)
            CLEARING -> clearChannel(channel, velocity)
            else -> if(uiState.value.seqIsRecording) {
                recordNote(channel, pitch, velocity)
                _uiState.value.channelIsActive[channel] = velocity > 0
            } else kmmk.noteOn(channel, pitch, velocity)
        }.also {
            _uiState.update { a -> a.copy(
                visualArray = sequence,
                visualArrayRefresh = !uiState.value.visualArrayRefresh
            ) }
        }
    }

}