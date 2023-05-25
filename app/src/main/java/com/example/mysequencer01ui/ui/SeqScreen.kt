package com.example.mysequencer01ui.ui


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mysequencer01ui.KmmkComponentContext
import com.example.mysequencer01ui.ui.theme.MySequencer01UiTheme


@Composable
fun SeqScreen(kmmk: KmmkComponentContext, seqViewModel: SeqViewModel = viewModel()
) {
    val seqUiState by seqViewModel.uiState.collectAsState()
    KeepScreenOn()

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF161616))
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ){
            MidiSelector(kmmk)

            Text("${seqUiState.bpm} BPM", color = Color.Gray)

            Canvas(modifier = Modifier.size(40.dp)){
                drawCircle(Color.White, style = Stroke(
                width = 3f,
                    pathEffect = PathEffect.dashPathEffect(FloatArray(2){((1 - it) + 0.44f) * 20f}, 0f)
                )
                )
                //drawOval(Color.White, Offset(0f, 0f), Offset(30f, 20f), )
            }
                Spacer(modifier = Modifier.width(1.dp))
        }

        //KeyboardRow(kmmk, 1)

        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally){

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()){
                EraseButton(seqViewModel, seqUiState.eraseButtonState)

                    Spacer(modifier = Modifier.width(30.dp))

                MuteButton(seqViewModel, seqUiState.muteButtonState)

                    Spacer(modifier = Modifier.width(30.dp))

                ClearButton(seqViewModel, seqUiState.clearButtonState)
            }

                    Spacer(modifier = Modifier.height(30.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()){
                RecButton(seqViewModel, seqUiState.seqIsRecording)

                    Spacer(modifier = Modifier.width(30.dp))

                PlayButton(seqViewModel, seqUiState.seqIsPlaying)

                    Spacer(modifier = Modifier.width(30.dp))

                StopButton(seqViewModel, kmmk)
            }

                    Spacer(modifier = Modifier.height(40.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()){
                VisualArray(seqUiState)

                AllButton(seqViewModel)
            }

                    Spacer(modifier = Modifier.height(40.dp))

            PadsGrid(seqViewModel, seqUiState.channelIsActive, seqUiState.seqMode, seqUiState.seqIsPlaying, seqUiState.seqIsRecording)
        }
    }
}






@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MySequencer01UiTheme(darkTheme = true) {
        SeqScreen(KmmkComponentContext())
    }
}