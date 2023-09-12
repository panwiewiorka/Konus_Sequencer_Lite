package com.example.mysequencer01ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import dev.atsushieno.ktmidi.ci.CIFactory
import dev.atsushieno.ktmidi.ci.MidiCIProtocolTypeInfo
import dev.atsushieno.ktmidi.*
import kotlinx.coroutines.Runnable

class KmmkComponentContext {
    // states

    // In this application, we record the *number of* note-ons for each key, instead of an on-off state flag
    // so that it can technically send more than one note on operations on the same key.
    var noteOnStates = SnapshotStateList<Int>().also { it.addAll(List(128) { 0 }) }

    var shouldRecordMml = mutableStateOf(false)

    var useDrumChannel = mutableStateOf(false)

    var shouldOutputNoteLength = mutableStateOf(false)
    var currentTempo = mutableStateOf(120.0)

    var midiProtocol = mutableStateOf(MidiCIProtocolType.MIDI1)

    val compilationDiagnostics = mutableStateListOf<String>()
    val midiPlayers = mutableListOf<MidiPlayer>()

    // FIXME: once we sort out which development model to take, take it out from "model".
    var program = mutableStateOf(0)

    var midiOutputPorts = mutableStateListOf<MidiPortDetails>()

    // ryjtyj
    var midiInputPorts = mutableStateListOf<MidiPortDetails>()

    // non-states

    val noteNames = arrayOf("c", "c+", "d", "d+", "e", "f", "f+", "g", "g+", "a", "a+", "b")

    val midiDeviceManager = MidiDeviceManager()

    var defaultVelocity: Byte = 100

    private fun sendToAll(bytes: ByteArray, timestamp: Long) {
        midiDeviceManager.sendToAll(bytes, timestamp)
    }

//    private val targetChannel: Int
//        get() = if (useDrumChannel.value) 9 else 0

    private fun calculateLength(msec: Double): String {
        if (!shouldOutputNoteLength.value)
            return ""
        // 500 = full note at BPM 120. Minimum by 1/8.
        val n8th = (currentTempo.value / 120.0 * msec / (500.0 / 8)).toInt()
        val longPart = "1^".repeat(n8th / 8)
        val remaining = arrayOf("0", "8", "4", "4.", "2", "2^8", "2.", "2..")
        return longPart + remaining[n8th % 8]
    }

    fun noteOn(channel: Int, key: Int, velocity: Int = 100) {
        if (key < 0 || key >= 128) // invalid operation
            return

//        val existingPlayingNotes = noteOnStates.any { it > 0 }

        noteOnStates[key]++

        val nOn = byteArrayOf(
                (MidiChannelStatus.NOTE_ON + channel).toByte(),
                key.toByte(),
                velocity.toByte()
            )
        sendToAll(nOn, 0)
    }


    fun noteOff(key: Int, channel: Int) {
        if (key < 0 || key >= 128 || noteOnStates[key] == 0) // invalid operation
            return
        noteOnStates[key]--


            val nOff = byteArrayOf(
                (MidiChannelStatus.NOTE_OFF + channel).toByte(),
                key.toByte(),
                0
            )
        sendToAll(nOff, 0)
    }

    fun allNotesOff(
//        channel: Int
    ) {
        val allOff = byteArrayOf(
            (MidiChannelStatus.CC + 0).toByte(),
            MidiCC.ALL_NOTES_OFF.toByte(),
            0
        )
        sendToAll(allOff, 0)
//        noteOn(0, 4,0)
    }

    fun startClock() {
        sendToAll(byteArrayOf(MidiSystemStatus.START.toByte()), 0)
    }

    fun stopClock() {
        sendToAll(byteArrayOf(MidiSystemStatus.STOP.toByte()), 0)
    }

    fun sendTimingClock() {
        sendToAll(byteArrayOf(MidiSystemStatus.TIMING_CLOCK.toByte()), 0)
    }

    fun  sendProgramChange(programToChange: Int, channel: Int = 0) {
        this.program.value = programToChange
        if (midiProtocol.value == MidiCIProtocolType.MIDI2) {
            val nOff = Ump(
                UmpFactory.midi2Program(
                    0,
                    channel,
                    0,
                    programToChange,
                    0,
                    0
                )
            ).toPlatformNativeBytes()
            sendToAll(nOff, 0)
        } else {
            val bytes = byteArrayOf(
                (MidiChannelStatus.PROGRAM + channel).toByte(),
                programToChange.toByte()
            )
            sendToAll(bytes, 0)
        }
    }

    fun registerMusic1(music: MidiMusic, playOnInput: Boolean) {
        val output =
            (if (playOnInput) midiDeviceManager.virtualMidiOutput else midiDeviceManager.midiOutput)
                ?: return
        val player = Midi1Player(music, output)
        midiPlayers.add(player)
        player.finished = Runnable { midiPlayers.remove(player) }
        player.play()
    }

    fun registerMusic2(music: Midi2Music, playOnInput: Boolean) {
        val output =
            (if (playOnInput) midiDeviceManager.virtualMidiOutput else midiDeviceManager.midiOutput)
                ?: return
        val player = Midi2Player(music, output)
        midiPlayers.add(player)
        player.finished = Runnable { midiPlayers.remove(player) }
        player.play()
    }

    var octaveShift = mutableStateOf(2)
    var noteShift = mutableStateOf(0)

    class KeyTonalitySettings(val name: String, val notesPerKey: Array<Array<Int>>)

    val tonalities = arrayOf(
        KeyTonalitySettings(
            "Diatonic", arrayOf(
                arrayOf(
                    44,
                    46,
                    Int.MIN_VALUE,
                    49,
                    51,
                    Int.MIN_VALUE,
                    54,
                    56,
                    58,
                    Int.MIN_VALUE,
                    61,
                    63,
                    Int.MIN_VALUE,
                    66
                ),
                arrayOf(45, 47, 48, 50, 52, 53, 55, 57, 59, 60, 62, 64, 65, 67),
                arrayOf(
                    32,
                    34,
                    Int.MIN_VALUE,
                    37,
                    39,
                    Int.MIN_VALUE,
                    42,
                    44,
                    46,
                    Int.MIN_VALUE,
                    49,
                    51
                ),
                arrayOf(33, 35, 36, 38, 40, 41, 43, 45, 47, 48, 50, 52, 53)
            )
        ), KeyTonalitySettings(
            "Chromatic", arrayOf(
                arrayOf(44, 46, 48, 50, 52, 54, 56, 58, 60, 62, 64, 66, 68),
                arrayOf(45, 47, 49, 51, 53, 55, 57, 59, 61, 63, 65, 67, 69),
                arrayOf(32, 34, 36, 38, 40, 42, 44, 46, 48, 50, 52, 54, 56),
                arrayOf(33, 35, 37, 39, 41, 43, 45, 47, 49, 51, 53, 55, 57)
            )
        )
    )

    class KeyboardConfiguration(val name: String, val keys: Array<String>)

    val keyboards = arrayOf(
        KeyboardConfiguration(
            "ASCII Qwerty",
            arrayOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm")
        ),
        KeyboardConfiguration(
            "US101",
            arrayOf("1234567890_=", "qwertyuiop[]", "asdfghjkl;']", "zxcvbnm,./")
        ),
        KeyboardConfiguration(
            "JP106",
            arrayOf("1234567890-^\\", "qwertyuiop@[", "asdfghjkl;:]", "zxcvbnm,./")
        )
    )

    var selectedKeyboard = mutableStateOf(0)
    var selectedTonality = mutableStateOf(0)

    fun setKeyboard(index: Int) {
        selectedKeyboard.value = index
    }

    fun setTonality(index: Int) {
        selectedTonality.value = index
    }

    fun getNoteFromKeyCode(utf16CodePoint: Int): Int {
        val ch = utf16CodePoint.toChar()
        keyboards[selectedKeyboard.value].keys.forEachIndexed { indexOfLines, line ->
            val idx = line.indexOf(ch)
            val notesPerKey = tonalities[selectedTonality.value].notesPerKey
            if (idx >= 0 && idx < notesPerKey[indexOfLines].size)
                return notesPerKey[indexOfLines][idx] + octaveShift.value * 12 + noteShift.value
        }
        return -1
    }

    fun setOutputDevice(id: String) {
        midiDeviceManager.midiOutputDeviceId = id
    }

    // ryjtyj
    fun setInputDevice(id: String) {
        midiDeviceManager.midiInputDeviceId = id
    }

    fun onMidiProtocolUpdated() {
        midiProtocol.value =
            if (midiProtocol.value == MidiCIProtocolType.MIDI2) MidiCIProtocolType.MIDI1 else MidiCIProtocolType.MIDI2

        // Generate a MIDI CI Set New Protocol Message...
        val bytes = MutableList<Byte>(19) { 0 }
        val protocolValue: Byte = if (midiProtocol.value == MidiCIProtocolType.MIDI2) 2 else 1
        CIFactory.midiCIProtocolSet(
            bytes, 0, 0, 0,
            MidiCIProtocolTypeInfo(protocolValue, 0, 0, 0, 0)
        )
        bytes.add(0, 0xF0.toByte())
        bytes.add(0xF7.toByte())

        // ...and send it...
        if (protocolValue * 1 == 2) {
            // ... in MIDI1 sysex
            sendToAll(bytes.toByteArray(), 0)
        } else {
            // ... in MIDI2 UMP.
            val umpInBytes = mutableListOf<Byte>()
            UmpFactory.sysex7Process(0, bytes, { p, _ ->
                umpInBytes.addAll(Ump(p).toPlatformNativeBytes().toTypedArray())
            }, null)
            sendToAll(umpInBytes.toByteArray(), 0)
        }
        // S6.6 "After the Initiator sends this Set New Protocol message, it shall switch its
        // own Protocol while also waiting 100ms to allow the Responder to switch Protocol."
        // FIXME: some synchronized wait, or make this function suspend
        /*GlobalScope.runBlocking {
        delay(100)
    }*/
    }

    fun updateMidiDeviceList() {
        midiOutputPorts.clear()
        midiOutputPorts.addAll(midiDeviceManager.midiOutputPorts)
    }

    // ryjtyj
    fun updateMidiInputDeviceList() {
        midiInputPorts.clear()
        midiInputPorts.addAll(midiDeviceManager.midiInputPorts)
    }
}