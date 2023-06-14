package com.example.mysequencer01ui.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysequencer01ui.KmmkComponentContext
import com.example.mysequencer01ui.PadsMode
import com.example.mysequencer01ui.PadsMode.*

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.example.mysequencer01ui.ui.theme.*
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlin.math.sqrt


val Int.nonScaledSp
    @Composable
    get() = (this / LocalDensity.current.fontScale).sp

val padsSize = 119.dp
val buttonsPadding = PaddingValues(top = 1.dp, end = 1.dp)
val buttonsShape = RoundedCornerShape(0.dp)
const val buttonTextSize = 12


@Composable
fun StepSequencer(seqUiState: SeqUiState, buttonsSize: Dp) {
    Box(
        modifier = Modifier
            .size(padsSize * 2, buttonsSize + 1.dp)
            .padding(1.dp)
            .background(screensBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {

            // STEP LINES
            val step = (padsSize / 8).toPx()
            for (i in 0..15) {
                drawLine(
                    if((i + 4) % 4 == 0) Color.White else Color.Gray,
                    Offset(i * step, if((i + 4) % 4 == 0) 0f else (size.height / 1.1f)),
                    Offset(i * step, size.height)
                )
            }

            for(c in 0..3) {
                with(seqUiState.sequences[c]) {
                    val playhead = (deltaTime / totalTime * size.width).toFloat()
                    var playheadRepeat = (deltaTimeRepeat / totalTime * size.width).toFloat()
                    playheadRepeat = if(playheadRepeat < 0) playheadRepeat + size.width else playheadRepeat

                    // NOTES
                    for (i in notes.indices) {
                        val noteStart = notes[i].time.toFloat() / totalTime * size.width
                        val noteWidth =
                            if(notes[i].length != 0) {   // TODO null instead of 0?  0 might be a rare case of wrap-around note?
                                notes[i].length.toFloat() / totalTime * size.width   // TODO minimum visible length
                            } else {
                                if(seqUiState.isRepeating) playheadRepeat - noteStart else playhead - noteStart  // live-writing note (grows in length)
                            }
                        if(notes[i].velocity > 0) {
                            if(notes[i].length >= 0 && noteWidth >= 0) { // normal note (not wrap-around)  // TODO {&& noteWidth >= 0} unnecessary?
                                drawRect(
                                    if(seqUiState.sequences[seqUiState.selectedChannel].channel == c) selectedNoteSquare else noteSquare,
                                    Offset(noteStart, size.height - ((c + 1) * size.height / 4)),
                                    Size(
                                        width = noteWidth,
                                        height = size.height / 4)
                                )
                            } else {   // wrap-around note
                                drawRect(
                                    if(seqUiState.sequences[seqUiState.selectedChannel].channel == c) selectedNoteSquare else noteSquare,
                                    Offset(noteStart,size.height - ((c + 1) * size.height / 4)),
                                    Size(
                                        width = size.width - noteStart,
                                        height = size.height / 4)
                                )
                                drawRect(
                                    if(seqUiState.sequences[seqUiState.selectedChannel].channel == c) selectedNoteSquare else noteSquare,
                                    Offset(0f,size.height - ((c + 1) * size.height / 4)),
                                    Size(
                                        width = noteWidth + noteStart,  // noteWidth is negative here
                                        height = size.height / 4)
                                )
                            }
                        }
                    }

                    // playhead color
                    val conditionalColor = when (seqUiState.padsMode) {
                        MUTING -> violet
                        ERASING -> warmRed
                        CLEARING -> notWhite
                        else -> {
                            if (seqUiState.seqIsRecording) warmRed
                            else playGreen
                        }
                    }
                    // PLAYHEAD
                    drawLine(
                        color = conditionalColor,
                        start = Offset(playhead, 0f),
                        end = Offset(playhead, size.height)
                    )
                    // REPEAT PLAYHEAD
                    if(seqUiState.isRepeating && seqUiState.seqIsPlaying) {
                        drawLine(
                            color = conditionalColor,
                            start = Offset(playheadRepeat, 0f),
                            end = Offset(playheadRepeat, size.height),
                            4f,
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun VisualArray(seqUiState: SeqUiState, height: Dp) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(1.dp)
            .background(screensBg),
        contentAlignment = Alignment.TopStart
    ) {
        val playhead = ((seqUiState.sequences[0].deltaTime) / seqUiState.sequences[0].totalTime * maxWidth.value).dp  // TODO replace hardcoded channel
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                if (seqUiState.seqIsRecording || seqUiState.padsMode == ERASING) warmRed else if(seqUiState.padsMode == CLEARING) Color.White else violet,
                Offset(playhead.toPx(), 0.dp.toPx()),
                Offset(playhead.toPx(), (height - 2.dp).toPx())
            )
            if(seqUiState.visualArrayRefresh) drawPoints(List(1){ Offset(0f,0f) }, PointMode.Points, violet)
        }
        for(c in 0..3){
            for (i in seqUiState.sequences[c].notes.indices) {
                Text(
                    text = if (seqUiState.sequences[c].notes[i].velocity > 0) "[" else "]",
                    color = if(seqUiState.sequences[c].notes[i].velocity > 0) Color(0xFFFFFFFF) else Color(0xFF999999),
                    modifier = Modifier.offset(
                        (seqUiState.sequences[c].notes[i].time.toFloat() / seqUiState.sequences[c].totalTime * maxWidth.value).dp,
                        height - height / 4 * (c + 1) - 2.dp
                    )
                )
            }
        }
    }
}


@Composable
fun PadButton(
    channel: Int,
    pitch: Int,
    seqViewModel: SeqViewModel,
    seqUiState: SeqUiState,
){
    val interactionSource = remember { MutableInteractionSource() }
    var elapsedTime = remember { 0L }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    seqViewModel.pressPad(channel, pitch, 100)
                    elapsedTime = System.currentTimeMillis()
                }
                is PressInteraction.Release -> {
                    seqViewModel.pressPad(channel, pitch, 0, elapsedTime)
                }
                is PressInteraction.Cancel -> { seqViewModel.pressPad(channel, pitch, 0, elapsedTime) }
            }
        }
    }
    Box(modifier = Modifier.padding(buttonsPadding)){
        Button(
            interactionSource = interactionSource,
            onClick = {},
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = buttonsColor
            ),
            modifier = Modifier
                .size(padsSize)
                .border(
                    width = 4.dp,
                    color = if (seqUiState.sequences[channel].channelIsPlayingNotes) {
                        when (seqUiState.padsMode) {
                            MUTING -> violet
                            ERASING -> warmRed
                            CLEARING -> notWhite
                            else -> {
                                if (seqUiState.seqIsRecording) warmRed
                                else if (seqUiState.seqIsPlaying) playGreen
                                else Color.Transparent
                            }
                        }
                    } else Color.Transparent
                )
//                .border(width = 4.dp, brush = Brush.radialGradient(
//                    if (seqUiState.sequences[channel].channelIsPlayingNotes) {
//                        when (seqUiState.seqMode) {
//                            MUTING -> listOf(violet, violet, Color.Transparent)
//                            ERASING -> listOf(warmRed, warmRed, Color.Transparent)
//                            CLEARING -> listOf(Color.White, Color.White, Color.Transparent)
//                            else -> {
//                                if (seqUiState.seqIsRecording) listOf(warmRed, warmRed, Color.Transparent)
//                                else if (seqUiState.seqIsPlaying) listOf(Color(0xFF008800), Color(0xFF008800), Color.Transparent)
//                                else listOf(Color.Transparent, Color.Transparent)
//                            }
//                        }
//                    } else listOf(Color.Transparent, Color.Transparent), radius = 250f
//                ),
//                    shape = RoundedCornerShape(0.dp)
//                )
        ) {
            if(seqUiState.sequences[channel].isMuted) Text("MUTED")
        }
    }
}


@Composable
fun PadsGrid(seqViewModel: SeqViewModel, seqUiState: SeqUiState){
    Box(
        contentAlignment = Alignment.Center,
    ){
        Column {
            Row(
                //modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PadButton(2, 26, seqViewModel, seqUiState)
                PadButton(3, 39, seqViewModel, seqUiState)
            }
            Row(
                //modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PadButton(0, 4, seqViewModel, seqUiState)
                PadButton(1, 14, seqViewModel, seqUiState)
            }
        }
    }
}


@Composable
fun AllButton(seqViewModel: SeqViewModel, buttonsSize: Dp){
    val interactionSource = remember { MutableInteractionSource() }
    var elapsedTime = remember { 0L }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    for(i in 0..15){
                        seqViewModel.pressPad(i, 26, 100, allButton = true)
                    }
                    seqViewModel.recomposeVisualArray()
                    elapsedTime = System.currentTimeMillis()
                }
                is PressInteraction.Release -> {
                    for(i in 0..15){
                        seqViewModel.pressPad(i, 26, 0, elapsedTime, allButton = true)
                    }
                    seqViewModel.recomposeVisualArray()
                }
                is PressInteraction.Cancel -> {
                    for(i in 0..15){
                        seqViewModel.pressPad(i, 26, 0, elapsedTime, allButton = true)
                    }
                    seqViewModel.recomposeVisualArray()
                }
            }
        }
    }
    Button(
        interactionSource = interactionSource,
        onClick = {},
        shape = RoundedCornerShape(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = buttonsColor
        ),
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
    ) {
        Box{
            Text("ALL", fontSize = buttonTextSize.nonScaledSp, color = Color.White, modifier = Modifier
                .blur(6.dp, BlurredEdgeTreatment.Unbounded)
                .alpha(0.6f))
            Text("ALL", fontSize = buttonTextSize.nonScaledSp, color = notWhite)
        }
    }
}


@Composable
fun RecButton(seqViewModel: SeqViewModel, padsMode: PadsMode, seqIsRecording: Boolean, buttonsSize: Dp){
    Button(
        interactionSource = buttonInteraction(
            seqViewModel.toggleTime,
            { seqViewModel.changeRecState() }
        ),
        onClick = {  },
        shape = buttonsShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(padsMode == DEFAULT && seqIsRecording) warmRed else buttonsColor
        ),
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
    ) {
        Box{
            if (!(seqIsRecording && padsMode == DEFAULT)) {
                Canvas(modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.6f)
                    .blur(4.dp)){
                    recSymbol(padsMode, seqIsRecording)
                }
            }
            Canvas(modifier = Modifier.fillMaxSize()){
                recSymbol(padsMode, seqIsRecording)
            }
        }
    }
}


@Composable
fun PlayButton(seqViewModel: SeqViewModel, seqIsPlaying: Boolean, buttonsSize: Dp){
    Button(
        interactionSource = buttonInteraction(
            seqViewModel.toggleTime,
            { seqViewModel.startSeq() },
            { seqViewModel.stopSeq() }
        ),
        onClick = {  },
        shape = buttonsShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
//            backgroundColor = if(seqIsPlaying) Color(0xFF00AA00) else bg2
            backgroundColor = buttonsColor
        ),
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding),
    ) {
        Box{
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(6.dp)
                .alpha(0.6f)){
                playSymbol(seqIsPlaying)
            }
            Canvas(modifier = Modifier.fillMaxSize()){
                playSymbol(seqIsPlaying)
            }
        }
    }
}

/*
@Composable
fun StopButton(seqViewModel: SeqViewModel, kmmk: KmmkComponentContext){
    val interactionSource = remember { MutableInteractionSource() }
    var stopIsPressed by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    seqViewModel.stopSeq()
                    kmmk.allNotesOff()
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
        shape = buttonsShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(stopIsPressed) Color(0xFFBFBF00) else bg2
        ),
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding),
    ) {
        Box{
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(if (stopIsPressed) 0.dp else 4.dp)
                .alpha(0.6f)){
                stopSymbol(stopIsPressed)
            }
            Canvas(modifier = Modifier.fillMaxSize()){
                stopSymbol(stopIsPressed)
            }
        }
    }
}
*/

@Composable
fun EraseButton(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonsSize: Dp){
    Button(
        interactionSource = buttonInteraction(
            seqViewModel.toggleTime,
            { seqViewModel.editCurrentMode(ERASING) },
            { seqViewModel.editCurrentMode(ERASING, true) }
        ),
        onClick = {  },
        shape = buttonsShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(padsMode == ERASING) warmRed else buttonsColor
        ),
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding),
    ) {
        Box{
//            Text("X>>", fontSize = buttonTextSize.nonScaledSp, color = notWhite, modifier = Modifier
//                .blur(6.dp, BlurredEdgeTreatment.Unbounded)
//                .alpha(0.6f))
//            Text("X>>", fontSize = buttonTextSize.nonScaledSp, color = notWhite)
            val r = Canvas(modifier = Modifier
                .fillMaxSize()
                .alpha(0.6f)
                .blur(if (padsMode == ERASING) 0.dp else 5.dp)){
                eraseSymbol(padsMode)
            }
            Canvas(modifier = Modifier.fillMaxSize()){
                eraseSymbol(padsMode)
            }
        }
    }
}


@Composable
fun MuteButton(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonsSize: Dp){
    Button(
        interactionSource = buttonInteraction(
            seqViewModel.toggleTime,
            { seqViewModel.editCurrentMode(MUTING) },
            { seqViewModel.editCurrentMode(MUTING, true) }
        ),
        onClick = {  },
        shape = buttonsShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(padsMode == MUTING) violet else buttonsColor
        ),
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding),
    ) {
        Box{
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(if (padsMode == MUTING) 0.dp else 6.dp)
                .alpha(0.6f)){
                muteSymbol(padsMode)
            }
            Canvas(modifier = Modifier.fillMaxSize()){
                muteSymbol(padsMode)
            }
        }
    }
}


@Composable
fun SoloButton(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonsSize: Dp){
    Button(
        interactionSource = buttonInteraction(
            seqViewModel.toggleTime,
            {  },
            {  }
        ),
        onClick = {  },
        shape = buttonsShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(false) notWhite else buttonsColor
        ),
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding),
    ) {
        Box(modifier = Modifier.rotate(24f)){
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(if (false) 0.dp else 6.dp)
                .alpha(0.6f)){
                soloSymbol()
            }
            Canvas(modifier = Modifier.fillMaxSize()){
                soloSymbol()
            }
        }
    }
}


@Composable
fun ClearButton(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonsSize: Dp){
    Button(
        interactionSource = buttonInteraction(
            0,
            { seqViewModel.editCurrentMode(CLEARING) },
            { seqViewModel.editCurrentMode(CLEARING, true) }
        ),
        onClick = {  },
        shape = buttonsShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(padsMode == CLEARING) notWhite else buttonsColor
        ),
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding),
    ) {
        Box{
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(if (padsMode == CLEARING) 0.dp else 6.dp)
                .alpha(0.6f)){
                clearSymbol(padsMode)
            }
            Canvas(modifier = Modifier.fillMaxSize()){
                clearSymbol(padsMode)
            }
        }
    }
}


@Composable
fun QuantizeButton(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonsSize: Dp){
    Button(
        interactionSource = buttonInteraction(
            seqViewModel.toggleTime,
            {  },
            {  }
        ),
        onClick = {  },
        shape = buttonsShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(false) notWhite else buttonsColor
        ),
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding),
    ) {
        Box{
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(if (false) 0.dp else 6.dp)
                .alpha(0.6f)){
                quantizeSymbol(padsMode)
            }
            Canvas(modifier = Modifier.fillMaxSize()){
                quantizeSymbol(padsMode)
            }
        }
    }
}


@Composable
fun SaveButton(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonsSize: Dp){  // similar to ClearButton, same as LoadButton
    Button(
        interactionSource = buttonInteraction(
            0,
            { seqViewModel.editCurrentMode(SAVING) },
            { seqViewModel.editCurrentMode(SAVING, true) }
        ),
        onClick = {  },
        shape = buttonsShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(padsMode == SAVING) dusk else buttonsColor
        ),
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()){
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(if (padsMode == SAVING) 0.dp else 6.dp) // TODO move IF up from blur to Canvas (in all Composables)
                .alpha(0.6f)){
                saveLoadSymbol(padsMode)
                saveArrow(padsMode)
            }
            Canvas(modifier = Modifier
                .fillMaxSize()){
                saveLoadSymbol(padsMode)
                saveArrow(padsMode)
            }
        }
    }
}


@Composable
fun LoadButton(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonsSize: Dp){
    Button(
        interactionSource = buttonInteraction(
            0,
            { seqViewModel.editCurrentMode(LOADING) },
            { seqViewModel.editCurrentMode(LOADING, true) }
        ),
        onClick = {  },
        shape = buttonsShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(padsMode == LOADING) dusk else buttonsColor
        ),
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()){
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(if (padsMode == LOADING) 0.dp else 6.dp) // TODO move IF up from blur to Canvas (in all Composables)
                .alpha(0.6f)){
                saveLoadSymbol(padsMode)
                loadArrow(padsMode)
            }
            Canvas(modifier = Modifier
                .fillMaxSize()){
                saveLoadSymbol(padsMode)
                loadArrow(padsMode)
            }
        }
    }
}


@Composable
fun RepeatButton(
    seqViewModel: SeqViewModel,
    width: Dp,
    divisor: Int,
    divisorState: Int,
    triplet: Boolean = false,
){
    val leftToHexShape = GenericShape { size: Size, layoutDirection: LayoutDirection ->
        val c = size.height / sqrt(3f)
        val y = size.height / 2
        val x = sqrt(c * c - y * y)
        val o = x / 2
        lineTo(o + c, 0f)
        lineTo(o + c + x, y)
        lineTo(o + c, y * 2)
        lineTo(0f, y * 2)
        lineTo(0f, 0f)
        close()
    }
    val offset = if(triplet) 0.dp else width / -16
    val hexagonShape = GenericShape { size: Size, layoutDirection: LayoutDirection ->
        val c = size.height / sqrt(3f)
        val y = size.height / 2
        val x = sqrt(c * c - y * y)
        moveTo(x, 0f)
        lineTo(x + c, 0f)
        lineTo(x + c + x, y)
        lineTo(x + c, y * 2)
        lineTo(x, y * 2)
        lineTo(0f, y)
        lineTo(x, 0f)
        close()
    }

    val interactionSource = remember { MutableInteractionSource() }
    var buttonIsPressed by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    seqViewModel.repeat(divisor)
                    buttonIsPressed = true
                }
                is PressInteraction.Release -> {
                    seqViewModel.repeat(0)
                    buttonIsPressed = false
                }
                is PressInteraction.Cancel -> { }
            }
        }
    }
    Button(
        interactionSource = interactionSource,
        onClick = {  },
        shape = if(triplet) hexagonShape else leftToHexShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(divisor == divisorState) dusk else buttonsColor
        ),
        modifier = Modifier
            .height(width)
            .width(if(triplet) (2 * width.value / sqrt(3f)).dp else width)
            .padding(buttonsPadding),
    ) {
        Box(modifier = Modifier.offset(offset, 0.dp)){
            if(divisor != divisorState) {
                Text(divisor.toString(), fontSize = buttonTextSize.nonScaledSp, color = if(triplet) night else dusk, modifier = Modifier
                    .blur(6.dp, BlurredEdgeTreatment.Unbounded)
                    .alpha(0.6f)
                    )
            }
            Text(divisor.toString(),
                fontSize = buttonTextSize.nonScaledSp,
                color = if(divisor != divisorState) {
                    if(triplet) night else dusk
                } else buttonsColor,
                )
        }
    }
}


@Composable
fun SeqView(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonsSize: Dp, text: String, textColor: Color = dusk, fontSize: Int = buttonTextSize,){
    val rightToHexShape = GenericShape { size: Size, layoutDirection: LayoutDirection ->
        val c = size.height / sqrt(3f)
        val y  = size.height / 2
        val x = sqrt(c * c - y * y)
        val o = x / 2
        moveTo(x, 0f)
        lineTo(x + c + o, 0f)
        lineTo(x + c + o, y * 2)
        lineTo(x, y * 2)
        lineTo(0f, y)
        lineTo(x, 0f)
        close()
    }
    Button(
        interactionSource = buttonInteraction(
            seqViewModel.toggleTime,
            {  },
            {  }
        ),
        onClick = {  },
        shape = buttonsShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(text == "ϴ") night else screensBg
        ),
        modifier = Modifier
            .height(buttonsSize)
            .width(buttonsSize)
            .padding(buttonsPadding)
            .clip(rightToHexShape),
    ) {
        Box(modifier = Modifier.offset(buttonsSize / 12, 0.dp)){
            if(text != "ϴ") {
                Text(text, fontSize = fontSize.nonScaledSp, color = if(text == "ϴ") BackGray else textColor, modifier = Modifier
                    .blur(6.dp, BlurredEdgeTreatment.Unbounded)
                    .alpha(0.6f))
            }
            Text(text, fontSize = fontSize.nonScaledSp, color = if(text == "ϴ") BackGray else textColor)
//            Canvas(modifier = Modifier
//                .fillMaxSize()
//                .blur(if (false) 0.dp else 6.dp)
//                .alpha(0.6f)){
//                quantizeSymbol(padsMode)
//            }
//            Canvas(modifier = Modifier.fillMaxSize()){
//                quantizeSymbol(padsMode)
//            }
        }
    }
}


@Composable
fun SymbolButton(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonsSize: Dp){
    Button(
        interactionSource = buttonInteraction(
            seqViewModel.toggleTime,
            {  },
            {  }
        ),
        onClick = {  },
        shape = buttonsShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(false) notWhite else buttonsColor
        ),
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding),
    ) {
        Box{
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(if (false) 0.dp else 6.dp)
                .alpha(0.6f)){
                quantizeSymbol(padsMode)
            }
            Canvas(modifier = Modifier.fillMaxSize()){
                quantizeSymbol(padsMode)
            }
        }
    }
}


@Composable
fun TextButton(
    seqViewModel: SeqViewModel,
    padsMode: PadsMode,
    buttonsSize: Dp,
    text: String = " ",
    textColor: Color = notWhite,
    fontSize: Int = buttonTextSize,
){
    Button(
        interactionSource = buttonInteraction(
            seqViewModel.toggleTime,
            {  },
            {  }
        ),
        onClick = {  },
        shape = buttonsShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(false) notWhite else buttonsColor
        ),
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding),
    ) {
        Box{
            Text(text, fontSize = fontSize.nonScaledSp, color = textColor, modifier = Modifier
                .blur(6.dp, BlurredEdgeTreatment.Unbounded)
                .alpha(0.6f))
            Text(text, fontSize = fontSize.nonScaledSp, color = textColor)
        }
    }
}


@Composable
fun ShiftButton(
    seqViewModel: SeqViewModel,
    padsMode: PadsMode,
    buttonsSize: Dp,
    text: String = " ",
    textColor: Color = notWhite,
    fontSize: Int = buttonTextSize,
){
    Button(
        interactionSource = buttonInteraction(
            seqViewModel.toggleTime,
            {  },
            {  }
        ),
        onClick = {  },
        shape = buttonsShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(false) notWhite else buttonsColor
        ),
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding),
    ) {
        Box{
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(if (false) 0.dp else 6.dp)
                .alpha(0.6f)){
                shiftSymbol(padsMode)
            }
            Canvas(modifier = Modifier.fillMaxSize()){
                shiftSymbol(padsMode)
            }
        }
//        BoxWithConstraints{
//            Text(text, fontSize = fontSize.nonScaledSp, color = textColor, modifier = Modifier
//                .blur(6.dp, BlurredEdgeTreatment.Unbounded)
//                .alpha(0.6f)
//                .offset(0.dp, maxHeight / 24))
//            Text(text, fontSize = fontSize.nonScaledSp, color = textColor, modifier = Modifier.offset(0.dp, maxHeight / 24))
//        }
    }
}



@Composable
fun buttonInteraction(
    toggleTime: Int,
    function1: () -> Unit,
    function2: () -> Unit = { function1() }
): MutableInteractionSource {
    val interactionSource = remember { MutableInteractionSource() }
    var elapsedTime = 0L
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    function1(); elapsedTime = System.currentTimeMillis()
                }
                is PressInteraction.Release -> {
                    if ((System.currentTimeMillis() - elapsedTime) > toggleTime) function2()
                }
                is PressInteraction.Cancel -> {
                    if ((System.currentTimeMillis() - elapsedTime) > toggleTime) function2()
                }
            }
        }
    }
    Log.d("emptyTag", " ") // to hold in imports
    return interactionSource

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



@Stable
fun Modifier.recomposeHighlighter(): Modifier = this.then(recomposeModifier)

// Use a single instance + @Stable to ensure that recompositions can enable skipping optimizations
// Modifier.composed will still remember unique data per call site.
private val recomposeModifier =
    Modifier.composed(inspectorInfo = debugInspectorInfo { name = "recomposeHighlighter" }) {
        // The total number of compositions that have occurred. We're not using a State<> here be
        // able to read/write the value without invalidating (which would cause infinite
        // recomposition).
        val totalCompositions = remember { arrayOf(0L) }
        totalCompositions[0]++

        // The value of totalCompositions at the last timeout.
        val totalCompositionsAtLastTimeout = remember { mutableStateOf(0L) }

        // Start the timeout, and reset everytime there's a recomposition. (Using totalCompositions
        // as the key is really just to cause the timer to restart every composition).
        LaunchedEffect(totalCompositions[0]) {
            delay(3000)
            totalCompositionsAtLastTimeout.value = totalCompositions[0]
        }

        Modifier.drawWithCache {
            onDrawWithContent {
                // Draw actual content.
                drawContent()

                // Below is to draw the highlight, if necessary. A lot of the logic is copied from
                // Modifier.border
                val numCompositionsSinceTimeout =
                    totalCompositions[0] - totalCompositionsAtLastTimeout.value

                val hasValidBorderParams = size.minDimension > 0f
                if (!hasValidBorderParams || numCompositionsSinceTimeout <= 0) {
                    return@onDrawWithContent
                }

                val (color, strokeWidthPx) =
                    when (numCompositionsSinceTimeout) {
                        // We need at least one composition to draw, so draw the smallest border
                        // color in blue.
                        1L -> Color.Blue to 1f
                        // 2 compositions is _probably_ okay.
                        2L -> violet to 2.dp.toPx()
                        // 3 or more compositions before timeout may indicate an issue. lerp the
                        // color from yellow to red, and continually increase the border size.
                        else -> {
                            lerp(
                                Color.Yellow.copy(alpha = 0.8f),
                                warmRed.copy(alpha = 0.5f),
                                min(1f, (numCompositionsSinceTimeout - 1).toFloat() / 100f)
                            ) to numCompositionsSinceTimeout.toInt().dp.toPx()
                        }
                    }

                val halfStroke = strokeWidthPx / 2
                val topLeft = Offset(halfStroke, halfStroke)
                val borderSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)

                val fillArea = (strokeWidthPx * 2) > size.minDimension
                val rectTopLeft = if (fillArea) Offset.Zero else topLeft
                val size = if (fillArea) size else borderSize
                val style = if (fillArea) Fill else Stroke(strokeWidthPx)

                drawRect(
                    brush = SolidColor(color),
                    topLeft = rectTopLeft,
                    size = size,
                    style = style
                )
            }
        }
    }