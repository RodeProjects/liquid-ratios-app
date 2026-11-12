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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

private val HeaderSpacing = 10.dp

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
        val portrait = maxHeight > maxWidth
        val vesselWidth = ((maxHeight - 56.dp) * VESSEL_ASPECT_RATIO).coerceIn(180.dp, 460.dp)
        Column(
            Modifier.widthIn(max = 580.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .heightIn(min = if (portrait) maxHeight else 0.dp)
                .padding(horizontal = 14.dp).padding(top = 5.dp, bottom = if (portrait) 32.dp else 22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
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
            }
            Column(Modifier.padding(top = 8.dp)) {
                TotalSection(mix, dispatch)
            }
            BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                val wrapCards = maxWidth.value / LocalDensity.current.fontScale < 275f
                Column {
                    mix.amounts.components.forEachIndexed { index, amount ->
                        AmountCard(index, amount, mix.unit, wrapCards) {
                            dispatch(EditorAction.OpenField(InputField.Component(index)))
                        }
                    }
                }
            }
            VesselSection(mix, vesselWidth, Modifier.padding(top = 19.dp)) {
                dispatch(EditorAction.OpenField(InputField.Maximum))
            }
        }
        editor.prompt?.let { MixDialog(it, mix, dispatch) }
    }
}

@Composable
private fun Header(mix: Mix, dispatch: (EditorAction) -> Unit) {
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val openAbout = { showAbout = true }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scaledWidth = maxWidth.value / LocalDensity.current.fontScale
        if (scaledWidth >= 300f) {
            val roomy = scaledWidth >= 410f
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CountControl(
                    mix, if (roomy) 44.dp else 34.dp, dispatch,
                    Modifier.weight(1f), compactLabel = scaledWidth < 370f,
                )
                Spacer(Modifier.width(HeaderSpacing))
                UnitControl(mix, if (roomy) 44.dp else 30.dp, dispatch)
                SettingsMenu(dispatch, if (roomy) 48.dp else 40.dp, openAbout)
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(bottom = 5.dp)) {
                CountControl(mix, 48.dp, dispatch, Modifier.fillMaxWidth())
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UnitControl(mix, 48.dp, dispatch)
                    SettingsMenu(dispatch, 48.dp, openAbout)
                }
            }
        }
    }
    if (showAbout) AboutDialog { showAbout = false }
}

@Composable
private fun CountControl(
    mix: Mix,
    itemWidth: Dp,
    dispatch: (EditorAction) -> Unit,
    modifier: Modifier,
    compactLabel: Boolean = false,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(HeaderSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(if (compactLabel) R.string.component_count_compact else R.string.component_count),
            Modifier.weight(1f, fill = false),
            color = Ink, fontSize = 13.sp, lineHeight = 16.sp,
        )
        Segments {
            for (count in MIN_COMPONENTS..MAX_COMPONENTS) {
                Segment(
                    count.toString(), pluralStringResource(R.plurals.select_component_count, count, count),
                    "count-$count", mix.count == count, itemWidth,
                ) { dispatch(EditorAction.SelectCount(count)) }
            }
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
private fun SettingsMenu(dispatch: (EditorAction) -> Unit, width: Dp, openAbout: () -> Unit) {
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
            DropdownMenuItem(
                text = { Text(stringResource(R.string.about_title)) },
                onClick = {
                    expanded = false
                    openAbout()
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
                            .clip(shape).background(colors.tint.copy(alpha = .9f))
                            .border(1.5.dp, colors.accent.copy(alpha = .85f), shape)
                            .padding(vertical = 4.dp, horizontal = 3.dp),
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
        EditableAmount(
            value = current,
            label = title,
            modifier = Modifier.widthIn(max = 185.dp).testTag("total"),
            style = TextStyle(color = Ink, fontSize = if (current.length > 11) 15.sp else 20.sp),
        ) {
            dispatch(EditorAction.OpenField(InputField.Total))
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
internal fun EditableAmount(
    value: String,
    label: String,
    modifier: Modifier,
    style: TextStyle,
    iconSize: Dp = 14.dp,
    edit: () -> Unit,
) {
    Row(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClickLabel = label, onClick = edit)
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            value, Modifier.weight(1f, fill = false),
            style = style, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End,
        )
        Pencil(iconSize)
    }
}

@Composable
private fun Pencil(iconSize: Dp) {
    Canvas(Modifier.size(iconSize)) {
        val color = Color(0xFFCE9AF7)
        val outline = Path().apply {
            moveTo(size.width * .1f, size.height * .9f)
            lineTo(size.width * .19f, size.height * .64f)
            lineTo(size.width * .73f, size.height * .1f)
            quadraticTo(size.width * .81f, size.height * .02f, size.width * .88f, size.height * .1f)
            lineTo(size.width * .93f, size.height * .15f)
            quadraticTo(size.width, size.height * .22f, size.width * .92f, size.height * .29f)
            lineTo(size.width * .38f, size.height * .82f)
            close()
        }
        drawPath(outline, color, style = Stroke(1.05.dp.toPx()))
        drawLine(
            color, Offset(size.width * .65f, size.height * .18f),
            Offset(size.width * .84f, size.height * .37f), 1.dp.toPx(),
        )
    }
}

@Composable
private fun AmountCard(
    index: Int,
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
    val tint = colors.tint.copy(alpha = .9f)
    val cardModifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape)
        .testTag("component-$index")
        .clickable(role = Role.Button, onClickLabel = description, onClick = edit)
        .padding(vertical = 3.dp)
        .background(Brush.horizontalGradient(listOf(tint, tint.copy(alpha = .43f), tint)), shape)
        .border(1.dp, colors.accent.copy(alpha = .77f), shape)
        .padding(horizontal = 11.dp, vertical = 5.dp)
    if (wrap) {
        Column(cardModifier) {
            ComponentLabel(componentName, colors.accent, Modifier.fillMaxWidth())
            ComponentAmount(formattedAmount, Modifier.fillMaxWidth().padding(start = 18.dp, top = 3.dp))
        }
    } else {
        Row(cardModifier, verticalAlignment = Alignment.CenterVertically) {
            ComponentLabel(componentName, colors.accent, Modifier.weight(1f).padding(end = 8.dp))
            ComponentAmount(formattedAmount)
        }
    }
}

@Composable
private fun ComponentLabel(name: String, accent: Color, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(accent.copy(alpha = .87f), CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(
            name, color = Ink, fontSize = 13.sp,
            lineHeight = 16.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ComponentAmount(amount: String, modifier: Modifier = Modifier) {
    Text(
        amount, modifier, color = Ink,
        fontSize = if (amount.length > 9) 14.sp else 21.sp,
        lineHeight = if (amount.length > 9) 17.sp else 25.sp,
        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End,
    )
}
