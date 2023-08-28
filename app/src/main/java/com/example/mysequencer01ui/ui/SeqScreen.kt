package com.example.mysequencer01ui.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mysequencer01ui.KmmkComponentContext
import com.example.mysequencer01ui.PadsMode.*
import com.example.mysequencer01ui.SeqView.*
import com.example.mysequencer01ui.ui.theme.*
import com.example.mysequencer01ui.ui.viewTabs.LiveView
import com.example.mysequencer01ui.ui.viewTabs.PianoView
import com.example.mysequencer01ui.ui.viewTabs.SettingsView
import com.example.mysequencer01ui.ui.viewTabs.StepView


@Composable
fun SeqScreen(kmmk: KmmkComponentContext, seqViewModel: SeqViewModel = viewModel()
) {
    val seqUiState by seqViewModel.uiState.collectAsState()

    if (seqUiState.seqIsPlaying) KeepScreenOn()

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
            val pressPad = remember {seqViewModel::pressPad}
            val updateSequencesUiState = remember {seqViewModel::updateSequencesUiState}
            val editCurrentPadsMode = remember {seqViewModel::editCurrentPadsMode}
            val switchPadsToQuantizingMode = remember {seqViewModel::switchPadsToQuantizingMode}
            val switchQuantization = remember {seqViewModel::switchQuantization}
            val changeRecState = remember {seqViewModel::changeRecState}
            val startSeq = remember {seqViewModel::startSeq}
            val stopSeq = remember {seqViewModel::stopSeq}
            val changeSeqViewState = remember {seqViewModel::changeSeqViewState}
            val cancelPadInteraction = remember {seqViewModel::cancelPadInteraction}

            // ----==== LEFT BUTTONS
            Column(
                modifier = Modifier.fillMaxHeight()
            ) {
                if(seqUiState.padsMode != DEFAULT && seqUiState.padsMode != SELECTING && seqUiState.padsMode != LOADING)
                    AllButton(pressPad, updateSequencesUiState, buttonsSize,
                        (seqUiState.padsMode == SOLOING && seqUiState.soloIsOn) || (seqUiState.padsMode == MUTING && seqUiState.muteIsOn))
                else PadsModeButton(editCurrentPadsMode, seqUiState.padsMode == SELECTING, SELECTING, buttonsSize, dusk, 0)
                PadsModeButton(editCurrentPadsMode, seqUiState.padsMode == SAVING, SAVING, buttonsSize, dusk, 0)
                PadsModeButton(editCurrentPadsMode, seqUiState.padsMode == SOLOING, SOLOING, buttonsSize, violet, seqViewModel.toggleTime)
                PadsModeButton(editCurrentPadsMode, seqUiState.padsMode == ERASING, ERASING, buttonsSize, notWhite, seqViewModel.toggleTime)
                RecButton(changeRecState, seqUiState.padsMode == DEFAULT, seqUiState.seqIsRecording, buttonsSize, seqViewModel.toggleTime)
            }
            Column(
                modifier = Modifier.fillMaxHeight()
            ) {
                QuantizeButton(switchPadsToQuantizingMode, switchQuantization, seqUiState.padsMode == QUANTIZING, buttonsSize, seqUiState.isQuantizing, seqUiState.quantizeModeTimer, seqViewModel.toggleTime)
                PadsModeButton(editCurrentPadsMode, seqUiState.padsMode == LOADING, LOADING, buttonsSize, dusk, seqViewModel.toggleTime)
                PadsModeButton(editCurrentPadsMode, seqUiState.padsMode == MUTING, MUTING, buttonsSize, violet, seqViewModel.toggleTime)
                PadsModeButton(editCurrentPadsMode, seqUiState.padsMode == CLEARING, CLEARING, buttonsSize, notWhite, 0)
                if(!seqUiState.seqIsPlaying && seqUiState.padsMode == SELECTING)
                    StopButton(seqViewModel::stopAllNotes, buttonsSize)
                else PlayButton(startSeq, stopSeq, seqUiState.seqIsPlaying, buttonsSize, seqViewModel.toggleTime)
            }

            // ----==== SeqView CONTENT
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                contentAlignment = Alignment.BottomStart
            ) {
                when(seqUiState.seqView) {
                    LIVE -> LiveView(seqViewModel, seqUiState, buttonsSize)
                    PIANO -> PianoView(seqViewModel, seqUiState, buttonsSize)
                    STEP -> StepView(seqViewModel, seqUiState, buttonsSize)
                    AUTOMATION -> { Text("// TODO =)", color = playGreen, fontSize = 20.sp, fontStyle = FontStyle.Italic, modifier = Modifier.align(Alignment.Center)) }
                    SETTINGS -> SettingsView(seqViewModel, seqUiState, buttonsSize, kmmk)
                }

                if(seqUiState.seqView != LIVE && seqUiState.padsMode != DEFAULT) {
                    Row{
                        Box(modifier = Modifier.width(10.dp).background(buttonsBg))
                        PadsGrid(seqViewModel = seqViewModel, seqUiState = seqUiState, padsSize = buttonsSize)
                    }
                }
            }

            // ----==== RIGHT SeqView TABS
            Column(
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                SeqViewButton(changeSeqViewState, cancelPadInteraction, seqUiState.seqView == LIVE, LIVE, buttonsSize, "ϴ", seqViewModel.toggleTime)
                SeqViewButton(changeSeqViewState, cancelPadInteraction, seqUiState.seqView == PIANO, PIANO, buttonsSize, "ϡ", seqViewModel.toggleTime)
                SeqViewButton(changeSeqViewState, cancelPadInteraction, seqUiState.seqView == STEP, STEP, buttonsSize, "ʭ", seqViewModel.toggleTime)
                SeqViewButton(changeSeqViewState, cancelPadInteraction, seqUiState.seqView == AUTOMATION, AUTOMATION, buttonsSize, "֎", seqViewModel.toggleTime)
                SeqViewButton(changeSeqViewState, cancelPadInteraction, seqUiState.seqView == SETTINGS, SETTINGS, buttonsSize, "╪", seqViewModel.toggleTime)
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