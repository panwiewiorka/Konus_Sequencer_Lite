package com.example.mysequencer01ui.ui.viewTabs

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.Sequence
import com.example.mysequencer01ui.ui.SeqUiState
import com.example.mysequencer01ui.ui.SeqViewModel
import com.example.mysequencer01ui.ui.theme.BackGray
import com.example.mysequencer01ui.ui.theme.buttonsBg
import com.example.mysequencer01ui.ui.theme.buttonsColor
import com.example.mysequencer01ui.ui.theme.notWhite
import com.example.mysequencer01ui.ui.theme.playGreen
import com.example.mysequencer01ui.ui.theme.violet
import com.example.mysequencer01ui.ui.theme.warmRed
import com.example.mysequencer01ui.ui.thickness
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PianoView(seqViewModel: SeqViewModel, seqUiState: SeqUiState, buttonsSize: Dp) {
    Log.d("emptyTag", "for holding Log in import")

    val keyHeight = buttonsSize * 1.9f
    val notesPadding = 1.dp
    val spaceBetweenSliders = 20.dp

    val sequence = seqUiState.sequences[seqUiState.selectedChannel]

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        val scrollStateLowWhite = rememberLazyListState()
        val scrollStateLowBlack = rememberLazyListState()
        val scrollStateHighWhite = rememberLazyListState()
        val scrollStateHighBlack = rememberLazyListState()

        LaunchedEffect(
            key1 = scrollStateLowWhite, key2 = scrollStateHighWhite
        ) {
            CoroutineScope(Dispatchers.Main).launch {
                scrollStateLowWhite.scrollToItem(sequence.pianoViewLowPianoScroll)
                scrollStateLowBlack.scrollToItem(sequence.pianoViewLowPianoScroll)
                scrollStateHighWhite.scrollToItem(sequence.pianoViewHighPianoScroll)
                scrollStateHighBlack.scrollToItem(sequence.pianoViewHighPianoScroll)
            }
        }

        //==============

        val highKeyboardScrollState = rememberScrollState(sequence.pianoViewHighKeyboardScroll.toInt())
        val lowKeyboardScrollState = rememberScrollState(sequence.pianoViewLowKeyboardScroll.toInt())

        val keyWidth = (maxWidth - notesPadding * 26) / 14

        val maxScrollValue = keyWidth.value * LocalDensity.current.density * 65
        val sliderWidth = (maxWidth - spaceBetweenSliders) / 2
        val factor = maxScrollValue / sliderWidth.value / LocalDensity.current.density

        val highSliderScrollState = rememberScrollState((sequence.pianoViewHighKeyboardScroll / factor).toInt())
        val lowSliderScrollState = rememberScrollState((sequence.pianoViewLowKeyboardScroll / factor).toInt())

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if(seqUiState.lazyKeyboard) {
                LazyPianoKeyboard(seqUiState.selectedChannel, seqUiState.seqIsRecording, keyWidth, keyHeight, notesPadding, scrollStateHighWhite, scrollStateHighBlack, seqViewModel::pressPad)
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val updatePianoViewXScroll = seqUiState.sequences[seqUiState.selectedChannel]::changePianoViewXScroll
                    OctaveButton(buttonsSize, scrollStateLowWhite, scrollStateLowBlack, scrollStateHighWhite, scrollStateHighBlack, updatePianoViewXScroll, true)
                    OctaveButton(buttonsSize, scrollStateLowWhite, scrollStateLowBlack, scrollStateHighWhite, scrollStateHighBlack, updatePianoViewXScroll, true, true)
                    Spacer(modifier = Modifier.width(buttonsSize))
                    OctaveButton(buttonsSize, scrollStateLowWhite, scrollStateLowBlack, scrollStateHighWhite, scrollStateHighBlack, updatePianoViewXScroll, false)
                    OctaveButton(buttonsSize, scrollStateLowWhite, scrollStateLowBlack, scrollStateHighWhite, scrollStateHighBlack, updatePianoViewXScroll, false, true)

                }
                LazyPianoKeyboard(seqUiState.selectedChannel, seqUiState.seqIsRecording, keyWidth, keyHeight, notesPadding, scrollStateLowWhite, scrollStateLowBlack, seqViewModel::pressPad)
            } else {
                PianoKeyboard(seqUiState.selectedChannel, seqUiState.seqIsRecording, keyWidth, keyHeight, notesPadding, highKeyboardScrollState, seqViewModel::pressPad)
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CustomSlider(
                        thumbWidth = 1.dp,
                        thumbHeight = 20.dp,
                        up = false,
                        width = (sliderWidth.value * LocalDensity.current.density).toInt(),
                        contourColor = violet,
                        fillColor = buttonsBg,
                        scrollState = lowKeyboardScrollState,
                        sliderScrollState = lowSliderScrollState,
                        factor = factor,
                        sequence = sequence,
                        updateSequencesUiState = seqViewModel::updateSequencesUiState,
                        modifier = Modifier.width(sliderWidth)
                    )
//                    KeyboardSlider(sequence, true,
//                        Modifier
//                            .weight(1f)
//                            .offset(0.dp, 7.dp), lowKeyboardScrollState, seqViewModel::updateSequencesUiState)
                    Spacer(modifier = Modifier.width(spaceBetweenSliders))
//                    KeyboardSlider(sequence, false,
//                        Modifier
//                            .weight(1f)
//                            .offset(0.dp, (-7).dp), highKeyboardScrollState, seqViewModel::updateSequencesUiState)
                    CustomSlider(
                        thumbWidth = 1.dp,
                        thumbHeight = 20.dp,
                        up = true,
                        width = (sliderWidth.value * LocalDensity.current.density).toInt(),
                        contourColor = violet,
                        fillColor = buttonsBg,
                        scrollState = highKeyboardScrollState,
                        sliderScrollState = highSliderScrollState,
                        factor = factor,
                        sequence = sequence,
                        updateSequencesUiState = seqViewModel::updateSequencesUiState,
                        modifier = Modifier.width(sliderWidth)
                    )
                }
                PianoKeyboard(seqUiState.selectedChannel, seqUiState.seqIsRecording, keyWidth, keyHeight, notesPadding, lowKeyboardScrollState, seqViewModel::pressPad)
            }
        }
    }
}


@Composable
fun PianoKeyboard(
    selectedChannel: Int,
//    playingNotes: Array<Boolean>,
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
                    PianoKey(seqIsRecording, pressPad, selectedChannel, pitch, keyWidth, keyHeight, notesPadding, true)
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
                else PianoKey(seqIsRecording, pressPad, selectedChannel, pitch, keyWidth, keyHeight, notesPadding, false)
            }
        }
    }
}


@Composable
fun LazyPianoKeyboard(
    selectedChannel: Int,
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
                    PianoKey(seqIsRecording, pressPad, selectedChannel, pitch, keyWidth, keyHeight, notesPadding, true)
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
                else PianoKey(seqIsRecording, pressPad, selectedChannel, pitch, keyWidth, keyHeight, notesPadding, false)
            }
        }
    }
}


@Composable
fun PianoKey(
    seqIsRecording: Boolean,
//    noteIsPlaying: Boolean,
    pressPad: (Int, Int, Int) -> Unit,
    selectedChannel: Int,
    pitch: Int,
    keyWidth: Dp,
    keyHeight: Dp,
    notesPadding: Dp,
    whiteKey: Boolean = true,
) {
    val keyColor = remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource, selectedChannel) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressPad(selectedChannel, pitch, 100)
                    keyColor.value = true
                }
                is PressInteraction.Release -> {
                    pressPad(selectedChannel, pitch, 0)
                    keyColor.value = false
                }
                is PressInteraction.Cancel -> {  }
            }
        }
    }
    val color = if (seqIsRecording) warmRed else playGreen
    Box(
        modifier = Modifier
            .padding(notesPadding, 0.dp)
//            .border(notesPadding, if(noteIsPlaying) color else Color.Transparent)
            .background(
                if (keyColor.value) {
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

                        sequence.changePianoViewKeyboardScroll(scrollState.value + dragAmount * factor, !up)
                        CoroutineScope(Dispatchers.Main).launch {
                            val tempScrollValue = scrollState.value
                            scrollState.scrollBy(dragAmount * factor)
//                                scrollState.animateScrollBy(dragAmount * factor)
                            if (scrollState.value == tempScrollValue) sequence.changePianoViewKeyboardScroll(scrollState.value.toFloat(), !up)
                            sliderScrollState.scrollBy(dragAmount)
                            if(sliderScrollState.value > width - thumbWidth.toPx() * 2) sliderScrollState.scrollTo((width - thumbWidth.toPx() * 2).toInt())
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
fun OctaveButton(
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