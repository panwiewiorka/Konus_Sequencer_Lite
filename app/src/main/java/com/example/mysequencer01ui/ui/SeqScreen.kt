package com.example.mysequencer01ui.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
    KeepScreenOn()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxHeight()
            .background(buttonsBg)
    ) {
        val buttonsSize = maxHeight / 5

        Row(
            modifier = Modifier.fillMaxSize()
        ) {

            // ----==== LEFT BUTTONS
            Column(
                modifier = Modifier.fillMaxHeight()
            ) {
                if(seqUiState.padsMode != DEFAULT && seqUiState.padsMode != SELECTING && seqUiState.padsMode != LOADING)
                    AllButton(seqViewModel, buttonsSize)
                else PadsModeButton(seqViewModel, seqUiState.padsMode, SELECTING, buttonsSize, dusk, 0)
                PadsModeButton(seqViewModel, seqUiState.padsMode, SAVING, buttonsSize, dusk, 0)
                PadsModeButton(seqViewModel, seqUiState.padsMode, SOLOING, buttonsSize, violet, seqViewModel.toggleTime)
                PadsModeButton(seqViewModel, seqUiState.padsMode, ERASING, buttonsSize, notWhite, seqViewModel.toggleTime)
                RecButton(seqViewModel, seqUiState.padsMode, seqUiState.seqIsRecording, buttonsSize)
            }
            Column(
                modifier = Modifier.fillMaxHeight()
            ) {
                QuantizeButton(seqViewModel, seqUiState.padsMode, buttonsSize, seqUiState.isQuantizing, seqUiState.quantizeModeTimer)
                PadsModeButton(seqViewModel, seqUiState.padsMode, LOADING, buttonsSize, dusk, 0)
                PadsModeButton(seqViewModel, seqUiState.padsMode, MUTING, buttonsSize, violet, seqViewModel.toggleTime)
                PadsModeButton(seqViewModel, seqUiState.padsMode, CLEARING, buttonsSize, notWhite, 0)
                if(!seqUiState.seqIsPlaying && seqUiState.padsMode == SELECTING)
                    StopButton(seqViewModel, buttonsSize)
                else PlayButton(seqViewModel, seqUiState.seqIsPlaying, buttonsSize)
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
                    STEP -> StepView(seqViewModel, seqUiState, buttonsSize, kmmk)
                    PIANO -> PianoView(seqViewModel, seqUiState, buttonsSize)
                    AUTOMATION -> { }
                    SETTINGS -> SettingsView(seqViewModel, seqUiState, buttonsSize, kmmk)
                }

                if(seqUiState.seqView != LIVE && seqUiState.padsMode != DEFAULT) {
                    PadsGrid(seqViewModel = seqViewModel, seqUiState = seqUiState, padsSize = buttonsSize)
                }
            }

            // ----==== RIGHT SeqView TABS
            Column(
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                SeqViewButton(seqViewModel, seqViewModel::cancelInteractionWhenSwitchingViews, seqUiState.seqView, LIVE, buttonsSize, "ϴ")
                SeqViewButton(seqViewModel, seqViewModel::cancelInteractionWhenSwitchingViews, seqUiState.seqView, STEP, buttonsSize, "ʭ")
                SeqViewButton(seqViewModel, seqViewModel::cancelInteractionWhenSwitchingViews, seqUiState.seqView, PIANO, buttonsSize, "ϡ")
                SeqViewButton(seqViewModel, seqViewModel::cancelInteractionWhenSwitchingViews, seqUiState.seqView, AUTOMATION, buttonsSize, "֎")
                SeqViewButton(seqViewModel, seqViewModel::cancelInteractionWhenSwitchingViews, seqUiState.seqView, SETTINGS, buttonsSize, "╪")
            }
        }
    }
    

    /*
Canvas(modifier = Modifier.size(40.dp)){
drawCircle(Color.White, style = Stroke(
width = 3f,
    pathEffect = PathEffect.dashPathEffect(FloatArray(2){((1 - it) + 0.44f) * 20f}, 0f)
)
)
//drawOval(Color.White, Offset(0f, 0f), Offset(30f, 20f), )
}
*/

    Log.d("emptyTag", " ") // to hold in imports
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MySequencer01UiTheme(darkTheme = true) {
        SeqScreen(KmmkComponentContext())
    }
}