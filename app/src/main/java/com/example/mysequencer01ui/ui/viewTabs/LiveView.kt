package com.example.mysequencer01ui.ui.viewTabs

import android.util.Log
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
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.mysequencer01ui.NoteIndexAndTime
import com.example.mysequencer01ui.ui.ChannelState
import com.example.mysequencer01ui.ui.PadsGrid
import com.example.mysequencer01ui.ui.SeqUiState
import com.example.mysequencer01ui.ui.SeqViewModel
import com.example.mysequencer01ui.ui.buttonTextSize
import com.example.mysequencer01ui.ui.buttonsPadding
import com.example.mysequencer01ui.ui.nonScaledSp
import com.example.mysequencer01ui.ui.playHeads
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
import kotlin.math.pow
import kotlin.math.sqrt

private object NoRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = Color.Unspecified

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleAlpha(0f,0f,0f,0f)
}


@Composable
fun LiveView(seqViewModel: SeqViewModel, seqUiState: SeqUiState, buttonsSize: Dp) {
    val spacerSize = 0.dp
    val c = buttonsSize.value / sqrt(3f)
    val y = buttonsSize.value / 2
    val columnsOffset = -sqrt(c * c - y * y)

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            PatternsScreen(seqViewModel, seqUiState, buttonsSize)

            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // to avoid unnecessary recompositions:
                val pressPad = remember {seqViewModel::pressPad}
                val rememberInteraction = remember {seqViewModel::rememberInteraction}

                PadsGrid(
                    channelSequences = seqViewModel.channelSequences,
                    pressPad = pressPad,
                    rememberInteraction = rememberInteraction,
                    padsMode = seqUiState.padsMode,
                    selectedChannel = seqUiState.selectedChannel,
                    seqIsRecording = seqUiState.seqIsRecording,
                    padsSize = buttonsSize
                )
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
fun PatternsScreen(seqViewModel: SeqViewModel, seqUiState: SeqUiState, buttonsSize: Dp) {
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
        val channel0State by seqViewModel.channelSequences[0].channelState.collectAsState()

        Canvas(modifier = Modifier.fillMaxSize()) {

            // STEP LINES
            val horizontalStep = (maxWidth / 16).toPx()
            for (i in 1..15) {
                drawLine(
                    color = if((i + 4) % 4 == 0) buttons else buttonsBg,
                    start = Offset(i * horizontalStep, 0f),
                    end = Offset(i * horizontalStep, size.height),
                    strokeWidth = 2f,
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

            val widthFactor = size.width / channel0State.totalTime
            val playhead = (widthFactor * channel0State.deltaTime).toFloat()
            val playheadRepeat = (widthFactor * channel0State.deltaTimeRepeat).toFloat()
            playHeads(seqUiState.seqIsPlaying, seqUiState.isRepeating, seqUiState.playHeadsColor, playhead, playheadRepeat)
            if(seqUiState.isRepeating) repeatBounds(channel0State.totalTime, channel0State.repeatStartTime, channel0State.repeatEndTime, widthFactor, 0.65f)
        }

        val height = maxHeight / seqViewModel.channelSequences.size
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            for(c in seqViewModel.channelSequences.indices) {
                val channelNumber = seqViewModel.channelSequences.lastIndex - c
                val channelState by seqViewModel.channelSequences[channelNumber].channelState.collectAsState()
                val getNotePairedIndexAndTime by remember { mutableStateOf(seqViewModel.channelSequences[channelNumber]::getNotePairedIndexAndTime)}

                ChannelScreen(
                    channelState = channelState,
                    getNotePairedIndexAndTime = getNotePairedIndexAndTime,
                    seqIsPlaying = seqUiState.seqIsPlaying,
                    isRepeating = seqUiState.isRepeating,
                    selectedChannel = seqUiState.selectedChannel,
                    quantizationTime = seqUiState.quantizationTime,
                    factorBpm = seqUiState.factorBpm,
                    playHeadsColor = seqUiState.playHeadsColor,
                    soloIsOn = seqUiState.soloIsOn,
                    height = height,
                )
            }
        }

        if (seqUiState.visualDebugger) {
            VisualDebugger(seqViewModel, seqUiState, buttonsSize)
        }
    }
}


@Composable
fun ChannelScreen(
    channelState: ChannelState,
    getNotePairedIndexAndTime: (Int, Boolean) -> NoteIndexAndTime,
    seqIsPlaying: Boolean,
    isRepeating: Boolean,
    selectedChannel: Int,
    quantizationTime: Double,
    factorBpm: Double,
    playHeadsColor: Color,
    soloIsOn: Boolean,
    height: Dp,
    ) {

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        with(channelState) {
            val widthFactor = size.width / totalTime
            val playhead = (widthFactor * deltaTime).toFloat()
//    val playhead = (widthFactor * playheadState.value)
            val playheadRepeat = (widthFactor * deltaTimeRepeat).toFloat()

            // NOTES
            for (i in notes.indices) {
                if(notes[i].velocity > 0) {
                    val noteStart = (widthFactor * notes[i].time).toFloat()
                    val (noteOffIndex, noteOffTime) = getNotePairedIndexAndTime(i, true)
                    val noteLength = widthFactor * (noteOffTime - notes[i].time).toFloat()
                    val deltaWidth = if (isRepeating) playheadRepeat - noteStart else playhead - noteStart
                    val noteWidth =
                        if (noteOffIndex != -1) {
                            if(noteLength > 0 && noteLength < size.height) {
                                size.height
                            } else noteLength
                        } else {
                            deltaWidth  // live-writing note (grows in length)
                        }

                    val noteHeight = height.toPx() - 1
                    val fasterThanHalfOfQuantize = System.currentTimeMillis() - pressedNotes[notes[i].pitch].noteOnTimestamp <= quantizationTime / factorBpm / 2
                    val radius = CornerRadius(size.height, size.height)
                    val channelIsNotSilent = !(!isSoloed && (isMuted || soloIsOn))
                    val channelIsSelected = selectedChannel == channelStateNumber
                    val color = when {
                        channelIsSelected && channelIsNotSilent -> violet
                        channelIsSelected -> selectedButton
                        channelIsNotSilent -> darkViolet
                        else -> repeatButtons
                    }

                    if (noteWidth > 0  || (noteOffIndex == -1 && fasterThanHalfOfQuantize)) {   // normal note (not wrap-around) [...]
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(noteStart, 0f),
                            size = Size(
                                width = noteWidth,
                                height = noteHeight),
                            cornerRadius = radius
                        )
                    } else {   // wrap-around note

                        // [..
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(noteStart,0f),
                            size = Size(
                                width = size.width - noteStart,
                                height = noteHeight),
                            cornerRadius = radius
                        )
                        val halfTheLength = noteStart + (size.width - noteStart) / 2
                        drawRect(
                            color = color,
                            topLeft = Offset(halfTheLength,0f),
                            size = Size(
                                width = size.width - halfTheLength,
                                height = noteHeight),
                        )

                        // ..]
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(0f,0f),
                            size = Size(
                                width = noteWidth + noteStart,  // noteWidth is negative here
                                height = noteHeight),
                            cornerRadius = radius
                        )
                        drawRect(
                            color = color,
                            topLeft = Offset(0f,0f),
                            size = Size(
                                width = (noteWidth + noteStart) / 2,  // noteWidth is negative here
                                height = noteHeight),
                        )
                    }
                }
            }

//            playHeads(
//                seqIsPlaying = seqIsPlaying,
//                isRepeating = isRepeating,
//                playHeadsColor = playHeadsColor,
//                playhead = playhead,
//                playheadRepeat = playheadRepeat
//            )
//
//            if (isRepeating) repeatBounds(
//                totalTime = totalTime,
//                repeatStartTime = repeatStartTime,
//                repeatEndTime = repeatEndTime,
//                widthFactor = widthFactor,
//                alpha = 0.65f
//            )
        }
    }
}


@Composable
fun VisualDebugger(seqViewModel: SeqViewModel, seqUiState: SeqUiState, height: Dp) {
    Log.d("emptyTag", "") // for keeping in imports
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(buttonsPadding)
            .background(Color.Transparent),
        contentAlignment = Alignment.TopStart
    ) {
        Column() {
            with(seqViewModel.channelSequences[seqUiState.selectedChannel]) {
                Text(
                    "Channel ${seqUiState.selectedChannel}:   noteId = ${noteId - Int.MIN_VALUE},    " +
                        "notes ON: ${playingNotes[60]},     ",
                    color = selectedButton,
                    fontSize = 14.nonScaledSp,
                    )
                Text(
                    "Channel ${seqUiState.selectedChannel}:   #toPlay = $indexToPlay,   " +
                        "#toRepeat = $indexToRepeat    " +
                        "#toStartRepeating = $indexToStartRepeating",
                    color = selectedButton,
                    fontSize = 14.nonScaledSp,
                    )
            }
        }
        for(c in seqViewModel.channelSequences.indices){
            val channelState by seqViewModel.channelSequences[c].channelState.collectAsState()
            with(channelState) {
                for (i in notes.indices) {
                    Text(
                        text = when (seqUiState.debuggerViewSetting) {
                            0 -> "$i"
                            else -> "${notes[i].id - Int.MIN_VALUE}" },
                        color = if(notes[i].velocity > 0) Color(0xFFFFFFFF) else Color(0xFF999999),
                        fontSize = 12.nonScaledSp,
                        modifier = Modifier.offset(
                            (notes[i].time.toFloat() / totalTime * maxWidth.value).dp,
                            height - height / seqViewModel.channelSequences.size * (c + 4) + 3.dp
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
    CompositionLocalProvider(LocalRippleTheme provides NoRippleTheme) {
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
}