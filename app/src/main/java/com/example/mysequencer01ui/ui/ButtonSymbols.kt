package com.example.mysequencer01ui.ui


import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.example.mysequencer01ui.ui.theme.*


const val thickness = 4f
val style = Stroke( width = thickness, cap = StrokeCap.Round, join = StrokeJoin.Round )

fun DrawScope.shiftSymbol(selecting: Boolean) {
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
        color = if (selecting) buttons else dusk,
        style = Stroke(
            width = thickness,
            cap = StrokeCap.Round
        )
    )
}


fun DrawScope.allSymbol(buttonPressed: Boolean) {
    val m = size.height / 8f
    val l = m * 2
    drawRect(
        color = if (buttonPressed) buttons else dusk,
        topLeft = Offset(center.x - m, center.y - m),
        size = Size(l, l),
        style = style
    )
    val path = Path()
    path.moveTo(center.x, center.y - m)
    path.lineTo(center.x, center.y + m)
    path.moveTo(center.x - m, center.y)
    path.lineTo(center.x + m, center.y)
    drawPath(
        path = path,
        color = if (buttonPressed) buttons else dusk,
        style = style
    )
}


fun DrawScope.quantizeSymbol(quantizingPadsMode: Boolean, isQuantizing: Boolean) {
    val m = size.height / 8
    val s = size.height / 16
    val xs = size.height / 32

    val color = if(isQuantizing) notWhite else selectedButton

    drawArc(
        color = if (quantizingPadsMode) buttons else color,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - m, center.y - m),
        size = size / 4f, style = Stroke(width = thickness, cap = StrokeCap.Round))

    val path = Path()
    path.moveTo(center.x + m, center.y - s - xs)
    path.lineTo(center.x + m, center.y - m)
    path.moveTo(center.x - m, center.y - s - xs)
    path.lineTo(center.x - m, center.y - m)
    path.moveTo(center.x + m, center.y)
    path.lineTo(center.x + m, center.y - s)
    path.moveTo(center.x - m, center.y)
    path.lineTo(center.x - m, center.y - s)

    drawPath(
        path = path,
        color = if (quantizingPadsMode) buttons else color,
        style = Stroke( width = thickness, join = StrokeJoin.Round)
    )
}

fun DrawScope.quantizeLineCounter(progress: Float) {
    drawLine(dusk, Offset(size.width, size.height), Offset(size.width, size.height - progress * size.height), thickness)
}


fun DrawScope.saveArrow(saving: Boolean) {
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
        color = if(saving) buttons else dusk,
        style = style
    )
    saveLoadSymbol()
}

fun DrawScope.loadArrow(loading: Boolean) {
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
        color = if(loading) buttons else dusk,
        style = style
    )
    saveLoadSymbol()
}

fun DrawScope.saveLoadSymbol() {
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


fun DrawScope.soloSymbol(soloing: Boolean) {
    val m = size.height / 8f
    val s = size.height / 16f
    drawArc(
        color = if(soloing) buttons else violet,
        startAngle = 90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - s, center.y - m),
        size = size / 8f,
        style = style)
    drawArc(
        color = if(soloing) buttons else violet,
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - s, center.y),
        size = size / 8f,
        style = style)
}

fun DrawScope.strikeStrip() {
    val m = size.height / 5.5f
    drawLine(
        color = buttons,
        start = Offset(center.x - m, center.y + m / 3),
        end = Offset(center.x + m, center.y - m / 3),
        strokeWidth = thickness * 2,
        cap = StrokeCap.Round
    )
    drawLine(
        color = notWhite,
        start = Offset(center.x - m, center.y + m / 3),
        end = Offset(center.x + m, center.y - m / 3),
        strokeWidth = thickness,
        cap = StrokeCap.Round
    )
}

fun DrawScope.muteSymbol(muting: Boolean) {
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
        color = if (muting) buttons else violet,
        style = style
    )
}


fun DrawScope.eraseSymbol(erasing: Boolean) {

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
        color = warmRed,
        style = style
    )

    val pathV = Path()
    pathV.moveTo(center.x + r3, center.y)
    pathV.lineTo(center.x, center.y + r3)
    pathV.moveTo(center.x + r3, center.y)
    pathV.lineTo(center.x, center.y - r3)

    drawPath(
        path = pathV,
        color = if (erasing) warmRed else notWhite,
        style = style
    )
}


fun DrawScope.clearSymbol(clearing: Boolean) {
    val first = size.height / 2.6f
    val second = size.height - first
    val path = Path()
    path.moveTo(first, first)
    path.lineTo(second, second)
    path.moveTo(first, second)
    path.lineTo(second, first)

    drawPath(
        path = path,
        color = if (clearing) buttons else notWhite,
        style = style
    )
}


fun DrawScope.recSymbol(padsModeIsDefault: Boolean, seqIsRecording: Boolean) {
    drawCircle(
        color = if (seqIsRecording && padsModeIsDefault) buttons else warmRed,
        radius = size.height / 7f,
        center = center,
        style = if(!padsModeIsDefault && seqIsRecording) Fill else Stroke(width = thickness),
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

    drawPath(
        path = path,
        color = notWhite,
        style = if (seqIsPlaying) Fill else style
    )
}

fun DrawScope.stopSymbol() {
    val m = size.height / 8f
    val l = m * 2
    drawRect(
        color =  notWhite,
        topLeft = Offset(center.x - m, center.y - m),
        size = Size(l, l),
        style = style
    )
}


fun DrawScope.tabLiveSymbol(color: Color) {
    val m = size.height / 8f
    val s = m/1.5f
    drawLine(color, Offset(center.x, center.y - s), Offset(center.x, center.y + s), thickness)
    drawLine(color, Offset(center.x - m, center.y), Offset(center.x + m, center.y), thickness)
}

fun DrawScope.tabPianoSymbol(color: Color) {
    val m = size.height / 8f
    val s = m/2
    drawLine(color, Offset(center.x - m, center.y + s), Offset(center.x, center.y + s), thickness)
    drawLine(color, Offset(center.x - s, center.y), Offset(center.x + s, center.y), thickness)
    drawLine(color, Offset(center.x, center.y - s), Offset(center.x + m, center.y - s), thickness)
}

fun DrawScope.tabStepSymbol(color: Color) {
    val m = size.height / 8f
    val s = m/2
    drawLine(color, Offset(center.x - m, center.y - s), Offset(center.x - m, center.y + s), thickness)
    drawLine(color, Offset(center.x - m, center.y - s), Offset(center.x + m, center.y - s), thickness)
    drawLine(color, Offset(center.x - m, center.y + s), Offset(center.x + m, center.y + s), thickness)
    drawLine(color, Offset(center.x + m, center.y + s), Offset(center.x + m, center.y - s), thickness)
}

fun DrawScope.tabAutomationSymbol(color: Color) {
    val m = size.height / 8f
    val s = m/2
    drawArc(color, 180f, 180f, false, Offset(center.x - m, center.y - s + 1), Size(m, m), style = style)
    drawArc(color, 180f, -180f, false, Offset(center.x, center.y - s), Size(m, m), style = style)
}

fun DrawScope.tabSettingsSymbol(color: Color) {
    val m = size.height / 8f
    val r = m/5
    val s = m/8
    drawCircle(color, r, Offset(center.x - m + s, center.y), style = style)
    drawCircle(color, r, Offset(center.x, center.y), style = style)
    drawCircle(color, r, Offset(center.x + m - s, center.y), style = style)
}