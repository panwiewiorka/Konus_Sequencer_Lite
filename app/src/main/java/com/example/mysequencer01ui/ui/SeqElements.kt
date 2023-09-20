package com.example.mysequencer01ui.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
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
import com.example.mysequencer01ui.ChannelSequence
import com.example.mysequencer01ui.PadsMode
import com.example.mysequencer01ui.PadsMode.CLEARING
import com.example.mysequencer01ui.PadsMode.DEFAULT
import com.example.mysequencer01ui.PadsMode.ERASING
import com.example.mysequencer01ui.PadsMode.LOADING
import com.example.mysequencer01ui.PadsMode.MUTING
import com.example.mysequencer01ui.PadsMode.SAVING
import com.example.mysequencer01ui.PadsMode.SELECTING
import com.example.mysequencer01ui.PadsMode.SOLOING
import com.example.mysequencer01ui.SeqView
import com.example.mysequencer01ui.ui.theme.BackGray
import com.example.mysequencer01ui.ui.theme.buttons
import com.example.mysequencer01ui.ui.theme.buttonsBg
import com.example.mysequencer01ui.ui.theme.dusk
import com.example.mysequencer01ui.ui.theme.night
import com.example.mysequencer01ui.ui.theme.notWhite
import com.example.mysequencer01ui.ui.theme.playGreen
import com.example.mysequencer01ui.ui.theme.repeatButtons
import com.example.mysequencer01ui.ui.theme.selectedButton
import com.example.mysequencer01ui.ui.theme.violet
import com.example.mysequencer01ui.ui.theme.warmRed
import kotlinx.coroutines.delay
import kotlin.math.min


val Int.nonScaledSp
    @Composable
    get() = (this / LocalDensity.current.fontScale).sp

val buttonsPadding = PaddingValues(top = 1.dp, end = 1.dp)
const val buttonTextSize = 12


@Composable
fun PadButton(
    interactionSource: MutableInteractionSource,
    channel: Int,
    pitch: Int,
    pressPad: (Int, Int, Int, Long, Boolean) -> Unit,
    rememberInteraction: (Int, Int, PressInteraction.Press) -> Unit,
    padsMode: PadsMode,
    padsSize: Dp,
    selectedChannel: Int,
    seqIsRecording: Boolean,
    channelIsPlayingNotes: Boolean,
    isPressed: Boolean,
    isMuted: Boolean,
    isSoloed: Boolean,
){
    var elapsedTime = remember { 0L }
    val buttonIsPressed by interactionSource.collectIsPressedAsState()
    LaunchedEffect(interactionSource, pitch, isPressed, padsMode) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressPad(channel, pitch, 100, 0, false)
                    elapsedTime = System.currentTimeMillis()
                    rememberInteraction(channel, pitch, interaction)
                }
                is PressInteraction.Release -> {
                    if (isPressed || padsMode != DEFAULT) {
                        pressPad(channel, pitch, 0, elapsedTime, false)
                    }
                }
                is PressInteraction.Cancel -> {
                    if (isPressed || padsMode != DEFAULT) {
                        pressPad(channel, pitch, 0, elapsedTime, false)
                    }
                    Log.d(TAG, "PressInteraction.Cancel -> ")
                }
            }
        }
    }
    val notActiveColor = when {
        selectedChannel == channel -> selectedButton
        isMuted -> repeatButtons
        else -> buttons
    }

    val color = when (padsMode) {
        SAVING -> dusk
        LOADING -> dusk
        SOLOING -> violet
        MUTING -> violet
        ERASING -> warmRed
        CLEARING -> notWhite
        else -> {
            if (seqIsRecording) warmRed
            else playGreen
        }
    }

    /*
    val backgroundColor by animateColorAsState(
        targetValue = when {
            buttonIsPressed && padsMode == SELECTING -> selectedButton
            buttonIsPressed || (sequences[channel].channelIsPlayingNotes > 0) -> color
            else -> notActiveColor
        },
        animationSpec = tween(
            durationMillis = if (buttonIsPressed || (sequences[channel].channelIsPlayingNotes > 0)) 0 else 200
        ),
        label = "padBG"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            sequences[channel].channelIsPlayingNotes > 0 -> color
            else -> notActiveColor
        },
        animationSpec = tween(
            durationMillis = if (sequences[channel].channelIsPlayingNotes > 0) 0 else 200
        ),
        label = "padBorder"
    )
     */

    Box (
        modifier = Modifier
            .padding(buttonsPadding)
            .size(padsSize - 1.dp)
            .background(
                when {
                    buttonIsPressed && padsMode == SELECTING -> selectedButton
                    buttonIsPressed -> color
//                    isSoloed && selectedChannel == channel -> violet
//                    isSoloed -> darkViolet
                    else -> notActiveColor
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {}
            .clipToBounds()
    ){
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isMuted && !isSoloed) 0.25f else 1f)
        ) {
            Canvas(modifier = Modifier
                .fillMaxSize()
            ){
                if (channelIsPlayingNotes) {
                    drawRect(
                        color = color,
                        topLeft = Offset(0f, 0f),
                        size = size,
                        style = Stroke(
                            width = 8.dp.toPx(),
                            pathEffect = if(padsMode == ERASING || padsMode == CLEARING) {
                                PathEffect.dashPathEffect(floatArrayOf(size.width / 2, size.width / 2), size.width / 4)
                            } else null
                        )
                    )
                }
            }
        }
        if (isSoloed) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .rotate(24f)) {
                soloSymbol((padsMode == SOLOING && buttonIsPressed)) // || selectedChannel == channel)
            }
        } else if (isMuted) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .alpha(if (selectedChannel == channel || buttonIsPressed) 1f else 0.25f)
            ) {
                muteSymbol(padsMode == MUTING && buttonIsPressed)
            }
        }
    }
}

@Composable
fun PadsGrid(
    channelSequences: MutableList<ChannelSequence>,
    pressPad: (Int, Int, Int, Long, Boolean) -> Unit,
    rememberInteraction: (Int, Int, PressInteraction.Press) -> Unit,
    padsMode: PadsMode,
    selectedChannel: Int,
    seqIsRecording: Boolean,
    padsSize: Dp,
    showChannelNumber: Boolean,
){
    Box(
        modifier = Modifier.background(buttonsBg),
        contentAlignment = Alignment.Center,
    ){
        Column {
            for (y in 0..3) {
                Row {
                    for(x in 0..3) {
                        Box {
                            val channel = x + (3 - y) * 4
                            val channelState by channelSequences[channel].channelState.collectAsState()
                            val pitch = channelState.padPitch
                            PadButton(
                                interactionSource = channelSequences[channel].interactionSources[pitch].interactionSource,
                                channel = channel,
                                pitch = pitch,
                                pressPad = pressPad,
                                rememberInteraction = rememberInteraction,
                                padsMode = padsMode,
                                padsSize = padsSize,
                                selectedChannel = selectedChannel,
                                seqIsRecording = seqIsRecording,
                                channelIsPlayingNotes = channelState.channelIsPlayingNotes > 0,
                                isPressed = channelState.pressedNotes[pitch].isPressed,
                                isMuted = channelState.isMuted,
                                isSoloed = channelState.isSoloed
                            )
                            if (showChannelNumber) {
                                Text(
                                    text = "${channel + 1}",
                                    color = BackGray,
                                    fontSize = 14.nonScaledSp,
                                    modifier = Modifier
                                        .padding(start = 9.dp, bottom = 6.dp)
                                        .align(Alignment.BottomStart)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun PadsModeButton(
    editCurrentPadsMode: (PadsMode, Boolean, Boolean) -> Unit,
    buttonIsSelected: Boolean,
    buttonType: PadsMode,
    buttonsSize: Dp,
    color: Color,
    toggleTime: Int
){
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(if (buttonIsSelected) color else buttons)
            .clickable(
                interactionSource = buttonInteraction(
                    toggleTime,
                    { editCurrentPadsMode(buttonType, true, false) },
                    { editCurrentPadsMode(buttonType, false, true) }
                ),
                indication = null
            ) { }
    ){
        if (!buttonIsSelected) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(6.dp)
                .alpha(0.6f)
                .rotate(if (buttonType == SOLOING) 24f else 0f)
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
            .rotate(if (buttonType == SOLOING) 24f else 0f)
        ){
            when(buttonType) {
                SELECTING -> shiftSymbol(buttonIsSelected)
                SAVING -> saveArrow(buttonIsSelected)
                LOADING -> loadArrow(buttonIsSelected)
                SOLOING -> soloSymbol(buttonIsSelected)
                MUTING -> muteSymbol(buttonIsSelected)
                ERASING -> eraseSymbol(buttonIsSelected)
                else -> clearSymbol(buttonIsSelected)
            }
        }
    }
}


@Composable
fun AllButton(
    pressPad: (Int, Int, Int, Long, Boolean) -> Unit,
    buttonsSize: Dp,
    showStrikeStripe: Boolean
){
    val interactionSource = remember { MutableInteractionSource() }
    var elapsedTime = remember { 0L }
    val buttonPressed by interactionSource.collectIsPressedAsState()
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    for(i in 0..15){
                        pressPad(i, 26, 100, 0, true)
                    }
                    elapsedTime = System.currentTimeMillis()
                }
                is PressInteraction.Release -> {
                    for(i in 0..15){
                        pressPad(i, 26, 0, elapsedTime, true)
                    }
                }
                is PressInteraction.Cancel -> {
                    for(i in 0..15){
                        pressPad(i, 26, 0, elapsedTime, true)
                    }
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(if (buttonPressed) dusk else buttons)
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
fun QuantizeButton(
    switchPadsToQuantizingMode: (Boolean) -> Unit,
    switchQuantization: () -> Unit,
    padsModeIsQuantizing: Boolean,
    buttonsSize: Dp,
    isQuantizing: Boolean,
    quantizeModeTimer: Int,
    toggleTime: Int,
){

    val interactionSource = remember { MutableInteractionSource() }
    var elapsedTime = 0L
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    elapsedTime = System.currentTimeMillis()
                    switchPadsToQuantizingMode(true)
                }
                is PressInteraction.Release -> {
                    if ((System.currentTimeMillis() - elapsedTime) < toggleTime) {
                        switchQuantization()
                    }
                    switchPadsToQuantizingMode(false)
                }
                is PressInteraction.Cancel -> {
                    if ((System.currentTimeMillis() - elapsedTime) < toggleTime) {
                        switchQuantization()
                    }
                    switchPadsToQuantizingMode(false)
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(if (padsModeIsQuantizing) dusk else buttons)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { }
    ){
        val progress = quantizeModeTimer.toFloat() / toggleTime
        if (!padsModeIsQuantizing) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .blur(6.dp)
                .alpha(0.6f)){
                quantizeSymbol(false, isQuantizing)
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()){
            quantizeSymbol(padsModeIsQuantizing, isQuantizing)
            quantizeLineCounter(progress)
        }
    }
}


@Composable
fun RecButton(changeRecState: () -> Unit, padsModeIsDefault: Boolean, seqIsRecording: Boolean, buttonsSize: Dp, toggleTime: Int){
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(if (padsModeIsDefault && seqIsRecording) warmRed else buttons)
            .clickable(
                interactionSource = buttonInteraction(
                    toggleTime,
                    { changeRecState() }
                ),
                indication = null
            ) { }
    ){
        if (!(seqIsRecording && padsModeIsDefault)) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .alpha(0.6f)
                .blur(4.dp)){
                recSymbol(padsModeIsDefault, seqIsRecording)
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()){
            recSymbol(padsModeIsDefault, seqIsRecording)
        }
    }
}


@Composable
fun PlayButton(startSeq: () -> Unit, stopSeq: () -> Unit, seqIsPlaying: Boolean, buttonsSize: Dp, toggleTime: Int){
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(buttons)
            .clickable(
                interactionSource = buttonInteraction(
                    toggleTime,
                    { startSeq() },
                    { stopSeq() }
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
fun StopButton(stopAllNotes: () -> Unit, buttonsSize: Dp) {
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(buttonsPadding)
            .background(buttons)
            .clickable(
                interactionSource = buttonInteraction(
                    0,
                    { stopAllNotes() },
                    { }
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
    changeSeqViewState: (SeqView) -> Unit,
    cancelAllPadsInteraction: () -> Unit,
    seqView: SeqView,
    buttonSeqView: SeqView,
    buttonsSize: Dp,
    toggleTime: Int, // potential momentary behaviour, need to speed up composition of StepView (& PianoView)
){
    val seqViewIsSelected = (seqView == buttonSeqView)
    var savedSeqView by remember { mutableStateOf(seqView) }
    var elapsedTime by remember { mutableStateOf(0L) }
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource, seqView, elapsedTime) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    elapsedTime = System.currentTimeMillis()
                    savedSeqView = seqView
                    changeSeqViewState(buttonSeqView)
                    cancelAllPadsInteraction()
                }
                is PressInteraction.Release -> {
                    if ((System.currentTimeMillis() - elapsedTime) > toggleTime) {
                        changeSeqViewState(savedSeqView)
                        cancelAllPadsInteraction()
                    }
                }
                is PressInteraction.Cancel -> {
                    if ((System.currentTimeMillis() - elapsedTime) > toggleTime) {
                        changeSeqViewState(savedSeqView)
                        cancelAllPadsInteraction()
                    }
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .size(buttonsSize)
            .padding(top = 1.dp)
            .background(if (seqViewIsSelected) night else buttons)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { },
        contentAlignment = Alignment.Center
    ) {
        val color = if (seqViewIsSelected) repeatButtons else dusk

        if (!seqViewIsSelected) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(6.dp)
                    .alpha(0.4f)
            ) {
                when (buttonSeqView) {
                    SeqView.LIVE -> tabLiveSymbol(color)
                    SeqView.PIANO -> tabPianoSymbol(color)
                    SeqView.STEP -> tabStepSymbol(color)
                    SeqView.AUTOMATION -> tabAutomationSymbol(color)
                    SeqView.SETTINGS -> tabSettingsSymbol(color)
                }
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (buttonSeqView) {
                SeqView.LIVE -> tabLiveSymbol(color)
                SeqView.PIANO -> tabPianoSymbol(color)
                SeqView.STEP -> tabStepSymbol(color)
                SeqView.AUTOMATION -> tabAutomationSymbol(color)
                SeqView.SETTINGS -> tabSettingsSymbol(color)
            }
        }
    }
}


fun DrawScope.playHeads(
    seqIsPlaying: Boolean,
    isRepeating: Boolean,
    playHeadsColor: Color,
    playhead: Float,
    playheadRepeat: Float
) {
    if (seqIsPlaying) {
        drawLine(
            color = playHeadsColor,
            start = Offset(playhead, 0f),
            end = Offset(playhead, size.height),
            1f
        )
        if (isRepeating) {
            drawLine(
                color = playHeadsColor,
                start = Offset(playheadRepeat, 0f),
                end = Offset(playheadRepeat, size.height),
                4f,
            )
        }
    }
}


fun DrawScope.repeatBounds(
    totalTime: Int,
    repeatStartTime: Double,
    repeatEndTime: Double,
    widthFactor: Float,
    alpha: Float,
) {
    val repeatEnd = widthFactor * repeatEndTime.toFloat()
    if (repeatStartTime < repeatEndTime) {
        drawRect(
            color = buttons,
            topLeft = Offset(0f, 0f),
            size = Size(
                width = widthFactor * repeatStartTime.toFloat(),
                height = size.height
            ),
            alpha = alpha
        )
        drawRect(
            color = buttons,
            topLeft = Offset(repeatEnd, 0f),
            size = Size(
                width = widthFactor * (totalTime - repeatEndTime).toFloat(),
                height = size.height
            ),
            alpha = alpha
        )
    } else
        drawRect(
            color = buttons,
            topLeft = Offset(repeatEnd, 0f),
            size = Size(
                width = (widthFactor * (repeatStartTime - repeatEndTime)).toFloat(),
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
    LaunchedEffect(interactionSource, toggleTime) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    function1(); elapsedTime = System.currentTimeMillis()
                }
                is PressInteraction.Release -> {
                    if ((System.currentTimeMillis() - elapsedTime) > toggleTime) function2()
                }
                is PressInteraction.Cancel -> {
                    Log.d(TAG, "PressInteraction.Cancel")
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