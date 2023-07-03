package com.example.mysequencer01ui.ui.viewTabs
//
//import android.util.Log
//import androidx.compose.foundation.*
//import androidx.compose.foundation.gestures.detectDragGestures
//import androidx.compose.foundation.gestures.detectTapGestures
//import androidx.compose.foundation.interaction.MutableInteractionSource
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.Slider
//import androidx.compose.material.SliderColors
//import androidx.compose.material.SliderDefaults
//import androidx.compose.material.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.geometry.Size
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.TransformOrigin
//import androidx.compose.ui.graphics.drawscope.DrawScope
//import androidx.compose.ui.graphics.graphicsLayer
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.layout.layout
//import androidx.compose.ui.unit.Constraints
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.dp
//import com.example.mysequencer01ui.Note
//import com.example.mysequencer01ui.PadsMode.*
//import com.example.mysequencer01ui.ui.SeqUiState
//import com.example.mysequencer01ui.ui.SeqViewModel
//import com.example.mysequencer01ui.ui.theme.*
//
//
//@Composable
//fun StepView1(seqViewModel: SeqViewModel, seqUiState: SeqUiState, buttonsSize: Dp) {
//    Log.d("emptyTag", "for holding Log in import")
//    Row(
//        verticalAlignment = Alignment.CenterVertically,
//        modifier = Modifier
//            .fillMaxSize()
//            .background(screensBg)
//    ) {
////        var value by remember { mutableStateOf(20f) }
////        VerticalSlider(
////            value = value,
////            onValueChange = { value = it; seqViewModel.changePianoRollNoteHeight(it.dp) },
////            modifier = Modifier
////                .fillMaxWidth(0.9f)
////                .height(50.dp)
////                .background(screensBg),
////            valueRange = 5f..40f
////        )
//
//        // TODO padding for notesGrid
//
////        Box(
////            modifier = Modifier
////                .padding(16.dp)
////                .border(BorderStroke(2.dp, buttonsColor))
////        ) {
//            BoxWithConstraints(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(BackGray)
//                    .background(
//                        if (seqUiState.visualArrayRefresh) Color(0x00000000) else Color(
//                            0x01000000
//                        )
//                    )
//                    //.recomposeHighlighter(),
//            ){
////                Row(
////                    horizontalArrangement = Arrangement.SpaceBetween,
////                    modifier = Modifier
////                        .fillMaxSize()
////                        .background(Color.Transparent)
////                ){
////                    for (i in 0..16) {
////                        Box(
////                            modifier = Modifier
////                                .fillMaxHeight()
////                                .width(0.6.dp)
////                                .background(if ((i + 4) % 4 == 0) buttonsColor else buttonsBg)
////                        )
////                    }
////                }
//
//                NotesGrid1(seqViewModel, seqUiState)
//
////                val sequence = seqUiState.sequences[seqUiState.selectedChannel]
////                val playheadOffset = (sequence.deltaTime / sequence.totalTime * maxWidth.value).dp
////
////                Playhead1(
////                    seqUiState = seqUiState,
////                    modifier = Modifier
////                        .offset(playheadOffset, 0.dp)
////                        .width(0.6.dp)
////                )
////
////                var playheadRepeatOffset = (sequence.deltaTimeRepeat / sequence.totalTime * maxWidth.value).dp
////                playheadRepeatOffset = if(playheadRepeatOffset.value < 0) playheadRepeatOffset + maxWidth else playheadRepeatOffset
////
////                if(seqUiState.isRepeating) {
////                    Playhead1(
////                        seqUiState = seqUiState,
////                        modifier = Modifier
////                            .offset(playheadRepeatOffset, 0.dp)
////                            .width(2.dp)
////                    )
////                }
//            }
////        }
//    }
//}
//
//
//@Composable
//fun NotesGrid1(
//    seqViewModel: SeqViewModel, seqUiState: SeqUiState
//) {
//    with(seqUiState.sequences[seqUiState.selectedChannel]){
//        val noteHeight = seqUiState.stepViewNoteHeight
//
//        // TODO check if everything here is needed:
//        val scrY by remember { mutableStateOf(StepViewYScroll) }
//        val scrollState = rememberScrollState(scrY)
//        updateStepViewYScroll(scrollState.value)
//
//        BoxWithConstraints(
//            modifier = Modifier.height(noteHeight * 128)
//        ) {
//            val maxHeight = maxHeight
//            val maxWidth = maxWidth
//
//            Canvas(modifier = Modifier
//                .fillMaxSize()
//                .background(Color.Transparent)
//                .verticalScroll(scrollState, reverseScrolling = true)
//                .pointerInput(Unit) {
//                    detectTapGestures(
//                        onTap = {
//                            val pitch =
//                                127 - (127 * it.y.toDp().value / (noteHeight * 128).value + 0.5).toInt() // TODO fix rounding or whatever (drawing doesn't align with tap)
//                            recordNote(
//                                pitch = pitch,
//                                velocity = 100,
//                                staticNoteOffTime = seqViewModel.staticNoteOffTime,
//                                seqIsPlaying = seqUiState.seqIsPlaying,
//                                isRepeating = seqUiState.isRepeating,
//                                repeatLength = seqUiState.repeatLength,
//                                customRecTime = ((it.x.toDp().value / maxWidth.value) * totalTime).toInt(),
//                                stepRecord = true,
//                            )
//
//                            recordNote(
//                                pitch = pitch,
//                                velocity = 0,
//                                staticNoteOffTime = seqViewModel.staticNoteOffTime,
//                                seqIsPlaying = seqUiState.seqIsPlaying,
//                                isRepeating = seqUiState.isRepeating,
//                                repeatLength = seqUiState.repeatLength,
//                                customRecTime = 100 + ((it.x.toDp().value / maxWidth.value) * totalTime).toInt(),
//                                stepRecord = true,
//                            )
//                            seqViewModel.updateNotesGridState()
//                        }
//                    )
//                }
//            ){
//                for(i in notes.indices) {
//                    stepNote1(
//                        indexNoteOn = i,
//                        indexNoteOff = searchForPairedNoteOFF(i, notes),
//                        noteHeight = noteHeight,
//                        maxX = maxWidth,
//                        maxY = maxHeight,
//                        note = notes[i],
//                        isRepeating = seqUiState.isRepeating,
//                        updateNotesGridState = seqViewModel::updateNotesGridState,
//                        changePairedNoteOffPitch = ::changePairedNoteOffPitch,
//                        changePairedNoteOffTime = ::changePairedNoteOffTime,
//                        erasing = ::erasing,
//                        toggleTime = seqViewModel.toggleTime,
//                        modifier = Modifier
//                            .offset(maxWidth * notes[i].time / 2000, noteHeight * (127 - notes[i].pitch))
//                            .pointerInput(Unit) {
//                                detectTapGestures(
//                                    onTap = {
//                                        erasing(seqUiState.isRepeating, i)
//                                        seqViewModel.updateNotesGridState()
//                                    }
//                                )
//                            }
//                    )
//                }
//            }
//
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(Color.Transparent)
//                    .verticalScroll(scrollState, reverseScrolling = true)
//                    .pointerInput(Unit) {
//                        detectTapGestures(
//                            onTap = {
//                                val pitch =
//                                    127 - (127 * it.y.toDp().value / (noteHeight * 128).value + 0.5).toInt() // TODO fix rounding or whatever (drawing doesn't align with tap)
//                                recordNote(
//                                    pitch = pitch,
//                                    velocity = 100,
//                                    staticNoteOffTime = seqViewModel.staticNoteOffTime,
//                                    seqIsPlaying = seqUiState.seqIsPlaying,
//                                    isRepeating = seqUiState.isRepeating,
//                                    repeatLength = seqUiState.repeatLength,
//                                    customRecTime = ((it.x.toDp().value / maxWidth.value) * totalTime).toInt(),
//                                    stepRecord = true,
//                                )
//
//                                recordNote(
//                                    pitch = pitch,
//                                    velocity = 0,
//                                    staticNoteOffTime = seqViewModel.staticNoteOffTime,
//                                    seqIsPlaying = seqUiState.seqIsPlaying,
//                                    isRepeating = seqUiState.isRepeating,
//                                    repeatLength = seqUiState.repeatLength,
//                                    customRecTime = 100 + ((it.x.toDp().value / maxWidth.value) * totalTime).toInt(),
//                                    stepRecord = true,
//                                )
//                                seqViewModel.updateNotesGridState()
//                            }
//                        )
//                    }
//            ) {
//                Column(
//                    modifier = Modifier.fillMaxSize()
//                ){
//                    for (i in 0..127) {
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(noteHeight)
//                                .background(Color.Transparent)
//                                .border(BorderStroke(0.3.dp, buttonsBg))
//                        )
//                    }
//                }
//
//                for(i in notes.indices) {
//                    StepNote(
//                        indexNoteOn = i,
//                        indexNoteOff = searchForPairedNoteOFF(i, notes),
//                        noteHeight = noteHeight,
//                        maxX = maxWidth,
//                        maxY = maxHeight,
//                        note = notes[i],
//                        isRepeating = seqUiState.isRepeating,
//                        updateNotesGridState = seqViewModel::updateNotesGridState,
//                        changePairedNoteOffPitch = ::changePairedNoteOffPitch,
//                        changePairedNoteOffTime = ::changePairedNoteOffTime,
//                        erasing = ::erasing,
//                        toggleTime = seqViewModel.toggleTime,
//                        checkSorting = ::checkSorting
//                    )
//                }
//            }
//        }
//    }
//
//}
//
//
//fun DrawScope.stepNote1(
//    indexNoteOn: Int,
//    indexNoteOff: Int,
//    noteHeight: Dp,
//    maxX: Dp,
//    maxY: Dp,
//    note: Note,
//    isRepeating: Boolean,
//    updateNotesGridState: () -> Unit,
//    changePairedNoteOffPitch: (index: Int, pitch: Int) -> Unit,
//    changePairedNoteOffTime: (index: Int, time: Int) -> Unit,
//    erasing: (Boolean, Int) -> Boolean,
//    toggleTime: Int,
//    modifier: Modifier
//) {
//    drawRect(
//        color = buttonsBg,
//        topLeft = Offset(0f, 0f),
//        size = Size(100f, noteHeight.toPx())
//    )
//
////    Box(modifier = Modifier
////        .offset(offsetX, offsetY)
////        .height(noteHeight)
////        .width(maxX * note.length / 2000)
////        .background(buttonsBg)
////        .border(BorderStroke(0.6.dp, playGreen), RoundedCornerShape(0.dp))
////        .pointerInput(Unit) {
////            detectTapGestures(
////                onTap = {
////                    erasing(isRepeating, indexNoteOn)
////                    updateNotesGridState()
////                }
////            )
////        }
////        .pointerInput(Unit) {
////            detectDragGestures(
////                onDragEnd = { offsetY = noteHeight * (127 - note.pitch) }
////            ) { change, dragAmount ->
////                change.consume()
////                offsetX += dragAmount.x.toDp()
////                val savedNoteTime = note.time
////                note.time = (offsetX * 2000 / maxX).toInt()
////                changePairedNoteOffTime(indexNoteOff, note.time - savedNoteTime)
////
////                offsetY += dragAmount.y.toDp()
////
////                /* BOUNDARIES
////                when {
////                    offsetX < 0.dp -> offsetX = 0.dp
////                    offsetX > maxX - width -> offsetX = maxX - width
////                    else -> offsetX += dragAmount.x.toDp()
////                }
////                when {
////                    offsetY < 0.dp -> offsetY = 0.dp
////                    offsetY > maxY - height -> offsetY = maxY - height
////                    else -> {
////                 */
////
////                val roundingY = if (offsetY > offY) 0.5 else -0.5
////                offsetThreshold = ((offsetY - offY).value / noteHeight.value + roundingY).toInt()
////                if (offsetThreshold > savedThreshold) {
////                    note.pitch -= 1
////                    changePairedNoteOffPitch(indexNoteOff, note.pitch)
////                    //updateNotesGridState()
////                    savedThreshold = offsetThreshold
////                } else if (offsetThreshold < savedThreshold) {
////                    note.pitch += 1
////                    changePairedNoteOffPitch(indexNoteOff, note.pitch)
////                    //updateNotesGridState()
////                    savedThreshold = offsetThreshold
////                }
//////                    }
//////                }
////            }
////        }
////    ) {
////        Text("$indexNoteOn")
////    }
//}
//
//fun searchForPairedNoteOFF1(index: Int, notes: Array<Note>): Int {
//    val searchedPitch = notes[index].pitch
//    var pairedNoteOffIndex = -1
//    var searchedIndex: Int
//    // searching forward from indexToPlay
//    if (index <= notes.lastIndex) {
//        searchedIndex = notes
//            .copyOfRange(index, notes.size)
//            .indexOfFirst { it.pitch == searchedPitch && it.velocity == 0 }
//        pairedNoteOffIndex = if (searchedIndex == -1) -1 else searchedIndex + index
//    }
//    // searching forward from 0
//    if (pairedNoteOffIndex == -1) {
//        searchedIndex = notes
//            .copyOfRange(0, index)
//            .indexOfFirst { it.pitch == searchedPitch && it.velocity == 0 }
//        pairedNoteOffIndex = if (searchedIndex == -1) -1 else searchedIndex
//    }
//    return pairedNoteOffIndex
//}
//
//
//@Composable
//fun Playhead1(seqUiState: SeqUiState, modifier: Modifier) {
//    val conditionalColor = when (seqUiState.padsMode) {
//        MUTING -> violet
//        ERASING -> warmRed
//        CLEARING -> notWhite
//        else -> {
//            if (seqUiState.seqIsRecording) warmRed
//            else playGreen
//        }
//    }
//    Box(
//        modifier = modifier
//            .fillMaxHeight()
//            .background(conditionalColor)
//    )
//}
//
//
//@Composable
//fun VerticalSlider1(
//    value: Float,
//    onValueChange: (Float) -> Unit,
//    modifier: Modifier = Modifier,
//    enabled: Boolean = true,
//    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
//    /*@IntRange(from = 0)*/
//    steps: Int = 0,
//    onValueChangeFinished: (() -> Unit)? = null,
//    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
//    colors: SliderColors = SliderDefaults.colors()
//){
//    Slider(
//        colors = colors,
//        interactionSource = interactionSource,
//        onValueChangeFinished = onValueChangeFinished,
//        steps = steps,
//        valueRange = valueRange,
//        enabled = enabled,
//        value = value,
//        onValueChange = onValueChange,
//        modifier = Modifier
//            .graphicsLayer {
//                rotationZ = 270f
//                transformOrigin = TransformOrigin(0f, 0f)
//            }
//            .layout { measurable, constraints ->
//                val placeable = measurable.measure(
//                    Constraints(
//                        minWidth = constraints.minHeight,
//                        maxWidth = constraints.maxHeight,
//                        minHeight = constraints.minWidth,
//                        maxHeight = constraints.maxHeight,
//                    )
//                )
//                layout(placeable.height, placeable.width) {
//                    placeable.place(-placeable.width, 0)
//                }
//            }
//            .then(modifier)
//    )
//}
