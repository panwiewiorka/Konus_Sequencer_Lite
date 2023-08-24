package com.example.mysequencer01ui.ui.viewTabs

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.ui.BARTIME
import com.example.mysequencer01ui.ui.PadsGrid
import com.example.mysequencer01ui.ui.SeqUiState
import com.example.mysequencer01ui.ui.SeqViewModel
import com.example.mysequencer01ui.ui.buttonTextSize
import com.example.mysequencer01ui.ui.buttonsPadding
import com.example.mysequencer01ui.ui.nonScaledSp
import com.example.mysequencer01ui.ui.playHeads
import com.example.mysequencer01ui.ui.recomposeHighlighter
import com.example.mysequencer01ui.ui.repeatBounds
import com.example.mysequencer01ui.ui.theme.BackGray
import com.example.mysequencer01ui.ui.theme.buttons
import com.example.mysequencer01ui.ui.theme.buttonsBg
import com.example.mysequencer01ui.ui.theme.darkViolet
import com.example.mysequencer01ui.ui.theme.dusk
import com.example.mysequencer01ui.ui.theme.night
import com.example.mysequencer01ui.ui.theme.selectedButton
import com.example.mysequencer01ui.ui.theme.repeatButtons
import com.example.mysequencer01ui.ui.theme.violet
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun LiveView(seqViewModel: SeqViewModel, seqUiState: SeqUiState, buttonsSize: Dp) {
    val spacerSize = 0.dp
    val c = buttonsSize.value / sqrt(3f)
    val y = buttonsSize.value / 2
    val columnsOffset = -sqrt(c * c - y * y)

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        //KeyboardRow(kmmk, 1)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            PatternsScreen(seqUiState, buttonsSize)
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
//                Knob(buttonsSize, seqUiState.bpm, seqViewModel::changeBPM)
                PadsGrid(seqViewModel, seqUiState, buttonsSize)
            }
        }
        Spacer(modifier = Modifier.width(spacerSize))
        Row(horizontalArrangement = Arrangement.spacedBy(columnsOffset.dp)){
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxHeight()
            ) {
                repeat(5) {
                    RepeatButton(
                        changeRepeatDivisor = seqViewModel::changeRepeatDivisor,
                        width = buttonsSize,
                        divisor = 2f.pow(it + 1).toInt(),
                        divisorState = seqUiState.divisorState
                    )
                }
            }
            Column(){
                Spacer(modifier = Modifier.height(buttonsSize / 2))
                repeat(4) {
                    RepeatButton(
                        changeRepeatDivisor = seqViewModel::changeRepeatDivisor,
                        width = buttonsSize,
                        divisor = 3 * 2f.pow(it).toInt(),
                        divisorState = seqUiState.divisorState,
                        triplet = true
                    )
                }
            }
        }
    }
}


@Composable
fun PatternsScreen(seqUiState: SeqUiState, buttonsSize: Dp) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(buttonsSize)
            .padding(buttonsPadding)
            .background(BackGray)
    ) {
//        val playheadState = animateFloatAsState(
//            targetValue = if(!seqUiState.seqIsPlaying) 0f else BARTIME.toFloat(),
//            animationSpec = infiniteRepeatable(
//                TweenSpec(BARTIME, easing = LinearEasing)
//            ),
//            label = "playhead"
//        )

        Canvas(modifier = Modifier.fillMaxSize()) {

            // STEP LINES
            val horizontalStep = (maxWidth / 16).toPx()
            for (i in 1..15) {
                drawLine(
                    color = if((i + 4) % 4 == 0) buttons else buttonsBg,
                    start = Offset(i * horizontalStep, 0f),
                    end = Offset(i * horizontalStep, size.height),
                    strokeWidth = 2f,
//                    pathEffect = if((i + 4) % 4 == 0) null else PathEffect.dashPathEffect(floatArrayOf(size.height / 32, (size.height / 4 - size.height / 32)), size.height / 64)
                )
            }

            val verticalStep = (maxHeight / 4).toPx()
            for (i in 1..3) {
                drawLine(
                    color = buttonsBg,
                    start = Offset(0f, i * verticalStep),
                    end = Offset(size.width, i * verticalStep),
                    strokeWidth = 2f,
                )
            }

            for(c in 0..15) {
                with(seqUiState.sequences[c]) {
                    val widthFactor = size.width / totalTime
                    val playhead = (widthFactor * deltaTime).toFloat()
//                    val playhead = (widthFactor * playheadState.value)
                    val playheadRepeat = (widthFactor * deltaTimeRepeat).toFloat()

                    // NOTES
                    for (i in notes.indices) {
                        if(notes[i].velocity > 0) {
                            val noteStart = (widthFactor * notes[i].time).toFloat()
                            val noteOffIndexAndTime = getNotePairedIndexAndTime(i)
                            val noteOffIndex = noteOffIndexAndTime.index
                            val noteLength = widthFactor * (noteOffIndexAndTime.time - notes[i].time).toFloat()
                            val noteWidth =
                                if (noteOffIndex != -1) {
                                    if(noteLength > 0f && noteLength <= size.height / 16) {
                                        size.height / 16
                                    } else noteLength
                                } else {
                                    if (seqUiState.isRepeating) playheadRepeat - noteStart else playhead - noteStart  // live-writing note (grows in length)
                                }

                            val fasterThanHalfOfQuantize = System.currentTimeMillis() - pressedNotes[notes[i].pitch].noteOnTimestamp <= seqUiState.quantizationTime / seqUiState.factorBpm / 2
                            if (noteWidth > 0  || (noteOffIndex == -1 && fasterThanHalfOfQuantize)) {   // normal note (not wrap-around) [...]
                                drawRoundRect(
                                    color = if (seqUiState.sequences[seqUiState.selectedChannel].channel == c) violet else darkViolet,
                                    topLeft = Offset(noteStart, size.height - ((c + 1) * size.height / 16)),
                                    size = Size(
                                        width = noteWidth,
                                        height = size.height / 16),
                                    cornerRadius = CornerRadius(size.height, size.height)
                                )
                            } else {   // wrap-around note

                                // [..
                                drawRoundRect(
                                    color = if (seqUiState.sequences[seqUiState.selectedChannel].channel == c) violet else darkViolet,
                                    topLeft = Offset(noteStart,size.height - ((c + 1) * size.height / 16)),
                                    size = Size(
                                        width = size.width - noteStart,
                                        height = size.height / 16),
                                    cornerRadius = CornerRadius(size.height, size.height)
                                )
                                val halfTheLength = noteStart + (size.width - noteStart) / 2
                                drawRect(
                                    color = if (seqUiState.sequences[seqUiState.selectedChannel].channel == c) violet else darkViolet,
                                    topLeft = Offset(halfTheLength,size.height - ((c + 1) * size.height / 16)),
                                    size = Size(
                                        width = size.width - halfTheLength,
                                        height = size.height / 16),
                                )

                                // ..]
                                drawRoundRect(
                                    color = if (seqUiState.sequences[seqUiState.selectedChannel].channel == c) violet else darkViolet,
                                    topLeft = Offset(0f,size.height - ((c + 1) * size.height / 16)),
                                    size = Size(
                                        width = noteWidth + noteStart,  // noteWidth is negative here
                                        height = size.height / 16),
                                    cornerRadius = CornerRadius(size.height, size.height)
                                )
                                drawRect(
                                    color = if (seqUiState.sequences[seqUiState.selectedChannel].channel == c) violet else darkViolet,
                                    topLeft = Offset(0f,size.height - ((c + 1) * size.height / 16)),
                                    size = Size(
                                        width = (noteWidth + noteStart) / 2,  // noteWidth is negative here
                                        height = size.height / 16),
                                )
                            }
                        }
                    }

                    if (c == 15) playHeads(seqUiState, playhead, playheadRepeat)

                    if (seqUiState.isRepeating) repeatBounds(this, widthFactor, 0.1f)
                }
            }
        }
        if (seqUiState.visualDebugger) {
            VisualDebugger(seqUiState, buttonsSize)
        }
    }
}


@Composable
fun VisualDebugger(seqUiState: SeqUiState, height: Dp) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(buttonsPadding)
            .background(Color.Transparent),
        contentAlignment = Alignment.TopStart
    ) {
        Column() {
            with(seqUiState.sequences[0]) {
                Text(
                    "noteId = ${noteId - Int.MIN_VALUE},    " +
                        "notes ON: ${playingNotes[60]},     ",
                    color = selectedButton)
                Text(
                    "#toPlay = $indexToPlay,     " +
                        "#toRepeat = $indexToRepeat      " +
                        "#toStartRepeating = $indexToStartRepeating",
                    color = selectedButton)
            }
        }
        for(c in 0..3){
            with(seqUiState.sequences[c]) {
                for (i in notes.indices) {
                    Text(
                        text =
                            when (seqUiState.debuggerViewSetting) {
                                0 -> "$i"
                                else-> "${notes[i].id - Int.MIN_VALUE}"
                            }
                        ,
                        color = if(notes[i].velocity > 0) Color(0xFFFFFFFF) else Color(0xFF999999),
                        modifier = Modifier.offset(
                            (notes[i].time.toFloat() / totalTime * maxWidth.value).dp,
                            height - height / 4 * (c + 1) - 2.dp
                        )
                    )
                }
            }
        }
    }
}


@Composable
fun RepeatButton(
    changeRepeatDivisor: (Int) -> Unit,
    width: Dp,
    divisor: Int,
    divisorState: Int,
    triplet: Boolean = false,
){
    val leftToHexShape = GenericShape { size, _ ->
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
    val offset = if(triplet) width / 16 else width / -16

        /*
    val hexagonShape = GenericShape { size, _ ->
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
         */

    val rightToHexShape = GenericShape { size, _ ->
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

    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource, divisorState) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    changeRepeatDivisor(divisor)
                }
                is PressInteraction.Release -> {
                    if (divisorState == divisor) changeRepeatDivisor(0)
                }
                is PressInteraction.Cancel -> { }
            }
        }
    }

    Button(
        interactionSource = interactionSource,
        //elevation = null,
        onClick = {  },
        shape = if(triplet) rightToHexShape else leftToHexShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(divisor == divisorState) dusk else repeatButtons
        ),
        modifier = Modifier
            .height(width)
            .width(width)
            //.clip(if(triplet) rightToHexShape else leftToHexShape)
            //.border(BorderStroke(0.3.dp, buttonsColor))
            .padding(buttonsPadding),
    ) {
        Box(
            modifier = Modifier.offset(offset, 0.dp)
        ) {
            if (divisor != divisorState) {
                Text(
                    divisor.toString(),
                    fontSize = buttonTextSize.nonScaledSp,
                    color = if (triplet) night else dusk,
                    modifier = Modifier
                        .blur(6.dp, BlurredEdgeTreatment.Unbounded)
                        .alpha(0.6f)
                )
            }
            Text(
                divisor.toString(),
                fontSize = buttonTextSize.nonScaledSp,
                color = if (divisor != divisorState) {
                    if (triplet) night else dusk
                } else buttons,
            )
        }
    }
}