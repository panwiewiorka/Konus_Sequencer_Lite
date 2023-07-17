package com.example.mysequencer01ui.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysequencer01ui.PadsMode
import com.example.mysequencer01ui.PadsMode.CLEARING
import com.example.mysequencer01ui.PadsMode.DEFAULT
import com.example.mysequencer01ui.PadsMode.ERASING
import com.example.mysequencer01ui.PadsMode.LOADING
import com.example.mysequencer01ui.PadsMode.MUTING
import com.example.mysequencer01ui.PadsMode.QUANTIZING
import com.example.mysequencer01ui.PadsMode.SAVING
import com.example.mysequencer01ui.PadsMode.SELECTING
import com.example.mysequencer01ui.PadsMode.SOLOING
import com.example.mysequencer01ui.SeqView
import com.example.mysequencer01ui.ui.theme.BackGray
import com.example.mysequencer01ui.ui.theme.buttonsBg
import com.example.mysequencer01ui.ui.theme.buttonsColor
import com.example.mysequencer01ui.ui.theme.dusk
import com.example.mysequencer01ui.ui.theme.night
import com.example.mysequencer01ui.ui.theme.notWhite
import com.example.mysequencer01ui.ui.theme.playGreen
import com.example.mysequencer01ui.ui.theme.selectedButton
import com.example.mysequencer01ui.ui.theme.violet
import com.example.mysequencer01ui.ui.theme.warmRed
import kotlinx.coroutines.delay
import kotlin.math.min


val Int.nonScaledSp
    @Composable
    get() = (this / LocalDensity.current.fontScale).sp

val buttonsPadding = PaddingValues(top = 1.dp, end = 1.dp)
val buttonsShape = RoundedCornerShape(0.dp)
const val buttonTextSize = 12


@Composable
fun PadButton(
    channel: Int,
    pitch: Int,
    seqViewModel: SeqViewModel,
    seqUiState: SeqUiState,
    padsSize: Dp,
){
    val interactionSource = remember { MutableInteractionSource() }
    var elapsedTime = remember { 0L }
    var buttonIsPressed by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    seqViewModel.pressPad(channel, pitch, 100)
                    elapsedTime = System.currentTimeMillis()
                    buttonIsPressed = true
                }
                is PressInteraction.Release -> {
                    seqViewModel.pressPad(channel, pitch, 0, elapsedTime)
                    buttonIsPressed = false
                }
                is PressInteraction.Cancel -> {
                    seqViewModel.pressPad(channel, pitch, 0, elapsedTime)
                    buttonIsPressed = false
                }
            }
        }
    }
    val color = when (seqUiState.padsMode) {
        MUTING -> violet
        ERASING -> warmRed
        CLEARING -> notWhite
        else -> {
            if (seqUiState.seqIsRecording) warmRed
            else if (seqUiState.seqIsPlaying) playGreen
            else Color.Transparent
        }
    }
//    Log.d("ryjtyj", "${}")
    Box(modifier = Modifier.padding(buttonsPadding)){
        Button(
            interactionSource = interactionSource,
            onClick = {},
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if(seqUiState.selectedChannel == channel) selectedButton else buttonsColor
            ),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .size(padsSize - 1.dp)
                .border(
                    width = 4.dp,
                    color = if (seqUiState.sequences[channel].channelIsPlayingNotes) color else Color.Transparent
                )
//                .border(width = 4.dp, brush = Brush.radialGradient(
//                    if (seqUiState.sequences[channel].channelIsPlayingNotes) {
//                        when (seqUiState.seqMode) {
//                            MUTING -> listOf(violet, violet, Color.Transparent)
//                            ERASING -> listOf(warmRed, warmRed, Color.Transparent)
//                            CLEARING -> listOf(Color.White, Color.White, Color.Transparent)
//                            else -> {
//                                if (seqUiState.seqIsRecording) listOf(warmRed, warmRed, Color.Transparent)
//                                else if (seqUiState.seqIsPlaying) listOf(Color(0xFF008800), Color(0xFF008800), Color.Transparent)
//                                else listOf(Color.Transparent, Color.Transparent)
//                            }
//                        }
//                    } else listOf(Color.Transparent, Color.Transparent), radius = 250f
//                ),
//                    shape = RoundedCornerShape(0.dp)
//                )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (buttonIsPressed) color else Color.Transparent)
            ) {
                if(seqUiState.sequences[channel].isMuted) {
                    Canvas(modifier = Modifier.fillMaxSize()){
                        muteSymbol(DEFAULT)
                    }
                }
//                Text("MUTED", fontSize = buttonTextSize.nonScaledSp, color = notWhite)
            }
        }
    }
}


@Composable
fun PadsGrid(seqViewModel: SeqViewModel, seqUiState: SeqUiState, padsSize: Dp){
    Box(
        modifier = Modifier.background(buttonsBg),
        contentAlignment = Alignment.Center,
    ){
        Column {
            for (y in 0..3) {
                Row {
                    for(x in 0..3) {
                        Box {
                            PadButton(x + (3 - y) * 4, 60, seqViewModel, seqUiState, padsSize)
//                            Text("${x + (3 - y) * 4}")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AllButton(seqViewModel: SeqViewModel, buttonsSize: Dp){
    val interactionSource = remember { MutableInteractionSource() }
    var elapsedTime = remember { 0L }
    var buttonPressed by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    for(i in 0..15){
                        seqViewModel.pressPad(i, 26, 100, allButton = true)
                        buttonPressed = true
                    }
                    seqViewModel.updateSequencesUiState()
                    elapsedTime = System.currentTimeMillis()
                }
                is PressInteraction.Release -> {
                    for(i in 0..15){
                        seqViewModel.pressPad(i, 26, 0, elapsedTime, allButton = true)
                    }
                    buttonPressed = false
                    seqViewModel.updateSequencesUiState()
                }
                is PressInteraction.Cancel -> {
                    for(i in 0..15){
                        seqViewModel.pressPad(i, 26, 0, elapsedTime, allButton = true)
                    }
                    buttonPressed = false
                    seqViewModel.updateSequencesUiState()
                }
            }
        }
    }
        Box(
            modifier = Modifier
                .size(buttonsSize)
                .padding(buttonsPadding)
                .background(if (buttonPressed) dusk else buttonsColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {}
        ) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(if (buttonPressed) 0.dp else 6.dp)
                .alpha(0.6f)){
                allSymbol(buttonPressed)
            }
            Canvas(modifier = Modifier.fillMaxSize()){
                allSymbol(buttonPressed)
            }
        }
//    }
}


/*
@Composable
fun SymbolButton(seqViewModel: SeqViewModel, buttonsSize: Dp, padsMode: PadsMode, buttonType: PadsMode, bgColor: Color){
    val symbol = when(buttonType) {
        SELECTING -> DrawScope.shiftSymbol(padsMode)
    }
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(if(padsMode == buttonType) bgColor else buttonsColor)
            .clickable (
                interactionSource = buttonInteraction(
                    0,
                    { seqViewModel.editCurrentMode(SELECTING) },
                    { seqViewModel.editCurrentMode(SELECTING, true) }
                ),
                indication = null
            ) { }
    ){
        Canvas(modifier = Modifier
            .fillMaxSize()
            .blur(if (padsMode == buttonType) 0.dp else 6.dp)
            .alpha(0.6f)){
            symbol(padsMode, buttonType, bgColor)
        }
        Canvas(modifier = Modifier.fillMaxSize()){
            symbol(padsMode, buttonType, bgColor)
        }
    }
}
 */


@Composable
fun ShiftButton(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonsSize: Dp, ){
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(if (padsMode == SELECTING) dusk else buttonsColor)
            .clickable(
                interactionSource = buttonInteraction(
                    0,
                    { seqViewModel.editCurrentPadsMode(SELECTING) },
                    { seqViewModel.editCurrentPadsMode(SELECTING, true) }
                ),
                indication = null
            ) { }
    ){
        Canvas(modifier = Modifier
            .fillMaxSize()
            .blur(if (padsMode == SELECTING) 0.dp else 6.dp)
            .alpha(0.6f)){
            shiftSymbol(padsMode)
        }
        Canvas(modifier = Modifier.fillMaxSize()){
            shiftSymbol(padsMode)
        }
    }
}


@Composable
fun QuantizeButton(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonsSize: Dp, isQuantizing: Boolean, quantizeModeTimer: Int){
    val interactionSource = remember { MutableInteractionSource() }
    var elapsedTime = 0L
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    elapsedTime = System.currentTimeMillis()
                    seqViewModel.switchPadsToQuantizingMode(true)
                }
                is PressInteraction.Release -> {
                    if ((System.currentTimeMillis() - elapsedTime) < seqViewModel.toggleTime) {
                        seqViewModel.switchQuantization()
                    }
                    seqViewModel.switchPadsToQuantizingMode(false)
                }
                is PressInteraction.Cancel -> {
                    if ((System.currentTimeMillis() - elapsedTime) < seqViewModel.toggleTime) {
                        seqViewModel.switchQuantization()
                    }
                    seqViewModel.switchPadsToQuantizingMode(false)
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(if (padsMode == QUANTIZING) dusk else buttonsColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { }
    ){
        val progress = quantizeModeTimer.toFloat() / seqViewModel.toggleTime
        if (padsMode != QUANTIZING) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(6.dp)
                .alpha(0.6f)){
//                if(isQuantizing) quantizeSymbolColor(false) else quantizeSymbol(false)
                quantizeSymbol(false, isQuantizing)
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()){
//            if(isQuantizing) quantizeSymbolColor(padsMode == QUANTIZING) else quantizeSymbol(padsMode == QUANTIZING)
            quantizeSymbol(padsMode == QUANTIZING, isQuantizing)
            quantizeLineCounter(progress)
        }
    }
}


@Composable
fun SaveButton(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonsSize: Dp){  // similar to ClearButton, same as LoadButton
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(if (padsMode == SAVING) dusk else buttonsColor)
            .clickable(
                interactionSource = buttonInteraction(
                    0,
                    { seqViewModel.editCurrentPadsMode(SAVING) },
                    { seqViewModel.editCurrentPadsMode(SAVING, true) }
                ),
                indication = null
            ) { }
    ){
        Canvas(modifier = Modifier
            .fillMaxSize()
            .blur(if (padsMode == SAVING) 0.dp else 6.dp) // TODO move IF up from blur to Canvas (in all Composables)
            .alpha(0.6f)){
            saveArrow(padsMode)
        }
        Canvas(modifier = Modifier
            .fillMaxSize()){
            saveArrow(padsMode)
        }
    }
}


@Composable
fun LoadButton(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonsSize: Dp){
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(if (padsMode == LOADING) dusk else buttonsColor)
            .clickable(
                interactionSource = buttonInteraction(
                    0,
                    { seqViewModel.editCurrentPadsMode(LOADING) },
                    { seqViewModel.editCurrentPadsMode(LOADING, true) }
                ),
                indication = null
            ) { }
    ){
        Canvas(modifier = Modifier
            .fillMaxSize()
            .blur(if (padsMode == LOADING) 0.dp else 6.dp) // TODO move IF up from blur to Canvas (in all Composables)
            .alpha(0.6f)){
            loadArrow(padsMode)
        }
        Canvas(modifier = Modifier
            .fillMaxSize()){
            loadArrow(padsMode)
        }
    }
}


@Composable
fun SoloButton(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonsSize: Dp){
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(if (padsMode == SOLOING) violet else buttonsColor)
            .rotate(24f)
            .clickable(
                interactionSource = buttonInteraction(
                    seqViewModel.toggleTime,
                    { seqViewModel.editCurrentPadsMode(SOLOING) },
                    { seqViewModel.editCurrentPadsMode(SOLOING, true) }
                ),
                indication = null
            ) { }
    ){
        Canvas(modifier = Modifier
            .fillMaxSize()
            .blur(if (padsMode == SOLOING) 0.dp else 6.dp)
            .alpha(0.6f)){
            soloSymbol(padsMode)
        }
        Canvas(modifier = Modifier.fillMaxSize()){
            soloSymbol(padsMode)  // TODO offset to the left
        }
    }
}


@Composable
fun MuteButton(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonsSize: Dp){
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(if (padsMode == MUTING) violet else buttonsColor)
            .clickable(
                interactionSource = buttonInteraction(
                    seqViewModel.toggleTime,
                    { seqViewModel.editCurrentPadsMode(MUTING) },
                    { seqViewModel.editCurrentPadsMode(MUTING, true) }
                ),
                indication = null
            ) { }
    ){
        Canvas(modifier = Modifier
            .fillMaxSize()
            .blur(if (padsMode == MUTING) 0.dp else 6.dp)
            .alpha(0.6f)){
            muteSymbol(padsMode)
        }
        Canvas(modifier = Modifier.fillMaxSize()){
            muteSymbol(padsMode)
        }
    }
}


@Composable
fun EraseButton(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonsSize: Dp){
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(if (padsMode == ERASING) notWhite else buttonsColor)
            .clickable(
                interactionSource = buttonInteraction(
                    seqViewModel.toggleTime,
                    { seqViewModel.editCurrentPadsMode(ERASING) },
                    { seqViewModel.editCurrentPadsMode(ERASING, true) }
                ),
                indication = null
            ) { }
    ){
        Canvas(modifier = Modifier
            .fillMaxSize()
            .alpha(0.6f)
            .blur(if (padsMode == ERASING) 0.dp else 5.dp)){
            eraseSymbol(padsMode)
        }
        Canvas(modifier = Modifier.fillMaxSize()){
            eraseSymbol(padsMode)
        }
    }
}


@Composable
fun ClearButton(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonsSize: Dp){
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(if (padsMode == CLEARING) notWhite else buttonsColor)
            .clickable(
                interactionSource = buttonInteraction(
                    0,
                    { seqViewModel.editCurrentPadsMode(CLEARING) },
                    { seqViewModel.editCurrentPadsMode(CLEARING, true) }
                ),
                indication = null
            ) { }
    ){
        Canvas(modifier = Modifier
            .fillMaxSize()
            .blur(if (padsMode == CLEARING) 0.dp else 6.dp)
            .alpha(0.6f)){
            clearSymbol(padsMode)
        }
        Canvas(modifier = Modifier.fillMaxSize()){
            clearSymbol(padsMode)
        }
    }
}


@Composable
fun RecButton(seqViewModel: SeqViewModel, padsMode: PadsMode, seqIsRecording: Boolean, buttonsSize: Dp){
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(if (padsMode == DEFAULT && seqIsRecording) warmRed else buttonsColor)
            .clickable(
                interactionSource = buttonInteraction(
                    seqViewModel.toggleTime,
                    { seqViewModel.changeRecState() }
                ),
                indication = null
            ) { }
    ){
        if (!(seqIsRecording && padsMode == DEFAULT)) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .alpha(0.6f)
                .blur(4.dp)){
                recSymbol(padsMode, seqIsRecording)
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()){
            recSymbol(padsMode, seqIsRecording)
        }
    }
}


@Composable
fun PlayButton(seqViewModel: SeqViewModel, seqIsPlaying: Boolean, buttonsSize: Dp){
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(buttonsColor)
            .clickable(
                interactionSource = buttonInteraction(
                    seqViewModel.toggleTime,
                    { seqViewModel.startSeq() },
                    { seqViewModel.stopSeq() }
                ),
                indication = LocalIndication.current
            ) { }
    ){
        Canvas(modifier = Modifier
            .fillMaxSize()
            .blur(6.dp)
            .alpha(0.6f)){
            playSymbol(seqIsPlaying)
        }
        Canvas(modifier = Modifier.fillMaxSize()){
            playSymbol(seqIsPlaying)
        }
    }
}

@Composable
fun StopButton(seqViewModel: SeqViewModel, buttonsSize: Dp) {
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(buttonsColor)
            .clickable(
                interactionSource = buttonInteraction(
                    0,
                    { seqViewModel.stopAllNotes() },
                ),
                indication = LocalIndication.current
            ) { }
    ){
        Canvas(modifier = Modifier
            .fillMaxSize()
            .blur(6.dp)
            .alpha(0.6f)){
            stopSymbol()
        }
        Canvas(modifier = Modifier.fillMaxSize()){
            stopSymbol()
        }
    }
}


@Composable
fun SeqViewButton(
    seqViewModel: SeqViewModel,
    seqView: SeqView,
    buttonSeqView: SeqView,
    buttonsSize: Dp,
    text: String,
    textColor: Color = dusk,
    fontSize: Int = buttonTextSize,
){
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(if (seqView == buttonSeqView) night else buttonsColor)
            .clickable(
                interactionSource = buttonInteraction(
                    seqViewModel.toggleTime,
                    { seqViewModel.changeSeqViewState(buttonSeqView) }
                ),
                indication = null
            ) {},
        contentAlignment = Alignment.Center
    ) {
        if (seqView != buttonSeqView) {
            Text(
                text,
                fontSize = fontSize.nonScaledSp,
                color = textColor,
                modifier = Modifier
                    .blur(6.dp, BlurredEdgeTreatment.Unbounded)
                    .alpha(0.6f)
            )
        }
        Text(
            text,
            fontSize = fontSize.nonScaledSp,
            color = if (seqView == buttonSeqView) BackGray else textColor
        )
    }
}


@Composable
fun TextButton(
    seqViewModel: SeqViewModel,
    padsMode: PadsMode,
    buttonsSize: Dp,
    text: String = " ",
    textColor: Color = notWhite,
    fontSize: Int = buttonTextSize,
){
    Button(
        interactionSource = buttonInteraction(
            seqViewModel.toggleTime,
            {  },
            {  }
        ),
        onClick = {  },
        shape = buttonsShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if(false) notWhite else buttonsColor
        ),
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding),
    ) {
        Box{
            Text(text, fontSize = fontSize.nonScaledSp, color = textColor, modifier = Modifier
                .blur(6.dp, BlurredEdgeTreatment.Unbounded)
                .alpha(0.6f))
            Text(text, fontSize = fontSize.nonScaledSp, color = textColor)
        }
    }
}



@Composable
fun buttonInteraction(
    toggleTime: Int,
    function1: () -> Unit,
    function2: () -> Unit = { function1() }
): MutableInteractionSource {
    val interactionSource = remember { MutableInteractionSource() }
    var elapsedTime = 0L
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    function1(); elapsedTime = System.currentTimeMillis()
                }
                is PressInteraction.Release -> {
                    if ((System.currentTimeMillis() - elapsedTime) > toggleTime) function2()
                }
                is PressInteraction.Cancel -> {
                    if ((System.currentTimeMillis() - elapsedTime) > toggleTime) function2()
                }
            }
        }
    }
    Log.d("emptyTag", " ") // to hold in imports
    return interactionSource

}



@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}



@Stable
fun Modifier.recomposeHighlighter(): Modifier = this.then(recomposeModifier)

// Use a single instance + @Stable to ensure that recompositions can enable skipping optimizations
// Modifier.composed will still remember unique data per call site.
private val recomposeModifier =
    Modifier.composed(inspectorInfo = debugInspectorInfo { name = "recomposeHighlighter" }) {
        // The total number of compositions that have occurred. We're not using a State<> here be
        // able to read/write the value without invalidating (which would cause infinite
        // recomposition).
        val totalCompositions = remember { arrayOf(0L) }
        totalCompositions[0]++

        // The value of totalCompositions at the last timeout.
        val totalCompositionsAtLastTimeout = remember { mutableStateOf(0L) }

        // Start the timeout, and reset everytime there's a recomposition. (Using totalCompositions
        // as the key is really just to cause the timer to restart every composition).
        LaunchedEffect(totalCompositions[0]) {
            delay(3000)
            totalCompositionsAtLastTimeout.value = totalCompositions[0]
        }

        Modifier.drawWithCache {
            onDrawWithContent {
                // Draw actual content.
                drawContent()

                // Below is to draw the highlight, if necessary. A lot of the logic is copied from
                // Modifier.border
                val numCompositionsSinceTimeout =
                    totalCompositions[0] - totalCompositionsAtLastTimeout.value

                val hasValidBorderParams = size.minDimension > 0f
                if (!hasValidBorderParams || numCompositionsSinceTimeout <= 0) {
                    return@onDrawWithContent
                }

                val (color, strokeWidthPx) =
                    when (numCompositionsSinceTimeout) {
                        // We need at least one composition to draw, so draw the smallest border
                        // color in blue.
                        1L -> Color.Blue to 1f
                        // 2 compositions is _probably_ okay.
                        2L -> violet to 2.dp.toPx()
                        // 3 or more compositions before timeout may indicate an issue. lerp the
                        // color from yellow to red, and continually increase the border size.
                        else -> {
                            lerp(
                                Color.Yellow.copy(alpha = 0.8f),
                                warmRed.copy(alpha = 0.5f),
                                min(1f, (numCompositionsSinceTimeout - 1).toFloat() / 100f)
                            ) to numCompositionsSinceTimeout.toInt().dp.toPx()
                        }
                    }

                val halfStroke = strokeWidthPx / 2
                val topLeft = Offset(halfStroke, halfStroke)
                val borderSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)

                val fillArea = (strokeWidthPx * 2) > size.minDimension
                val rectTopLeft = if (fillArea) Offset.Zero else topLeft
                val size = if (fillArea) size else borderSize
                val style = if (fillArea) Fill else Stroke(strokeWidthPx)

                drawRect(
                    brush = SolidColor(color),
                    topLeft = rectTopLeft,
                    size = size,
                    style = style
                )
            }
        }
    }