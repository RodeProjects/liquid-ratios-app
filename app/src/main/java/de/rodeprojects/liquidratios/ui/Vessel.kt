package de.rodeprojects.liquidratios.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.rodeprojects.liquidratios.R
import de.rodeprojects.liquidratios.domain.MAX_COMPONENTS
import de.rodeprojects.liquidratios.domain.MeasureUnit
import de.rodeprojects.liquidratios.domain.Mix
import de.rodeprojects.liquidratios.domain.NumberText
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

internal const val VESSEL_ASPECT_RATIO = 1.31f

@Composable
internal fun VesselSection(mix: Mix, maxVesselWidth: Dp, modifier: Modifier, editMaximum: () -> Unit) {
    val max = amountText(mix.maximum, mix.unit)
    val editLabel = stringResource(R.string.edit_maximum)
    val layers = mix.amounts.components.mapIndexed { index, amount ->
        val share = componentPercent(amount, mix.amounts.total)
        stringResource(R.string.vessel_layer, letter(index), amountText(amount, mix.unit), share)
    }
    val percent = mix.amounts.total.movePointRight(2).divide(mix.maximum, MathContext.DECIMAL64)
    val description = stringResource(
        R.string.vessel_description, NumberText.display(percent), layers.joinToString(),
    )
    var cumulative = BigDecimal.ZERO
    val levels = List(MAX_COMPONENTS) { index ->
        cumulative += mix.amounts.components.getOrElse(index) { BigDecimal.ZERO }
        animateFloatAsState(
            cumulative.divide(mix.maximum, MathContext.DECIMAL64).toFloat().coerceIn(0f, 1f),
            animationSpec = tween(170),
            label = "Liquid level ${letter(index)}",
        )
    }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            Modifier.widthIn(max = maxVesselWidth).fillMaxWidth().aspectRatio(VESSEL_ASPECT_RATIO),
        ) {
            val density = LocalDensity.current
            val compactScale = maxWidth.value / density.fontScale < 284f
            val largeLimitText = max.length <= 7 && !compactScale
            val leftFraction = max(.231f, with(density) { 68.sp.toPx() / maxWidth.toPx() }).coerceAtMost(.42f)
            GlassVessel(
                levels,
                leftFraction,
                Modifier.matchParentSize().testTag("vessel").semantics { contentDescription = description },
            )
            ComponentPercentLabels(mix, levels, leftFraction, maxWidth)
            EditableAmount(
                value = max,
                label = editLabel,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 7.dp)
                    .fillMaxWidth(.27f).testTag("maximum"),
                style = TextStyle(
                    color = SecondaryInk, fontSize = if (largeLimitText) 17.sp else 12.sp,
                    lineHeight = if (largeLimitText) 21.sp else 15.sp,
                ),
                iconSize = if (compactScale) 12.dp else 14.dp,
                edit = editMaximum,
            )
        }
        Text(
            stringResource(if (mix.unit == MeasureUnit.GRAMS) R.string.weight_caption else R.string.volume_caption),
            Modifier.padding(top = 9.dp), color = Ink, textAlign = TextAlign.Center,
            fontSize = 12.sp, lineHeight = 16.sp,
        )
    }
}

private fun componentPercent(amount: BigDecimal, total: BigDecimal): String {
    if (total.signum() == 0 || amount.signum() == 0) return "0"
    val percent = amount.movePointRight(2).divide(total, MathContext.DECIMAL64)
    return NumberText.ratio(percent)
}

internal fun arrangePointOffsets(
    anchors: List<Float>,
    labels: List<Float>,
    baseLengths: List<Float>,
    pointSpacing: Float = 7f,
): List<Float> {
    val count = anchors.size
    fun crosses(offsets: List<Float>): Boolean {
        val lengths = baseLengths.indices.map { baseLengths[it] + offsets[it] }
        for (first in 0 until count - 1) {
            for (second in first + 1 until count) {
                val x = minOf(lengths[first], lengths[second])
                val y1 = labels[first] + (anchors[first] - labels[first]) * x / lengths[first]
                val y2 = labels[second] + (anchors[second] - labels[second]) * x / lengths[second]
                if (y1 > y2 + .0001f) return true
            }
        }
        return false
    }

    val zero = List(count) { 0f }
    val crowded = (0 until count - 1).any { anchors[it + 1] - anchors[it] < 6f }
    if (!crowded && !crosses(zero)) return zero

    val preferred = MutableList(count) { 0f }
    anchors.indices.sortedBy { abs(labels[it] - anchors[it]) }
        .forEachIndexed { rank, row -> preferred[row] = rank * pointSpacing }
    if (!crosses(preferred)) return preferred

    // At most four components yield 256 candidates, including the 24 permutations.
    var candidateCount = 1
    repeat(count) { candidateCount *= count }
    for (candidate in 0 until candidateCount) {
        var remaining = candidate
        val ranks = List(count) {
            val rank = remaining % count
            remaining /= count
            rank
        }
        if (ranks.distinct().size != count) continue
        val offsets = ranks.map { it * pointSpacing }
        if (!crosses(offsets)) return offsets
    }
    val commonLength = baseLengths.max()
    return baseLengths.map { commonLength - it }
}

internal data class LabelRoutes(val lengths: List<Float>, val angled: Boolean)

internal fun arrangeLabelRoutes(
    anchors: List<Float>,
    labels: List<Float>,
    baseLengths: List<Float>,
    maxLengths: List<Float>,
): LabelRoutes {
    val count = anchors.size
    fun fits(lengths: List<Float>): Boolean {
        fun yAt(row: Int, x: Float): Float {
            val delta = anchors[row] - labels[row]
            val bend = (lengths[row] - abs(delta)).coerceAtLeast(0f)
            if (x <= bend || delta == 0f) return labels[row]
            return labels[row] + delta * (x - bend) / (lengths[row] - bend)
        }
        for (first in 0 until count - 1) {
            for (second in first + 1 until count) {
                if (hypot(lengths[first] - lengths[second], anchors[first] - anchors[second]) < 5f) return false
                val end = minOf(lengths[first], lengths[second])
                val bend1 = (lengths[first] - abs(anchors[first] - labels[first])).coerceIn(0f, end)
                val bend2 = (lengths[second] - abs(anchors[second] - labels[second])).coerceIn(0f, end)
                for (x in listOf(0f, bend1, bend2, end)) {
                    if (yAt(first, x) > yAt(second, x) - .0001f) return false
                }
            }
        }
        return true
    }
    fun candidate(offsets: List<Float>): List<Float>? {
        val lengths = anchors.indices.map { row ->
            val desired = maxOf(baseLengths[row] + offsets[row], abs(anchors[row] - labels[row]))
            minOf(desired, maxLengths[row])
        }
        return lengths.takeIf(::fits)
    }

    val pointSpacing = minOf(
        12f, ((maxLengths.min() - baseLengths.max()) / (count - 1)).coerceAtLeast(0f),
    )
    val preferred = arrangePointOffsets(anchors, labels, baseLengths, pointSpacing)
    candidate(preferred)?.let { return LabelRoutes(it, angled = true) }
    var candidateCount = 1
    repeat(count) { candidateCount *= count }
    for (number in 0 until candidateCount) {
        var remaining = number
        val ranks = List(count) {
            val rank = remaining % count
            remaining /= count
            rank
        }
        if (ranks.distinct().size != count) continue
        candidate(ranks.map { it * pointSpacing })?.let { return LabelRoutes(it, angled = true) }
    }
    return LabelRoutes(baseLengths.indices.map { baseLengths[it] + preferred[it] }, angled = false)
}

@Composable
private fun BoxScope.ComponentPercentLabels(mix: Mix, levels: List<State<Float>>, leftFraction: Float, width: Dp) {
    val density = LocalDensity.current
    val height = width / VESSEL_ASPECT_RATIO
    val labelWidth = width * leftFraction - 6.dp
    val lineHeight = minOf(with(density) { 16.sp.toDp() }, (height - 6.dp) / mix.count - 4.dp)
    val fontSize = with(density) { (lineHeight * (11f / 16f)).toSp() }
    val labelStyle = TextStyle(
        color = Ink, fontSize = fontSize, lineHeight = with(density) { lineHeight.toSp() },
        textAlign = TextAlign.End, letterSpacing = 0.sp,
    )
    val indices = mix.amounts.components.indices.reversed()
    val percentages = indices.map { index ->
        stringResource(
            R.string.component_percent, componentPercent(mix.amounts.components[index], mix.amounts.total),
        )
    }
    val textMeasurer = rememberTextMeasurer()
    val letterStyle = labelStyle.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
    val percentStyle = labelStyle.copy(textAlign = TextAlign.Start)
    val letterWidth = indices.maxOf { textMeasurer.measure(letter(it), style = letterStyle).size.width }
    val spaceWidth = textMeasurer.measure("\u00a0\u00a0", style = labelStyle).size.width
    val percentWidth = percentages.maxOf { textMeasurer.measure(it, style = percentStyle).size.width }
    val labelStart = with(density) {
        (labelWidth.toPx() - letterWidth - spaceWidth - percentWidth).coerceAtLeast(0f).toDp()
    }
    val spacing = lineHeight + 4.dp
    val top = lineHeight
    val bottom = height - 3.dp
    val anchors = mix.amounts.components.indices.reversed().map { index ->
        val lower = if (index == 0) 0f else levels[index - 1].value
        val upper = levels[index].value
        height * (.979f - .956f * (lower + upper) / 2f)
    }
    val labelLines = anchors.map { it.coerceIn(top, bottom) }.toMutableList()
    for (index in 1 until labelLines.size) {
        labelLines[index] = maxOf(labelLines[index], labelLines[index - 1] + spacing)
    }
    if (labelLines.last() > bottom) {
        labelLines[labelLines.lastIndex] = bottom
        for (index in labelLines.lastIndex - 1 downTo 0) {
            labelLines[index] = minOf(labelLines[index], labelLines[index + 1] - spacing)
        }
    }
    val inset = width * (.711f - leftFraction) * .105f
    val radius = width * (.711f - leftFraction) * .142f
    val baseLengths = anchors.map { anchor ->
        val depth = ((anchor / height - .023f) / .956f).coerceIn(0f, 1f)
        (11.dp + inset * depth + radius).value
    }
    val maxLength = (width * (.711f - leftFraction) / 2f + 3.dp).value
    val maxLengths = List(anchors.size) { maxLength }
    val routes = arrangeLabelRoutes(
        anchors.map { it.value }, labelLines.map { it.value }, baseLengths, maxLengths,
    )
    Canvas(Modifier.matchParentSize()) {
        val left = size.width * leftFraction
        mix.amounts.components.indices.reversed().forEachIndexed { row, index ->
            val targetY = anchors[row].toPx()
            val labelY = labelLines[row].toPx()
            val elbow = Offset(left - 3.dp.toPx(), labelY)
            val end = Offset(elbow.x + routes.lengths[row].dp.toPx(), targetY)
            val start = Offset(labelStart.toPx(), labelY)
            val lineColor = SecondaryInk.copy(alpha = .8f)
            if (mix.amounts.components[index].signum() == 0) {
                drawLine(lineColor, start, elbow, 1.dp.toPx(), StrokeCap.Round)
                return@forEachIndexed
            }
            val bend = if (routes.angled) {
                Offset(maxOf(elbow.x, end.x - abs(targetY - labelY)), labelY)
            } else elbow
            drawLine(lineColor, start, bend, 1.dp.toPx(), StrokeCap.Round)
            drawLine(lineColor, bend, end, 1.dp.toPx(), StrokeCap.Round)
            drawCircle(Ink, 2.dp.toPx(), end)
        }
    }
    indices.forEachIndexed { row, index ->
        Row(
            Modifier.offset(x = labelStart, y = labelLines[row] - lineHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                letter(index), Modifier.width(with(density) { letterWidth.toDp() }),
                style = letterStyle.copy(color = liquidColors[index].accent), maxLines = 1,
            )
            Spacer(Modifier.width(with(density) { spaceWidth.toDp() }))
            Text(percentages[row], style = percentStyle, maxLines = 1)
        }
    }
}

@Composable
private fun GlassVessel(levels: List<State<Float>>, leftFraction: Float, modifier: Modifier) {
    Box(modifier.drawWithCache {
        val left = size.width * leftFraction
        val right = size.width * .711f
        val top = size.height * .023f
        val base = size.height * .979f
        val span = right - left
        val height = base - top
        val shoulder = height * .081f
        val inset = span * .105f
        val radius = span * .142f
        val glass = Path().apply {
            moveTo(left, top)
            lineTo(left + inset, base - shoulder)
            quadraticTo(left + inset + radius * .12f, base, left + inset + radius, base)
            lineTo(right - inset - radius, base)
            quadraticTo(right - inset - radius * .12f, base, right - inset, base - shoulder)
            lineTo(right, top)
            close()
        }
        val interior = Brush.horizontalGradient(
            0f to Color(0xFF3B4148),
            .14f to Color(0xFF242A30),
            .43f to Color(0xFF11161B),
            .66f to Color(0xFF101419),
            .88f to Color(0xFF1C232A),
            1f to Color(0xFF41474D),
            startX = left,
            endX = right,
        )
        val reflection = Brush.horizontalGradient(
            0f to Color.White.copy(alpha = .34f),
            .07f to Color.White.copy(alpha = .05f),
            .17f to Color.Transparent,
            .82f to Color.Transparent,
            .94f to Color.White.copy(alpha = .055f),
            1f to Color.White.copy(alpha = .38f),
            startX = left,
            endX = right,
        )
        val liquidBrushes = liquidColors.map { colors ->
            Brush.horizontalGradient(
                0f to colors.light,
                .44f to colors.deep,
                .63f to colors.deep,
                1f to colors.light,
                startX = left,
                endX = right,
            )
        }
        val rim = Brush.verticalGradient(
            listOf(Color(0xFFDDE2E5), Color(0xFFAEB8BC), Color(0xFF9CA8AE), Color(0xFFE9F1F2)),
            startY = top, endY = base,
        )
        onDrawBehind {
            drawPath(glass, interior)
            clipPath(glass) {
                var previousY = base
                levels.forEachIndexed { index, level ->
                    val y = base - height * level.value
                    if (y < previousY) {
                        drawRect(
                            liquidBrushes[index], Offset(left, y), Size(span, previousY - y + .5f),
                        )
                        drawLine(liquidColors[index].light.copy(alpha = .77f), Offset(left, y), Offset(right, y), .9.dp.toPx())
                        previousY = y
                    }
                }
                drawPath(glass, reflection)
            }
            drawPath(glass, Color(0xFF7A8388), style = Stroke(5.dp.toPx(), join = StrokeJoin.Round))
            drawPath(glass, rim, style = Stroke(2.4.dp.toPx(), join = StrokeJoin.Round))
            drawLine(
                Color(0xFFF0F1F2), Offset(left, top), Offset(right, top),
                3.6.dp.toPx(), StrokeCap.Round,
            )
            for (step in 0..4) {
                val proportion = step / 4f
                val y = top + height * proportion
                val x = right - inset * proportion - if (step == 4) radius else 0f
                val start = if (step == 4) x else x - span * .0312f
                val end = if (step == 4) start + (radius + span * .0576f) * .7875f else x + span * .0576f
                drawLine(
                    Color(0xFFC4CAD0).copy(alpha = .82f),
                    Offset(start, y), Offset(end, y),
                    1.9.dp.toPx(),
                )
            }
        }
    })
}
