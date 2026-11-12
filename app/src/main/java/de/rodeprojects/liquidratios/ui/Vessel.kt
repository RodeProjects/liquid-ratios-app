package de.rodeprojects.liquidratios.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

internal const val VESSEL_ASPECT_RATIO = 1.31f

@Composable
internal fun VesselSection(mix: Mix, maxVesselWidth: Dp, modifier: Modifier, editMaximum: () -> Unit) {
    val zero = amountText(BigDecimal.ZERO, mix.unit)
    val max = amountText(mix.maximum, mix.unit)
    val editLabel = stringResource(R.string.edit_maximum)
    val layers = mix.amounts.components.mapIndexed { index, amount ->
        stringResource(R.string.vessel_layer, letter(index), amountText(amount, mix.unit))
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
            val compactScale = maxWidth.value / LocalDensity.current.fontScale < 284f
            val largeLimitText = max.length <= 7 && !compactScale
            GlassVessel(
                levels,
                Modifier.matchParentSize().testTag("vessel").semantics { contentDescription = description },
            )
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
            Text(
                zero, Modifier.align(Alignment.BottomStart)
                    .padding(start = maxWidth * .744f, bottom = 13.dp),
                color = SecondaryInk, fontSize = if (compactScale) 14.sp else 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            stringResource(if (mix.unit == MeasureUnit.GRAMS) R.string.weight_caption else R.string.volume_caption),
            Modifier.padding(top = 9.dp), color = Ink, textAlign = TextAlign.Center,
            fontSize = 12.sp, lineHeight = 16.sp,
        )
    }
}

@Composable
private fun GlassVessel(levels: List<State<Float>>, modifier: Modifier) {
    Box(modifier.drawWithCache {
        val left = size.width * .231f
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
            for (step in 1..4) {
                val proportion = step / 5f
                val y = top + height * proportion
                val x = left + inset * proportion
                drawLine(
                    Color(0xFFC4CAD0).copy(alpha = .82f),
                    Offset(x - span * .039f, y), Offset(x + span * .072f, y),
                    1.9.dp.toPx(),
                )
            }
        }
    })
}
