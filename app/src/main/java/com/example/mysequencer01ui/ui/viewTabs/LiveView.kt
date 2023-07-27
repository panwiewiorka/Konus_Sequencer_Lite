package com.example.mysequencer01ui.ui.viewTabs

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.PadsMode
import com.example.mysequencer01ui.ui.PadsGrid
import com.example.mysequencer01ui.ui.SeqUiState
import com.example.mysequencer01ui.ui.SeqViewModel
import com.example.mysequencer01ui.ui.buttonTextSize
import com.example.mysequencer01ui.ui.buttonsPadding
import com.example.mysequencer01ui.ui.nonScaledSp
import com.example.mysequencer01ui.ui.playHeads
import com.example.mysequencer01ui.ui.repeatBounds
import com.example.mysequencer01ui.ui.theme.BackGray
import com.example.mysequencer01ui.ui.theme.buttonsColor
import com.example.mysequencer01ui.ui.theme.dusk
import com.example.mysequencer01ui.ui.theme.night
import com.example.mysequencer01ui.ui.theme.notWhite
import com.example.mysequencer01ui.ui.theme.noteSquare
import com.example.mysequencer01ui.ui.theme.playGreen
import com.example.mysequencer01ui.ui.theme.selectedButton
import com.example.mysequencer01ui.ui.theme.selectedNoteSquare
import com.example.mysequencer01ui.ui.theme.seqBg
import com.example.mysequencer01ui.ui.theme.violet
import com.example.mysequencer01ui.ui.theme.warmRed
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
//                .width(300.dp)
        ) {
            PatternsScreen(seqUiState, buttonsSize)
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Knob(buttonsSize, seqUiState.bpm, seqViewModel::changeBPM)
                PadsGrid(seqViewModel, seqUiState, buttonsSize)
            }
        }
        Spacer(modifier = Modifier.width(spacerSize))
        Row(horizontalArrangement = Arrangement.spacedBy(columnsOffset.dp)){
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxHeight()
            ) {
                RepeatButton(seqViewModel, buttonsSize, 2, seqUiState.divisorState)
                RepeatButton(seqViewModel, buttonsSize, 4, seqUiState.divisorState)
                RepeatButton(seqViewModel, buttonsSize, 8, seqUiState.divisorState)
                RepeatButton(seqViewModel, buttonsSize, 16, seqUiState.divisorState)
                RepeatButton(seqViewModel, buttonsSize, 32, seqUiState.divisorState)
            }
            Column(){
                Spacer(modifier = Modifier.height(buttonsSize / 2))
                RepeatButton(seqViewModel, buttonsSize, 3, seqUiState.divisorState, true)
                RepeatButton(seqViewModel, buttonsSize, 6, seqUiState.divisorState, true)
                RepeatButton(seqViewModel, buttonsSize, 12, seqUiState.divisorState, true)
                RepeatButton(seqViewModel, buttonsSize, 24, seqUiState.divisorState, true)
            }
        }
    }
}


@Composable
fun PatternsScreen(seqUiState: SeqUiState, buttonsSize: Dp) {    // TODO move into LiveView
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(buttonsSize)
            .padding(buttonsPadding)
            .background(BackGray)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {

            // STEP LINES
            val step = (maxWidth / 16).toPx()
            for (i in 0..15) {
                drawLine(
                    color = if((i + 4) % 4 == 0) buttonsColor else buttonsColor,
                    start = Offset(i * step, if((i + 4) % 4 == 0) 0f else (size.height / 2 - size.height / 32)),
                    end = Offset(i * step, if((i + 4) % 4 == 0) size.height else size.height / 2 + size.height / 32),
                    strokeWidth = 2f
                )
            }

            for(c in 0..3) {
                with(seqUiState.sequences[c]) {
                    val widthFactor = size.width / totalTime
                    val playhead = (widthFactor * deltaTime).toFloat()
                    val playheadRepeat = (widthFactor * fullDeltaTimeRepeat).toFloat()

                    // NOTES
                    for (i in notes.indices) {
                        if(notes[i].velocity > 0) {
                            val noteStart = widthFactor * notes[i].time
                            val noteOffIndexAndTime = returnPairedNoteOffIndexAndTime(i)
                            val noteOffIndex = noteOffIndexAndTime.first
                            val noteLength = noteOffIndexAndTime.second - notes[i].time
                            val noteWidth =
                                if(noteOffIndex != -1) {
                                    widthFactor * noteLength   // TODO minimum visible length
                                } else {
                                    if(seqUiState.isRepeating) playheadRepeat - noteStart else playhead - noteStart  // live-writing note (grows in length)
                                }

                            if(noteWidth >= 0) { // normal note (not wrap-around)
                                drawRect(
                                    color = if(seqUiState.sequences[seqUiState.selectedChannel].channel == c) selectedNoteSquare else noteSquare,
                                    topLeft = Offset(noteStart, size.height - ((c + 1) * size.height / 4)),
                                    size = Size(
                                        width = noteWidth,
                                        height = size.height / 4)
                                )
                            } else {   // wrap-around note
                                drawRect(
                                    color = if(seqUiState.sequences[seqUiState.selectedChannel].channel == c) selectedNoteSquare else noteSquare,
                                    topLeft = Offset(noteStart,size.height - ((c + 1) * size.height / 4)),
                                    size = Size(
                                        width = size.width - noteStart,
                                        height = size.height / 4)
                                )
                                drawRect(
                                    color = if(seqUiState.sequences[seqUiState.selectedChannel].channel == c) selectedNoteSquare else noteSquare,
                                    topLeft = Offset(0f,size.height - ((c + 1) * size.height / 4)),
                                    size = Size(
                                        width = noteWidth + noteStart,  // noteWidth is negative here
                                        height = size.height / 4)
                                )
                            }
                        }
                    }

                    playHeads(seqUiState, playhead, playheadRepeat)

                    if(seqUiState.isRepeating) repeatBounds(this, widthFactor, 0.3f)
                }
            }
        }
        if(seqUiState.visualDebugger) {
            VisualDebugger(seqUiState, buttonsSize)
        }
    }
}


@Composable
fun VisualDebugger(seqUiState: SeqUiState, height: Dp) {    // TODO move into LiveView
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
                    "#toPlay = ${indexToPlay},     " +
                        "#toRepeat = $indexToRepeat      " +
                        "#toStartRepeating = $indexToStartRepeating",
                    color = selectedButton)
                Text(
                    "#indexToRepeat - wrapIndex = ${indexToRepeat - wrapIndex}",
                    color = selectedButton)
            }
        }
//        val playhead = ((seqUiState.sequences[0].deltaTime) / seqUiState.sequences[0].totalTime * maxWidth.value).dp  // TODO replace hardcoded channel
        Canvas(modifier = Modifier.fillMaxSize()) {
//            drawLine(
////                if (seqUiState.seqIsRecording || seqUiState.padsMode == PadsMode.ERASING) warmRed else if(seqUiState.padsMode == PadsMode.CLEARING) Color.White else violet,
//                Color.Transparent,
//                Offset(playhead.toPx(), 0.dp.toPx()),
//                Offset(playhead.toPx(), (height - 2.dp).toPx())
//            )
            if(seqUiState.visualArrayRefresh) drawPoints(List(1){ Offset(0f,0f) }, PointMode.Points, violet)
        }
        for(c in 0..3){
            with(seqUiState.sequences[c]) {
                for (i in notes.indices) {
                    Text(
                        text =
                            when (seqUiState.debuggerViewSetting) {
                                0 -> if (notes[i].velocity > 0) "${notes[i].pitch}" else "]"
                                1 -> "$i"
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
    val offset = if(triplet) width / 16 else width / -16
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
        //elevation = null,
        onClick = {  },
        shape = if(triplet) rightToHexShape else leftToHexShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(divisor == divisorState) dusk else seqBg
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
                } else buttonsColor,
            )
        }
    }
}