package com.example.mysequencer01ui.ui.viewTabs

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.ScrollableDefaults.flingBehavior
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Slider
import androidx.compose.material.SliderColors
import androidx.compose.material.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.KmmkComponentContext
import com.example.mysequencer01ui.ui.BARTIME
import com.example.mysequencer01ui.ui.SeqUiState
import com.example.mysequencer01ui.ui.SeqViewModel
import com.example.mysequencer01ui.ui.playHeads
import com.example.mysequencer01ui.ui.repeatBounds
import com.example.mysequencer01ui.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        with(seqUiState.sequences[seqUiState.selectedChannel]) {
            val scrollState = rememberScrollState(stepViewYScroll)
            VerticalSlider(
                value = stepViewYScroll.toFloat(),
                onValueChange = {
                    changeStepViewYScroll(it.toInt())
                    CoroutineScope(Dispatchers.Main).launch {
                        scrollState.scrollTo(stepViewYScroll)
                        seqViewModel.updateSequencesUiState()
                    } },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(50.dp),
                valueRange = 0f..scrollState.maxValue.toFloat(), // TODO hardcode
            colors = SliderDefaults.colors(
                thumbColor = violet,
                activeTrackColor = darkViolet,
                inactiveTrackColor = darkViolet
            )
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
                                .background(if ((i + 4) % 4 == 0) buttons else buttonsBg)
                        )
                    }
                }

                NotesGrid(seqViewModel, seqUiState, kmmk, scrollState)

                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val widthFactor = size.width / totalTime
                    val playhead = widthFactor * deltaTime.toFloat()
                    val playheadRepeat = widthFactor * deltaTimeRepeat.toFloat()

                    playHeads(seqUiState, playhead, playheadRepeat)

                    if(seqUiState.isRepeating) repeatBounds(this@with, widthFactor, 0.4f)
                }
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
                                    ((offset.x.toDp().value / maxWidth.value) * totalTime).toDouble()

                                val noteOnIndex1 = notes.indexOfLast {
                                    it.pitch == pitch && it.velocity > 0 && it.time < time
                                }
//                                if (noteOnIndex == -1 && notes.isNotEmpty() && notes[notes.lastIndex].velocity > 0) {
//                                    noteOnIndex = notes.lastIndex
//                                }
                                val noteOffIndex1 =
                                    if (noteOnIndex1 > -1) getPairedNoteOffIndexAndTime(
                                        noteOnIndex1
                                    ).index else -1

                                val noteOffIndex2 = notes.indexOfFirst {
                                    it.pitch == pitch && it.velocity == 0 && it.time > time
                                }
                                val noteOnIndex2 =
                                    if (noteOffIndex2 > -1) getPairedNoteOnIndexAndTime(
                                        noteOffIndex2
                                    ).index else -1

                                // if note exists where we tap - erase it, else record new note
                                if (
                                    (
                                        (noteOnIndex1 > -1 && noteOffIndex1 > -1) && (
                                            (
                                                time in notes[noteOnIndex1].time..notes[noteOffIndex1].time)  // normal case (not wrap-around) [---]
                                                ||
                                                (noteOnIndex1 > noteOffIndex1) && (time in notes[noteOnIndex1].time..totalTime.toDouble()) // wrap-around case, searching right part [--
                                            )
                                        ) ||
                                    (
                                        noteOffIndex2 > -1 && noteOnIndex2 > -1) && (
                                        (noteOnIndex1 > noteOffIndex1) && (time in 0.0..notes[noteOffIndex1].time) // wrap-around case, searching left part --]
                                        )
                                ) {
                                    erasing(seqUiState.isRepeating, noteOnIndex1, true)

                                    if (seqUiState.isRepeating) {
                                        indexToRepeat = notes.indexOfLast { it.time < deltaTimeRepeat } + 1
//                                        if (indexToRepeat == -1) indexToRepeat = 0
                                    } else {
                                        indexToPlay = notes.indexOfLast { it.time < deltaTime } + 1
//                                        if (indexToPlay == -1) indexToPlay = 0
                                    }

                                } else {
                                    time = seqViewModel.quantizeTime(time - seqUiState.quantizationTime / 2)
                                    val noteOffTime = time + seqUiState.quantizationTime

//                                    val otherNoteOffInTheMiddle =
//                                        notes.indexOfFirst { it.pitch == pitch && it.velocity == 0 && it.time in time..noteOffTime }
//                                    if (otherNoteOffInTheMiddle != -1) {
//                                        changeNoteTime(otherNoteOffInTheMiddle, time)
//                                    }

                                    recordNote(
                                        pitch = pitch,
                                        velocity = 100,
                                        id = noteId,
                                        quantizationTime = seqUiState.quantizationTime,
                                        factorBpm = seqUiState.factorBpm,
                                        seqIsPlaying = seqUiState.seqIsPlaying,
                                        isRepeating = seqUiState.isRepeating,
                                        quantizeTime = seqViewModel::quantizeTime,
                                        customTime = time,
                                        stepRecord = true,
                                    )
                                    recordNote(
                                        pitch = pitch,
                                        velocity = 0,
                                        id = noteId,
                                        quantizationTime = seqUiState.quantizationTime,
                                        factorBpm = seqUiState.factorBpm,
                                        seqIsPlaying = seqUiState.seqIsPlaying,
                                        isRepeating = seqUiState.isRepeating,
                                        quantizeTime = seqViewModel::quantizeTime,
                                        customTime = noteOffTime,
                                        stepRecord = true,
                                    )
                                    increaseNoteId()
                                }
                                seqViewModel.updateSequencesUiState()
                            }
                        )
                    }
                    .pointerInput(seqUiState.seqIsPlaying, seqUiState.isQuantizing) {
                        var noteDetected = false
                        var pitch = -1
                        var time: Double
                        var timeQuantized = -1.0
                        var noteDeltaTime = -1.0
                        var dragDeltaTime: Double
                        var noteOnIndex = -1
                        var noteOffIndex = -1
                        var changeLengthAreaDetected = false

                        detectDragGestures(
                            onDragStart = { offset ->
                                xOffset = offset.x
                                yOffset = offset.y
                                pitch = 127 - (yOffset / noteHeight.toPx()).toInt()
                                time = ((xOffset / maxWidth.toPx()) * totalTime).toDouble()

                                noteOnIndex = notes.indexOfLast {
                                    it.pitch == pitch && it.time <= time
                                }
                                if (noteOnIndex == -1 || notes[noteOnIndex].velocity == 0) {
                                    noteOffIndex = notes.indexOfFirst { it.pitch == pitch && it.time > time }
                                    if (noteOffIndex != -1 && notes[noteOffIndex].velocity == 0) noteOnIndex = getPairedNoteOnIndexAndTime(noteOffIndex).index
                                } else {
                                    noteOffIndex = getPairedNoteOffIndexAndTime(noteOnIndex).index
                                }

                                Log.d("ryjtyj", "noteOnIndex = $noteOnIndex, noteOffIndex = $noteOffIndex")

                                if (noteOnIndex == -1) noteOffIndex = -1
                                if (noteOffIndex == -1) noteOnIndex = -1

                                var widthOfChangeLengthArea = if (noteOnIndex < noteOffIndex) {   // normal case (not wrap-around)
                                    (notes[noteOffIndex].time - notes[noteOnIndex].time) / 3
                                } else if (noteOnIndex > noteOffIndex) {   // wrap-around case
                                    (notes[noteOffIndex].time - notes[noteOnIndex].time + totalTime) / 3
                                } else 0.0
                                if (widthOfChangeLengthArea !in 0.0..totalTime / 16.0) widthOfChangeLengthArea = totalTime / 16.0

                                val rightmostAreaOfChangeLength = if (noteOffIndex == -1) 0.0 else {
                                    (totalTime.toDouble() - widthOfChangeLengthArea + notes[noteOffIndex].time).coerceAtMost(totalTime.toDouble())
                                }
                                val leftmostAreaOfChangeLength = if (noteOffIndex == -1) 0.0 else {
                                    (notes[noteOffIndex].time - widthOfChangeLengthArea).coerceAtLeast(0.0)
                                }

                                // does note exists where we start dragging?
                                noteDetected =
                                    (noteOnIndex > -1 && noteOffIndex > -1) &&
                                        (
                                            (time in notes[noteOnIndex].time..notes[noteOffIndex].time)  // normal case (not wrap-around)
                                                || ((noteOnIndex > noteOffIndex) &&                            // wrap-around case
                                                (time in notes[noteOnIndex].time..totalTime.toDouble()) || (time in 0.0..notes[noteOffIndex].time))
                                            )

                                changeLengthAreaDetected =
                                    (noteOnIndex > -1 && noteOffIndex > -1) &&
                                        (
                                            (time in (notes[noteOffIndex].time - widthOfChangeLengthArea)..notes[noteOffIndex].time)  // normal case (not wrap-around)
                                                || ((noteOnIndex > noteOffIndex) &&                            // wrap-around case
                                                (time in rightmostAreaOfChangeLength..totalTime.toDouble()) || (time in leftmostAreaOfChangeLength..notes[noteOffIndex].time))
                                            )

                                if (noteDetected) {

                                    if (seqUiState.isQuantizing) {
                                        if (!changeLengthAreaDetected) {
                                            val tempTime = notes[noteOnIndex].time
                                            changeNoteTime(
                                                noteOnIndex,
                                                seqViewModel.quantizeTime(tempTime)
                                            )
                                            noteDeltaTime = notes[noteOnIndex].time - tempTime
                                            changeNoteTime(
                                                noteOffIndex,
                                                noteDeltaTime,
                                                true
                                            )
                                        } else {
                                            val tempTime = notes[noteOffIndex].time
                                            changeNoteTime(
                                                noteOffIndex,
                                                seqViewModel.quantizeTime(tempTime)
                                            )
                                            noteDeltaTime = notes[noteOffIndex].time - tempTime
                                        }
                                        timeQuantized = seqViewModel.quantizeTime(time + noteDeltaTime)
                                    }

                                    // if dragging note that is playing -> fire noteOff on time
                                    val currentIndex =
                                        if (seqUiState.isRepeating) indexToRepeat else indexToPlay
                                    if (seqUiState.seqIsPlaying &&
                                        (
                                            currentIndex in noteOnIndex + 1..noteOffIndex // normal case (not wrap-around)
                                                || noteOnIndex > noteOffIndex && (   // wrap-around case
                                                currentIndex in noteOnIndex + 1..notes.size || currentIndex in 0..noteOffIndex
                                                )
                                            )
                                    ) {
                                        val tempPitch = pitch
                                        CoroutineScope(Dispatchers.Default).launch {
                                            val dt =
                                                if (seqUiState.isRepeating) deltaTimeRepeat else deltaTime
                                            val delayTime = if (dt > notes[noteOffIndex].time) {
                                                notes[noteOffIndex].time - dt + totalTime
                                            } else {
                                                notes[noteOffIndex].time - dt
                                            }
                                            delay((delayTime / seqUiState.factorBpm).toLong())
                                            kmmk.noteOn(channel, tempPitch, 0)
                                            changePlayingNotes(tempPitch, 0)
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                draggedNoteOnIndex = -1
                                draggedNoteOffIndex = -1
                                if (noteDetected) dePressedNotesOnRepeat[pitch] = true
                                //offsetY = noteHeight * (127 - note.pitch)
                                // TODO if releasing on top of another note -> delete another
                            }
                        ) { change, dragAmount ->
                            change.consume()

                            // if note exists where we drag -> move it, else scroll the grid
                            if (noteDetected) {
                                xOffset += dragAmount.x
                                if (!changeLengthAreaDetected) {
                                    yOffset += dragAmount.y
                                }
                                time = ((xOffset * totalTime / maxWidth.toPx())).toInt() + noteDeltaTime

                                if (seqUiState.isQuantizing) {
                                    if (seqViewModel.quantizeTime(time) != timeQuantized) {
                                        val deltaQuantized = seqViewModel.quantizeTime(time) - timeQuantized
                                        if (!changeLengthAreaDetected) changeNoteTime(noteOnIndex, deltaQuantized, true)
                                        changeNoteTime(noteOffIndex, deltaQuantized, true)
                                        timeQuantized = seqViewModel.quantizeTime(time)
                                    }
                                } else {
                                    dragDeltaTime = (dragAmount.x.toDp() * BARTIME / maxWidth).toDouble()
                                    if (!changeLengthAreaDetected) changeNoteTime(noteOnIndex, dragDeltaTime, true)
                                    changeNoteTime(noteOffIndex, dragDeltaTime, true)
                                }

                                if (!changeLengthAreaDetected) {
                                    if (pitch != 127 - (yOffset / noteHeight.toPx()).toInt()) {
                                        pitch = 127 - (yOffset / noteHeight.toPx()).toInt()
                                        changeNotePitch(noteOnIndex, pitch)
                                        changeNotePitch(noteOffIndex, pitch)
                                    }
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
                                        getPairedNoteOffIndexAndTime(noteOnIndex).index

                                    if (seqUiState.isRepeating) {
                                        indexToRepeat = (notes.indexOfLast { it.time < deltaTimeRepeat } + 1).coerceAtLeast(0)
                                    } else {
                                        indexToPlay = (notes.indexOfLast { it.time < deltaTime } + 1).coerceAtLeast(0)
                                    }
                                }
                                draggedNoteOnIndex = noteOnIndex
                                draggedNoteOffIndex = noteOffIndex

                                seqViewModel.updateSequencesUiState()
                            } else {
                                changeStepViewYScroll(stepViewYScroll + dragAmount.y.toInt())
                                CoroutineScope(Dispatchers.Main).launch {
                                    val tempScrollValue = scrollState.value
                                    scrollState.scrollTo(stepViewYScroll)
                                    if (scrollState.value == tempScrollValue) changeStepViewYScroll(
                                        scrollState.value
                                    )
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
                        val widthFactor = maxWidth / BARTIME
                        val offsetX = widthFactor * notes[i].time.toInt()
                        val noteOffIndexAndTime = getPairedNoteOffIndexAndTime(i)
                        val noteOffIndex = noteOffIndexAndTime.index
                        var noteLength = widthFactor * (noteOffIndexAndTime.time - notes[i].time).toInt()
                        if (noteLength < 0.dp) noteLength = maxWidth + noteLength
                        val wrapAround = i > noteOffIndex
                        val changeLengthArea = (if (noteLength / 3 < maxWidth / 16) noteLength / 3 else maxWidth / 16)

//                        if (noteOffIndex != -1)
                        StepNote(
                            offsetX = offsetX,
                            offsetY = noteHeight * (127 - notes[i].pitch),
                            noteLength = noteLength,
                            indexNoteOn = i,
                            noteHeight = noteHeight,
                            wrapAround = wrapAround,
                            changeLengthArea = changeLengthArea,
                            maxWidth = maxWidth
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
    changeLengthArea: Dp,
    maxWidth: Dp,
) {
    NoteBox(
        offsetX = offsetX,
        offsetY = offsetY,
        width = if (wrapAround) maxWidth - offsetX else noteLength,
        noteHeight = noteHeight,
        wrapAround = wrapAround,
        changeLengthArea = if (wrapAround) changeLengthArea - noteLength + maxWidth - offsetX else changeLengthArea,
        indexNoteOn = indexNoteOn
    )

    if(wrapAround) {
        NoteBox(
            offsetX = 0.dp,
            offsetY = offsetY,
            width = offsetX + noteLength - maxWidth,
            noteHeight = noteHeight,
            wrapAround = true,
            changeLengthArea = changeLengthArea,
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
    wrapAround: Boolean,
    changeLengthArea: Dp,
    indexNoteOn: Int,
) {
    Box(
        modifier = Modifier
            .offset(offsetX, offsetY)
            .height(noteHeight)
            .width(width)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(buttonsBg)
                .border(0.6.dp, playGreen),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .height(noteHeight)
                    .width(changeLengthArea)
                    .border(0.6.dp, buttonsBg)
                    .background(com.example.mysequencer01ui.ui.theme.changeLengthArea)
            )
//        Text(text = "$indexNoteOn")
        }
        if (wrapAround) {
            Canvas (modifier = Modifier.fillMaxSize()) {
                val endOfScreen = offsetX != 0.dp
                val x = if (endOfScreen) size.width else 0f
                drawLine(if (endOfScreen) com.example.mysequencer01ui.ui.theme.changeLengthArea else buttonsBg, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
            }
        }
    }

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
