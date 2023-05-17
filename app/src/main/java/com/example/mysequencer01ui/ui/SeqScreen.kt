package com.example.mysequencer01ui.ui


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mysequencer01ui.KmmkComponentContext
import com.example.mysequencer01ui.ui.theme.MySequencer01UiTheme


@Composable
fun SeqScreen(kmmk: KmmkComponentContext, seqViewModel: SeqViewModel = viewModel()
) {
    val seqUiState by seqViewModel.uiState.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.LightGray)
    ) {

        MidiDeviceSelector(kmmk)
        val midiOutputError by remember { kmmk.midiDeviceManager.midiOutputError }
        if (midiOutputError != null) {
            var showErrorDetails by remember { mutableStateOf(false) }
            if (showErrorDetails) {
                val closeDeviceErrorDialog = { showErrorDetails = false }
                AlertDialog(onDismissRequest = closeDeviceErrorDialog,
                    confirmButton = { Button(onClick = closeDeviceErrorDialog) { Text("OK") } },
                    title = { Text("MIDI device error") },
                    text = {
                        Column {
                            Row {
                                Text("MIDI output is disabled until new device is selected.")
                            }
                            Row {
                                Text(midiOutputError?.message ?: "(error details lost...)")
                            }
                        }
                    }
                )
            }
            Button(onClick = { showErrorDetails = true }) {
                Text(text = "!!", color = Color.Red)
            }
        }

        //KeyboardRow(kmmk, 1)

        Column(modifier = Modifier.padding(20.dp)){

            Row{
                Button(onClick = { seqViewModel.startSeq() }) {
                    Text(text = if(seqUiState.seqIsPlaying) "Restart" else "Start")
                }

                Spacer(modifier = Modifier.width(30.dp))

                Button(onClick = { seqViewModel.stopSeq() }) {
                    Text("Stop")
                }
            }

            Text("BPM = ${seqUiState.bpm}")

            Text("seqStartTime = ${seqUiState.seqStartTime}")

//            var deltaTime = 0L
//            LaunchedEffect(key1 = seqUiState.deltaTime){
//                deltaTime = seqUiState.deltaTime
//            }
            Text("deltaTime = ${seqUiState.deltaTime}")

            Text("seqTotalTime = ${seqUiState.seqTotalTime}")

            Text("note startTime = ${seqUiState.noteStartTime}")

            Text("Note legnth = ${seqUiState.noteLegnth}")

        }

        Row(modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            VoiceButton(4, 0, kmmk, seqViewModel, note1IsPlaying = seqUiState.note1IsPlaying)
            VoiceButton(14, 1, kmmk, seqViewModel, note1IsPlaying = seqUiState.note2IsPlaying)
            VoiceButton(26, 2, kmmk, seqViewModel, note1IsPlaying = seqUiState.note3IsPlaying)
            VoiceButton(39, 3, kmmk, seqViewModel, note1IsPlaying = seqUiState.note4IsPlaying)
        }
    }
}


@Composable
fun VoiceButton(
    note: Int,
    channel: Int,
    kmmk: KmmkComponentContext,
    seqViewModel: SeqViewModel,
    note1IsPlaying: Boolean,
){
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> { kmmk.noteOn(note, channel); seqViewModel.recordNoteOn() }
                is PressInteraction.Release -> { kmmk.noteOff(note, channel); seqViewModel.recordNoteOff() }
                is PressInteraction.Cancel -> { kmmk.noteOff(note, channel); seqViewModel.recordNoteOff() }
            }
        }
    }
    Button(interactionSource = interactionSource,
        onClick = {},
        modifier = Modifier
            .size(80.dp)
            .border(width = 4.dp, color = if(note1IsPlaying) Color(0xFFFF0000) else Color(0x00000000))
            //.background(Color.Gray, shape = RoundedCornerShape(0.dp)
    ) {

    }
}


@Composable
fun MidiDeviceSelector(kmmk: KmmkComponentContext) {
    var midiOutputDialogState by remember { mutableStateOf(false) }

    DropdownMenu(expanded = midiOutputDialogState, onDismissRequest = { midiOutputDialogState = false}) {
        val onClick: (String) -> Unit = { id ->
            if (id.isNotEmpty()) {
                kmmk.setOutputDevice(id)
            }
            midiOutputDialogState = false
        }
        if (kmmk.midiOutputPorts.any())
            for (d in kmmk.midiOutputPorts)
                DropdownMenuItem(onClick = { onClick(d.id) }) {
                    Text(d.name ?: "(unnamed)")
                }
        else
            DropdownMenuItem(onClick = { onClick("") }) { Text("(no MIDI output)") }
        DropdownMenuItem(onClick = { onClick("") }) { Text("(Cancel)") }
    }
    Card(
        modifier = Modifier
            .clickable(onClick = {
                kmmk.updateMidiDeviceList()
                midiOutputDialogState = true
            })
            .padding(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colors.primaryVariant)
    ) {
        Text(kmmk.midiDeviceManager.midiOutput?.details?.name ?: "-- Select MIDI output --")
    }
}




@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MySequencer01UiTheme(darkTheme = true) {
        SeqScreen(KmmkComponentContext())
    }
}