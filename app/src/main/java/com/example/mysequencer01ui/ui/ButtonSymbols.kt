package com.example.mysequencer01ui.ui

import androidx.compose.foundation.layout.offset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.dp
import com.example.mysequencer01ui.PadsMode
import com.example.mysequencer01ui.ui.theme.*


const val thickness = 4f

fun DrawScope.recSymbol(padsMode: PadsMode, seqIsRecording: Boolean) {
    drawCircle(
        color = if (seqIsRecording && padsMode == PadsMode.DEFAULT) buttonsColor else warmRed,
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
        style = Stroke( width = thickness, cap = StrokeCap.Round, join = StrokeJoin.Round )
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


fun DrawScope.muteSymbol(padsMode: PadsMode) {
    val l = size.height / 8
    val m = size.height / 12
    val s = size.height / 16
    val path = Path()
    path.moveTo(center.x + s, center.y + l)
    path.lineTo(center.x + m, center.y - l)
    path.lineTo(center.x, center.y + s)
    path.lineTo(center.x - s, center.y - l)
    path.lineTo(center.x - m, center.y + l)

    drawPath(
        path = path,
        color = if (padsMode == PadsMode.MUTING) buttonsColor else violet,
        style = Stroke( width = thickness, join = StrokeJoin.Round )
    )
}


fun DrawScope.soloSymbol() {
    // TODO offset to the center before rotation to avoid messy constants
    val m = size.height / 20f
    val s = size.height / 46f
    drawArc(violet, -90f, 180f, false, Offset(center.x - m, center.y - s), size = size / 8f, style = Stroke(width = thickness, cap = StrokeCap.Round))
    drawArc(violet, 90f, 180f, false, Offset(center.x - m, center.y - size.height / 8f - s), size = size / 8f, style = Stroke(width = thickness, cap = StrokeCap.Round))
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
        color = if (padsMode == PadsMode.CLEARING) buttonsColor else notWhite,
        style = Stroke(
            width = thickness,
            cap = StrokeCap.Round
        )
    )
}


fun DrawScope.quantizeSymbolColor(padsMode: PadsMode) {
    val m = size.height / 8
    val s = size.height / 16
    drawArc(warmRed, 0f, 90f, false, Offset(center.x - m, center.y - m), size / 4f, style = Stroke(width = thickness, cap = StrokeCap.Round))
    drawArc(night, 90f, 90f, false, Offset(center.x - m, center.y - m), size / 4f, style = Stroke(width = thickness, cap = StrokeCap.Round))

    val pathRed = Path()
    pathRed.moveTo(center.x + m, center.y)
    pathRed.lineTo(center.x + m, center.y - m)
    drawPath(
        path = pathRed,
        color = if (false) buttonsColor else warmRed,
        style = Stroke( width = thickness, join = StrokeJoin.Round )
    )
    val pathBlue = Path()
    pathBlue.moveTo(center.x - m, center.y)
    pathBlue.lineTo(center.x - m, center.y - m)
    drawPath(
        path = pathBlue,
        color = if (false) buttonsColor else night,
        style = Stroke( width = thickness, join = StrokeJoin.Round )
    )
    val pathWhite = Path()
    pathWhite.moveTo(center.x + m, center.y - s)
    pathWhite.lineTo(center.x + m, center.y - m)
    pathWhite.moveTo(center.x - m, center.y - s)
    pathWhite.lineTo(center.x - m, center.y - m)
    drawPath(
        path = pathWhite,
        color = if (false) buttonsColor else dusk,
        style = Stroke( width = thickness, join = StrokeJoin.Round )
    )
}

fun DrawScope.quantizeSymbol(padsMode: PadsMode) {
    val m = size.height / 8
    val s = size.height / 16
    val xs = size.height / 32

    drawArc(dusk, 0f, 90f, false, Offset(center.x - m, center.y - m), size / 4f, style = Stroke(width = thickness, cap = StrokeCap.Round))
    drawArc(dusk, 90f, 90f, false, Offset(center.x - m, center.y - m), size / 4f, style = Stroke(width = thickness, cap = StrokeCap.Round))

    val pathRed = Path()
    pathRed.moveTo(center.x + m, center.y)
    pathRed.lineTo(center.x + m, center.y - s)
    drawPath(
        path = pathRed,
        color = if (false) buttonsColor else dusk,
        style = Stroke( width = thickness, join = StrokeJoin.Round )
    )
    val pathBlue = Path()
    pathBlue.moveTo(center.x - m, center.y)
    pathBlue.lineTo(center.x - m, center.y - s)
    drawPath(
        path = pathBlue,
        color = if (false) buttonsColor else dusk,
        style = Stroke( width = thickness, join = StrokeJoin.Round )
    )
    val pathWhite = Path()
    pathWhite.moveTo(center.x + m, center.y - s - xs)
    pathWhite.lineTo(center.x + m, center.y - m)
    pathWhite.moveTo(center.x - m, center.y - s - xs)
    pathWhite.lineTo(center.x - m, center.y - m)
    drawPath(
        path = pathWhite,
        color = if (false) buttonsColor else dusk,
        style = Stroke( width = thickness, join = StrokeJoin.Round )
    )
}


fun DrawScope.saveArrow(padsMode: PadsMode) {
    val l = size.height / 8
    val m = size.height / 16
    val s = size.height / 32
    val path = Path()

    path.moveTo(center.x + l - s, center.y + l - s)
    path.lineTo(center.x - s, center.y - s)
    path.lineTo(center.x - s, center.y + m - s)
    path.moveTo(center.x - s, center.y - s)
    path.lineTo(center.x + m - s, center.y - s)

    drawPath(
        path = path,
        color = if (false) buttonsColor else dusk,
        style = Stroke(
            width = thickness,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

fun DrawScope.loadArrow(padsMode: PadsMode) {
    val l = size.height / 8
    val m = size.height / 16
    val s = size.height / 32
    val path = Path()

    path.moveTo(center.x - s, center.y - s)
    path.lineTo(center.x + l - s, center.y + l - s)
    path.lineTo(center.x + l - s, center.y + m - s)
    path.moveTo(center.x + l - s, center.y + l - s)
    path.lineTo(center.x + m - s, center.y + l - s)

    drawPath(
        path = path,
        color = if (false) buttonsColor else dusk,
        style = Stroke(
            width = thickness,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

fun DrawScope.saveLoadSymbol(padsMode: PadsMode) {
    val m = size.width / 12f
    val s = size.width / 16f
    drawLine(
        color = night,
        start = Offset(center.x + m - s, center.y - m - s),
        end = Offset(center.x - m - s, center.y + m - s),
        strokeWidth = thickness,
        cap = StrokeCap.Round
    )
}


fun DrawScope.shiftSymbol(padsMode: PadsMode) {
    val l = size.height / 22f
    val m = size.height / 80f
    val s = size.height / 220f
    val path = Path()
    path.moveTo(center.x, center.y)
    path.lineTo(center.x, center.y - l)
    path.moveTo(center.x, center.y)
    path.lineTo(center.x + l, center.y - m)
    path.moveTo(center.x, center.y)
    path.lineTo(center.x - l, center.y - m)
    path.moveTo(center.x, center.y)
    path.lineTo(center.x + m + m, center.y + l - s)
    path.moveTo(center.x, center.y)
    path.lineTo(center.x - m - m, center.y + l - s)

    drawPath(
        path = path,
        color = if (padsMode == PadsMode.CLEARING) buttonsColor else dusk,
        style = Stroke(
            width = thickness,
            cap = StrokeCap.Round
        )
    )
}