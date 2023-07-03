package com.example.mysequencer01ui.ui.viewTabs

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Slider
import androidx.compose.material.SliderColors
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.Note
import com.example.mysequencer01ui.PadsMode.*
import com.example.mysequencer01ui.ui.SeqUiState
import com.example.mysequencer01ui.ui.SeqViewModel
import com.example.mysequencer01ui.ui.theme.*


@Composable
fun StepView(seqViewModel: SeqViewModel, seqUiState: SeqUiState, buttonsSize: Dp) {
    Log.d("emptyTag", "for holding Log in import")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .background(screensBg)
    ) {
//        var value by remember { mutableStateOf(20f) }
//        VerticalSlider(
//            value = value,
//            onValueChange = { value = it; seqViewModel.changePianoRollNoteHeight(it.dp) },
//            modifier = Modifier
//                .fillMaxWidth(0.9f)
//                .height(50.dp)
//                .background(screensBg),
//            valueRange = 5f..40f
//        )

        // TODO padding for notesGrid

//        Box(
//            modifier = Modifier
//                .padding(16.dp)
//                .border(BorderStroke(2.dp, buttonsColor))
//        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackGray)
                    .background(if(seqUiState.visualArrayRefresh) Color(0x00000000) else Color(0x01000000))
                    //.recomposeHighlighter(),
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
                                .background(if ((i + 4) % 4 == 0) buttonsColor else buttonsBg)
                        )
                    }
                }

                NotesGrid(seqViewModel, seqUiState)

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
//        }
    }
}


@Composable
fun NotesGrid(
    seqViewModel: SeqViewModel, seqUiState: SeqUiState
) {
    with(seqUiState.sequences[seqUiState.selectedChannel]){
        val noteHeight = seqUiState.stepViewNoteHeight

        // TODO check if everything here is needed:
        val scrY by remember { mutableStateOf(StepViewYScroll) }
        val scrollState = rememberScrollState(scrY)
        updateStepViewYScroll(scrollState.value)

        BoxWithConstraints(
            modifier = Modifier.height(noteHeight * 128)
        ) {
            val maxHeight = maxHeight
            val maxWidth = maxWidth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .verticalScroll(scrollState, reverseScrolling = true)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                val pitch = 127 - (127 * it.y.toDp().value / (noteHeight * 128).value + 0.5).toInt() // TODO fix rounding or whatever (drawing doesn't align with tap)
                                recordNote(
                                    pitch = pitch,
                                    velocity = 100,
                                    staticNoteOffTime = seqViewModel.staticNoteOffTime,
                                    seqIsPlaying = seqUiState.seqIsPlaying,
                                    isRepeating = seqUiState.isRepeating,
                                    repeatLength = seqUiState.repeatLength,
                                    customRecTime = ((it.x.toDp().value / maxWidth.value) * totalTime).toInt(),
                                    stepRecord = true,
                                )

                                recordNote(
                                    pitch = pitch,
                                    velocity = 0,
                                    staticNoteOffTime = seqViewModel.staticNoteOffTime,
                                    seqIsPlaying = seqUiState.seqIsPlaying,
                                    isRepeating = seqUiState.isRepeating,
                                    repeatLength = seqUiState.repeatLength,
                                    customRecTime = 100 + ((it.x.toDp().value / maxWidth.value) * totalTime).toInt(),
                                    stepRecord = true,
                                )
                                seqViewModel.updateNotesGridState()
                            }
                        )
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
                        StepNote(
                            indexNoteOn = i,
                            //indexNoteOff = searchForPairedNoteOFF(i, notes),
                            noteHeight = noteHeight,
                            maxX = maxWidth,
                            note = notes[i],
                            isRepeating = seqUiState.isRepeating,
                            searchForPairedNoteOffIndexAndTime = ::searchForPairedNoteOffIndexAndTime,
                            updateNotesGridState = seqViewModel::updateNotesGridState,
                            changePairedNoteOffPitch = ::changePairedNoteOffPitch,
                            changePairedNoteOffTime = ::changePairedNoteOffTime,
                            changeLength = ::changeNoteLength,
                            erasing = ::erasing,
                            checkSorting = ::checkSorting
                        )
                    }
                }
            }
        }
    }

}


@Composable
fun StepNote(
    indexNoteOn: Int,
    //indexNoteOff: Int,
    noteHeight: Dp,
    maxX: Dp,
    note: Note,
    isRepeating: Boolean,
    searchForPairedNoteOffIndexAndTime: (index: Int) -> Pair<Int, Int>,
    updateNotesGridState: () -> Unit,
    changePairedNoteOffPitch: (index: Int, pitch: Int) -> Unit,
    changePairedNoteOffTime: (index: Int, time: Int) -> Unit,
    changeLength: (index: Int, length: Int) -> Unit,
    erasing: (Boolean, Int) -> Boolean,
    checkSorting: () -> Unit
) {
    val offY = noteHeight * (127 - note.pitch)
    var offsetX by remember { mutableStateOf(maxX * note.time / 2000) }
    var offsetY by remember { mutableStateOf(offY) }
    var offsetThreshold by remember { mutableStateOf(0) }
    var savedThreshold by remember { mutableStateOf(0) }

    val indexNoteOff = searchForPairedNoteOffIndexAndTime(indexNoteOn).first
    val noteOffTime = searchForPairedNoteOffIndexAndTime(indexNoteOn).second
    val noteLength = noteOffTime - note.time


    Box(
        modifier = Modifier
            .offset(offsetX, offsetY)
            .height(noteHeight)
            .width(
                if(indexNoteOn > indexNoteOff)
                    maxX - offsetX
                else maxX * noteLength / 2000
//                if(note.length < 0 || maxX < offsetX + maxX * note.length / 2000) {
//                    (maxX - offsetX)
//                } else maxX * note.length / 2000
            )
            .background(buttonsBg)
            .border(BorderStroke(0.6.dp, playGreen), RoundedCornerShape(0.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        erasing(isRepeating, indexNoteOn)
                        updateNotesGridState()
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        offsetY = noteHeight * (127 - note.pitch)
                        checkSorting()  // TODO not on the DragEnd, but every crossing of other note
                    }
                ) { change, dragAmount ->
                    change.consume()

                    offsetX += dragAmount.x.toDp()
                    val savedNoteTime = note.time
                    note.time = (offsetX * 2000 / maxX).toInt()
                    changePairedNoteOffTime(indexNoteOff, note.time - savedNoteTime)
                    if(note.time >= 2000) note.time -= 2000
                    else if(note.time < 0) note.time += 2000

                    offsetY += dragAmount.y.toDp()

                    /* BOUNDARIES
                    when {
                        offsetX < 0.dp -> offsetX = 0.dp
                        offsetX > maxX - width -> offsetX = maxX - width
                        else -> offsetX += dragAmount.x.toDp()
                    }
                    when {
                        offsetY < 0.dp -> offsetY = 0.dp
                        offsetY > maxY - height -> offsetY = maxY - height
                        else -> {
                     */

                    val roundingY = if (offsetY > offY) 0.5 else -0.5
                    offsetThreshold = ((offsetY - offY).value / noteHeight.value + roundingY).toInt()
                    if (offsetThreshold > savedThreshold) {
                        note.pitch -= 1
                        changePairedNoteOffPitch(indexNoteOff, note.pitch)
                        //updateNotesGridState()
                        savedThreshold = offsetThreshold
                    } else if (offsetThreshold < savedThreshold) {
                        note.pitch += 1
                        changePairedNoteOffPitch(indexNoteOff, note.pitch)
                        //updateNotesGridState()
                        savedThreshold = offsetThreshold
                    }
    //                    }
    //                }
                }
            }
    ) {
        Text("$indexNoteOn")
    }

    Box(
        modifier = Modifier
            .alpha(if(indexNoteOn > indexNoteOff) 1f else 0f)
            .offset(0.dp, offsetY)
            .height(noteHeight)
            .width(offsetX + maxX * noteLength / 2000)
            .background(buttonsBg)
            .border(BorderStroke(0.6.dp, playGreen), RoundedCornerShape(0.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        erasing(isRepeating, indexNoteOn)
                        updateNotesGridState()
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        offsetY = noteHeight * (127 - note.pitch)
                        checkSorting()  // TODO not on the DragEnd, but every crossing of other note
                    }
                ) { change, dragAmount ->
                    change.consume()

                    offsetX += dragAmount.x.toDp()
                    val savedNoteTime = note.time
                    note.time = (offsetX * 2000 / maxX).toInt()
                    changePairedNoteOffTime(indexNoteOff, note.time - savedNoteTime)
                    if(note.time >= 2000) note.time -= 2000
                    else if(note.time < 0) note.time += 2000

                    offsetY += dragAmount.y.toDp()

                    val roundingY = if (offsetY > offY) 0.5 else -0.5
                    offsetThreshold = ((offsetY - offY).value / noteHeight.value + roundingY).toInt()
                    if (offsetThreshold > savedThreshold) {
                        note.pitch -= 1
                        changePairedNoteOffPitch(indexNoteOff, note.pitch)
                        savedThreshold = offsetThreshold
                    } else if (offsetThreshold < savedThreshold) {
                        note.pitch += 1
                        changePairedNoteOffPitch(indexNoteOff, note.pitch)
                        savedThreshold = offsetThreshold
                    }
                }
            }
    ) {
        Text("$indexNoteOn w")
    }

    /*
    // WRAP-AROUND NOTE
    Box(
        modifier = Modifier
            .alpha(if(note.length < 0) 1f else 0f)
            .offset(0.dp, offsetY)
            .height(noteHeight)
            .width(offsetX + maxX * note.length / 2000)
            .background(buttonsBg)
            .border(BorderStroke(0.6.dp, playGreen), RoundedCornerShape(0.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        erasing(isRepeating, indexNoteOn)
                        updateNotesGridState()
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        offsetY = noteHeight * (127 - note.pitch)
                        checkSorting()
                    }
                ) { change, dragAmount ->
                    change.consume()

                    val indexNoteOff = searchForPairedNoteOffIndexAndTime(indexNoteOn)

                    offsetX += dragAmount.x.toDp()
                    val savedNoteTime = note.time
                    note.time = (offsetX * 2000 / maxX).toInt()
                    if(note.length < 0 && note.time + note.length <= 0) {  // drag left until end of wrap-around
                        changeLength(indexNoteOn, 2000 + note.length)
                    }
                    changePairedNoteOffTime(indexNoteOff, note.time - savedNoteTime)
                    if(note.time >= 2000) {  // drag right until END OF wrap-around
                        note.time = 0
                        changeLength(indexNoteOn, 2000 + note.length)
                        changePairedNoteOffTime(indexNoteOff, note.length)
                    }

                    offsetY += dragAmount.y.toDp()

                    /* BOUNDARIES
                    when {
                        offsetX < 0.dp -> offsetX = 0.dp
                        offsetX > maxX - width -> offsetX = maxX - width
                        else -> offsetX += dragAmount.x.toDp()
                    }
                    when {
                        offsetY < 0.dp -> offsetY = 0.dp
                        offsetY > maxY - height -> offsetY = maxY - height
                        else -> {
                     */

                    val roundingY = if (offsetY > offY) 0.5 else -0.5
                    offsetThreshold = ((offsetY - offY).value / noteHeight.value + roundingY).toInt()
                    if (offsetThreshold > savedThreshold) {
                        note.pitch -= 1
                        changePairedNoteOffPitch(indexNoteOff, note.pitch)
                        //updateNotesGridState()
                        savedThreshold = offsetThreshold
                    } else if (offsetThreshold < savedThreshold) {
                        note.pitch += 1
                        changePairedNoteOffPitch(indexNoteOff, note.pitch)
                        //updateNotesGridState()
                        savedThreshold = offsetThreshold
                    }
//                    }
//                }
                }
            }
    ) {
        Text("$indexNoteOn w")
    }
    */
}


//fun searchForPairedNoteOFF(index: Int, notes: Array<Note>): Int {
//    val searchedPitch = notes[index].pitch
//    var pairedNoteOffIndex = -1
//    var searchedIndex: Int
//    // searching forward from indexToPlay
//    if (index < notes.lastIndex) {
//        searchedIndex = notes
//            .copyOfRange(index + 1, notes.size)
//            .indexOfFirst { it.pitch == searchedPitch && it.velocity == 0 }
//        pairedNoteOffIndex = if (searchedIndex == -1) -1 else index + 1 + searchedIndex
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
