package com.example.mysequencer01ui.ui.viewTabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.ui.PadsGrid
import com.example.mysequencer01ui.ui.RepeatButton
import com.example.mysequencer01ui.ui.SeqUiState
import com.example.mysequencer01ui.ui.SeqViewModel
import com.example.mysequencer01ui.ui.StepSequencer
import kotlin.math.sqrt

@Composable
fun LiveView(seqViewModel: SeqViewModel, seqUiState: SeqUiState, buttonsSize: Dp) {
    val spacerSize = 0.dp
    val c = buttonsSize.value / sqrt(3f)
    val y = buttonsSize.value / 2
    val columnsOffset = -sqrt(c * c - y * y)

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        //KeyboardRow(kmmk, 1)
        Spacer(modifier = Modifier.width(spacerSize))
        Column(modifier = Modifier.weight(1f)) {
            StepSequencer(seqUiState, buttonsSize)
            Row {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    //VisualArray(seqUiState, buttonsSize)
                    Spacer(modifier = Modifier.weight(1f))
                    PadsGrid(seqViewModel, seqUiState, buttonsSize * 1.5f)
                }
            }
        }
        Spacer(modifier = Modifier.width(spacerSize))
        Row(horizontalArrangement = Arrangement.spacedBy(columnsOffset.dp)){
            Column(
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
        }
    }
}