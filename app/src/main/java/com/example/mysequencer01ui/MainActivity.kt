package com.example.mysequencer01ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import com.example.mysequencer01ui.ui.theme.MySequencer01UiTheme
import com.example.mysequencer01ui.ui.SeqScreen
import com.example.mysequencer01ui.ui.SeqViewModel
import dev.atsushieno.ktmidi.AndroidMidiAccess

class MainActivity : ComponentActivity() {

//    private val db by lazy{
//        Room.databaseBuilder(
//            applicationContext,
//            SettingsDatabase::class.java,
//            name = "settings.db"
//        ).build()
//    }
//    private val viewModel by viewModels<SeqViewModel>(
//        factoryProducer = {
//            object: ViewModelProvider.Factory {
//                override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                    return SeqViewModel(
//                        kmmk = KmmkComponentContext()
//                    ) as T
//                }
//            }
//        }
//    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val kmmk = KmmkComponentContext()
            kmmk.midiDeviceManager.midiAccess = AndroidMidiAccess(applicationContext)
            MySequencer01UiTheme(darkTheme = true) {
                SeqScreen(kmmk, SeqViewModel(kmmk))
            }
        }
    }
}
