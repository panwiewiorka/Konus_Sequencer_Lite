package com.example.mysequencer01ui.ui

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.SeqMode
import com.example.mysequencer01ui.ui.theme.BackGray

fun DrawScope.recSymbol(seqMode: SeqMode, seqIsRecording: Boolean) {
    drawCircle(
        color = if (seqIsRecording && seqMode == SeqMode.DEFAULT) BackGray else Color.Red,
        radius = size.height / 6,
        center = center,
        style = if(seqMode != SeqMode.DEFAULT && seqIsRecording) Fill else Stroke(width = 3.dp.toPx()),
    )
}

fun DrawScope.playSymbol(seqIsPlaying: Boolean) {
    val path = Path()
    val third = size.height / 3
    val twoThirds = third + third
    path.moveTo(third, third)
    path.lineTo(third, twoThirds)
    path.lineTo(twoThirds, size.height / 2f)
    path.lineTo(third, third)
    path.lineTo(third, twoThirds)

    drawPath(
        path = path,
        color = if (seqIsPlaying) BackGray else Color(0xFF00AA00),
        style = Stroke(
            width = 3.dp.toPx(),
            join = StrokeJoin.Round
        )
    )
}

//fun DrawScope.stopSymbol(stopIsPressed: Boolean) {
//    drawRect(
//        topLeft = Offset(26.dp.toPx(), 26.dp.toPx()),
//        size = Size(28.dp.toPx(), 28.dp.toPx()),
//        color = if (stopIsPressed) BackGray else Color(0xFFBFBF00),
//        style = Stroke(
//            width = 3.dp.toPx(),
//            join = StrokeJoin.Round
//        )
//    )
//}
fun DrawScope.stopSymbol() {
    val third = size.height / 3
    drawRect(
        topLeft = Offset(third, third),
        size = Size(third, third),
        color = if (true) BackGray else Color(0xFFBFBF00),
        style = Stroke(
            width = 3.dp.toPx(),
            join = StrokeJoin.Round
        )
    )
}

fun DrawScope.eraseSymbol(seqMode: SeqMode) {
    val path = Path()
    path.moveTo(28.dp.toPx(), 36.dp.toPx())
    path.lineTo(36.dp.toPx(), 44.dp.toPx())
    path.moveTo(36.dp.toPx(), 36.dp.toPx())
    path.lineTo(28.dp.toPx(), 44.dp.toPx())
    path.moveTo(42.dp.toPx(), 52.dp.toPx())
    path.lineTo(54.dp.toPx(), 40.dp.toPx())
    path.lineTo(42.dp.toPx(), 28.dp.toPx())

    drawPath(
        path = path,
        color = if (seqMode == SeqMode.ERASING) BackGray else Color.Red,
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    )
}

@Composable
fun MuteSymbol(seqMode: SeqMode, modifier: Modifier = Modifier) {
    Text(
        "MUTE",
        fontSize = 12.nonScaledSp,
        color = if (seqMode == SeqMode.MUTING) BackGray else Color.Green,
        modifier = modifier,
    )
}

fun DrawScope.clearSymbol(seqMode: SeqMode) {
    val path = Path()
    val third = size.height / 3
    val twoThirds = third + third
    path.moveTo(third, third)
    path.lineTo(twoThirds, twoThirds)
    path.moveTo(third, twoThirds)
    path.lineTo(twoThirds, third)

    drawPath(
        path = path,
        color = if (seqMode == SeqMode.CLEARING) BackGray else Color.LightGray,
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    )
}