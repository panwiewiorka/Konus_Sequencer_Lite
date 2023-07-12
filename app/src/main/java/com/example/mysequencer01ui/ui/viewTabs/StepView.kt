package com.example.mysequencer01ui.ui.viewTabs

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.ScrollableDefaults.flingBehavior
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Slider
import androidx.compose.material.SliderColors
import androidx.compose.material.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.KmmkComponentContext
import com.example.mysequencer01ui.PadsMode.*
import com.example.mysequencer01ui.ui.SeqUiState
import com.example.mysequencer01ui.ui.SeqViewModel
import com.example.mysequencer01ui.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun StepView(seqViewModel: SeqViewModel, seqUiState: SeqUiState, buttonsSize: Dp, kmmk: KmmkComponentContext) {
    Log.d("emptyTag", "for holding Log in import")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .background(BackGray)
    ) {
        val selectedChannel = seqUiState.sequences[seqUiState.selectedChannel]
        val scrollState = rememberScrollState(selectedChannel.stepViewYScroll)
        VerticalSlider(
            value = selectedChannel.stepViewYScroll.toFloat(),
            onValueChange = {
                selectedChannel.changeStepViewYScroll(it.toInt())
                CoroutineScope(Dispatchers.Main).launch {
                    scrollState.scrollTo(selectedChannel.stepViewYScroll)
                    seqViewModel.updateSequencesUiState()
                } },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(50.dp),
            valueRange = 0f..scrollState.maxValue.toFloat() // TODO hardcode
        )

        BoxWithConstraints(
            modifier = Modifier
                .padding(top = 10.dp, end = 10.dp, bottom = 10.dp)
                .fillMaxSize()
                .border(0.5.dp, selectedButton)
                .background(BackGray)
        ){
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            ){
                for (i in 0..16) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(0.6.dp)
                            .background(if ((i + 4) % 4 == 0) shadeOfGray else buttonsBg)
                    )
                }
            }

            NotesGrid(seqViewModel, seqUiState, kmmk, scrollState)

            val sequence = seqUiState.sequences[seqUiState.selectedChannel]
            val playheadOffset = (sequence.deltaTime / sequence.totalTime * maxWidth.value).dp

            Playhead(
                seqUiState = seqUiState,
                modifier = Modifier
                    .offset(playheadOffset, 0.dp)
                    .width(0.6.dp)
            )

            var playheadRepeatOffset = (sequence.deltaTimeRepeat / sequence.totalTime * maxWidth.value).dp
            playheadRepeatOffset = if(playheadRepeatOffset.value < 0) playheadRepeatOffset + maxWidth else playheadRepeatOffset

            if(seqUiState.isRepeating) {
                Playhead(
                    seqUiState = seqUiState,
                    modifier = Modifier
                        .offset(playheadRepeatOffset, 0.dp)
                        .width(2.dp)
                )
            }
        }
    }
}


@Composable
fun NotesGrid(
    seqViewModel: SeqViewModel, seqUiState: SeqUiState, kmmk: KmmkComponentContext, scrollState: ScrollState,
) {
    with(seqUiState.sequences[seqUiState.selectedChannel]){

        val noteHeight = seqUiState.stepViewNoteHeight

        BoxWithConstraints(
            modifier = Modifier
                .padding(0.5.dp)
                .height(noteHeight * 128)
        ) {
            val maxWidth = maxWidth
            var xOffset by remember { mutableStateOf(0f) }
            var yOffset by remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .verticalScroll(
                        scrollState,
                        reverseScrolling = true,
                        flingBehavior = flingBehavior()
                    )
                    .pointerInput(seqUiState.selectedChannel) {
                        detectTapGestures(
                            onTap = { offset ->
                                val pitch = 127 - (offset.y / noteHeight.toPx()).toInt()
                                var time =
                                    ((offset.x.toDp().value / maxWidth.value) * totalTime).toInt()

                                var noteOnIndex = notes.indexOfLast {
                                    it.pitch == pitch && it.velocity > 0 && it.time < time
                                }
                                if (noteOnIndex == -1 && notes.isNotEmpty() && notes[notes.lastIndex].velocity > 0) {
                                    noteOnIndex = notes.lastIndex
                                }
                                val noteOffIndex =
                                    if (noteOnIndex > -1) returnPairedNoteOffIndexAndTime(
                                        noteOnIndex
                                    ).first else -1

                                // if note exists where we tap - erase it, else record new note
                                if (
                                    (noteOnIndex > -1 && noteOffIndex > -1) &&
                                    (
                                            (time in notes[noteOnIndex].time..notes[noteOffIndex].time)  // normal case (not wrap-around)
                                                    || ((noteOnIndex > noteOffIndex) &&                                // wrap-around case
                                                    (time in notes[noteOnIndex].time until totalTime) || (time in 0..notes[noteOffIndex].time))
                                            )
                                ) {
                                    erasing(kmmk, seqUiState.isRepeating, noteOnIndex)
                                } else {
                                    time = seqViewModel.quantizeTime(time)
                                    val noteOffTime =
                                        time + totalTime / seqUiState.quantizationValue
                                    val noteOffInTheMiddle =
                                        notes.indexOfFirst { it.pitch == pitch && it.velocity == 0 && it.time in time until noteOffTime }
                                    if (noteOffInTheMiddle != -1) changeNoteTime(
                                        noteOffInTheMiddle,
                                        time
                                    )
                                    recordNote(
                                        pitch = pitch,
                                        velocity = 100,
                                        staticNoteOffTime = seqViewModel.staticNoteOffTime,
                                        seqIsPlaying = seqUiState.seqIsPlaying,
                                        isRepeating = seqUiState.isRepeating,
                                        repeatLength = seqUiState.repeatLength,
                                        customTime = time,
                                        stepRecord = true,
                                    )
                                    recordNote(
                                        pitch = pitch,
                                        velocity = 0,
                                        staticNoteOffTime = seqViewModel.staticNoteOffTime,
                                        seqIsPlaying = seqUiState.seqIsPlaying,
                                        isRepeating = seqUiState.isRepeating,
                                        repeatLength = seqUiState.repeatLength,
                                        customTime = noteOffTime,
                                        stepRecord = true,
                                    )
                                }
                                seqViewModel.updateSequencesUiState()
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        var noteDetected = false
                        var pitch = 0
                        var time: Int
                        var deltaTime: Int
                        var noteOnIndex = -1
                        var noteOffIndex = -1

                        detectDragGestures(
                            onDragStart = { offset ->
                                xOffset = offset.x
                                yOffset = offset.y
                                pitch = 127 - (yOffset / noteHeight.toPx()).toInt()
                                time = ((xOffset / maxWidth.toPx()) * totalTime).toInt()

                                noteOnIndex = notes.indexOfLast {
                                    it.pitch == pitch && it.velocity > 0 && it.time < time
                                }
                                if (noteOnIndex == -1 && notes.isNotEmpty() && notes[notes.lastIndex].velocity > 0) {
                                    noteOnIndex = notes.lastIndex
                                }
                                noteOffIndex =
                                    if (noteOnIndex > -1) returnPairedNoteOffIndexAndTime(
                                        noteOnIndex
                                    ).first else -1

                                // does note exists where we start dragging?
                                noteDetected =
                                    (noteOnIndex > -1 && noteOffIndex > -1) &&
                                            (
                                                    (time in notes[noteOnIndex].time..notes[noteOffIndex].time)  // normal case (not wrap-around)
                                                            || ((noteOnIndex > noteOffIndex) &&                            // wrap-around case
                                                            (time in notes[noteOnIndex].time until totalTime) || (time in 0..notes[noteOffIndex].time))
                                                    )
                            },
                            onDragEnd = {
                                //offsetY = noteHeight * (127 - note.pitch)
                            }
                        ) { change, dragAmount ->
                            change.consume()

                            // if note exists where we start dragging - move it, else scroll
                            if (noteDetected) {
                                xOffset += dragAmount.x
                                yOffset += dragAmount.y

                                deltaTime = (dragAmount.x.toDp() * 2000 / maxWidth).toInt()
                                changeNoteTime(noteOnIndex, deltaTime, true)
                                changeNoteTime(noteOffIndex, deltaTime, true)

                                if (pitch != 127 - (yOffset / noteHeight.toPx()).toInt()) {
                                    pitch = 127 - (yOffset / noteHeight.toPx()).toInt()
                                    changeNotePitch(noteOnIndex, pitch)
                                    changeNotePitch(noteOffIndex, pitch)
                                }

                                val tempNoteOnTime = notes[noteOnIndex].time
                                val tempNoteOffTime = notes[noteOffIndex].time
                                sortNotesByTime()

                                if (
                                    notes[noteOnIndex].time != tempNoteOnTime ||
                                    notes[noteOffIndex].time != tempNoteOffTime
                                ) {
                                    noteOnIndex =
                                        notes.indexOfFirst { it.time == tempNoteOnTime && it.pitch == pitch }
                                    noteOffIndex =
                                        returnPairedNoteOffIndexAndTime(noteOnIndex).first
                                }

                                seqViewModel.updateSequencesUiState()
                            } else {
                                changeStepViewYScroll(stepViewYScroll + dragAmount.y.toInt())
                                CoroutineScope(Dispatchers.Main).launch {
                                    val tempScrollValue = scrollState.value
                                    scrollState.scrollTo(stepViewYScroll)
                                    if (scrollState.value == tempScrollValue) changeStepViewYScroll(scrollState.value)
                                    seqViewModel.updateSequencesUiState()
                                }
                            }
                        }
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ){
                    for (i in 0..127) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(noteHeight)
                                .background(Color.Transparent)
                                .border(BorderStroke(0.3.dp, buttonsBg))
                        )
                    }
                }

                for(i in notes.indices) {
                    if(notes[i].velocity > 0) {

                        val offsetX = maxWidth * notes[i].time / 2000
                        val noteOffIndexAndTime = returnPairedNoteOffIndexAndTime(i)
                        val noteOffIndex = noteOffIndexAndTime.first
                        val noteLength = noteOffIndexAndTime.second - notes[i].time

                        StepNote(
                            offsetX = offsetX,
                            offsetY = noteHeight * (127 - notes[i].pitch),
                            noteLength = if (i > noteOffIndex)
                                maxWidth - offsetX
                            else maxWidth * noteLength / 2000,
                            indexNoteOn = i,
                            noteHeight = noteHeight,
                            wrapAround = i > noteOffIndex,
                            wrapAroundNoteLength = offsetX + maxWidth * noteLength / 2000,
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun StepNote(
    offsetX: Dp,
    offsetY: Dp,
    noteLength: Dp,
    indexNoteOn: Int,
    noteHeight: Dp,
    wrapAround: Boolean,
    wrapAroundNoteLength: Dp,
) {
    NoteBox(
        offsetX = offsetX,
        offsetY = offsetY,
        width = noteLength,
        noteHeight = noteHeight,
        indexNoteOn = indexNoteOn
    )

    if(wrapAround) {
        NoteBox(
            offsetX = 0.dp,
            offsetY = offsetY,
            width = wrapAroundNoteLength,
            noteHeight = noteHeight,
            indexNoteOn = indexNoteOn
        )
    }
}

@Composable
fun NoteBox(
    offsetX: Dp,
    offsetY: Dp,
    width: Dp,
    noteHeight: Dp,
    indexNoteOn: Int,
) {
    Box(
        modifier = Modifier
            .offset(offsetX, offsetY)
            .height(noteHeight)
            .width(width)
            .background(buttonsBg)
            .border(BorderStroke(0.6.dp, playGreen), RoundedCornerShape(0.dp))
    ) {
//        Text("$indexNoteOn")
    }
}


@Composable
fun Playhead(seqUiState: SeqUiState, modifier: Modifier) {
    val conditionalColor = when (seqUiState.padsMode) {
        MUTING -> violet
        ERASING -> warmRed
        CLEARING -> notWhite
        else -> {
            if (seqUiState.seqIsRecording) warmRed
            else playGreen
        }
    }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(conditionalColor)
    )
}


@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    /*@IntRange(from = 0)*/
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SliderColors = SliderDefaults.colors()
){
    Slider(
        colors = colors,
        interactionSource = interactionSource,
        onValueChangeFinished = onValueChangeFinished,
        steps = steps,
        valueRange = valueRange,
        enabled = enabled,
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxHeight,
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
            .then(modifier)
    )
}
