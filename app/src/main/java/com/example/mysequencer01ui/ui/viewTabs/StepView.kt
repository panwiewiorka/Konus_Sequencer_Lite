package com.example.mysequencer01ui.ui.viewTabs

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Slider
import androidx.compose.material.SliderColors
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.ui.BARTIME
import com.example.mysequencer01ui.ui.ChannelState
import com.example.mysequencer01ui.ui.SeqUiState
import com.example.mysequencer01ui.ui.SeqViewModel
import com.example.mysequencer01ui.ui.TAG
import com.example.mysequencer01ui.ui.nonScaledSp
import com.example.mysequencer01ui.ui.playHeads
import com.example.mysequencer01ui.ui.repeatBounds
import com.example.mysequencer01ui.ui.theme.BackGray
import com.example.mysequencer01ui.ui.theme.buttons
import com.example.mysequencer01ui.ui.theme.buttonsBg
import com.example.mysequencer01ui.ui.theme.changeLengthAreaColor
import com.example.mysequencer01ui.ui.theme.darkViolet
import com.example.mysequencer01ui.ui.theme.playGreen
import com.example.mysequencer01ui.ui.theme.selectedButton
import com.example.mysequencer01ui.ui.theme.stepViewBlackRows
import com.example.mysequencer01ui.ui.theme.violet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun StepView(
    seqViewModel: SeqViewModel,
    seqUiState: SeqUiState,
    maxHeight: Dp,
) {

    val channelState by seqViewModel.channelSequences[seqUiState.selectedChannel].channelState.collectAsState()
    val maxScroll = (seqUiState.stepViewNoteHeight * 129 - (maxHeight - seqUiState.stepViewNoteHeight - 2.dp)).value * LocalDensity.current.density

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .background(BackGray)
    ) {
        with(seqViewModel.channelSequences[seqUiState.selectedChannel]) {
            var sliderScrollValue by remember { mutableStateOf(channelState.stepViewYScroll.toFloat())}
            val scrollState = rememberScrollState(channelState.stepViewYScroll)

            LaunchedEffect(seqUiState.selectedChannel) {
                scrollState.scrollTo(channelState.stepViewYScroll)
            }
            LaunchedEffect(scrollState.value) {
                sliderScrollValue = scrollState.value.toFloat()
                updateStepViewYScroll(scrollState.value)
            }

            VerticalSlider(
                value = sliderScrollValue,
                onValueChange = {
                    sliderScrollValue = it
                    CoroutineScope(Dispatchers.Main).launch { scrollState.scrollTo(it.toInt()) }
                                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(50.dp),
                valueRange = 0f..maxScroll,
                colors = SliderDefaults.colors(
                    thumbColor = violet,
                    activeTrackColor = darkViolet,
                    inactiveTrackColor = darkViolet
                )
            )

            BoxWithConstraints(
                modifier = Modifier
                    .padding(top = 10.dp, end = 10.dp, bottom = 10.dp)
                    .weight(1f)
                    .border(0.5.dp, selectedButton)
                    .background(BackGray)
            ){
                val keyboardWidth = 48.dp
                val gridWidth = maxWidth - keyboardWidth

                NotesGrid(
                    seqViewModel = seqViewModel,
                    seqUiState = seqUiState,
                    channelState = channelState,
                    scrollState = scrollState,
                    gridWidth = gridWidth,
                    keyboardWidth = keyboardWidth - 1.dp
                )

                Canvas(
                    modifier = Modifier
                        .width(gridWidth)
                        .fillMaxHeight()
                        .offset(x = keyboardWidth - 1.dp)
                ) {
                    val widthFactor = size.width / channelState.totalTime
                    val playhead = widthFactor * channelState.deltaTime.toFloat()
                    val playheadRepeat = widthFactor * channelState.deltaTimeRepeat.toFloat()

                    playHeads(
                        seqIsPlaying = seqUiState.seqIsPlaying,
                        isRepeating = seqUiState.isRepeating,
                        playHeadsColor = seqUiState.playHeadsColor,
                        playhead = playhead,
                        playheadRepeat = playheadRepeat
                    )

                    if(seqUiState.isRepeating) repeatBounds(
                        this@with.channelState.value.totalTime,
                        this@with.channelState.value.repeatStartTime,
                        this@with.channelState.value.repeatEndTime,
                        widthFactor,
                        0.5f
                    )
                }
            }
        }
    }
}


@Composable
fun NotesGrid(
    seqViewModel: SeqViewModel,
    seqUiState: SeqUiState,
    channelState: ChannelState,
    scrollState: ScrollState,
    gridWidth: Dp,
    keyboardWidth: Dp,
) {
    with(seqViewModel.channelSequences[seqUiState.selectedChannel]){

        val noteHeight = seqUiState.stepViewNoteHeight

        BoxWithConstraints(
            modifier = Modifier
                .padding(top = 1.dp, bottom = 1.dp, end = 1.dp)
                .height(noteHeight * 128)
        ) {
            val rememberInteraction = remember { seqViewModel::rememberInteraction }
            val pressPad = remember { seqViewModel::addToPressPadList }
            val updatePadPitchOnChannel = remember { seqViewModel::updatePadPitchOnChannel }
            val savePadPitchToDatabase = remember { seqViewModel::savePadPitchToDatabase }

            var xOffset by remember { mutableStateOf(0f) }
            var yOffset by remember { mutableStateOf(0f) }
            val halfOfQuantize = seqUiState.quantizationTime / seqUiState.factorBpm / 2

            Row(
                modifier = Modifier
                    .verticalScroll(
                        scrollState,
                        reverseScrolling = true,
                    )
            ) {
                Box(
                    modifier = Modifier.fillMaxHeight()
                ) {
                    repeat(128) {
                        val pitch = 127-it
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier.offset(x = 0.dp, y = noteHeight * it)
                        ) {
                            PianoKey(
                                stepView = true,
                                interactionSource = interactionSources[pitch].interactionSource,
                                rememberInteraction = rememberInteraction,
                                seqIsRecording = seqUiState.seqIsRecording,
                                noteIsPlaying = playingNotes[pitch] > 0,
                                isPressed = pressedNotes[pitch].isPressed,
                                pressPad = pressPad,
                                updatePadPitchOnChannel = updatePadPitchOnChannel,
                                setPadPitchByPianoKey = seqUiState.setPadPitchByPianoKey,
                                savePadPitchToDatabase = savePadPitchToDatabase,
                                selectedChannel = seqUiState.selectedChannel,
                                pitch = pitch,
                                keyWidth = keyboardWidth,
                                keyHeight = noteHeight,
                                notesPadding = 0.dp,
                                whiteKey = seqViewModel.keysPattern[127-it]
                            )
                            if (pitch % 12 == 0) {
                                Text(
                                    text = "${(pitch / 12) - 1}",
                                    fontSize = 14.nonScaledSp,
                                    color = BackGray,
                                    modifier = Modifier.clipToBounds()
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(buttonsBg)
                        .pointerInput(
                            seqUiState.selectedChannel, seqUiState.seqIsPlaying, channelState.notes
                        ) {
                            with(channelState) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        val pitch = 127 - (offset.y / noteHeight.toPx()).toInt()
                                        var time = ((offset.x.toDp().value / gridWidth.value) * totalTime).toDouble()

                                        val (noteOnIndex, noteOffIndex: Int) = findNoteIndicesAroundGivenTime(pitch, time, Int.MAX_VALUE)

                                        val noteExistsWhereWeTap =
                                            (noteOnIndex > -1 && noteOffIndex > -1) && (
                                                (time in notes[noteOnIndex].time..notes[noteOffIndex].time)  // [...]
                                                    || (noteOnIndex > noteOffIndex &&
                                                    (time in 0.0..notes[noteOffIndex].time || time in notes[noteOnIndex].time..totalTime.toDouble())) // ..] & [..
                                                )
                                        Log.d(TAG, "onTap: noteExistsWhereWeTap = $noteExistsWhereWeTap")

                                        // if note exists where we tap - erase it, else record new note
                                        if (noteExistsWhereWeTap) {

                                            erasing(seqUiState.isRepeating, noteOnIndex, true)
                                            refreshIndices()
                                            updateNotes()

                                        } else {
                                            time = seqViewModel.quantizeTime(time - seqUiState.quantizationTime / 2)
                                            val noteOffTime = time + seqUiState.quantizationTime

                                            recordNote(
                                                pitch = pitch,
                                                velocity = 100,
                                                id = noteId,
                                                quantizationTime = seqUiState.quantizationTime,
                                                quantizeTime = seqViewModel::quantizeTime,
                                                factorBpm = seqUiState.factorBpm,
                                                seqIsPlaying = seqUiState.seqIsPlaying,
                                                isRepeating = seqUiState.isRepeating,
                                                isQuantizing = seqUiState.isQuantizing,
                                                isStepView = true,
                                                isStepRecord = true,
                                                allowRecordShortNotes = seqUiState.allowRecordShortNotes,
                                                customTime = time,
                                            )

                                            recordNote(
                                                pitch = pitch,
                                                velocity = 0,
                                                id = noteId,
                                                quantizationTime = seqUiState.quantizationTime,
                                                quantizeTime = seqViewModel::quantizeTime,
                                                factorBpm = seqUiState.factorBpm,
                                                seqIsPlaying = seqUiState.seqIsPlaying,
                                                isRepeating = seqUiState.isRepeating,
                                                isQuantizing = seqUiState.isQuantizing,
                                                isStepView = true,
                                                isStepRecord = true,
                                                allowRecordShortNotes = seqUiState.allowRecordShortNotes,
                                                customTime = noteOffTime,
                                            )

                                            increaseNoteId()
                                            if(!seqUiState.seqIsPlaying) updateNotes()
                                        }
                                    }
                                )
                            }
                        }
                        .pointerInput(
                            seqUiState.seqIsPlaying,
                            seqUiState.selectedChannel,
                            seqUiState.isRepeating,
                            seqUiState.isQuantizing,
                            channelState.notes,
                        ) {
                            var noteDetected = false
                            var pitch = -1
                            var time: Double
                            var timeQuantized = -1.0
                            var noteDeltaTime = -1.0
                            var dragDeltaTime: Double
                            var noteOnIndex = -1
                            var noteOffIndex = -1
                            var changeLengthAreaDetected = false
                            with(channelState) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        xOffset = offset.x
                                        yOffset = offset.y
                                        pitch = 127 - (yOffset / noteHeight.toPx()).toInt()
                                        time = ((xOffset / gridWidth.toPx()) * totalTime).toDouble()

                                        val (getNoteOn, getNoteOff) = getNoteOnAndNoteOffIndices(pitch, time)
                                        noteOnIndex = getNoteOn
                                        noteOffIndex = getNoteOff

                                        noteDetected = isNoteDetected(noteOnIndex, noteOffIndex, time)
                                        changeLengthAreaDetected =
                                            if (noteOnIndex == -1 || noteOffIndex == -1) false
                                            else {
                                                isChangeLengthAreaDetected(noteOnIndex, noteOffIndex, time)
                                            }

                                        Log.d(TAG, "onDragStart: noteDetected = $noteDetected")

                                        if (noteDetected) {
                                            draggedNoteOnIndex = noteOnIndex
                                            draggedNoteOffIndex = noteOffIndex

                                            if (seqUiState.isQuantizing) {
                                                noteDeltaTime = getNoteDeltaTime(changeLengthAreaDetected, noteOnIndex, noteOffIndex, seqViewModel::quantizeTime)
                                                timeQuantized = seqViewModel.quantizeTime(time + noteDeltaTime)
                                            }

                                            fireNoteOffInTime(seqUiState.seqIsPlaying, seqUiState.isRepeating, seqUiState.factorBpm, noteOnIndex, noteOffIndex, pitch)
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()

                                        if (!noteDetected) {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                scrollState.scrollTo(scrollState.value + dragAmount.y.toInt())
                                            }
                                        } else {
                                            xOffset += dragAmount.x
                                            if (!changeLengthAreaDetected) {
                                                yOffset += dragAmount.y
                                            }
                                            time = (xOffset * channelState.totalTime / gridWidth.toPx()).toInt() + noteDeltaTime

                                            // horizontal drag
                                            when {
                                                !seqUiState.isQuantizing -> {
                                                    dragDeltaTime = (dragAmount.x.toDp() * BARTIME / gridWidth).toDouble()
                                                    if (!changeLengthAreaDetected) changeNoteTime(noteOnIndex, dragDeltaTime, true)
                                                    changeNoteTime(noteOffIndex, dragDeltaTime, true)
                                                }
                                                seqViewModel.quantizeTime(time) != timeQuantized -> {
                                                    val deltaQuantized = seqViewModel.quantizeTime(time) - timeQuantized
                                                    if (!changeLengthAreaDetected) changeNoteTime(noteOnIndex, deltaQuantized, true)
                                                    changeNoteTime(noteOffIndex, deltaQuantized, true)
                                                    timeQuantized = seqViewModel.quantizeTime(time)
                                                }
                                            }

                                            val draggedNoteId = notes[noteOnIndex].id

                                            // vertical drag
                                            if (!changeLengthAreaDetected && (pitch != 127 - (yOffset / noteHeight.toPx()).toInt())) {
                                                pitch = (127 - (yOffset / noteHeight.toPx()).toInt()).coerceIn(0..127)
                                                changeNotePitch(noteOnIndex, pitch)
                                                changeNotePitch(noteOffIndex, pitch)
                                            }

                                            val getNoteIndices = swapNoteOffAndNoteOnIfWrapAroundNote(changeLengthAreaDetected, noteOnIndex, noteOffIndex, draggedNoteId)
                                            noteOnIndex = getNoteIndices.first
                                            noteOffIndex = getNoteIndices.second

                                            refreshIndices()

                                            if (!seqUiState.seqIsPlaying) updateStepViewNoteGrid()
                                        }
                                    },
                                    onDragEnd = {
                                        if (noteDetected) {
                                            removeNoteOffFromIgnore(seqUiState.isRepeating)

                                            draggedNoteOnIndex = -1
                                            draggedNoteOffIndex = -1

                                            eraseOverlappingNotes(noteOnIndex, noteOffIndex, pitch, seqUiState.isRepeating)

                                            if (draggedNoteOffJobPitch != pitch && draggedNoteOffJob.isActive) {
                                                draggedNoteOffJob.cancel()
                                                Log.d(TAG, "draggedNoteOffJob.cancel()")
                                                kmmk.noteOn(channelNumber, draggedNoteOffJobPitch, 0)
                                                changePlayingNotes(draggedNoteOffJobPitch, 0)
                                            }

                                            if (!seqUiState.seqIsPlaying) updateNotes()
                                        }
                                    }
                                )
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
//                        modifier = Modifier.height(noteHeight * 128).fillMaxWidth(),
                    ){
                        for (i in 0..127) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(noteHeight)
                                    .offset(x = 0.dp, y = noteHeight * i)
                                    .padding(vertical = 0.3.dp)
                                    .background(
                                        if (seqViewModel.keysPattern[127-i]) BackGray else stepViewBlackRows
                                    )
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.width(gridWidth)
                    ){
                        for (i in 1..15) {
                            Box(
                                modifier = Modifier
                                    .height(noteHeight * 128)
                                    .width(0.6.dp)
                                    .background(if ((i + 4) % 4 == 0) buttons else buttonsBg)
                            )
                        }
                    }

                    val widthFactor = gridWidth / BARTIME
                    val playhead = widthFactor * channelState.deltaTime.toInt()
                    val playheadRepeat = widthFactor * channelState.deltaTimeRepeat.toInt()

                    with(channelState) {
                        for(i in notes.indices) {
                            if(notes[i].velocity > 0) {
                                val offsetX = widthFactor * notes[i].time.toInt()
                                val (noteOffIndex, noteOffTime) = getNotePairedIndexAndTime(i, true)
                                val noteWidth = widthFactor * (noteOffTime - notes[i].time).toInt()
                                val deltaWidth = if (seqUiState.isRepeating) playheadRepeat - offsetX else playhead - offsetX
                                val fasterThanHalfOfQuantize =
                                    (System.currentTimeMillis() - pressedNotes[notes[i].pitch].noteOnTimestamp <= halfOfQuantize) && noteOffIndex == -1

                                var noteLength = when {
                                    noteOffIndex != -1 -> noteWidth
                                    !seqUiState.seqIsPlaying -> widthFactor * seqUiState.quantizationTime.toFloat()
                                    fasterThanHalfOfQuantize && deltaWidth > widthFactor * seqUiState.quantizationTime.toFloat() -> 0.0001.dp // fix for glitch when recording near loop end
                                    else -> deltaWidth  // live-writing note (grows in length)
                                }

                                val wrapIndices = if (noteOffIndex != -1) i > noteOffIndex else true // fixing rare bug: changeLength unQuantized wrapAround results in wrapAround view but non-wrapAround indices (Dp.toInt() != Dp)

                                if (noteLength <= 0.dp && !fasterThanHalfOfQuantize && wrapIndices) noteLength += gridWidth

                                val wrapAround = if (noteOffIndex != -1) i > noteOffIndex else offsetX + noteLength >= gridWidth

                                val changeLengthArea =
                                    (if (noteLength / 2.5f < gridWidth / 12) noteLength / 2.5f else gridWidth / 12)

                                StepNote(
                                    offsetX = offsetX - 0.3.dp,
                                    offsetY = noteHeight * (127 - notes[i].pitch) - 0.3.dp,
                                    noteLength = noteLength + 0.6.dp,
                                    indexNoteOn = i,
                                    noteHeight = noteHeight + 0.6.dp,
                                    wrapAround = wrapAround,
                                    changeLengthArea = changeLengthArea,
                                    maxWidth = gridWidth
                                )
                            }
                        }
                    }
                    if(channelState.stepViewRefresh) Box(modifier = Modifier.size(0.1.dp).background(Color(0x01000000)))
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
                    .background(changeLengthAreaColor)
            )
//        Text(text = "$indexNoteOn") // here for debugging
        }
        if (wrapAround) {
            Canvas (modifier = Modifier.fillMaxSize()) {
                val x = if (offsetX != 0.dp) size.width else 1.dp.toPx()
                drawLine( buttonsBg, Offset(x, 0.6.dp.toPx()), Offset(x, size.height - 0.6.dp.toPx()), 2.dp.toPx())
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
    Log.d("emptyTag", "for holding Log in import")
}
