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

    BoxWithConstraints(Modifier.fillMaxHeight()) {
        val buttonsSize = maxHeight / 5

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(buttonsBg)
        ) {

            // ----==== LEFT BUTTONS
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(buttonsBg)
            ) {
//                SymbolButton(seqViewModel, buttonsSize, seqUiState.padsMode, SELECTING, dusk)
                if(seqUiState.padsMode != DEFAULT && seqUiState.padsMode != SELECTING && seqUiState.padsMode != LOADING)
                    AllButton(seqViewModel, buttonsSize)
                else ShiftButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                SaveButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                SoloButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                EraseButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                RecButton(seqViewModel, seqUiState.padsMode, seqUiState.seqIsRecording, buttonsSize)
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(buttonsBg)
            ) {
                QuantizeButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                LoadButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                MuteButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                ClearButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                PlayButton(seqViewModel, seqUiState.seqIsPlaying, buttonsSize)
            }

            // ----==== SeqView CONTENT
            Box(
                modifier = Modifier.fillMaxHeight().weight(1f),
                contentAlignment = Alignment.BottomStart
            ) {
                when(seqUiState.seqView) {
                    LIVE -> LiveView(seqViewModel, seqUiState, buttonsSize)
                    STEP -> StepView(seqViewModel, seqUiState, buttonsSize)
                    PIANO -> PianoView(seqViewModel, seqUiState, buttonsSize)
                    AUTOMATION -> { }
                    SETTINGS -> SettingsView(seqViewModel, seqUiState, buttonsSize, kmmk)
                }

                if(seqUiState.seqView != LIVE && seqUiState.padsMode != DEFAULT) {
                    PadsGrid(seqViewModel = seqViewModel, seqUiState = seqUiState, padsSize = buttonsSize * 1.5f)
                }
            }

            // ----==== MAIN TABS
            Column(
                //verticalArrangement = Arrangement.SpaceBetween,
                //horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .fillMaxHeight()
                    .background(buttonsBg)
            ) {
                SeqViewButton(seqViewModel, seqUiState.seqView, LIVE, buttonsSize, "ϴ")
                SeqViewButton(seqViewModel, seqUiState.seqView, STEP, buttonsSize, "ʭ")
                SeqViewButton(seqViewModel, seqUiState.seqView, PIANO, buttonsSize, "ϡ")
                SeqViewButton(seqViewModel, seqUiState.seqView, AUTOMATION, buttonsSize, "֎")
                SeqViewButton(seqViewModel, seqUiState.seqView, SETTINGS, buttonsSize, "╪")
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