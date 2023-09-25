package com.example.mysequencer01ui.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mysequencer01ui.KmmkComponentContext
import com.example.mysequencer01ui.PadsMode.CLEARING
import com.example.mysequencer01ui.PadsMode.DEFAULT
import com.example.mysequencer01ui.PadsMode.ERASING
import com.example.mysequencer01ui.PadsMode.LOADING
import com.example.mysequencer01ui.PadsMode.MUTING
import com.example.mysequencer01ui.PadsMode.QUANTIZING
import com.example.mysequencer01ui.PadsMode.SAVING
import com.example.mysequencer01ui.PadsMode.SELECTING
import com.example.mysequencer01ui.PadsMode.SOLOING
import com.example.mysequencer01ui.SeqView.AUTOMATION
import com.example.mysequencer01ui.SeqView.LIVE
import com.example.mysequencer01ui.SeqView.PIANO
import com.example.mysequencer01ui.SeqView.SETTINGS
import com.example.mysequencer01ui.SeqView.STEP
import com.example.mysequencer01ui.ui.theme.MySequencer01UiTheme
import com.example.mysequencer01ui.ui.theme.buttonsBg
import com.example.mysequencer01ui.ui.theme.dusk
import com.example.mysequencer01ui.ui.theme.notWhite
import com.example.mysequencer01ui.ui.theme.playGreen
import com.example.mysequencer01ui.ui.theme.violet
import com.example.mysequencer01ui.ui.viewTabs.LiveView
import com.example.mysequencer01ui.ui.viewTabs.PianoView
import com.example.mysequencer01ui.ui.viewTabs.SettingsView
import com.example.mysequencer01ui.ui.viewTabs.StepView


@Composable
fun SeqScreen(kmmk: KmmkComponentContext, seqViewModel: SeqViewModel = viewModel()
) {
    val seqUiState by seqViewModel.uiState.collectAsState()
    val view = LocalView.current

    LaunchedEffect(key1 = seqUiState.fullScreen) {
        if (seqUiState.fullScreen) seqViewModel.goFullScreen(view)
    }

    if (seqUiState.seqIsPlaying || seqUiState.keepScreenOn) KeepScreenOn()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxHeight()
            .background(buttonsBg)
    ) {
        val buttonsSize = maxHeight / 5

        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // to avoid unnecessary recompositions:
            val addToPressPadList = remember {seqViewModel::addToPressPadList}
            val editCurrentPadsMode = remember {seqViewModel::editCurrentPadsMode}
            val switchPadsToQuantizingMode = remember {seqViewModel::switchPadsToQuantizingMode}
            val switchQuantization = remember {seqViewModel::switchQuantization}
            val changeRecState = remember {seqViewModel::changeRecState}
            val startSeq = remember {seqViewModel::startSeq}
            val stopSeq = remember {seqViewModel::stopSeq}
            val changeSeqViewState = remember {seqViewModel::changeSeqViewState}
            val cancelAllPadsInteraction = remember {seqViewModel::cancelAllPadsInteraction}

            // ----==== LEFT BUTTONS
            Column(
                modifier = Modifier.fillMaxHeight()
            ) {
                if(seqUiState.padsMode != DEFAULT && seqUiState.padsMode != SELECTING && seqUiState.padsMode != LOADING)
                    AllButton(
                        addToPressPadList,
                        buttonsSize,
                        (seqUiState.padsMode == SOLOING && seqUiState.soloIsOn) || (seqUiState.padsMode == MUTING && seqUiState.muteIsOn)
                    )
                else PadsModeButton(editCurrentPadsMode, seqUiState.padsMode == SELECTING, SELECTING, buttonsSize, dusk, 0)
                PadsModeButton(editCurrentPadsMode, seqUiState.padsMode == SAVING, SAVING, buttonsSize, dusk, 0)
                PadsModeButton(editCurrentPadsMode, seqUiState.padsMode == SOLOING, SOLOING, buttonsSize, violet, seqUiState.toggleTime)
                PadsModeButton(editCurrentPadsMode, seqUiState.padsMode == ERASING, ERASING, buttonsSize, notWhite, seqUiState.toggleTime)
                RecButton(changeRecState, seqUiState.padsMode == DEFAULT, seqUiState.seqIsRecording, buttonsSize, seqUiState.toggleTime)
            }
            Column(
                modifier = Modifier.fillMaxHeight()
            ) {
                QuantizeButton(switchPadsToQuantizingMode, switchQuantization, seqUiState.padsMode == QUANTIZING, buttonsSize, seqUiState.isQuantizing, seqUiState.quantizeModeTimer, seqUiState.toggleTime)
                PadsModeButton(editCurrentPadsMode, seqUiState.padsMode == LOADING, LOADING, buttonsSize, dusk, seqUiState.toggleTime)
                PadsModeButton(editCurrentPadsMode, seqUiState.padsMode == MUTING, MUTING, buttonsSize, violet, seqUiState.toggleTime)
                PadsModeButton(editCurrentPadsMode, seqUiState.padsMode == CLEARING, CLEARING, buttonsSize, notWhite, 0)
                if(!seqUiState.seqIsPlaying && seqUiState.padsMode == SELECTING)
                    StopButton(seqViewModel::stopAllNotes, buttonsSize)
                else PlayButton(startSeq, stopSeq, seqUiState.seqIsPlaying, buttonsSize, seqUiState.toggleTime)
            }

            // ----==== SeqView CONTENT
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                contentAlignment = Alignment.BottomStart
            ) {
                when(seqUiState.seqView) {
                    LIVE -> LiveView(seqViewModel, seqUiState, buttonsSize)
                    PIANO -> PianoView(seqViewModel, seqUiState, buttonsSize)
                    STEP -> StepView(seqViewModel, seqUiState, maxHeight)
                    AUTOMATION -> { Text("// TODO =)", color = playGreen, fontSize = 20.nonScaledSp, fontStyle = FontStyle.Italic, modifier = Modifier.align(Alignment.Center)) }
                    SETTINGS -> SettingsView(seqViewModel, seqUiState, buttonsSize, kmmk)
                }

                if(seqUiState.seqView != LIVE && seqUiState.padsMode != DEFAULT) {
                    Row{
                        Spacer(modifier = Modifier.width(10.dp))
                        PadsGrid(
                            channelSequences = seqViewModel.channelSequences,
                            addToPressPadList = addToPressPadList,
                            rememberInteraction = seqViewModel::rememberInteraction,
                            padsMode = seqUiState.padsMode,
                            selectedChannel = seqUiState.selectedChannel,
                            seqIsRecording = seqUiState.seqIsRecording,
                            padsSize = buttonsSize,
                            showChannelNumber = seqUiState.showChannelNumberOnPads
                        )
                    }
                }
            }

            // ----==== RIGHT SeqView TABS
            Column(
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                SeqViewButton(changeSeqViewState, cancelAllPadsInteraction, seqUiState.seqView, LIVE, buttonsSize, Int.MAX_VALUE)
                SeqViewButton(changeSeqViewState, cancelAllPadsInteraction, seqUiState.seqView, PIANO, buttonsSize, Int.MAX_VALUE)
                SeqViewButton(changeSeqViewState, cancelAllPadsInteraction, seqUiState.seqView, STEP, buttonsSize, Int.MAX_VALUE)
                SeqViewButton(changeSeqViewState, cancelAllPadsInteraction, seqUiState.seqView, AUTOMATION, buttonsSize, Int.MAX_VALUE)
                SeqViewButton(changeSeqViewState, cancelAllPadsInteraction, seqUiState.seqView, SETTINGS, buttonsSize, Int.MAX_VALUE)
            }
        }
    }

    Log.d("emptyTag", " ") // to hold in imports
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MySequencer01UiTheme(darkTheme = true) {
        SeqScreen(KmmkComponentContext())
    }
}