package com.example.mysequencer01ui.ui.viewTabs

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.PianoKeysType
import com.example.mysequencer01ui.PianoKeysType.BLACK
import com.example.mysequencer01ui.PianoKeysType.WHITE
import com.example.mysequencer01ui.ui.SeqUiState
import com.example.mysequencer01ui.ui.SeqViewModel
import com.example.mysequencer01ui.ui.theme.BackGray
import com.example.mysequencer01ui.ui.theme.buttonsBg
import com.example.mysequencer01ui.ui.theme.dusk
import com.example.mysequencer01ui.ui.theme.notWhite
import com.example.mysequencer01ui.ui.theme.playGreen
import com.example.mysequencer01ui.ui.theme.warmRed
import com.example.mysequencer01ui.ui.thickness

@Composable
fun PianoView(seqViewModel: SeqViewModel, seqUiState: SeqUiState, buttonsSize: Dp) {
    Log.d("emptyTag", "for holding Log in import")

    val keyHeight = buttonsSize * 1.9f
    val notesPadding = 1.dp
    val spaceBetweenSliders = 20.dp
    val bordersPadding = 16.dp

    with(seqUiState.sequences[seqUiState.selectedChannel]) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(bordersPadding),
        ) {
            val keyWidth = (maxWidth - notesPadding * 26) / 14

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                PianoKeyboard(
                    seqViewModel.interactionSources[seqUiState.selectedChannel],
                    seqViewModel::rememberInteraction,
                    seqUiState.selectedChannel,
                    playingNotes,
                    seqUiState.seqIsRecording,
                    keyWidth,
                    keyHeight,
                    pianoViewOctaveHigh,
                    notesPadding,
                    seqViewModel::pressPad
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
                                ::changeKeyboardOctave,
                                seqViewModel::updateSequencesUiState
                            )
                            OctaveButton(
                                modifier = Modifier.weight(1f),
                                buttonsSize,
                                lowerPiano = true,
                                upButton = true,
                                ::changeKeyboardOctave,
                                seqViewModel::updateSequencesUiState
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
                                ::changeKeyboardOctave,
                                seqViewModel::updateSequencesUiState
                            )
                            OctaveButton(
                                modifier = Modifier.weight(1f),
                                buttonsSize,
                                lowerPiano = false,
                                upButton = true,
                                ::changeKeyboardOctave,
                                seqViewModel::updateSequencesUiState
                            )
                        }
                    }
                }
                PianoKeyboard(
                    seqViewModel.interactionSources[seqUiState.selectedChannel],
                    seqViewModel::rememberInteraction,
                    seqUiState.selectedChannel,
                    playingNotes,
                    seqUiState.seqIsRecording,
                    keyWidth,
                    keyHeight,
                    pianoViewOctaveLow,
                    notesPadding,
                    seqViewModel::pressPad
                )
            }
        }
    }
}


@Composable
fun PianoKeyboard(
    interactionSources: Array<Pair<MutableInteractionSource, PressInteraction.Press>>,
    rememberInteraction: (Int, Int, PressInteraction.Press) -> Unit,
    selectedChannel: Int,
    playingNotes: Array<Int>,
    seqIsRecording: Boolean,
    keyWidth: Dp,
    keyHeight: Dp,
    octave: Int,
    notesPadding: Dp,
    pressPad: (Int, Int, Int) -> Unit,
) {
    val startPitch = 0
    Column(
        verticalArrangement = Arrangement.spacedBy(-keyHeight / 2),
//        modifier = Modifier.horizontalScroll(keyboardScrollState),
    ) {
        Row() {
            repeat(24) {
                val key = chooseBlackOrWhiteKey(startPitch, it)
                val keyIsWhite = key.first.keyIsWhite
                val pitch = key.second + (octave + 1) * 12 // TODO bounds
                Box(
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if(keyIsWhite) PianoKey(
                        interactionSource = interactionSources[pitch].first,
                        rememberInteraction = rememberInteraction,
                        seqIsRecording = seqIsRecording,
                        noteIsPlaying = playingNotes[pitch] > 0,
                        pressPad = pressPad,
                        selectedChannel = selectedChannel,
                        pitch = pitch,
                        keyWidth = keyWidth,
                        keyHeight = keyHeight,
                        notesPadding = notesPadding,
                        whiteKey = true
                    )
                    if (pitch % 12 == 0) Text("${(pitch / 12) - 1}")
                }
            }
        }
        Row(
            modifier = Modifier.offset(-keyWidth / 2 - notesPadding, -keyHeight / 2)
        ) {
            repeat(24) {
                val key = chooseBlackOrWhiteKey(startPitch, it)
                val keyIsBlack = !key.first.keyIsWhite
                val pitch = key.second + (octave + 1) * 12 // TODO bounds

                if(keyIsBlack) PianoKey(
                    interactionSource = interactionSources[pitch].first,
                    rememberInteraction = rememberInteraction,
                    seqIsRecording = seqIsRecording,
                    noteIsPlaying = playingNotes[pitch] > 0,
                    pressPad = pressPad,
                    selectedChannel = selectedChannel,
                    pitch = pitch,
                    keyWidth = keyWidth,
                    keyHeight = keyHeight,
                    notesPadding = notesPadding,
                    whiteKey = false
                )
                if(key.second % 12 == 5 || key.second % 12 == 0) Spacer(modifier = Modifier.width(keyWidth + notesPadding * 2))
            }
        }
    }
}

fun chooseBlackOrWhiteKey(startPitch: Int, keyIndex: Int): Pair<PianoKeysType, Int> {
    val blackAndWhiteKeysPattern = listOf(WHITE, BLACK, WHITE, BLACK, WHITE, WHITE, BLACK, WHITE, BLACK, WHITE, BLACK, WHITE)
    return Pair(blackAndWhiteKeysPattern[(startPitch + keyIndex) % 12], startPitch + keyIndex)
}


@Composable
fun PianoKey(
    interactionSource: MutableInteractionSource,
    rememberInteraction: (Int, Int, PressInteraction.Press) -> Unit,
    seqIsRecording: Boolean,
    noteIsPlaying: Boolean,
    pressPad: (Int, Int, Int) -> Unit,
    selectedChannel: Int,
    pitch: Int,
    keyWidth: Dp,
    keyHeight: Dp,
    notesPadding: Dp,
    whiteKey: Boolean,
) {
    val keyIsPressed by interactionSource.collectIsPressedAsState()
//    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource, selectedChannel, pitch) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressPad(selectedChannel, pitch, 100)
                    rememberInteraction(selectedChannel, pitch, interaction)
                }
                is PressInteraction.Release -> {
                    pressPad(selectedChannel, pitch, 0)
                }
                is PressInteraction.Cancel -> {
                    pressPad(selectedChannel, pitch, 0)
                }
            }
        }
    }
    val color = if (seqIsRecording) warmRed else playGreen
    Box(
        modifier = Modifier
            .padding(notesPadding, 0.dp)
            .border(4.dp, if (noteIsPlaying) color else Color.Transparent)
            .background(
                if (keyIsPressed) {
                    color
                } else {
                    if (whiteKey) notWhite else BackGray
                }
            )
            .width(keyWidth)
            .height(if (whiteKey) keyHeight else keyHeight / 2)
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
    updateSeqUiState: () -> Unit
) {
    Button(
        elevation = null,
        onClick = {
            changeKeyboardOctave(lowerPiano, if(upButton) 1 else -1)
            updateSeqUiState()
        },
        shape = RoundedCornerShape(0f),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .fillMaxHeight()
            .rotate(if (upButton) 0f else 180f),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = buttonsBg
        ),
    ) {
        Box {
            Canvas(modifier = Modifier.fillMaxSize().blur(6.dp).alpha(0.6f)) {
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


/*
@Composable
fun PianoKeyboardOld(
    selectedChannel: Int,
    playingNotes: Array<Int>,
    seqIsRecording: Boolean,
    keyWidth: Dp,
    keyHeight: Dp,
    notesPadding: Dp,
    keyboardScrollState: ScrollState,
    pressPad: (Int, Int, Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(-keyHeight / 2),
        modifier = Modifier.horizontalScroll(keyboardScrollState),
    ) {
        Row() {
            repeat(75) {
                Box(
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val octave = (it + 7) / 7 - 1
                    val semitone = when( (it + 7) % 7) {
                        0 -> 0
                        1 -> 2
                        2 -> 4
                        3 -> 5
                        4 -> 7
                        5 -> 9
                        else -> 11
                    }
                    val pitch = semitone + octave * 12
                    PianoKey(seqIsRecording, playingNotes[pitch] > 0, pressPad, selectedChannel, pitch, keyWidth, keyHeight, notesPadding, true)
                    if ((it + 7) % 7 == 0) Text("${it / 7 - 4}")
                }
            }
        }
        Row(
            modifier = Modifier.offset(keyWidth / 2 + notesPadding, -keyHeight / 2)
        ) {
            repeat(75){
                val octave = (it + 7) / 7 - 1
                val semitone = when( (it + 7) % 7) {
                    0 -> 1
                    1 -> 3
                    3 -> 6
                    4 -> 8
                    5 -> 10
                    else -> 99
                }
                val pitch = semitone + octave * 12
                val a = (it + 7) % 7
                if(a == 2 || a == 6 || it == 74) Spacer(modifier = Modifier.width(keyWidth + notesPadding * 2))
                else PianoKey(seqIsRecording, playingNotes[pitch] > 0, pressPad, selectedChannel, pitch, keyWidth, keyHeight, notesPadding, false)
            }
        }
    }
}


@Composable
fun LazyPianoKeyboard(
    selectedChannel: Int,
    playingNotes: Array<Int>,
    seqIsRecording: Boolean,
    keyWidth: Dp,
    keyHeight: Dp,
    notesPadding: Dp,
    scrollStateWhite: LazyListState,
    scrollStateBlack: LazyListState,
    pressPad: (Int, Int, Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(-keyHeight / 2)
    ) {
        LazyRow(
            state = scrollStateWhite,
            userScrollEnabled = false
        ) {
            items(75) {
                Box(
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val octave = (it + 7) / 7 - 1
                    val semitone = when( (it + 7) % 7) {
                        0 -> 0
                        1 -> 2
                        2 -> 4
                        3 -> 5
                        4 -> 7
                        5 -> 9
                        else -> 11
                    }
                    val pitch = semitone + octave * 12
                    PianoKey(seqIsRecording,playingNotes[pitch] > 0, pressPad, selectedChannel, pitch, keyWidth, keyHeight, notesPadding, true)
                    if ((it + 7) % 7 == 0) Text("${it / 7 - 4}")
                }
            }
        }
        LazyRow(
            state = scrollStateBlack,
            userScrollEnabled = false,
            modifier = Modifier.offset(keyWidth / 2 + notesPadding, -keyHeight / 2)
        ) {
            items(75){
                val octave = (it + 7) / 7 - 1
                val semitone = when( (it + 7) % 7) {
                    0 -> 1
                    1 -> 3
                    3 -> 6
                    4 -> 8
                    5 -> 10
                    else -> 99
                }
                val pitch = semitone + octave * 12
                val a = (it + 7) % 7
                if(a == 2 || a == 6 || it == 74) Spacer(modifier = Modifier.width(keyWidth + notesPadding * 2))
                else PianoKey(seqIsRecording,playingNotes[pitch] > 0, pressPad, selectedChannel, pitch, keyWidth, keyHeight, notesPadding, false)
            }
        }
    }
}


@Composable
fun PianoKeyOld(
    seqIsRecording: Boolean,
    noteIsPlaying: Boolean,
    pressPad: (Int, Int, Int) -> Unit,
    selectedChannel: Int,
    pitch: Int,
    keyWidth: Dp,
    keyHeight: Dp,
    notesPadding: Dp,
    whiteKey: Boolean = true,
) {
    val keyIsPressed = remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource, selectedChannel) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressPad(selectedChannel, pitch, 100)
                    keyIsPressed.value = true
                }
                is PressInteraction.Release -> {
                    pressPad(selectedChannel, pitch, 0)
                    keyIsPressed.value = false
                }
                is PressInteraction.Cancel -> {  }
            }
        }
    }
    val color = if (seqIsRecording) warmRed else playGreen
    Box(
        modifier = Modifier
            .padding(notesPadding, 0.dp)
            .border(4.dp, if(noteIsPlaying) color else Color.Transparent)
            .background(
                if (keyIsPressed.value) {
                    color
                } else {
                    if (whiteKey) notWhite else BackGray
                }
            )
            .width(keyWidth)
            .height(if (whiteKey) keyHeight else keyHeight / 2)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { }
    ) { }
}


@Composable
fun KeyboardSlider(sequence: Sequence, lowKeyboard: Boolean, modifier: Modifier, scrollState: ScrollState, updateSequencesUiState: () -> Unit) {
    Slider(
        value = if(lowKeyboard) sequence.pianoViewLowKeyboardScroll else sequence.pianoViewHighKeyboardScroll,
        onValueChange = {
            sequence.changePianoViewKeyboardScroll( it, lowKeyboard )
            CoroutineScope(Dispatchers.Main).launch {
                scrollState.scrollTo(it.toInt())
                updateSequencesUiState()
            }
                        },
        modifier = modifier,
        valueRange = 0f..6832f,
        colors = SliderDefaults.colors(
            thumbColor = violet,
            activeTrackColor = violet,
            inactiveTrackColor = violet,
        )
    )
}


@Composable
fun CustomSlider(
    thumbWidth: Dp,
    thumbHeight: Dp,
    up: Boolean,
    width: Int,
    contourColor: Color,
    fillColor: Color,
    factor: Float,
    sequence: Sequence,
    scrollState: ScrollState,
    sliderScrollState: ScrollState,
    updateSequencesUiState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(thumbHeight * 2),
        contentAlignment = Alignment.Center
    ) {
//        val sliderMaxValue = (maxWidth - thumbRadius * 2).value * LocalDensity.current.density
//        val factor = scrollMaxValue / sliderMaxValue

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = contourColor,
                start = Offset(0f + thumbWidth.toPx(), center.y),
                end = Offset(size.width - thumbWidth.toPx(), center.y),
                strokeWidth = thickness,
                cap = StrokeCap.Round
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()

                        sequence.changePianoViewKeyboardScroll(
                            scrollState.value + dragAmount * factor,
                            !up
                        )
                        CoroutineScope(Dispatchers.Main).launch {
                            val tempScrollValue = scrollState.value
                            scrollState.scrollBy(dragAmount * factor)
//                                scrollState.animateScrollBy(dragAmount * factor)
                            if (scrollState.value == tempScrollValue) sequence.changePianoViewKeyboardScroll(
                                scrollState.value.toFloat(),
                                !up
                            )
                            sliderScrollState.scrollBy(dragAmount)
                            if (sliderScrollState.value > width - thumbWidth.toPx() * 2) sliderScrollState.scrollTo(
                                (width - thumbWidth.toPx() * 2).toInt()
                            )
                            updateSequencesUiState()
                        }
                    }
                }
        ) {
            drawRect(
                color = fillColor,
                topLeft = Offset(sliderScrollState.value.toFloat(), if(up) 0f else size.height / 2),
                size = Size(thumbWidth.toPx(), thumbHeight.toPx()),
                style = Fill,
            )
            drawRect(
                color = contourColor,
                topLeft = Offset(sliderScrollState.value.toFloat(), if(up) 0f else size.height / 2),
                size = Size(thumbWidth.toPx(), thumbHeight.toPx()),
                style = Stroke(
                    width = thickness,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}


@Composable
fun OctaveButtonOld(
    buttonsSize: Dp,
    scrollStateLowWhite: LazyListState,
    scrollStateLowBlack: LazyListState,
    scrollStateHighWhite: LazyListState,
    scrollStateHighBlack: LazyListState,
    updatePianoViewXScroll: (x: Int, lowerPiano: Boolean) -> Unit,
    lowerPiano: Boolean,
    upButton: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    Button(
        elevation = null,
        onClick = {
            coroutineScope.launch {
                when {
                    lowerPiano && upButton -> {
                        if(scrollStateLowWhite.firstVisibleItemIndex < 68 && scrollStateLowBlack.firstVisibleItemIndex < 68) { // TODO needless if bc firstIndex << 68
                            scrollStateLowWhite.scrollToItem(scrollStateLowWhite.firstVisibleItemIndex + 7, 0)
                            scrollStateLowBlack.scrollToItem(scrollStateLowBlack.firstVisibleItemIndex + 7, 0)
                        }
                    }
                    lowerPiano && !upButton -> {
                        if(scrollStateLowWhite.firstVisibleItemIndex >= 7 && scrollStateLowBlack.firstVisibleItemIndex >= 7) {
                            scrollStateLowWhite.scrollToItem(scrollStateLowWhite.firstVisibleItemIndex - 7, 0)
                            scrollStateLowBlack.scrollToItem(scrollStateLowBlack.firstVisibleItemIndex - 7, 0)
                        } else {
                            scrollStateLowWhite.scrollToItem(0, 0)
                            scrollStateLowBlack.scrollToItem(0, 0)
                        }
                    }
                    !lowerPiano && upButton -> {
                        if(scrollStateHighWhite.firstVisibleItemIndex < 68 && scrollStateHighBlack.firstVisibleItemIndex < 68) {
                            scrollStateHighWhite.scrollToItem(scrollStateHighWhite.firstVisibleItemIndex + 7, 0)
                            scrollStateHighBlack.scrollToItem(scrollStateHighBlack.firstVisibleItemIndex + 7, 0)
                        }
                    }
                    else -> {
                        if(scrollStateHighWhite.firstVisibleItemIndex >= 7 && scrollStateHighBlack.firstVisibleItemIndex >= 7) {
                            scrollStateHighWhite.scrollToItem(scrollStateHighWhite.firstVisibleItemIndex - 7, 0)
                            scrollStateHighBlack.scrollToItem(scrollStateHighBlack.firstVisibleItemIndex - 7, 0)
                        } else {
                            scrollStateHighWhite.scrollToItem(0, 0)
                            scrollStateHighBlack.scrollToItem(0, 0)
                        }
                    }
                }
            }.also {
                val scroll = if(lowerPiano) scrollStateLowWhite.firstVisibleItemIndex else scrollStateHighWhite.firstVisibleItemIndex
                updatePianoViewXScroll(scroll, lowerPiano)
            }
        },
        shape = RoundedCornerShape(0f),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .width(buttonsSize)
            .height(buttonsSize / 3),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = buttonsColor
        ),
    ) {
        Text(text = if(upButton) ">" else "<", color = notWhite)
    }
}

 */