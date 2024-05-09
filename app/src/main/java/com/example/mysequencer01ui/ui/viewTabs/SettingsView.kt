package com.example.mysequencer01ui.ui.viewTabs

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.KmmkComponentContext
import com.example.mysequencer01ui.ui.SeqUiState
import com.example.mysequencer01ui.ui.SeqViewModel
import com.example.mysequencer01ui.ui.nonScaledSp
import com.example.mysequencer01ui.ui.theme.buttons
import com.example.mysequencer01ui.ui.theme.changeLengthAreaColor
import com.example.mysequencer01ui.ui.theme.notWhite
import com.example.mysequencer01ui.ui.theme.selectedButton
import com.example.mysequencer01ui.ui.theme.violet
import com.example.mysequencer01ui.ui.theme.warmRed
import com.example.mysequencer01ui.ui.thickness
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun SettingsView(seqViewModel: SeqViewModel, seqUiState: SeqUiState, buttonsSize: Dp, kmmk: KmmkComponentContext) {

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .border(1.dp, buttons)
            .background(changeLengthAreaColor)
            .clipToBounds()
    ) {
        val view = LocalView.current
        val scrollState = rememberScrollState(seqUiState.settingsScrollPosition)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .verticalScroll(scrollState, reverseScrolling = true)
                .pointerInput(scrollState) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change: PointerInputChange, dragAmount: Float ->
                            change.consume()
                            CoroutineScope(Dispatchers.Main).launch {
                                scrollState.scrollTo(scrollState.value + dragAmount.toInt())
                            }
                        },
                        onDragEnd = { seqViewModel.saveSettingsScrollPosition(scrollState.value) }
                    )
                },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box { MidiSelector(kmmk) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Knob(buttonsSize, seqUiState.bpm, seqViewModel::changeBPM, seqViewModel::saveSettingsToDatabase)
                Spacer(modifier = Modifier.width(40.dp))
                TextAndSwitch("Transmit clock", seqUiState.transmitClock) { seqViewModel.switchClockTransmitting() }
            }

            Column (
                verticalArrangement = Arrangement.spacedBy((-16).dp)
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    TextAndSwitch("Use fullscreen", seqUiState.fullScreen) {
                        if (seqUiState.fullScreen) seqViewModel.goOutOfFullScreen(view) else seqViewModel.goFullScreen(view)
                        seqViewModel.switchFullScreenState()
                    }
                }

                TextAndSwitch("Show channel number on pads", seqUiState.showChannelNumberOnPads) { seqViewModel.switchShowChannelNumberOnPads() }

                TextAndSwitch("Keep screen on when sequencer is stopped", seqUiState.keepScreenOn) { seqViewModel.switchKeepScreenOn() }
            }

            Column (
                verticalArrangement = Arrangement.spacedBy((-16).dp)
            ) {
                TextAndSwitch("Allow to record notes shorter than quantization time", seqUiState.allowRecordShortNotes) { seqViewModel.switchRecordShortNotes() }

                TextAndSwitch("Set pad pitch to last played note on piano keys", seqUiState.setPadPitchByPianoKey) { seqViewModel.switchSetPadPitchByPianoKey() }
            }

            MomentarySettings(seqUiState.toggleTime, seqViewModel::changeToggleTime)

            Column (
                verticalArrangement = Arrangement.spacedBy((-16).dp)
            ) {
                RefreshRateSettings(true, seqUiState.uiRefreshRate, seqViewModel::changeUiRefreshRate)
                RefreshRateSettings(false, seqUiState.dataRefreshRate, seqViewModel::changeDataRefreshRate)
            }

            VisualDebuggerSettings(seqUiState.showVisualDebugger, seqViewModel::switchVisualDebugger, seqUiState.debuggerViewSetting, seqViewModel::selectDebuggerSetting)

            PolicyText()

            Spacer(modifier = Modifier.height(10.dp))
        }
        GradientBox(modifier = Modifier
            .align(Alignment.TopCenter)
            .rotate(180f))
        GradientBox(modifier = Modifier.align(Alignment.BottomCenter))
    }
}


@Composable
fun TextAndSwitch(text: String, switchState: Boolean, toDo: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            color = notWhite,
            fontSize = 14.nonScaledSp,
            modifier = Modifier.padding(end = 10.dp)
        )
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
fun Knob(buttonsSize: Dp, bpm: Float, changeBPM: (Float) -> Unit, saveSettingsToDatabase: () -> Unit) {
    var angle by remember { mutableStateOf(0f) }
    var dragStartedAngle by remember { mutableStateOf(0f) }
    var oldAngle by remember { mutableStateOf(angle) }
    var rotationAngle by remember { mutableStateOf((bpm - 40) * 270 / 240) }

    Box(
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
                    onDrag = { change, _ ->
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
                    },
                    onDragEnd = {
                        oldAngle = angle
                        saveSettingsToDatabase()
                    }
                )
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
fun MomentarySettings(toggleTimeState: Int, changeToggleTime: (Int) -> Unit ) {
    Column (
        verticalArrangement = Arrangement.spacedBy((-16).dp)
    ) {
        TextAndSwitch("Enable momentary behaviour of mode buttons", toggleTimeState != Int.MAX_VALUE) {
            changeToggleTime(
                if (toggleTimeState == Int.MAX_VALUE) 300 else Int.MAX_VALUE
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            var toggleTime by remember { mutableStateOf(sqrt(toggleTimeState.toDouble()).toFloat())}
            Box(
                modifier = Modifier.width(180.dp)
            ) {
                Text(
                    text = "Momentary time: ${if(toggleTimeState != Int.MAX_VALUE) "> ${(toggleTime * toggleTime).toInt()} ms" else "OFF" }",
                    color = notWhite,
                    fontSize = 14.nonScaledSp,
                    modifier = Modifier.padding(end = 10.dp)
                )
            }
            Slider(
                value = toggleTime,
                onValueChange = {
                    toggleTime = it
                },
                enabled = (toggleTimeState != Int.MAX_VALUE),
                valueRange = 10f..31.61f,
                onValueChangeFinished = {
                    changeToggleTime((toggleTime * toggleTime).toInt())
                }
            )
        }
    }
}


@Composable
fun RefreshRateSettings(ui: Boolean, refreshRateState: Int, changeRefreshRate: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        var refreshRate by remember { mutableStateOf(sqrt(refreshRateState.toDouble()).toFloat())}
        Box(
            modifier = Modifier.width(180.dp)
        ) {
            Text(
                text = "${if (ui) "UI" else "Data" } refresh rate: ${(refreshRate * refreshRate).toInt()} ms",
                color = notWhite,
                fontSize = 14.nonScaledSp,
                modifier = Modifier.padding(end = 10.dp)
            )
        }
        Slider(
            value = refreshRate,
            onValueChange = {
                refreshRate = it
            },
            valueRange = sqrt(3f)..sqrt(if (ui) 40f else 10f),
            onValueChangeFinished = {
                changeRefreshRate((refreshRate * refreshRate).toInt())
            }
        )
    }
}


@Composable
fun VisualDebuggerSettings(showVisualDebugger: Boolean, switchVisualDebugger: () -> Unit, debuggerViewSetting: Int, selectDebuggerSetting: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextAndSwitch("Visual debugger", showVisualDebugger) { switchVisualDebugger() }
        Spacer(modifier = Modifier.width(36.dp))
        if(showVisualDebugger) {
            Text("index", color = notWhite, fontSize = 14.nonScaledSp)
            RadioButton(
                selected = debuggerViewSetting == 0,
                onClick = { selectDebuggerSetting(0) },
                modifier = Modifier.padding(end = 16.dp),
                colors = RadioButtonDefaults.colors(selectedColor = violet)
            )
            Text("noteId", color = notWhite, fontSize = 14.nonScaledSp)
            RadioButton(
                selected = debuggerViewSetting == 1,
                onClick = { selectDebuggerSetting(1) },
                colors = RadioButtonDefaults.colors(selectedColor = violet)
            )
        }
    }
}


@Composable
fun PolicyText() {
    val uriHandler = LocalUriHandler.current
    Text(
        text = "v 0.1  /  Privacy policy",
        fontSize = 14.nonScaledSp,
        color = selectedButton,
        modifier = Modifier
            .clickable { uriHandler.openUri("https://docs.google.com/document/d/1eD8w5ZL09c3-SZZRabl88t0kqI_KcVIyeZjnC4Kdt3Y/edit?usp=sharing") }
    )
}


@Composable
fun GradientBox(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        changeLengthAreaColor
                    )
                )
            )
    )
}


@Composable
fun MidiSelector(kmmk: KmmkComponentContext) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        MidiDeviceSelector(kmmk)
        Spacer(modifier = Modifier.width(10.dp))
        val midiOutputError by remember { kmmk.midiDeviceManager.midiOutputError }
        if (midiOutputError != null) {
            var showErrorDetails by remember { mutableStateOf(false) }
            if (showErrorDetails) {
                val closeDeviceErrorDialog = { showErrorDetails = false }
                AlertDialog(onDismissRequest = closeDeviceErrorDialog,
                    confirmButton = { Button(onClick = closeDeviceErrorDialog) { Text("OK", fontSize = 14.nonScaledSp) } },
                    title = { Text("MIDI device error", fontSize = 14.nonScaledSp) },
                    text = {
                        Column {
                            Row {
                                Text("MIDI output is disabled until new device is selected.", fontSize = 14.nonScaledSp)
                            }
                            Row {
                                Text(midiOutputError?.message ?: "(error details lost...)", fontSize = 14.nonScaledSp)
                            }
                        }
                    }
                )
            }
            Button(onClick = { showErrorDetails = true }) {
                Text(text = "!!", color = warmRed, fontSize = 14.nonScaledSp)
            }
        }
    }
}

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
                DropdownMenuItem(onClick = { onClick(d.id) }) { Text(d.name ?: "(unnamed)", fontSize = 14.nonScaledSp) }
            }
        } else {
            DropdownMenuItem(onClick = { onClick("") }) { Text("(no MIDI output)", fontSize = 14.nonScaledSp) }
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
        Text(kmmk.midiDeviceManager.midiOutput?.details?.name ?: "-- Select MIDI output --", fontSize = 14.nonScaledSp)
    }
}

/*
@Composable
fun MidiInputSelector(kmmk: KmmkComponentContext) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        MidiInputDeviceSelector(kmmk)

        Spacer(modifier = Modifier.width(10.dp))
        val midiInputError by remember { kmmk.midiDeviceManager.midiInputError }
        if (midiInputError != null) {
            var showErrorDetails by remember { mutableStateOf(false) }
            if (showErrorDetails) {
                val closeDeviceErrorDialog = { showErrorDetails = false }
                AlertDialog(onDismissRequest = closeDeviceErrorDialog,
                    confirmButton = { Button(onClick = closeDeviceErrorDialog) { Text("OK", fontSize = 14.nonScaledSp) } },
                    title = { Text("MIDI device error", fontSize = 14.nonScaledSp) },
                    text = {
                        Column {
                            Row {
                                Text("MIDI input is disabled until new device is selected.", fontSize = 14.nonScaledSp)
                            }
                            Row {
                                Text(midiInputError?.message ?: "(error details lost...)", fontSize = 14.nonScaledSp)
                            }
                        }
                    }
                )
            }
            Button(onClick = { showErrorDetails = true }) {
                Text(text = "!!", color = warmRed, fontSize = 14.nonScaledSp)
            }
        }
    }
}

@Composable
fun MidiInputDeviceSelector(kmmk: KmmkComponentContext) {
    var midiInputDialogState by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = midiInputDialogState,
        onDismissRequest = { midiInputDialogState = false},
    ) {
        val onClick: (String) -> Unit = { id ->
            if (id.isNotEmpty()) {
                kmmk.setInputDevice(id)
            }
            midiInputDialogState = false
        }
        if (kmmk.midiInputPorts.any()) {
            for (d in kmmk.midiInputPorts) {
                DropdownMenuItem(onClick = { onClick(d.id) }) { Text(d.name ?: "(unnamed)", fontSize = 14.nonScaledSp) }

            }
//            DropdownMenuItem(onClick = { onClick("") }) { Text("(Cancel)", fontSize = 14.nonScaledSp) }
        } else {
            DropdownMenuItem(onClick = { onClick("") }) { Text("(no MIDI input)", fontSize = 14.nonScaledSp) }
        }
    }
    Card(
        modifier = Modifier
            .clickable(onClick = {
                kmmk.updateMidiInputDeviceList()
                midiInputDialogState = true
            })
            .border(1.dp, violet)
            .padding(12.dp)
    ) {
        Text(kmmk.midiDeviceManager.midiInput?.details?.name ?: "-- Select MIDI input --", fontSize = 14.nonScaledSp)
    }
}
*/