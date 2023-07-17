package com.example.mysequencer01ui.ui.viewTabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.KmmkComponentContext
import com.example.mysequencer01ui.ui.SeqUiState
import com.example.mysequencer01ui.ui.SeqViewModel
import com.example.mysequencer01ui.ui.theme.notWhite
import com.example.mysequencer01ui.ui.theme.warmRed

@Composable
fun SettingsView(seqViewModel: SeqViewModel, seqUiState: SeqUiState, buttonsSize: Dp, kmmk: KmmkComponentContext) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            MidiSelector(kmmk)

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier  = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${seqUiState.bpm} BPM",
                    color = Color.Gray,
                    modifier = Modifier.padding(end = 10.dp)
                )

                var sizeSliderPosition by remember { mutableStateOf(120f) }
                Slider(
                    value = sizeSliderPosition,
                    onValueChange = {
                        sizeSliderPosition = it
                        seqViewModel.changeBPM(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    valueRange = 40f..280f,
                    onValueChangeFinished = {  }
                )
            }

            TextAndSwitch("Clock transmit", seqUiState.transmitClock) { seqViewModel.switchClockTransmitting() }

            TextAndSwitch("LazyKeyboard", seqUiState.lazyKeyboard) { seqViewModel.switchLazyKeyboard() }

            TextAndSwitch("VisualDebugger", seqUiState.visualDebugger) { seqViewModel.switchVisualDebugger() }
        }
    }
}

@Composable
fun TextAndSwitch(text: String, switchState: Boolean, toDo: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = notWhite, modifier = Modifier.padding(end = 10.dp))
        Switch(checked = switchState, onCheckedChange = {toDo()})
    }
}



@Composable
fun MidiSelector(kmmk: KmmkComponentContext) {
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
            Text(text = "!!", color = warmRed)
        }
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