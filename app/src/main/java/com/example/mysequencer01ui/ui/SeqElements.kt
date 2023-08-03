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
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.example.mysequencer01ui.Sequence
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
    interactionSource: MutableInteractionSource,
    channel: Int,
    pitch: Int,
    seqViewModel: SeqViewModel,
    seqUiState: SeqUiState,
    padsSize: Dp,
){
//    val interactionSource = remember { MutableInteractionSource() }
    var elapsedTime = remember { 0L }
    val buttonIsPressed by interactionSource.collectIsPressedAsState()
    LaunchedEffect(interactionSource, pitch) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    seqViewModel.pressPad(channel, pitch, 100)
                    elapsedTime = System.currentTimeMillis()
                    seqViewModel.rememberInteraction(channel, pitch, interaction)
                }
                is PressInteraction.Release -> {
                    seqViewModel.pressPad(channel, pitch, 0, elapsedTime)
                }
                is PressInteraction.Cancel -> {
                    seqViewModel.pressPad(channel, pitch, 0, elapsedTime)
                }
            }
        }
    }
    val color = when (seqUiState.padsMode) {
        MUTING -> violet
        ERASING -> Color.Transparent
        CLEARING -> Color.Transparent
        else -> {
            if (seqUiState.seqIsRecording) warmRed
            else if (seqUiState.seqIsPlaying) playGreen
            else Color.Transparent
        }
    }
    Box{
        Box(modifier = Modifier
            .padding(buttonsPadding)
            .alpha(if (seqUiState.sequences[channel].isMuted && channel != seqUiState.selectedChannel) 0.25f else 1f)
        ){
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
                        color = if (seqUiState.sequences[channel].channelIsPlayingNotes > 0) color else Color.Transparent
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (buttonIsPressed) color else Color.Transparent)
                ) {
                    if (seqUiState.sequences[channel].channelIsPlayingNotes > 0) {
                        DashedBorder(
                            when (seqUiState.padsMode) {
                                ERASING -> warmRed
                                CLEARING -> notWhite
                                else -> Color.Transparent
                            }
                        )
                    }
                    if(seqUiState.sequences[channel].isMuted && !seqUiState.sequences[channel].isSoloed) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            muteSymbol(false)
                        }
                    }
                }
            }
        }
        if(seqUiState.sequences[channel].isSoloed) {
            Canvas(modifier = Modifier
                .size(padsSize - 1.dp)
                .rotate(24f)) {
                soloSymbol(false)
            }
        }
    }
}

@Composable
fun DashedBorder(color: Color) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()){
            drawRect(
                color = color,
                topLeft = Offset(0f, 0f),
                size = size,
                style = Stroke(
                    width = 8.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(size.width / 2, size.width / 2), size.width / 4)
                )
            )
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
                            val pitch = 60
                            PadButton(
                                seqViewModel.interactionSources[x + (3 - y) * 4][pitch].interactionSource,
                                x + (3 - y) * 4,
                                pitch,
                                seqViewModel,
                                seqUiState,
                                padsSize
                            )
//                            Text("${x + (3 - y) * 4}")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun PadsModeButton(seqViewModel: SeqViewModel, padsMode: PadsMode, buttonType: PadsMode, buttonsSize: Dp, color: Color, toggleTime: Int){
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(if (padsMode == buttonType) color else buttonsColor)
            .clickable(
                interactionSource = buttonInteraction(
                    toggleTime,
                    { seqViewModel.editCurrentPadsMode(buttonType, true) },
                    { seqViewModel.editCurrentPadsMode(buttonType, false, true) }
                ),
                indication = null
            ) { }
    ){
        if (padsMode != buttonType) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(6.dp)
                .alpha(0.6f)
                .rotate(if(buttonType == SOLOING) 24f else 0f)
            ){
                when(buttonType) {
                    SELECTING -> shiftSymbol(false)
                    SAVING -> saveArrow(false)
                    LOADING -> loadArrow(false)
                    SOLOING -> soloSymbol(false)
                    MUTING -> muteSymbol(false)
                    ERASING -> eraseSymbol(false)
                    else -> clearSymbol(false)
                }
            }
        }
        Canvas(modifier = Modifier
            .fillMaxSize()
            .rotate(if(buttonType == SOLOING) 24f else 0f)
        ){
            when(buttonType) {
                SELECTING -> shiftSymbol(padsMode == SELECTING)
                SAVING -> saveArrow(padsMode == SAVING)
                LOADING -> loadArrow(padsMode == LOADING)
                SOLOING -> soloSymbol(padsMode == SOLOING)
                MUTING -> muteSymbol(padsMode == MUTING)
                ERASING -> eraseSymbol(padsMode == ERASING)
                else -> clearSymbol(padsMode == CLEARING)
            }
        }
    }
}


@Composable
fun AllButton(seqViewModel: SeqViewModel, buttonsSize: Dp, showStrikeStripe: Boolean){
    val interactionSource = remember { MutableInteractionSource() }
    var elapsedTime = remember { 0L }
    val buttonPressed by interactionSource.collectIsPressedAsState()
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    for(i in 0..15){
                        seqViewModel.pressPad(i, 26, 100, allButton = true)
                    }
                    seqViewModel.updateSequencesUiState()
                    elapsedTime = System.currentTimeMillis()
                }
                is PressInteraction.Release -> {
                    for(i in 0..15){
                        seqViewModel.pressPad(i, 26, 0, elapsedTime, allButton = true)
                    }
                    seqViewModel.updateSequencesUiState()
                }
                is PressInteraction.Cancel -> {
                    for(i in 0..15){
                        seqViewModel.pressPad(i, 26, 0, elapsedTime, allButton = true)
                    }
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
            if(showStrikeStripe) strikeStrip()
        }
        Canvas(modifier = Modifier.fillMaxSize()){
            allSymbol(buttonPressed)
            if(showStrikeStripe) strikeStrip()
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
    cancelPadInteraction: () -> Unit, // TODO remove, use seqViewModel.cancelPadInteraction() instead
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
                    {
                        seqViewModel.changeSeqViewState(buttonSeqView)
                        cancelPadInteraction()
                    },
                    { },
                ),
                indication = null
            ) { },
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


fun DrawScope.playHeads(
    seqUiState: SeqUiState,
    playhead: Float,
    playheadRepeat: Float
) {
    // playhead color
    val conditionalColor = when (seqUiState.padsMode) {
        PadsMode.MUTING -> violet
        PadsMode.ERASING -> warmRed
        PadsMode.CLEARING -> notWhite
        else -> {
            if (seqUiState.seqIsRecording) warmRed
            else playGreen
        }
    }
    // PLAYHEAD
    drawLine(
        color = conditionalColor,
        start = Offset(playhead, 0f),
        end = Offset(playhead, size.height)
    )
    // REPEAT PLAYHEAD
    if (seqUiState.isRepeating && seqUiState.seqIsPlaying) {
        drawLine(
            color = conditionalColor,
            start = Offset(playheadRepeat, 0f),
            end = Offset(playheadRepeat, size.height),
            4f,
        )
    }
}


fun DrawScope.repeatBounds(
    sequence: Sequence,
    widthFactor: Float,
    alpha: Float,
) {
    if (sequence.repeatStartTime < sequence.repeatEndTime) {
        drawRect(
            color = buttonsColor,
            topLeft = Offset(0f, 0f),
            size = Size(
                width = widthFactor * sequence.repeatStartTime.toFloat(),
                height = size.height
            ),
            alpha = alpha
        )
        drawRect(
            color = buttonsColor,
            topLeft = Offset(widthFactor * sequence.repeatEndTime.toFloat(), 0f),
            size = Size(
                width = widthFactor * (sequence.totalTime - sequence.repeatEndTime).toFloat(),
                height = size.height
            ),
            alpha = alpha
        )
    } else
        drawRect(
            color = buttonsColor,
            topLeft = Offset(widthFactor * sequence.repeatEndTime.toFloat(), 0f),
            size = Size(
                width = (widthFactor * (sequence.repeatStartTime - sequence.repeatEndTime)).toFloat(),
                height = size.height
            ),
            alpha = alpha
        )
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
                    Log.d("ryjtyj", "PressInteraction.Cancel")
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