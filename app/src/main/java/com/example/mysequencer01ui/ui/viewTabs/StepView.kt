package com.example.mysequencer01ui.ui.viewTabs

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.ScrollableDefaults.flingBehavior
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.material.Slider
import androidx.compose.material.SliderColors
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
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
import com.example.mysequencer01ui.PressedNote
import com.example.mysequencer01ui.RememberedPressInteraction
import com.example.mysequencer01ui.ui.BARTIME
import com.example.mysequencer01ui.ui.ChannelState
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
fun StepView(seqViewModel: SeqViewModel, seqUiState: SeqUiState, buttonsSize: Dp) {
    Log.d("emptyTag", "for holding Log in import")

    val channelState by seqViewModel.channelSequences[seqUiState.selectedChannel].channelState.collectAsState()

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
                valueRange = 0f..scrollState.maxValue.toFloat(), // TODO calculate before composing and remember
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
                val keyboardWidth = 42.dp
                val gridWidth = maxWidth - keyboardWidth

                NotesGrid(seqViewModel, seqUiState, channelState, scrollState, gridWidth, keyboardWidth)

                Canvas(
                    modifier = Modifier
                        .width(gridWidth)
                        .fillMaxHeight()
                        .offset(x = keyboardWidth)
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
    seqViewModel: SeqViewModel, seqUiState: SeqUiState, channelState: ChannelState, scrollState: ScrollState, gridWidth: Dp, keyboardWidth: Dp,
) {
    with(seqViewModel.channelSequences[seqUiState.selectedChannel]){

        val noteHeight = seqUiState.stepViewNoteHeight

        BoxWithConstraints(
            modifier = Modifier
                .padding(top = 1.dp, bottom = 1.dp, end = 1.dp)
                .height(noteHeight * 128)
        ) {
            val rememberInteraction by remember { mutableStateOf(seqViewModel::rememberInteraction) }
            val pressPad by remember { mutableStateOf(seqViewModel::pressPad) }

            var xOffset by remember { mutableStateOf(0f) }
            var yOffset by remember { mutableStateOf(0f) }
            val halfOfQuantize = seqUiState.quantizationTime / seqUiState.factorBpm / 2

            Row{
                StepViewKeyboard(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(
                            scrollState,
                            reverseScrolling = true,
                        ),
                    interactionSources = interactionSources,
                    rememberInteraction = rememberInteraction,
                    selectedChannel = seqUiState.selectedChannel,
                    playingNotes = playingNotes,
                    pressedNotes = channelState.pressedNotes,
                    seqIsRecording = seqUiState.seqIsRecording,
                    keyWidth = keyboardWidth - 1.dp, // 1.dp for parent's padding
                    keyHeight = seqUiState.stepViewNoteHeight,
                    pressPad = pressPad,
                    halfOfQuantize = halfOfQuantize
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(buttonsBg)
                        .verticalScroll(
                            scrollState,
                            reverseScrolling = true,
//                            flingBehavior = flingBehavior()
                        )
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
                                        Log.d("ryjtyj", "onTap: noteExistsWhereWeTap = $noteExistsWhereWeTap")

                                        // if note exists where we tap - erase it, else record new note
                                        if (noteExistsWhereWeTap) {
                                            erasing(seqUiState.isRepeating, noteOnIndex, true)

                                            if (seqUiState.isRepeating) {
                                                indexToRepeat = notes.indexOfLast { it.time < deltaTimeRepeat } + 1
                                            } else {
                                                indexToPlay = notes.indexOfLast { it.time < channelState.deltaTime } + 1
                                            }
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
                                                isStepView = true,
                                                isStepRecord = true,
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
                                                isStepView = true,
                                                isStepRecord = true,
                                                customTime = noteOffTime,
                                            )

                                            increaseNoteId()
                                        }
                                    }
                                )
                            }
                        }
                        .pointerInput(
                            seqUiState.selectedChannel,
                            seqUiState.seqIsPlaying,
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

                                        noteOnIndex = notes.indexOfLast { it.pitch == pitch && it.time <= time }

                                        if (noteOnIndex == -1 || notes[noteOnIndex].velocity == 0) {
                                            noteOffIndex = -1
                                            noteOffIndex = notes.indexOfFirst { it.pitch == pitch && it.time > time }

                                            if (noteOffIndex != -1 && notes[noteOffIndex].velocity == 0) {
                                                noteOnIndex =
                                                    getNotePairedIndexAndTime(noteOffIndex).index
                                            } else noteOffIndex = -1
                                        } else {
                                            noteOffIndex = getNotePairedIndexAndTime(noteOnIndex).index
                                        }

//                                Log.d("ryjtyj", "noteOnIndex = $noteOnIndex, noteOffIndex = $noteOffIndex")

                                        if (noteOnIndex == -1) noteOffIndex = -1
                                        if (noteOffIndex == -1) noteOnIndex = -1

                                        var widthOfChangeLengthArea =
                                            if (noteOnIndex < noteOffIndex) {   // normal case (not wrap-around)
                                                (notes[noteOffIndex].time - notes[noteOnIndex].time) / 2.5
                                            } else if (noteOnIndex > noteOffIndex) {   // wrap-around case
                                                (notes[noteOffIndex].time - notes[noteOnIndex].time + channelState.totalTime) / 2.5
                                            } else 0.0
                                        if (widthOfChangeLengthArea !in 0.0..channelState.totalTime / 12.0) {
                                            widthOfChangeLengthArea = channelState.totalTime / 12.0
                                        }

                                        val rightmostAreaOfChangeLength =
                                            if (noteOffIndex == -1) 0.0 else {
                                                (channelState.totalTime.toDouble() - widthOfChangeLengthArea + notes[noteOffIndex].time)
                                                    .coerceAtMost(channelState.totalTime.toDouble())
                                            }
                                        val leftmostAreaOfChangeLength =
                                            if (noteOffIndex == -1) 0.0 else {
                                                (notes[noteOffIndex].time - widthOfChangeLengthArea).coerceAtLeast(0.0)
                                            }

                                        // does note exists where we start dragging?
                                        noteDetected =
                                            (noteOnIndex > -1 && noteOffIndex > -1) &&
                                                (
                                                    (time in notes[noteOnIndex].time..notes[noteOffIndex].time)  // normal case (not wrap-around)
                                                        || ((noteOnIndex > noteOffIndex) &&                            // wrap-around case
                                                        (time in notes[noteOnIndex].time..channelState.totalTime.toDouble()) || (time in 0.0..notes[noteOffIndex].time))
                                                    )

                                        changeLengthAreaDetected =
                                            (noteOnIndex > -1 && noteOffIndex > -1) &&
                                                (
                                                    (time in (notes[noteOffIndex].time - widthOfChangeLengthArea)..notes[noteOffIndex].time)  // normal case (not wrap-around)
                                                        || ((noteOnIndex > noteOffIndex) &&                            // wrap-around case
                                                        (time in rightmostAreaOfChangeLength..channelState.totalTime.toDouble()) || (time in leftmostAreaOfChangeLength..notes[noteOffIndex].time))
                                                    )

                                        Log.d("ryjtyj", "onDragStart: noteDetected = $noteDetected")

                                        if (noteDetected) {

                                            draggedNoteOnIndex = noteOnIndex
                                            draggedNoteOffIndex = noteOffIndex

                                            if (seqUiState.isQuantizing) {
                                                if (!changeLengthAreaDetected) {
                                                    val tempTime = notes[noteOnIndex].time
                                                    changeNoteTime(noteOnIndex, seqViewModel.quantizeTime(tempTime))
                                                    noteDeltaTime = notes[noteOnIndex].time - tempTime
                                                    changeNoteTime(noteOffIndex, noteDeltaTime, true)
                                                } else {
                                                    val tempTime = notes[noteOffIndex].time
                                                    changeNoteTime(noteOffIndex, seqViewModel.quantizeTime(tempTime))
                                                    noteDeltaTime = notes[noteOffIndex].time - tempTime
                                                }
                                                timeQuantized = seqViewModel.quantizeTime(time + noteDeltaTime)
                                            }

                                            // if dragging note that is playing -> fire noteOff on time
                                            val currentIndex =
                                                if (seqUiState.isRepeating) indexToRepeat else indexToPlay
                                            if (seqUiState.seqIsPlaying &&
                                                (currentIndex in noteOnIndex + 1..noteOffIndex // normal case (not wrap-around)
                                                    || noteOnIndex > noteOffIndex && (   // wrap-around case
                                                        currentIndex in noteOnIndex + 1 until notes.size || currentIndex in 0..noteOffIndex
                                                    )
                                                )
                                            ) {
                                                val tempPitch = pitch
                                                CoroutineScope(Dispatchers.Default).launch {
                                                    val dt =
                                                        if (seqUiState.isRepeating) deltaTimeRepeat else channelState.deltaTime
                                                    val delayTime =
                                                        if (dt > notes[noteOffIndex].time) {
                                                            notes[noteOffIndex].time - dt + channelState.totalTime
                                                        } else {
                                                            notes[noteOffIndex].time - dt
                                                        }
                                                    delay((delayTime / seqUiState.factorBpm).toLong())
                                                    kmmk.noteOn(channelStateNumber, tempPitch, 0)
                                                    changePlayingNotes(tempPitch, 0)
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        draggedNoteOnIndex = -1
                                        draggedNoteOffIndex = -1
                                        // TODO if releasing on top of another note -> delete another
//                                }

                                    }
                                ) { change, dragAmount ->
                                    change.consume()

                                    // if note exists where we drag -> move it or change it's length,  else scroll the grid
                                    if (noteDetected) {
                                        xOffset += dragAmount.x
                                        if (!changeLengthAreaDetected) {
                                            yOffset += dragAmount.y
                                        }
                                        time = (xOffset * channelState.totalTime / gridWidth.toPx()).toInt() + noteDeltaTime

                                        if (seqUiState.isQuantizing) {
                                            if (seqViewModel.quantizeTime(time) != timeQuantized) {
                                                val deltaQuantized =
                                                    seqViewModel.quantizeTime(time) - timeQuantized
                                                if (!changeLengthAreaDetected) {
                                                    changeNoteTime(noteOnIndex, deltaQuantized, true)
                                                }
                                                changeNoteTime(noteOffIndex, deltaQuantized, true)
                                                timeQuantized = seqViewModel.quantizeTime(time)
                                            }
                                        } else {
                                            dragDeltaTime =
                                                (dragAmount.x.toDp() * BARTIME / gridWidth).toDouble()
                                            if (!changeLengthAreaDetected) {
                                                changeNoteTime(noteOnIndex, dragDeltaTime, true)
                                            }
                                            changeNoteTime(noteOffIndex, dragDeltaTime, true)
                                        }

                                        val draggedNoteId = notes[noteOnIndex].id

                                        if (!changeLengthAreaDetected) {
                                            if (pitch != 127 - (yOffset / noteHeight.toPx()).toInt()) {
                                                pitch = 127 - (yOffset / noteHeight.toPx()).toInt()
                                                changeNotePitch(noteOnIndex, pitch)
                                                changeNotePitch(noteOffIndex, pitch)
                                            }
                                        } else {
                                            if (notes[noteOffIndex].time == notes[noteOnIndex].time && noteOffIndex > noteOnIndex) {
                                                notes[noteOffIndex] = notes[noteOnIndex].also {
                                                    notes[noteOnIndex] = notes[noteOffIndex]
                                                } // swapping two elements for wrap-around note when length = 0
                                            }
                                        }

                                        sortNotesByTime()

                                        val noteOnIndexAfterSort = notes.indexOfFirst { it.id == draggedNoteId && it.velocity > 0 }
                                        val noteOffIndexAfterSort = notes.indexOfFirst { it.id == draggedNoteId && it.velocity == 0 }

                                        if (noteOnIndexAfterSort != noteOnIndex || noteOffIndexAfterSort != noteOffIndex) {
                                            noteOnIndex = noteOnIndexAfterSort
                                            draggedNoteOnIndex = noteOnIndex
                                            noteOffIndex = noteOffIndexAfterSort
                                            draggedNoteOffIndex = noteOffIndex

                                            if (seqUiState.isRepeating) {
                                                indexToRepeat = (notes.indexOfLast { it.time < deltaTimeRepeat } + 1).coerceAtLeast(0)
                                            } else {
                                                indexToPlay = (notes.indexOfLast { it.time < channelState.deltaTime } + 1).coerceAtLeast(0)
                                            }
                                        }

                                        if (!seqUiState.seqIsPlaying) updateStepView()

                                    } else {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            scrollState.scrollTo(scrollState.value + dragAmount.y.toInt())
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    Box{
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ){
                            for (i in 0..127) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(noteHeight)
                                        .padding(vertical = 0.3.dp)
                                        .background(
                                            if (getKeyColorAndNumber(
                                                    0,
                                                    127 - i
                                                ).isBlack
                                            ) stepViewBlackRows else BackGray
                                        )
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .height(noteHeight * 128)
                                .width(gridWidth)
                        ){
                            for (i in 1..15) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
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
                                    val noteOffIndexAndTime = getNotePairedIndexAndTime(i)
                                    val noteOffIndex = noteOffIndexAndTime.index
                                    val noteWidth = widthFactor * (noteOffIndexAndTime.time - notes[i].time).toInt()
                                    var noteLength =
                                        if (noteOffIndex == -1) {
                                            if (seqUiState.isRepeating) playheadRepeat - offsetX else playhead - offsetX  // live-writing note (grows in length)
                                        } else noteWidth
                                    val fasterThanHalfOfQuantize = System.currentTimeMillis() - pressedNotes[notes[i].pitch].noteOnTimestamp <= halfOfQuantize

                                    if (noteLength <= 0.dp && !fasterThanHalfOfQuantize) noteLength += gridWidth

                                    val wrapAround = offsetX + noteLength > gridWidth
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
                    .background(changeLengthAreaColor)
            )
//        Text(text = "$indexNoteOn")
        }
        if (wrapAround) {
            Canvas (modifier = Modifier.fillMaxSize()) {
                val endOfScreen = offsetX != 0.dp
                val x = if (endOfScreen) size.width else 0f
                drawLine(if (endOfScreen) changeLengthAreaColor else buttonsBg, Offset(x, 0.6.dp.toPx()), Offset(x, size.height - 0.6.dp.toPx()), 2.dp.toPx())
            }
        }
    }

}


@Composable
fun StepViewKeyboard(
    modifier: Modifier,
    interactionSources: Array<RememberedPressInteraction>,
    rememberInteraction: (Int, Int, PressInteraction.Press) -> Unit,
    selectedChannel: Int,
    playingNotes: Array<Int>,
    pressedNotes: Array<PressedNote>,
    seqIsRecording: Boolean,
    keyWidth: Dp,
    keyHeight: Dp,
    pressPad: (Int, Int, Int, Long, Boolean) -> Unit,
    halfOfQuantize: Double,
) {
    Column(modifier = modifier) {
        repeat(128) {
            val key = getKeyColorAndNumber(0, 127 - it)
            val pitch = key.number
            Box(
                contentAlignment = Alignment.BottomCenter
            ) {
                StepViewKey(
                    interactionSource = interactionSources[pitch].interactionSource,
                    rememberInteraction = rememberInteraction,
                    seqIsRecording = seqIsRecording,
                    noteIsPlaying = playingNotes[pitch] > 0,
                    isPressed = pressedNotes[pitch].isPressed,
                    pressPad = pressPad,
                    selectedChannel = selectedChannel,
                    pitch = pitch,
                    keyWidth = keyWidth,
                    keyHeight = keyHeight,
                    notesPadding = 0.dp,
                    whiteKey = !key.isBlack,
                    halfOfQuantize = halfOfQuantize
                )
                if (pitch % 12 == 0) Text("${(pitch / 12) - 1}")
            }
        }
    }
}

@Composable
fun StepViewKey(  // TODO merge with PianoKey()?
    interactionSource: MutableInteractionSource,
    rememberInteraction: (Int, Int, PressInteraction.Press) -> Unit,
    seqIsRecording: Boolean,
    noteIsPlaying: Boolean,
    isPressed: Boolean,
    pressPad: (Int, Int, Int, Long, Boolean) -> Unit,
    selectedChannel: Int,
    pitch: Int,
    keyWidth: Dp,
    keyHeight: Dp,
    notesPadding: Dp,
    whiteKey: Boolean,
    halfOfQuantize: Double,
) {
//    val keyIsPressed by interactionSource.collectIsPressedAsState()
//    val interactionSource = remember { MutableInteractionSource() }
    var elapsedTime = 0L
    LaunchedEffect(interactionSource, selectedChannel, pitch, isPressed) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressPad(selectedChannel, pitch, 100, 0, false)
                    rememberInteraction(selectedChannel, pitch, interaction)
                    elapsedTime = System.currentTimeMillis()
                }
                is PressInteraction.Release -> {
                    // second condition needed for slow smartphones skipping noteOff due to 'isPressed' not updating in time // TODO test PAD_record on Xiaomi
                    if (isPressed || (System.currentTimeMillis() - elapsedTime) < halfOfQuantize) pressPad(selectedChannel, pitch, 0, 0, false)
                }
                is PressInteraction.Cancel -> {
                    if (isPressed || (System.currentTimeMillis() - elapsedTime) < halfOfQuantize) pressPad(selectedChannel, pitch, 0, 0, false)
                }
            }
        }
    }
    val color = if (seqIsRecording) warmRed else playGreen
    Box(
        modifier = Modifier
            .padding(notesPadding, 0.dp)
            .border(
                if (noteIsPlaying) BorderStroke(2.dp, color) else BorderStroke(
                    0.3.dp,
                    stepViewBlackRows
                )
            )
            .background(
                if (isPressed) {
                    color
                } else {
                    if (whiteKey) notWhite else repeatButtons
                }
            )
            .width(keyWidth)
            .height(keyHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { }
    ) { }
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
