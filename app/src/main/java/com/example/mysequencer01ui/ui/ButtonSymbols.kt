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
import com.example.mysequencer01ui.PadsMode
import com.example.mysequencer01ui.ui.theme.bg2
import com.example.mysequencer01ui.ui.theme.notWhite
import com.example.mysequencer01ui.ui.theme.violet
import com.example.mysequencer01ui.ui.theme.warmRed


const val thickness = 4f

fun DrawScope.recSymbol(padsMode: PadsMode, seqIsRecording: Boolean) {
    drawCircle(
        color = if (seqIsRecording && padsMode == PadsMode.DEFAULT) bg2 else warmRed,
        radius = size.height / 6.67f,
        center = center,
        style = if(padsMode != PadsMode.DEFAULT && seqIsRecording) Fill else Stroke(width = thickness),
    )
}


fun DrawScope.playSymbol(seqIsPlaying: Boolean) {
    val first = size.height / 2.7f
    val second = first + size.height / 23
    val third = size.height - first
    val path = Path()
    path.moveTo(second, first)
    path.lineTo(second, third)
    path.lineTo(third, center.y)
    path.lineTo(second, first)
    path.lineTo(second, third)

    if (seqIsPlaying) {
        drawPath(
            path = path,
            color = notWhite,
            style = Fill
        )
    }
    drawPath(
        path = path,
        color = notWhite,
        style = Stroke( width = thickness, join = StrokeJoin.Round )
    )
}


fun DrawScope.stopSymbol() {
    val third = size.height / 3
    drawRect(
        topLeft = Offset(third, third),
        size = Size(third, third),
        color = if (true) bg2 else Color(0xFFBFBF00),
        style = Stroke(
            width = thickness,
            join = StrokeJoin.Round
        )
    )
}


fun DrawScope.eraseSymbol(padsMode: PadsMode) {

    val r = size.height / 24
    val r3 = r * 3
    val pathX = Path()
    pathX.moveTo(center.x, center.y)
    pathX.lineTo(center.x + r, center.y + r)
    pathX.moveTo(center.x, center.y)
    pathX.lineTo(center.x - r, center.y + r)
    pathX.moveTo(center.x, center.y)
    pathX.lineTo(center.x + r, center.y - r)
    pathX.moveTo(center.x, center.y)
    pathX.lineTo(center.x - r, center.y - r)
    pathX.translate(Offset(- r * 2f, 0f))

    drawPath(
        path = pathX,
        color = if (padsMode == PadsMode.ERASING) notWhite else warmRed,
        style = Stroke(
            width = thickness,
            cap = StrokeCap.Round
        )
    )

    val pathV = Path()
    pathV.moveTo(center.x + r3, center.y)
    pathV.lineTo(center.x, center.y + r3)
    pathV.moveTo(center.x + r3, center.y)
    pathV.lineTo(center.x, center.y - r3)

    drawPath(
        path = pathV,
        color = notWhite,
        style = Stroke(
            width = thickness,
            cap = StrokeCap.Round
        )
    )
}

//fun DrawScope.eraseSymbol(seqMode: SeqMode) {
//    val path = Path()
//    path.moveTo(28.dp.toPx(), 36.dp.toPx())
//    path.lineTo(36.dp.toPx(), 44.dp.toPx())
//    path.moveTo(36.dp.toPx(), 36.dp.toPx())
//    path.lineTo(28.dp.toPx(), 44.dp.toPx())
//    path.moveTo(42.dp.toPx(), 52.dp.toPx())
//    path.lineTo(54.dp.toPx(), 40.dp.toPx())
//    path.lineTo(42.dp.toPx(), 28.dp.toPx())
//
//    drawPath(
//        path = path,
//        color = if (seqMode == SeqMode.ERASING) bg2 else warmRed,
//        style = Stroke(
//            width = thickness,
//            cap = StrokeCap.Round
//        )
//    )
//}


@Composable
fun MuteSymbol(padsMode: PadsMode, modifier: Modifier = Modifier) {
    Text(
        "MUTE",
        fontSize = buttonTextSize.nonScaledSp,
        color = if (padsMode == PadsMode.MUTING) bg2 else violet,
        modifier = modifier,
    )
}


fun DrawScope.clearSymbol(padsMode: PadsMode) {
    val first = size.height / 2.6f
    val second = size.height - first
    val path = Path()
    path.moveTo(first, first)
    path.lineTo(second, second)
    path.moveTo(first, second)
    path.lineTo(second, first)

    drawPath(
        path = path,
        color = if (padsMode == PadsMode.CLEARING) bg2 else notWhite,
        style = Stroke(
            width = thickness,
            cap = StrokeCap.Round
        )
    )
}