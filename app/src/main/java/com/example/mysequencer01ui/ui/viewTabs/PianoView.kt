package com.example.mysequencer01ui.ui.viewTabs

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.ui.SeqUiState
import com.example.mysequencer01ui.ui.SeqViewModel
import com.example.mysequencer01ui.ui.theme.BackGray
import com.example.mysequencer01ui.ui.theme.buttonsColor
import com.example.mysequencer01ui.ui.theme.notWhite
import com.example.mysequencer01ui.ui.theme.playGreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PianoView(seqViewModel: SeqViewModel, seqUiState: SeqUiState, buttonsSize: Dp) {
    Log.d("emptyTag", "for holding Log in import")

    val keyWidth = 35.3.dp
    val keyHeight = buttonsSize * 1.9f
    val notesPadding = 1.dp

    val sequence = seqUiState.sequences[seqUiState.selectedChannel]

    val scrollStateLowWhite = rememberLazyListState()
    val scrollStateLowBlack = rememberLazyListState()
    val scrollStateHighWhite = rememberLazyListState()
    val scrollStateHighBlack = rememberLazyListState()

    LaunchedEffect(key1 = scrollStateLowWhite, key2 = scrollStateHighWhite) {
        CoroutineScope(Dispatchers.Main).launch {
            scrollStateLowWhite.scrollToItem(sequence.PianoViewLowPianoScroll + 7)
            scrollStateLowBlack.scrollToItem(sequence.PianoViewLowPianoScroll + 7)
            scrollStateHighWhite.scrollToItem(sequence.PianoViewHighPianoScroll + 7)
            scrollStateHighBlack.scrollToItem(sequence.PianoViewHighPianoScroll + 7)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            PianoKeyboard(seqUiState.selectedChannel, keyWidth, keyHeight, notesPadding, scrollStateHighWhite, scrollStateHighBlack, seqViewModel::pressPad)
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val updatePianoViewXScroll = seqUiState.sequences[seqUiState.selectedChannel]::updatePianoViewXScroll
                OctaveButton(buttonsSize, scrollStateLowWhite, scrollStateLowBlack, scrollStateHighWhite, scrollStateHighBlack, updatePianoViewXScroll, true)
                OctaveButton(buttonsSize, scrollStateLowWhite, scrollStateLowBlack, scrollStateHighWhite, scrollStateHighBlack, updatePianoViewXScroll, true, true)
                Spacer(modifier = Modifier.width(buttonsSize))
                OctaveButton(buttonsSize, scrollStateLowWhite, scrollStateLowBlack, scrollStateHighWhite, scrollStateHighBlack, updatePianoViewXScroll, false)
                OctaveButton(buttonsSize, scrollStateLowWhite, scrollStateLowBlack, scrollStateHighWhite, scrollStateHighBlack, updatePianoViewXScroll, false, true)
            }
            PianoKeyboard(seqUiState.selectedChannel, keyWidth, keyHeight, notesPadding, scrollStateLowWhite, scrollStateLowBlack, seqViewModel::pressPad)
        }
    }
}

@Composable
fun PianoKeyboard(
    selectedChannel: Int,
    keyWidth: Dp,
    keyHeight: Dp,
    notesPadding: Dp,
    scrollStateWhite: LazyListState,
    scrollStateBlack: LazyListState,
    pressPad: (Int, Int, Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(-keyHeight / 2)
    ) {
        // TODO check pattern: middle C == 60, -> lowest key = ?
        LazyRow(
            state = scrollStateWhite,
            userScrollEnabled = false
        ) {
            items(75) {
                Box(
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val octave = (it + 7) / 7
                    val semitone = when( (it + 7) % 7) {
                        0 -> 0
                        1 -> 2
                        2 -> 4
                        3 -> 5
                        4 -> 7
                        5 -> 9
                        else -> 11
                    }
                    val pitch = semitone + octave * 12
                    PianoKey(pressPad, selectedChannel, pitch, keyWidth, keyHeight, notesPadding, true)
                    if ((it + 7) % 7 == 0) Text("${it / 7 - 4}")
                }
            }
        }
        LazyRow(
            state = scrollStateBlack,
            userScrollEnabled = false,
            modifier = Modifier.offset(keyWidth / 2 + notesPadding, -keyHeight / 2)
        ) {
            items(75){
                val octave = (it + 7) / 7
                val semitone = when( (it + 7) % 7) {
                    0 -> 1
                    1 -> 3
                    3 -> 6
                    4 -> 8
                    5 -> 10
                    else -> 99
                }
                val pitch = semitone + octave * 12
                val a = (it + 7) % 7
                if(a == 2 || a == 6) Spacer(modifier = Modifier.width(keyWidth + notesPadding * 2))
                else PianoKey(pressPad, selectedChannel, pitch, keyWidth, keyHeight, notesPadding, false)
            }
        }
    }
}

@Composable
fun PianoKey(
    pressPad: (Int, Int, Int) -> Unit,
    selectedChannel: Int,
    pitch: Int,
    keyWidth: Dp,
    keyHeight: Dp,
    notesPadding: Dp,
    whiteKey: Boolean = true,
) {
    val keyColor = remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource, selectedChannel) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressPad(selectedChannel, pitch, 100)
                    keyColor.value = true
                }
                is PressInteraction.Release -> {
                    pressPad(selectedChannel, pitch, 0)
                    keyColor.value = false
                }
                is PressInteraction.Cancel -> {  }
            }
        }
    }
    Box(
        modifier = Modifier
            .padding(notesPadding, 0.dp)
            .background(
                if(keyColor.value) playGreen else {
                    if (whiteKey) notWhite else BackGray
                }
            )
            .width(keyWidth)
            .height(if (whiteKey) keyHeight else keyHeight / 2)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { }
    ) { }
}

@Composable
fun OctaveButton(
    buttonsSize: Dp,
    scrollStateLowWhite: LazyListState,
    scrollStateLowBlack: LazyListState,
    scrollStateHighWhite: LazyListState,
    scrollStateHighBlack: LazyListState,
    updatePianoViewXScroll: (x: Int, lowerPiano: Boolean) -> Unit,
    lowerPiano: Boolean,
    upButton: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    Button(
        elevation = null,
        onClick = {
            coroutineScope.launch {
                when {
                    lowerPiano && upButton -> {
                        if(scrollStateLowWhite.firstVisibleItemIndex < 68 && scrollStateLowBlack.firstVisibleItemIndex < 68) { // TODO needless if bc firstIndex << 68
                            scrollStateLowWhite.scrollToItem(scrollStateLowWhite.firstVisibleItemIndex + 7, 0)
                            scrollStateLowBlack.scrollToItem(scrollStateLowBlack.firstVisibleItemIndex + 7, 0)
                        }
                    }
                    lowerPiano && !upButton -> {
                        if(scrollStateLowWhite.firstVisibleItemIndex >= 7 && scrollStateLowBlack.firstVisibleItemIndex >= 7) {
                            scrollStateLowWhite.scrollToItem(scrollStateLowWhite.firstVisibleItemIndex - 7, 0)
                            scrollStateLowBlack.scrollToItem(scrollStateLowBlack.firstVisibleItemIndex - 7, 0)
                        } else {
                            scrollStateLowWhite.scrollToItem(0, 0)
                            scrollStateLowBlack.scrollToItem(0, 0)
                        }
                    }
                    !lowerPiano && upButton -> {
                        if(scrollStateHighWhite.firstVisibleItemIndex < 68 && scrollStateHighBlack.firstVisibleItemIndex < 68) {
                            scrollStateHighWhite.scrollToItem(scrollStateHighWhite.firstVisibleItemIndex + 7, 0)
                            scrollStateHighBlack.scrollToItem(scrollStateHighBlack.firstVisibleItemIndex + 7, 0)
                        }
                    }
                    else -> {
                        if(scrollStateHighWhite.firstVisibleItemIndex >= 7 && scrollStateHighBlack.firstVisibleItemIndex >= 7) {
                            scrollStateHighWhite.scrollToItem(scrollStateHighWhite.firstVisibleItemIndex - 7, 0)
                            scrollStateHighBlack.scrollToItem(scrollStateHighBlack.firstVisibleItemIndex - 7, 0)
                        } else {
                            scrollStateHighWhite.scrollToItem(0, 0)
                            scrollStateHighBlack.scrollToItem(0, 0)
                        }
                    }
                }
            }.also {
                val scroll = if(lowerPiano) scrollStateLowWhite.firstVisibleItemIndex else scrollStateHighWhite.firstVisibleItemIndex
                updatePianoViewXScroll(scroll, lowerPiano)
            }
        },
        shape = RoundedCornerShape(0f),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .width(buttonsSize)
            .height(buttonsSize / 3),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = buttonsColor
        ),
    ) {
        Text(text = if(upButton) ">" else "<", color = notWhite)
    }
}