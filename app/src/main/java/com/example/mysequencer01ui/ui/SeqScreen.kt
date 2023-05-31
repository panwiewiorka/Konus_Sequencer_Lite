package com.example.mysequencer01ui.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mysequencer01ui.KmmkComponentContext
import com.example.mysequencer01ui.ui.theme.*


@Composable
fun SeqScreen(kmmk: KmmkComponentContext, seqViewModel: SeqViewModel = viewModel()
) {
    val seqUiState by seqViewModel.uiState.collectAsState()
    KeepScreenOn()
    
    if(seqUiState.showSettings) {
        AlertDialog(
            onDismissRequest = { seqViewModel.showSettings() }, 
            buttons = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    MidiSelector(kmmk)

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier  =Modifier.fillMaxWidth()
                    ) {
                        Text("${seqUiState.bpm} BPM", Modifier.width(60.dp))
                        Spacer(modifier = Modifier.width(20.dp))


                        var sizeSliderPosition by remember { mutableStateOf(120f) }
                        Slider(
                            value = sizeSliderPosition,
                            onValueChange = {
                                sizeSliderPosition = it
                                seqViewModel.changeBPM(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            valueRange = 40f..280f,
                            onValueChangeFinished = {  }
                        )
                    }
                }
            },
            modifier = Modifier
                .width(320.dp)
//                .padding(20.dp)
            ,
            shape = CutCornerShape(14.dp)
        )
    }

    BoxWithConstraints(Modifier.fillMaxHeight()) {
        val buttonsSize = maxHeight / 5
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
        ) {
            //KeyboardRow(kmmk, 1)
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxHeight()
            ) {
                EmptyButton(seqViewModel, seqUiState.padsMode, buttonsSize, "*", dusk, 20) // ë°╪
                EmptyButton(seqViewModel, seqUiState.padsMode, buttonsSize, "SAVE", dusk)
                EmptyButton(seqViewModel, seqUiState.padsMode, buttonsSize, "SOLO", violet)
                EraseButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                RecButton(seqViewModel, seqUiState.padsMode, seqUiState.seqIsRecording, buttonsSize)
            }
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 16.dp)
            ) {
                AllButton(seqViewModel, buttonsSize)
                EmptyButton(seqViewModel, seqUiState.padsMode, buttonsSize, "LOAD", dusk)
                MuteButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                ClearButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                PlayButton(seqViewModel, seqUiState.seqIsPlaying, buttonsSize)
            }
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                StepSeq(seqUiState, buttonsSize)
                PadsGrid(seqViewModel, seqUiState)
            }
            Spacer(modifier = Modifier.weight(1f))

            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxHeight()
            ) {
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
                VisualArray(seqUiState)
                Text(
                    text = "${seqUiState.bpm} BPM",
                    color = Color.Gray,
                    modifier = Modifier
                        .padding(10.dp)
                        .clickable { seqViewModel.showSettings() }
                )
                Spacer(modifier = Modifier.weight(1f))
                RepeatButton(seqViewModel, buttonsSize, "1/3", 600) // TODO change
                RepeatButton(seqViewModel, buttonsSize, "1/4", 600)
                RepeatButton(seqViewModel, buttonsSize, "1/8", 600)
            }
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