package com.example.mysequencer01ui.ui


import android.util.Log
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
import kotlin.math.sqrt


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
        val c = buttonsSize.value / sqrt(3f)
        val y = buttonsSize.value / 2
        val columnsOffset = -sqrt(c * c - y * y)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(seqBg)
        ) {
            //KeyboardRow(kmmk, 1)
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxHeight()
            ) {
                ShiftButton(seqViewModel, seqUiState.padsMode, buttonsSize, "*", dusk, 20) // ë°╪
                SaveButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                SoloButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                EraseButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                RecButton(seqViewModel, seqUiState.padsMode, seqUiState.seqIsRecording, buttonsSize)
            }
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 16.dp)
            ) {
                QuantizeButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                LoadButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                MuteButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                ClearButton(seqViewModel, seqUiState.padsMode, buttonsSize)
                PlayButton(seqViewModel, seqUiState.seqIsPlaying, buttonsSize)
            }
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                StepSequencer(seqUiState, buttonsSize)
                PadsGrid(seqViewModel, seqUiState)
            }
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                VisualArray(seqUiState, buttonsSize)
                Spacer(modifier = Modifier.height(1.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ){
                    AllButton(seqViewModel, buttonsSize)
                    Text(
                        text = "${seqUiState.bpm} BPM",
                        color = Color.Gray,
                        modifier = Modifier
                            .padding(10.dp)
                            .clickable { seqViewModel.showSettings() }
                    )
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(columnsOffset.dp)){
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    RepeatButton(seqViewModel, buttonsSize, 2, seqUiState.divisorState)
                    RepeatButton(seqViewModel, buttonsSize, 4, seqUiState.divisorState)
                    RepeatButton(seqViewModel, buttonsSize, 8, seqUiState.divisorState)
                    RepeatButton(seqViewModel, buttonsSize, 16, seqUiState.divisorState)
                    RepeatButton(seqViewModel, buttonsSize, 32, seqUiState.divisorState)
                }
                Column(){
                    Spacer(modifier = Modifier.height(buttonsSize / 2))
                    RepeatButton(seqViewModel, buttonsSize, 3, seqUiState.divisorState, true)
                    RepeatButton(seqViewModel, buttonsSize, 6, seqUiState.divisorState, true)
                    RepeatButton(seqViewModel, buttonsSize, 12, seqUiState.divisorState, true)
                    RepeatButton(seqViewModel, buttonsSize, 24, seqUiState.divisorState, true)
                }
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    SeqView(seqViewModel, seqUiState.padsMode, buttonsSize, "ϴ")
                    SeqView(seqViewModel, seqUiState.padsMode, buttonsSize, "ʭ")
                    SeqView(seqViewModel, seqUiState.padsMode, buttonsSize, "ϡ")
                    SeqView(seqViewModel, seqUiState.padsMode, buttonsSize, "֎")
                    SeqView(seqViewModel, seqUiState.padsMode, buttonsSize, "╪")
                }
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