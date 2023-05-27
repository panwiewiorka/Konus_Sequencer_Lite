package com.example.mysequencer01ui.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysequencer01ui.KmmkComponentContext
import com.example.mysequencer01ui.SeqModes
import com.example.mysequencer01ui.ui.theme.BackGray
import com.example.mysequencer01ui.SeqModes.*



val Int.nonScaledSp
    @Composable
    get() = (this / LocalDensity.current.fontScale).sp


@Composable
fun PadButton(
    channel: Int,
    pitch: Int,
    seqViewModel: SeqViewModel,
    channelIsPlaying: Boolean,
    seqMode: SeqModes,
    seqIsPlaying: Boolean,
    seqIsRecording: Boolean
){
    val interactionSource = remember { MutableInteractionSource() }
    var elapsedTime = 0L
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> { seqViewModel.pressPad(channel, pitch, 100); elapsedTime = System.currentTimeMillis() }
                is PressInteraction.Release -> { seqViewModel.pressPad(channel, pitch, 0, elapsedTime) }
                is PressInteraction.Cancel -> { seqViewModel.pressPad(channel, pitch, 0, elapsedTime) }
            }
        }
    }
    Box(modifier = Modifier.padding(10.dp)){
        Button(
            interactionSource = interactionSource,
            onClick = {},
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = BackGray
            ),
            modifier = Modifier
                .size(120.dp)
                .border(
                    width = 4.dp, color = if (channelIsPlaying) {
                        when (seqMode) {
                            MUTING -> Color(0xFF00FF00)
                            ERASING -> Color(0xFFFF0000)
                            CLEARING -> Color(0xFFFFFFFF)
                            else -> {
                                if(seqIsRecording) Color(0xFFFF0000)
                                else if(seqIsPlaying) Color(0xFF008800)
                                else Color(0x00000000)
                            }
                        }
                    } else Color(0x00000000)
                )
        ) {
            if(seqViewModel.sequence[channel].isMuted) Text("MUTED")
        }
    }
}


@Composable
fun PadsGrid(
    seqViewModel: SeqViewModel,
    channelIsActive: Array<Boolean>,
    seqMode: SeqModes,
    seqIsPlaying: Boolean,
    seqIsRecording: Boolean
){
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ){
        Column {
            Row(
                //modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PadButton(2, 26, seqViewModel, channelIsActive[2], seqMode, seqIsPlaying, seqIsRecording)
                PadButton(3, 39, seqViewModel, channelIsActive[3], seqMode, seqIsPlaying, seqIsRecording)
            }
            Row(
                //modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PadButton(0, 4, seqViewModel, channelIsActive[0], seqMode, seqIsPlaying, seqIsRecording)
                PadButton(1, 14, seqViewModel, channelIsActive[1], seqMode, seqIsPlaying, seqIsRecording)
            }
        }
    }
}


@Composable
fun AllButton(seqViewModel: SeqViewModel){
    val interactionSource = remember { MutableInteractionSource() }
    var elapsedTime = 0L
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    for(i in 0..15){
                        seqViewModel.pressPad(i, 26, 100, allButton = true)
                    }
                    elapsedTime = System.currentTimeMillis()
                }
                is PressInteraction.Release -> {
                    for(i in 0..15){
                        seqViewModel.pressPad(i, 26, 0, elapsedTime, allButton = true)
                    }
                }
                is PressInteraction.Cancel -> {
                    for(i in 0..15){
                        seqViewModel.pressPad(i, 26, 0, elapsedTime, allButton = true)
                    }
                }
            }
        }
    }
    Button(
        interactionSource = interactionSource,
        onClick = {},
        shape = RoundedCornerShape(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = BackGray
        ),
        modifier = Modifier.size(80.dp)
    ) {
        Text("ALL")
    }
}


@Composable
fun RecButton(seqViewModel: SeqViewModel, seqIsRecording: Boolean){
    Button(
        interactionSource = buttonInteraction(seqViewModel.toggleTime, {seqViewModel.recMode()}),
        onClick = {  },
        shape = RoundedCornerShape(0.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(seqIsRecording) Color.Red else BackGray
        ),
        modifier = Modifier
            .size(80.dp),
    ) {
        Box{
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(if (seqIsRecording) 0.dp else 6.dp)){
                drawCircle(
                    color = if(seqIsRecording) BackGray else Color.Red,
                    radius = 14.dp.toPx(),
                    center = center,
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
            Canvas(modifier = Modifier.fillMaxSize()){
                drawCircle(
                    color = if(seqIsRecording) BackGray else Color.Red,
                    radius = 14.dp.toPx(),
                    center = center,
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }
    }
}


@Composable
fun PlayButton(seqViewModel: SeqViewModel, seqIsPlaying: Boolean){
    Button(
        interactionSource = buttonInteraction(seqViewModel.toggleTime, {seqViewModel.startSeq()}, {seqViewModel.stopSeq()}),
        onClick = {  },
        shape = RoundedCornerShape(0.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(seqIsPlaying) Color(0xFF00AA00) else BackGray
        ),
        modifier = Modifier
            .size(80.dp),
    ) {
        Box{
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(if (seqIsPlaying) 0.dp else 5.dp)
                .alpha(0.6f)){
                val path = Path()
                path.moveTo(30.dp.toPx(), 26.dp.toPx())
                path.lineTo(30.dp.toPx(), 54.dp.toPx())
                path.lineTo(54.dp.toPx(),size.height / 2f)
                path.lineTo(30.dp.toPx(), 26.dp.toPx())
                path.lineTo(30.dp.toPx(), 54.dp.toPx())

                drawPath(
                    path = path,
                    color = if(seqIsPlaying) BackGray else Color(0xFF00AA00),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        join = StrokeJoin.Round
                    )
                )
            }
            Canvas(modifier = Modifier.fillMaxSize()){
                val path = Path()
                path.moveTo(30.dp.toPx(), 26.dp.toPx())
                path.lineTo(30.dp.toPx(), 54.dp.toPx())
                path.lineTo(54.dp.toPx(),size.height / 2f)
                path.lineTo(30.dp.toPx(), 26.dp.toPx())
                path.lineTo(30.dp.toPx(), 54.dp.toPx())

                drawPath(
                    path = path,
                    color = if(seqIsPlaying) BackGray else Color(0xFF00AA00),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}


@Composable
fun StopButton(seqViewModel: SeqViewModel, kmmk: KmmkComponentContext){
    val interactionSource = remember { MutableInteractionSource() }
    var stopIsPressed by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    //                    if(seqUiState.seqIsPlaying)
                    seqViewModel.stopSeq()

                    kmmk.allNotesOff()
//                    else { for(i in 0..15) kmmk.allNotesOff(i) }
                    stopIsPressed = true
                }
                is PressInteraction.Release -> { stopIsPressed = false }
                is PressInteraction.Cancel -> { stopIsPressed = false }
            }
        }
    }
    Button(
        interactionSource = interactionSource,
        onClick = {},
        shape = RoundedCornerShape(0.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(stopIsPressed) Color(0xFFBFBF00) else BackGray
        ),
        modifier = Modifier
            .size(80.dp),
    ) {
        Box{
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(if (stopIsPressed) 0.dp else 4.dp)
                .alpha(0.6f)){
                drawRect(
                    topLeft = Offset(26.dp.toPx(), 26.dp.toPx()),
                    size = Size(28.dp.toPx(), 28.dp.toPx()),
                    color = if(stopIsPressed) BackGray else Color(0xFFBFBF00),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        join = StrokeJoin.Round
                    )
                )
            }
            Canvas(modifier = Modifier.fillMaxSize()){
                drawRect(
                    topLeft = Offset(26.dp.toPx(), 26.dp.toPx()),
                    size = Size(28.dp.toPx(), 28.dp.toPx()),
                    color = if(stopIsPressed) BackGray else Color(0xFFBFBF00),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}


@Composable
fun EraseButton(seqViewModel: SeqViewModel, eraseButtonState: Boolean){
    Button(
        interactionSource = buttonInteraction(seqViewModel.toggleTime, { seqViewModel.eraseMode() }),
        onClick = {  },
        shape = RoundedCornerShape(0.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(eraseButtonState) Color.Red else BackGray
        ),
        modifier = Modifier
            .size(80.dp),
    ) {
        Box{
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(if (eraseButtonState) 0.dp else 5.dp)){
                val path = Path()
                path.moveTo(28.dp.toPx(), 36.dp.toPx())
                path.lineTo(36.dp.toPx(), 44.dp.toPx())
                path.moveTo(36.dp.toPx(), 36.dp.toPx())
                path.lineTo(28.dp.toPx(), 44.dp.toPx())
                path.moveTo(42.dp.toPx(), 52.dp.toPx())
                path.lineTo(54.dp.toPx(), 40.dp.toPx())
                path.lineTo(42.dp.toPx(), 28.dp.toPx())

                drawPath(
                    path = path,
                    color = if(eraseButtonState) BackGray else Color.Red,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
            Canvas(modifier = Modifier.fillMaxSize()){
                val path = Path()
                path.moveTo(28.dp.toPx(), 36.dp.toPx())
                path.lineTo(36.dp.toPx(), 44.dp.toPx())
                path.moveTo(36.dp.toPx(), 36.dp.toPx())
                path.lineTo(28.dp.toPx(), 44.dp.toPx())
                path.moveTo(42.dp.toPx(), 52.dp.toPx())
                path.lineTo(54.dp.toPx(), 40.dp.toPx())
                path.lineTo(42.dp.toPx(), 28.dp.toPx())

                drawPath(
                    path = path,
                    color = if(eraseButtonState) BackGray else Color.Red,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}


@Composable
fun MuteButton(seqViewModel: SeqViewModel, muteButtonState: Boolean){
    Button(
        interactionSource = buttonInteraction( seqViewModel.toggleTime, { seqViewModel.muteMode() } ),
        onClick = {  },
        shape = RoundedCornerShape(0.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(muteButtonState) Color.Green else BackGray
        ),
        modifier = Modifier
            .size(80.dp),
    ) {
        Box{
            Text("MUTE", fontSize = 20.nonScaledSp, color = if(muteButtonState) BackGray else Color.Green, modifier = Modifier
                .blur(if (muteButtonState) 0.dp else 4.dp, BlurredEdgeTreatment.Unbounded)
                .alpha(0.6f))
            Text("MUTE", fontSize = 20.nonScaledSp, color = if(muteButtonState) BackGray else Color.Green)
        }
    }
}


@Composable
fun ClearButton(seqViewModel: SeqViewModel, clearButtonState: Boolean){
    Button(
        interactionSource = buttonInteraction(0L, { seqViewModel.clearMode() }
        ),
        onClick = {  },
        shape = RoundedCornerShape(0.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(clearButtonState) Color.LightGray else BackGray
        ),
        modifier = Modifier
            .size(80.dp),
    ) {
        Box{
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(if (clearButtonState) 0.dp else 6.dp)
                .alpha(0.6f)){
                val path = Path()
                path.moveTo(28.dp.toPx(), 28.dp.toPx())
                path.lineTo(52.dp.toPx(), 52.dp.toPx())
                path.moveTo(52.dp.toPx(), 28.dp.toPx())
                path.lineTo(28.dp.toPx(), 52.dp.toPx())

                drawPath(
                    path = path,
                    color = if(clearButtonState) BackGray else Color.LightGray,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
            Canvas(modifier = Modifier.fillMaxSize()){
                val path = Path()
                path.moveTo(28.dp.toPx(), 28.dp.toPx())
                path.lineTo(52.dp.toPx(), 52.dp.toPx())
                path.moveTo(52.dp.toPx(), 28.dp.toPx())
                path.lineTo(28.dp.toPx(), 52.dp.toPx())

                drawPath(
                    path = path,
                    color = if(clearButtonState) BackGray else Color.LightGray,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}


@Composable
fun buttonInteraction(modeTime: Long, function1: () -> Unit, function2: () -> Unit = function1): MutableInteractionSource {
    val interactionSource = remember { MutableInteractionSource() }
    var elapsedTime = 0L
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> { function1(); elapsedTime = System.currentTimeMillis() }
                is PressInteraction.Release -> { if((System.currentTimeMillis() - elapsedTime) > modeTime) function2() }
                is PressInteraction.Cancel -> { if((System.currentTimeMillis() - elapsedTime) > modeTime) function2() }
            }
        }
    }
    return interactionSource
}


@Composable
fun VisualArray(seqUiState: SeqUiState) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(80.dp)
            .background(BackGray),
        contentAlignment = Alignment.TopStart
    ) {
        val gg = (seqUiState.deltaTime[0].toFloat() / seqUiState.seqTotalTime[0] * 200).dp  // TODO replace hardcoded channel
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                if (seqUiState.seqIsRecording || seqUiState.seqMode == ERASING) Color.Red else Color.Green,
                Offset(gg.toPx(), 0.dp.toPx()),
                Offset(gg.toPx(), 80.dp.toPx())
            )
            if(seqUiState.visualArrayRefresh) drawPoints(List(1){ Offset(0f,0f) }, PointMode.Points, Color.Black)
        }
        for(c in 0..3){
            for (i in seqUiState.visualArray[c].notes.indices) {
                Text(
                    text = if (seqUiState.visualArray[c].notes[i].velocity > 0) "[" else "]",
                    color = if(seqUiState.visualArray[c].notes[i].velocity > 0) Color(0xFFFFFFFF) else Color(0xFF999999),
                    modifier = Modifier.offset(
                        (seqUiState.visualArray[c].notes[i].time.toFloat() / seqUiState.seqTotalTime[0] * 200).toInt().dp,  // TODO replace hardcoded channel
                        ((3 - c) * 20).dp
                    )
                )
            }
        }
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
            Text(text = "!!", color = Color.Red)
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



@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
