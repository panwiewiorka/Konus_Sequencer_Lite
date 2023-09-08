package com.example.mysequencer01ui.ui.viewTabs

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Switch
import androidx.compose.material.SwitchColors
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.KmmkComponentContext
import com.example.mysequencer01ui.ui.SeqUiState
import com.example.mysequencer01ui.ui.SeqViewModel
import com.example.mysequencer01ui.ui.nonScaledSp
import com.example.mysequencer01ui.ui.recomposeHighlighter
import com.example.mysequencer01ui.ui.theme.buttons
import com.example.mysequencer01ui.ui.theme.darkViolet
import com.example.mysequencer01ui.ui.theme.notWhite
import com.example.mysequencer01ui.ui.theme.selectedButton
import com.example.mysequencer01ui.ui.theme.violet
import com.example.mysequencer01ui.ui.theme.warmRed
import com.example.mysequencer01ui.ui.thickness
import kotlin.math.atan2

/*
@Composable
fun SettingsViewTestRecomposing(
    changeBPM: (Float) -> Unit,
    switchClockTransmitting: () -> Unit,
    switchVisualDebugger: () -> Unit,
    selectDebuggerSetting: (Int) -> Unit,
    bpm: Float,
    transmitClock: Boolean,
    visualDebugger: Boolean,
    debuggerViewSetting: Int,
    buttonsSize: Dp,
    kmmk: KmmkComponentContext,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .recomposeHighlighter()
        ) {
            MidiSelector(kmmk)

            Knob(buttonsSize, bpm, changeBPM)

            TextAndSwitch("Clock transmit", transmitClock) { switchClockTransmitting() }

            Row(
                modifier = Modifier.fillMaxWidth().recomposeHighlighter(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextAndSwitch("Visual Debugger", visualDebugger) { switchVisualDebugger() }
                Spacer(modifier = Modifier.width(20.dp))
                if(visualDebugger) {
                    Text("index", color = notWhite)
                    RadioButton(
                        selected = debuggerViewSetting == 0,
                        onClick = { selectDebuggerSetting(0) },
                        modifier = Modifier.padding(end = 10.dp))
                    Text("noteId", color = notWhite)
                    RadioButton(
                        selected = debuggerViewSetting == 1,
                        onClick = { selectDebuggerSetting(1) },
                        modifier = Modifier.padding(end = 10.dp))
                }
            }
        }
    }
}
 */

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
            Box { MidiSelector(kmmk) }

            Knob(buttonsSize, seqUiState.bpm, seqViewModel::changeBPM)

            TextAndSwitch("Transmit clock", seqUiState.transmitClock) { seqViewModel.switchClockTransmitting() }

            TextAndSwitch("Keep screen on when stopped", seqUiState.keepScreenOn) { seqViewModel.switchKeepScreenOn() }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextAndSwitch("Visual Debugger", seqUiState.visualDebugger) { seqViewModel.switchVisualDebugger() }
                Spacer(modifier = Modifier.width(20.dp))
                if(seqUiState.visualDebugger) {
                    Text("index", color = notWhite)
                    RadioButton(
                        selected = seqUiState.debuggerViewSetting == 0,
                        onClick = { seqViewModel.selectDebuggerSetting(0) },
                        modifier = Modifier.padding(end = 10.dp),
                        colors = RadioButtonDefaults.colors(selectedColor = violet)
                    )
                    Text("noteId", color = notWhite)
                    RadioButton(
                        selected = seqUiState.debuggerViewSetting == 1,
                        onClick = { seqViewModel.selectDebuggerSetting(1) },
                        modifier = Modifier.padding(end = 10.dp),
                        colors = RadioButtonDefaults.colors(selectedColor = violet)
                    )
                }
            }
        }
    }
}



@Composable
fun TextAndSwitch(text: String, switchState: Boolean, toDo: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = notWhite, modifier = Modifier.padding(end = 10.dp))
        Switch(
            checked = switchState,
            onCheckedChange = {toDo()},
            colors = SwitchDefaults.colors(
                checkedThumbColor = violet,
                checkedTrackColor = violet,
                checkedTrackAlpha = 0.5f,
                uncheckedThumbColor = selectedButton,
                uncheckedTrackColor = selectedButton,
                uncheckedTrackAlpha = 0.5f
            )
        )
    }
}


@Composable
fun Knob(buttonsSize: Dp, bpm: Float, changeBPM: (Float) -> Unit) {
    var angle by remember { mutableStateOf(0f) }
    var dragStartedAngle by remember { mutableStateOf(0f) }
    var oldAngle by remember { mutableStateOf(angle) }
    var rotationAngle by remember { mutableStateOf((bpm - 40) * 270 / 240) }

    Box(
//        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .padding(top = 20.dp, bottom = 10.dp)
            .size(buttonsSize)
            .pointerInput(true) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStartedAngle = -atan2(
                            y = size.center.x - offset.x,
                            x = size.center.y - offset.y
                        ) * (180f / Math.PI.toFloat())
                    },
                    onDragEnd = {
                        oldAngle = angle
                    }
                ) { change, _ ->
                    change.consume()

                    val tempAngle = angle

                    angle = -atan2(
                        y = size.center.x - change.position.x,
                        x = size.center.y - change.position.y
                    ) * (180f / Math.PI.toFloat()) - dragStartedAngle + oldAngle

                    var deltaAngle = angle - tempAngle

                    if (deltaAngle < -180) deltaAngle += 360
                    else if (deltaAngle > 180) deltaAngle -= 360

                    val tempRotationAngle = rotationAngle

                    rotationAngle = (rotationAngle + deltaAngle / 4).coerceIn(0f..270f)

                    if (tempRotationAngle != rotationAngle) {
                        changeBPM(rotationAngle * 240 / 270 + 40)
                    }
                }
            }
    ){
        Canvas(modifier = Modifier
            .size(buttonsSize / 1.5f)
            .align(Alignment.TopCenter)
        ) {
//            drawCircle(buttonsColor, size.height / 2)
            drawArc(
                color = selectedButton,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(0f, 0f),
                size = size,
                style = Stroke(width = thickness, cap = StrokeCap.Round)
            )
            drawArc(
                color = buttons,
                startAngle = 135f,
                sweepAngle = -90f,
                useCenter = false,
                topLeft = Offset(0f, 0f),
                size = size,
                style = Stroke(width = thickness, cap = StrokeCap.Round)
            )
        }
        Canvas(modifier = Modifier
            .size(buttonsSize / 1.5f)
            .rotate(135f + rotationAngle)
            .align(Alignment.TopCenter)
        ) {
            drawLine(
                color = notWhite,
                start = Offset(center.x * 1.4f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
        Text(
            text = "$bpm BPM",
            color = notWhite,
            fontSize = 12.nonScaledSp,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
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

/*
@Composable
fun MidiDeviceSelector(kmmk: KmmkComponentContext) {
    var midiOutputDialogState by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = midiOutputDialogState,
        onDismissRequest = { midiOutputDialogState = false},
    ) {
        val onClick: (String) -> Unit = { id ->
            if (id.isNotEmpty()) {
                kmmk.setOutputDevice(id)
            }
            midiOutputDialogState = false
        }
        if (kmmk.midiOutputPorts.any()) {
            for (d in kmmk.midiOutputPorts) {
                DropdownMenuItem(onClick = { onClick(d.id) }) { Text(d.name ?: "(unnamed)") }

            }
//            DropdownMenuItem(onClick = { onClick("") }) { Text("(Cancel)") }
        } else {
            DropdownMenuItem(onClick = { onClick("") }) { Text("(no MIDI output)") }
        }
    }
    Card(
        modifier = Modifier
            .clickable(onClick = {
                kmmk.updateMidiDeviceList()
                midiOutputDialogState = true
            })
            .border(1.dp, violet)
            .padding(12.dp)
    ) {
        Text(kmmk.midiDeviceManager.midiOutput?.details?.name ?: "-- Select MIDI output --")
    }
}
 */