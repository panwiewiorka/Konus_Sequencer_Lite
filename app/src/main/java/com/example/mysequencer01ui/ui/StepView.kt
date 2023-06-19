package com.example.mysequencer01ui.ui

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.Note
import com.example.mysequencer01ui.PadsMode.*
import com.example.mysequencer01ui.ui.theme.*


@Composable
fun StepView(seqViewModel: SeqViewModel, seqUiState: SeqUiState) {
    Log.d("emptyTag", "for holding Log in import")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .background(screensBg)
    ) {
        var value by remember { mutableStateOf(20f) }
        VerticalSlider(
            value = value,
            onValueChange = { value = it; seqViewModel.changePianoRollNoteHeight(it.dp) },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(50.dp)
                .background(screensBg),
            valueRange = 5f..40f
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()){
            NotesGrid(seqViewModel, seqUiState)

            // TODO move into NotesGrid
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()){
                for (i in 0..16) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(0.6.dp)
                            .background(if ((i + 4) % 4 == 0) buttonsColor else buttonsBg)
                    )
                }
            }

            val playheadOffset = (seqUiState.sequences[seqUiState.selectedChannel].deltaTime / seqUiState.sequences[seqUiState.selectedChannel].totalTime * maxWidth.value).dp
            Playhead(seqUiState = seqUiState, modifier = Modifier.offset(playheadOffset, 0.dp).width(0.6.dp))

            var playheadRepeatOffset = (seqUiState.sequences[seqUiState.selectedChannel].deltaTimeRepeat / seqUiState.sequences[seqUiState.selectedChannel].totalTime * maxWidth.value).dp
            playheadRepeatOffset = if(playheadRepeatOffset.value < 0) playheadRepeatOffset + maxWidth else playheadRepeatOffset

            if(seqUiState.isRepeating) Playhead(seqUiState = seqUiState, modifier = Modifier.offset(playheadRepeatOffset, 0.dp).width(2.dp))

        }
    }
}


@Composable
fun NotesGrid(
    seqViewModel: SeqViewModel, seqUiState: SeqUiState

//    height = seqUiState.pianoRollNoteHeight,
//    sequence = seqUiState.sequences[seqUiState.selectedChannel],
//    updateNotesGridState = seqViewModel::updateNotesGridState,
//    pianoRollYScroll = seqUiState.sequences[seqUiState.selectedChannel].pianoRollYScroll,
//    updatePianoRollYScroll = seqUiState.sequences[seqUiState.selectedChannel]::updatePianoRollYScroll,
//    changePairedNoteOffPitch = seqUiState.sequences[seqUiState.selectedChannel]::changePairedNoteOffPitch,
//    changePairedNoteOffTime = seqUiState.sequences[seqUiState.selectedChannel]::changePairedNoteOffTime

) {
    val sequence = seqUiState.sequences[seqUiState.selectedChannel]
    val height = seqUiState.pianoRollNoteHeight

    val coroutineScope = rememberCoroutineScope()
    val scr by remember { mutableStateOf(seqUiState.sequences[seqUiState.selectedChannel].pianoRollYScroll) }
    val scrollState = rememberScrollState(scr)
    seqUiState.sequences[seqUiState.selectedChannel].updatePianoRollYScroll(scrollState.value)
    //scr = scrollState.value
    //scr = scrollState.maxValue / 128
//    coroutineScope.launch {
//        scrollState.scrollTo(scr)
//    }
    BoxWithConstraints(
        modifier = Modifier
            .height(height * 128)
            .fillMaxWidth()
            .background(BackGray)
            .verticalScroll(scrollState, reverseScrolling = true)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { /* Called when the gesture starts */ },
                    onDoubleTap = { /* Called on Double Tap */ },
                    onLongPress = { /* Called on Long Press */ },
                    onTap = {
//                        sequence.recordNote(
//                            pitch = 4,
//                            velocity = 100,
//                            staticNoteOffTime = seqViewModel.staticNoteOffTime,
//                            seqIsPlaying = seqUiState.seqIsPlaying,
//                            isRepeating = seqUiState.isRepeating,
//                            repeatLength = seqUiState.repeatLength,
//                            customRecTime = 100
//                        )

                    }
                )
            }
//            .scrollable(scrollState, orientation = Orientation.Horizontal,)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier
            .fillMaxWidth()
            .height(maxHeight)){
            for (i in 0..128) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .background(BackGray)
                        .border(BorderStroke(0.3.dp, buttonsBg))
                )
            }
        }

        for(i in sequence.notes.indices) {
            StepNote(
                indexNoteOff = searchForPairedNoteOFF(i, sequence.notes),
                height = height,
                offY = height * (127 - sequence.notes[i].pitch),
                maxX = maxWidth,
                maxY = maxHeight,
                note = sequence.notes[i],
                updateNotesGridState = seqViewModel::updateNotesGridState,
                changePairedNoteOffPitch = sequence::changePairedNoteOffPitch,
                changePairedNoteOffTime = sequence::changePairedNoteOffTime
            )
        }
    }
}


@Composable
fun StepNote(
    indexNoteOff: Int,
    height: Dp,
    offY: Dp,
    maxX: Dp,
    maxY: Dp,
    note: Note,
    updateNotesGridState: () -> Unit,
    changePairedNoteOffPitch: (index: Int, pitch: Int) -> Unit,
    changePairedNoteOffTime: (index: Int, time: Int) -> Unit
) {
    var offsetX by remember { mutableStateOf(maxX * note.time / 2000) }
    var offsetY by remember { mutableStateOf(offY) }
    var offsetThreshold by remember { mutableStateOf(0) }
    var savedThreshold by remember { mutableStateOf(0) }
    Box(modifier = Modifier
        .offset(offsetX, offsetY)
        .height(height)
        .width(maxX * note.length / 2000)
        .background(buttonsBg)
        .border(BorderStroke(0.6.dp, playGreen), RoundedCornerShape(0.dp))
        .pointerInput(Unit) {
//                detectTapGestures(
//                    onPress = { /* Called when the gesture starts */ },
//                    onDoubleTap = { /* Called on Double Tap */ },
//                    onLongPress = { /* Called on Long Press */ },
//                    onTap = { /* Called on Tap */ }
//                )
            detectDragGestures(
                onDragEnd = { offsetY = height * (127 - note.pitch) }
            ) { change, dragAmount ->
                change.consume()
                offsetX += dragAmount.x.toDp()
                val savedNoteTime = note.time
                note.time = (offsetX * 2000 / maxX).toInt()
                changePairedNoteOffTime(indexNoteOff, note.time - savedNoteTime)

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
                offsetThreshold = ( (offsetY - offY).value / height.value + roundingY ).toInt()
                if (offsetThreshold > savedThreshold) {
                    note.pitch -= 1
                    changePairedNoteOffPitch(indexNoteOff, note.pitch)
                    updateNotesGridState() // TODO needed?
                    savedThreshold = offsetThreshold
                } else if (offsetThreshold < savedThreshold) {
                    note.pitch += 1
                    changePairedNoteOffPitch(indexNoteOff, note.pitch)
                    updateNotesGridState() // TODO needed?
                    savedThreshold = offsetThreshold
                }
//                    }
//                }
            }
        }
    )
}

fun searchForPairedNoteOFF(index: Int, notes: Array<Note>): Int {
    val searchedPitch = notes[index].pitch
    var pairedNoteOffIndex = -1
    var searchedIndex: Int
    // searching forward from indexToPlay
    if (index <= notes.lastIndex) {
        searchedIndex = notes
            .copyOfRange(index, notes.size)
            .indexOfFirst { it.pitch == searchedPitch && it.velocity == 0 }
        pairedNoteOffIndex = if (searchedIndex == -1) -1 else searchedIndex + index
    }
    // searching forward from 0
    if (pairedNoteOffIndex == -1) {
        searchedIndex = notes
            .copyOfRange(0, index)
            .indexOfFirst { it.pitch == searchedPitch && it.velocity == 0 }
        pairedNoteOffIndex = if (searchedIndex == -1) -1 else searchedIndex
    }
    return pairedNoteOffIndex
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
