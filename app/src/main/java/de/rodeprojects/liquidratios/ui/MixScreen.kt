package de.rodeprojects.liquidratios.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.rodeprojects.liquidratios.AppState
import de.rodeprojects.liquidratios.R
import de.rodeprojects.liquidratios.StorageError
import de.rodeprojects.liquidratios.domain.EditorAction
import de.rodeprojects.liquidratios.domain.InputField
import de.rodeprojects.liquidratios.domain.MAX_COMPONENTS
import de.rodeprojects.liquidratios.domain.MIN_COMPONENTS
import de.rodeprojects.liquidratios.domain.MeasureUnit
import de.rodeprojects.liquidratios.domain.Mix
import de.rodeprojects.liquidratios.domain.NumberText
import java.math.BigDecimal
import java.math.MathContext

internal fun letter(index: Int): String = ('A' + index).toString()

@Composable
internal fun amountText(amount: BigDecimal, unit: MeasureUnit): String =
    stringResource(R.string.number_with_unit, NumberText.display(amount), unit.symbol)

@Composable
internal fun unitName(unit: MeasureUnit): String =
    stringResource(if (unit == MeasureUnit.GRAMS) R.string.unit_grams else R.string.unit_milliliters)

@Composable
internal fun MixScreen(state: AppState, dispatch: (EditorAction) -> Unit) {
    val editor = state.editor
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Background)
            .windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (editor == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center).size(28.dp))
            return@BoxWithConstraints
        }
        val mix = editor.mix
        val vesselWidth = ((maxHeight - 56.dp) * VESSEL_ASPECT_RATIO).coerceIn(180.dp, 460.dp)
        Column(
            Modifier.widthIn(max = 580.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp).padding(top = 5.dp, bottom = 22.dp),
        ) {
            Header(mix, dispatch)
            HorizontalDivider(color = Divider)
            state.storageError?.let { error ->
                Text(
                    stringResource(if (error == StorageError.LOAD) R.string.storage_load_error else R.string.storage_save_error),
                    Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(10.dp))
            RatioSection(mix, dispatch)
            Spacer(Modifier.height(8.dp))
            TotalSection(mix, dispatch)
            Spacer(Modifier.height(8.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val wrapCards = maxWidth.value / LocalDensity.current.fontScale < 275f
                Column {
                    mix.amounts.components.forEachIndexed { index, amount ->
                        AmountCard(index, mix.ratios[index], amount, mix.unit, wrapCards) {
                            dispatch(EditorAction.OpenField(InputField.Component(index)))
                        }
                    }
                }
            }
            Spacer(Modifier.height(19.dp))
            VesselSection(mix, vesselWidth) { dispatch(EditorAction.OpenField(InputField.Maximum)) }
        }
        editor.prompt?.let { MixDialog(it, mix, dispatch) }
    }
}

@Composable
private fun Header(mix: Mix, dispatch: (EditorAction) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        val compact = maxWidth >= 328.dp && fontScale <= 1.05f
        if (compact) {
            val roomy = maxWidth >= 415.dp
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Wordmark()
                HeaderRule()
                Text(
                    stringResource(R.string.component_count_compact),
                    color = Ink, fontSize = 9.sp, lineHeight = 11.sp,
                )
                CountControl(mix, if (roomy) 44.dp else 27.dp, dispatch)
                HeaderRule()
                UnitControl(mix, if (roomy) 44.dp else 26.dp, dispatch)
                SettingsMenu(dispatch, if (roomy) 48.dp else 30.dp)
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(bottom = 5.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Wordmark(Modifier.weight(1f))
                    UnitControl(mix, 48.dp, dispatch)
                    SettingsMenu(dispatch, 48.dp)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.component_count),
                        Modifier.weight(1f).padding(end = 8.dp), color = Ink, fontSize = 13.sp,
                    )
                    CountControl(mix, 48.dp, dispatch)
                }
            }
        }
    }
}

@Composable
private fun Wordmark(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.app_name), modifier,
        color = Ink, fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun HeaderRule() {
    Box(Modifier.height(18.dp).width(1.dp).background(Divider))
}

@Composable
private fun CountControl(mix: Mix, itemWidth: Dp, dispatch: (EditorAction) -> Unit) {
    Segments {
        for (count in MIN_COMPONENTS..MAX_COMPONENTS) {
            Segment(
                count.toString(), pluralStringResource(R.plurals.select_component_count, count, count),
                "count-$count", mix.count == count, itemWidth,
            ) { dispatch(EditorAction.SelectCount(count)) }
        }
    }
}

@Composable
private fun UnitControl(mix: Mix, itemWidth: Dp, dispatch: (EditorAction) -> Unit) {
    Segments {
        MeasureUnit.entries.forEach { unit ->
            Segment(unit.symbol, unitName(unit), "unit-${unit.symbol}", mix.unit == unit, itemWidth) {
                dispatch(EditorAction.SelectUnit(unit))
            }
        }
    }
}

@Composable
private fun Segments(content: @Composable RowScope.() -> Unit) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier.matchParentSize().padding(vertical = 13.dp)
                .clip(CircleShape).background(Panel).border(1.dp, Divider, CircleShape),
        )
        Row(Modifier.selectableGroup().padding(horizontal = 2.dp), content = content)
    }
}

@Composable
private fun Segment(
    label: String,
    description: String,
    tag: String,
    selected: Boolean,
    width: Dp,
    click: () -> Unit,
) {
    Box(
        Modifier.width(width).height(48.dp).testTag(tag)
            .selectable(selected, role = Role.RadioButton, onClick = click)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = 18.dp).clip(CircleShape)
                .background(if (selected) Sky else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label, color = if (selected) Background else Ink,
                fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun SettingsMenu(dispatch: (EditorAction) -> Unit, width: Dp) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.menu)
    Box {
        IconButton(
            { expanded = true },
            Modifier.width(width).height(48.dp).testTag("settings")
                .semantics { contentDescription = description },
        ) {
            Canvas(Modifier.size(14.dp)) {
                for (index in -1..1) {
                    drawCircle(Ink, 1.55.dp.toPx(), center.copy(y = center.y + index * 5.dp.toPx()))
                }
            }
        }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reset)) },
                onClick = {
                    expanded = false
                    dispatch(EditorAction.OpenReset)
                },
            )
        }
    }
}

@Composable
private fun RatioSection(mix: Mix, dispatch: (EditorAction) -> Unit) {
    val letters = (0 until mix.count).joinToString("\u00A0:\u00A0", transform = ::letter)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.ratio_title, letters),
            color = Ink, fontSize = 15.sp, lineHeight = 19.sp, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(3.dp))
        Row(
            Modifier.widthIn(max = (mix.count * 55 + (mix.count - 1) * 16).dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            mix.activeRatios.forEachIndexed { index, ratio ->
                if (index > 0) {
                    Text(
                        ":", Modifier.width(16.dp),
                        color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
                val colors = liquidColors[index]
                val shape = RoundedCornerShape(8.dp)
                val label = stringResource(R.string.ratio_field, letter(index))
                val value = NumberText.ratio(ratio)
                Box(
                    Modifier.weight(1f).heightIn(min = 48.dp)
                        .clip(shape).testTag("ratio-$index")
                        .clickable(role = Role.Button, onClickLabel = label) {
                            dispatch(EditorAction.OpenField(InputField.Ratio(index)))
                        }.semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp)
                            .clip(shape).background(colors.tint)
                            .border(1.5.dp, colors.accent, shape).padding(vertical = 4.dp, horizontal = 3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            value, color = Ink, textAlign = TextAlign.Center,
                            fontSize = if (value.length <= 5) 20.sp else 13.sp,
                            lineHeight = if (value.length <= 5) 26.sp else 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
        Text(
            stringResource(if (mix.unit == MeasureUnit.GRAMS) R.string.by_weight else R.string.by_volume),
            color = Ink, fontSize = 12.sp, lineHeight = 16.sp,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TotalSection(mix: Mix, dispatch: (EditorAction) -> Unit) {
    val current = amountText(mix.amounts.total, mix.unit)
    val title = stringResource(R.string.total_amount)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title, Modifier.weight(1f).padding(end = 8.dp),
            color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 19.sp,
        )
        Box(
            Modifier.heightIn(min = 48.dp).widthIn(max = 185.dp).clip(RoundedCornerShape(8.dp))
                .testTag("total")
                .clickable(role = Role.Button, onClickLabel = title) {
                    dispatch(EditorAction.OpenField(InputField.Total))
                }.semantics { contentDescription = title },
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                current, color = Ink, fontSize = if (current.length > 11) 15.sp else 20.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End,
            )
        }
    }
    val position = mix.amounts.total.divide(mix.maximum, MathContext.DECIMAL64).toFloat().coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().height(26.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(17.dp)) {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(Color(0xFF1B1B1B), cornerRadius = radius)
            if (position > 0f) {
                val thumbRadius = 10.dp.toPx()
                val active = if (position == 1f) size.width
                else thumbRadius + (size.width - 2 * thumbRadius) * position
                drawRoundRect(
                    Brush.horizontalGradient(listOf(Color(0xFF43A3F0), Sky), endX = active),
                    size = Size(active, size.height),
                    cornerRadius = radius,
                )
            }
        }
        Slider(
            value = position,
            onValueChange = { dispatch(EditorAction.Slide(it)) },
            modifier = Modifier.fillMaxWidth().testTag("amount-slider")
                .onPreviewKeyEvent { event ->
                    val increase = when (event.key) {
                        Key.DirectionRight, Key.DirectionUp -> true
                        Key.DirectionLeft, Key.DirectionDown -> false
                        else -> return@onPreviewKeyEvent false
                    }
                    if (event.type == KeyEventType.KeyDown) dispatch(EditorAction.Step(increase))
                    true
                }.semantics {
                    contentDescription = title
                    stateDescription = current
                },
            thumb = { Box(Modifier.size(20.dp).background(Color(0xFFFCFCFD), CircleShape)) },
            track = { Spacer(Modifier.fillMaxWidth().height(17.dp)) },
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(amountText(BigDecimal.ZERO, mix.unit), color = Ink, fontSize = 13.sp)
        Text(amountText(mix.maximum, mix.unit), color = Ink, fontSize = 13.sp)
    }
}

@Composable
private fun AmountCard(
    index: Int,
    ratio: BigDecimal,
    amount: BigDecimal,
    unit: MeasureUnit,
    wrap: Boolean,
    edit: () -> Unit,
) {
    val colors = liquidColors[index]
    val shape = RoundedCornerShape(11.dp)
    val componentName = stringResource(R.string.component_name, letter(index))
    val description = stringResource(R.string.component_amount, letter(index))
    val formattedAmount = amountText(amount, unit)
    val share = stringResource(
        if (ratio.compareTo(BigDecimal.ONE) == 0) R.string.ratio_part else R.string.ratio_parts,
        NumberText.ratio(ratio),
    )
    val cardModifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape)
        .testTag("component-$index")
        .clickable(role = Role.Button, onClickLabel = description, onClick = edit)
        .padding(vertical = 3.dp)
        .background(Brush.horizontalGradient(listOf(colors.tint, colors.tint.copy(alpha = .48f), colors.tint)), shape)
        .border(1.dp, colors.accent.copy(alpha = .87f), shape)
        .padding(horizontal = 11.dp, vertical = 5.dp)
    if (wrap) {
        Column(cardModifier) {
            ComponentLabel(componentName, colors.accent, Modifier.padding(top = 3.dp))
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AmountAndShare(formattedAmount, share, TextAlign.Start)
            }
        }
    } else {
        Row(cardModifier, verticalAlignment = Alignment.CenterVertically) {
            ComponentLabel(componentName, colors.accent, Modifier.weight(1.12f))
            HeaderRule()
            AmountAndShare(formattedAmount, share, TextAlign.Center)
        }
    }
}

@Composable
private fun ComponentLabel(name: String, accent: Color, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(accent, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(
            name, color = Ink, fontSize = 13.sp,
            lineHeight = 16.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RowScope.AmountAndShare(amount: String, share: String, amountAlignment: TextAlign) {
    Text(
        amount, Modifier.weight(1.06f).padding(horizontal = 3.dp), color = Ink,
        fontSize = if (amount.length > 9) 14.sp else 21.sp,
        lineHeight = if (amount.length > 9) 17.sp else 25.sp,
        fontWeight = FontWeight.SemiBold, textAlign = amountAlignment,
    )
    Text(
        share, Modifier.weight(.82f).padding(start = 3.dp), color = Ink,
        fontSize = 12.sp, lineHeight = 15.sp, textAlign = TextAlign.Center,
    )
}
