package com.example.mysequencer01ui.ui.viewTabs

import android.widget.ToggleButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.KmmkComponentContext
import com.example.mysequencer01ui.ui.MidiSelector
import com.example.mysequencer01ui.ui.SeqUiState
import com.example.mysequencer01ui.ui.SeqViewModel
import com.example.mysequencer01ui.ui.theme.notWhite

@Composable
fun SettingsView(seqViewModel: SeqViewModel, seqUiState: SeqUiState, buttonsSize: Dp, kmmk: KmmkComponentContext) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            MidiSelector(kmmk)

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier  = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${seqUiState.bpm} BPM",
                    color = Color.Gray,
                    modifier = Modifier.padding(end = 10.dp)
                )

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

            TextAndSwitch("Clock transmit", seqUiState.transmitClock) { seqViewModel.switchClockTransmitting() }

            TextAndSwitch("LazyKeyboard", seqUiState.lazyKeyboard) { seqViewModel.switchLazyKeyboard() }
        }
    }
}

@Composable
fun TextAndSwitch(text: String, switchState: Boolean, toDo: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = notWhite, modifier = Modifier.padding(end = 10.dp))
        Switch(checked = switchState, onCheckedChange = {toDo()})
    }
}