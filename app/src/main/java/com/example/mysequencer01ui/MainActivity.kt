package com.example.mysequencer01ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.room.Room
import com.example.mysequencer01ui.data.SeqDao
import com.example.mysequencer01ui.data.SeqDatabase
import com.example.mysequencer01ui.ui.theme.MySequencer01UiTheme
import com.example.mysequencer01ui.ui.SeqScreen
import com.example.mysequencer01ui.ui.SeqViewModel
import dev.atsushieno.ktmidi.AndroidMidiAccess

class MainActivity : ComponentActivity() {

    private val db by lazy{
        Room.databaseBuilder(
            applicationContext,
            SeqDatabase::class.java,
            name = "seq.db"
        ).build()
    }
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
//            val dao: SeqDao
            val kmmk = KmmkComponentContext()
            kmmk.midiDeviceManager.midiAccess = AndroidMidiAccess(applicationContext)
//            kmmk.updateMidiDeviceList()
//            if (kmmk.midiOutputPorts.any()) {
//                Log.d("ryjtyj", "MIDI output = ${kmmk.midiOutputPorts[0].id}")
//                kmmk.setOutputDevice(kmmk.midiOutputPorts[0].id)
//            }
            MySequencer01UiTheme(darkTheme = true) {
                SeqScreen(kmmk, SeqViewModel(kmmk, db.dao))
            }
        }
        hideSystemUI()
    }

//    override fun onResume() {
//        super.onResume()
//        val kmmk = KmmkComponentContext()
//        kmmk.midiDeviceManager.midiAccess = AndroidMidiAccess(applicationContext)
//        kmmk.updateMidiDeviceList()
//    }


    private fun hideSystemUI() {
        //Hides action bar at the top
        actionBar?.hide()

        //Hide status bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.insetsController?.apply {
                hide(WindowInsets.Type.systemBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}