package com.example.mysequencer01ui.ui.viewTabs

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.PressedNote
import com.example.mysequencer01ui.RememberedPressInteraction
import com.example.mysequencer01ui.ui.SeqUiState
import com.example.mysequencer01ui.ui.SeqViewModel
import com.example.mysequencer01ui.ui.nonScaledSp
import com.example.mysequencer01ui.ui.theme.BackGray
import com.example.mysequencer01ui.ui.theme.buttonsBg
import com.example.mysequencer01ui.ui.theme.dusk
import com.example.mysequencer01ui.ui.theme.notWhite
import com.example.mysequencer01ui.ui.theme.playGreen
import com.example.mysequencer01ui.ui.theme.repeatButtons
import com.example.mysequencer01ui.ui.theme.stepViewBlackRows
import com.example.mysequencer01ui.ui.theme.warmRed
import com.example.mysequencer01ui.ui.thickness

@Composable
fun PianoView(seqViewModel: SeqViewModel, seqUiState: SeqUiState, buttonsSize: Dp) {
    Log.d("emptyTag", "for holding Log in import")

    val keyHeight = buttonsSize * 1.9f
    val notesPadding = 1.dp
    val bordersPadding = 16.dp

    with(seqViewModel.channelSequences[seqUiState.selectedChannel]) {
        val channelState by channelState.collectAsState()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(bordersPadding),
        ) {
            val rememberInteraction = remember { seqViewModel::rememberInteraction }
            val pressPad = remember { seqViewModel::addToPressPadList }
            val updatePadPitchOnChannel = remember { seqViewModel::updatePadPitchOnChannel }
            val savePadPitchToDatabase = remember { seqViewModel::savePadPitchToDatabase }

            val keyWidth = (maxWidth - notesPadding * 26) / 14

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                PianoKeyboard(
                    interactionSources = interactionSources,
                    rememberInteraction = rememberInteraction,
                    selectedChannel = seqUiState.selectedChannel,
                    playingNotes = channelState.playingNotes,
                    seqIsRecording = seqUiState.seqIsRecording,
                    keyIsWhite = seqViewModel.keysPattern,
                    keyWidth = keyWidth,
                    keyHeight = keyHeight,
                    octave = channelState.pianoViewOctaveHigh,
                    notesPadding = notesPadding,
                    pressedNotes = channelState.pressedNotes,
                    pressPad = pressPad,
                    updatePadPitchOnChannel = updatePadPitchOnChannel,
                    setPadPitchByPianoKey = seqUiState.setPadPitchByPianoKey,
                    savePadPitchToDatabase = savePadPitchToDatabase
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Spacer(modifier = Modifier.height(bordersPadding))
                        Row{
                            OctaveButton(
                                modifier = Modifier.weight(1f),
                                buttonsSize,
                                lowerPiano = true,
                                upButton = false,
                                ::changeKeyboardOctave
                            )
                            OctaveButton(
                                modifier = Modifier.weight(1f),
                                buttonsSize,
                                lowerPiano = true,
                                upButton = true,
                                ::changeKeyboardOctave
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.padding(bottom = bordersPadding)
                        ) {
                            OctaveButton(
                                modifier = Modifier.weight(1f),
                                buttonsSize,
                                lowerPiano = false,
                                upButton = false,
                                ::changeKeyboardOctave
                            )
                            OctaveButton(
                                modifier = Modifier.weight(1f),
                                buttonsSize,
                                lowerPiano = false,
                                upButton = true,
                                ::changeKeyboardOctave
                            )
                        }
                    }
                }
                PianoKeyboard(
                    interactionSources = interactionSources,
                    rememberInteraction = rememberInteraction,
                    selectedChannel = seqUiState.selectedChannel,
                    playingNotes = channelState.playingNotes,
                    seqIsRecording = seqUiState.seqIsRecording,
                    keyIsWhite = seqViewModel.keysPattern,
                    keyWidth = keyWidth,
                    keyHeight = keyHeight,
                    octave = channelState.pianoViewOctaveLow,
                    notesPadding = notesPadding,
                    pressedNotes = channelState.pressedNotes,
                    pressPad = pressPad,
                    updatePadPitchOnChannel = updatePadPitchOnChannel,
                    setPadPitchByPianoKey = seqUiState.setPadPitchByPianoKey,
                    savePadPitchToDatabase = savePadPitchToDatabase
                )
            }
        }
    }
}


@Composable
fun PianoKeyboard(
    interactionSources: Array<RememberedPressInteraction>,
    rememberInteraction: (Int, Int, PressInteraction.Press) -> Unit,
    selectedChannel: Int,
    playingNotes: Array<Int>,
    seqIsRecording: Boolean,
    keyIsWhite: Array<Boolean>,
    keyWidth: Dp,
    keyHeight: Dp,
    octave: Int,
    notesPadding: Dp,
    pressedNotes: Array<PressedNote>,
    pressPad: (Int, Int, Int, Long, Boolean) -> Unit,
    updatePadPitchOnChannel: (Int, Int) -> Unit,
    setPadPitchByPianoKey: Boolean,
    savePadPitchToDatabase: (Int, Int) -> Unit
) {
    val startPitch = 0
    Column(
        verticalArrangement = Arrangement.spacedBy(-keyHeight / 2),
    ) {
        Row {
            repeat(24) {
                val pitch = startPitch + it + (octave + 1) * 12
                Box(
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if(keyIsWhite[pitch]) PianoKey(
                        stepView = false,
                        interactionSource = interactionSources[pitch].interactionSource,
                        rememberInteraction = rememberInteraction,
                        seqIsRecording = seqIsRecording,
                        noteIsPlaying = playingNotes[pitch] > 0,
                        isPressed = pressedNotes[pitch].isPressed,
                        pressPad = pressPad,
                        updatePadPitchOnChannel = updatePadPitchOnChannel,
                        setPadPitchByPianoKey = setPadPitchByPianoKey,
                        savePadPitchToDatabase = savePadPitchToDatabase,
                        selectedChannel = selectedChannel,
                        pitch = pitch,
                        keyWidth = keyWidth,
                        keyHeight = keyHeight,
                        notesPadding = notesPadding,
                        whiteKey = true
                    )
                    if (pitch % 12 == 0) {
                        Text(
                            text = "${(pitch / 12) - 1}",
                            fontSize = 14.nonScaledSp,
                            color = BackGray,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.offset(-keyWidth / 2 - notesPadding, -keyHeight / 2)
        ) {
            repeat(24) {
                val pitch = startPitch + it + (octave + 1) * 12

                if(!keyIsWhite[pitch]) PianoKey(
                    stepView = false,
                    interactionSource = interactionSources[pitch].interactionSource,
                    rememberInteraction = rememberInteraction,
                    seqIsRecording = seqIsRecording,
                    noteIsPlaying = playingNotes[pitch] > 0,
                    isPressed = pressedNotes[pitch].isPressed,
                    pressPad = pressPad,
                    updatePadPitchOnChannel = updatePadPitchOnChannel,
                    setPadPitchByPianoKey = setPadPitchByPianoKey,
                    savePadPitchToDatabase = savePadPitchToDatabase,
                    selectedChannel = selectedChannel,
                    pitch = pitch,
                    keyWidth = keyWidth,
                    keyHeight = keyHeight,
                    notesPadding = notesPadding,
                    whiteKey = false
                )
                if(pitch % 12 == 5 || pitch % 12 == 0) Spacer(modifier = Modifier.width(keyWidth + notesPadding * 2))
            }
        }
    }
}


@Composable
fun PianoKey(
    stepView: Boolean,
    interactionSource: MutableInteractionSource,
    rememberInteraction: (Int, Int, PressInteraction.Press) -> Unit,
    seqIsRecording: Boolean,
    noteIsPlaying: Boolean,
    isPressed: Boolean,
    pressPad: (Int, Int, Int, Long, Boolean) -> Unit,
    updatePadPitchOnChannel: (Int, Int) -> Unit,
    setPadPitchByPianoKey: Boolean,
    savePadPitchToDatabase: (Int, Int) -> Unit,
    selectedChannel: Int,
    pitch: Int,
    keyWidth: Dp,
    keyHeight: Dp,
    notesPadding: Dp,
    whiteKey: Boolean,
) {
    LaunchedEffect(interactionSource, selectedChannel, pitch, setPadPitchByPianoKey, updatePadPitchOnChannel) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressPad(selectedChannel, pitch, 100, 0, false)
                    rememberInteraction(selectedChannel, pitch, interaction)
                    if (setPadPitchByPianoKey) {
                        updatePadPitchOnChannel(selectedChannel, pitch)
                        savePadPitchToDatabase(selectedChannel, pitch)
                    }
                }
                is PressInteraction.Release -> {
                    pressPad(selectedChannel, pitch, 0, 0, false)
                }
                is PressInteraction.Cancel -> {
                    pressPad(selectedChannel, pitch, 0, 0, false)
                }
            }
        }
    }
    val color = if (seqIsRecording) warmRed else playGreen
    Box(
        modifier = Modifier
            .padding(notesPadding, 0.dp)
            .border(
                width = when {
                    stepView && noteIsPlaying -> 3.dp
                    stepView -> 0.3.dp
                    else -> 4.dp
                },
                color = when {
                    noteIsPlaying -> color
                    stepView -> stepViewBlackRows
                    else -> Color.Transparent
                }
            )
            .background( when {
                isPressed -> color
                whiteKey -> notWhite
                stepView -> repeatButtons
                else -> BackGray
            })
            .width(keyWidth)
            .height(if (!whiteKey && !stepView) keyHeight/2 else keyHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { }
    ) { }
}


@Composable
fun OctaveButton(
    modifier: Modifier,
    buttonsSize: Dp,
    lowerPiano: Boolean,
    upButton: Boolean,
    changeKeyboardOctave: (Boolean, Int) -> Unit,
) {
    Button(
        elevation = null,
        onClick = {
            changeKeyboardOctave(lowerPiano, if(upButton) 1 else -1)
        },
        shape = RectangleShape,
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .fillMaxHeight()
            .rotate(if (upButton) 0f else 180f),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = buttonsBg
        ),
    ) {
        Box {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(6.dp)
                .alpha(0.6f)) {
                octaveArrow(buttonsSize)
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                octaveArrow(buttonsSize)
            }
        }
    }
}

fun DrawScope.octaveArrow(buttonsSize: Dp) {
    val m = buttonsSize.toPx() / 30
    val path = Path()
    path.moveTo(center.x - m, center.y - m * 2)
    path.lineTo(center.x + m, center.y)
    path.lineTo(center.x - m, center.y + m * 2)
    drawPath(
        path = path,
        color = dusk,
        style = Stroke( width = thickness, join = StrokeJoin.Round, cap = StrokeCap.Round )
    )
}