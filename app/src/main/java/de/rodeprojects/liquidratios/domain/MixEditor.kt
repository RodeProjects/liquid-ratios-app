package de.rodeprojects.liquidratios.domain

import java.math.BigDecimal
import java.math.RoundingMode

internal sealed interface InputField {
    data class Ratio(val index: Int) : InputField
    data class Component(val index: Int) : InputField
    data object Total : InputField
    data object Maximum : InputField
}

internal enum class InputError {
    AMOUNT, POSITIVE_AMOUNT, RATIO, BELOW_TOTAL
}

internal sealed interface Prompt {
    data class Edit(
        val field: InputField,
        val draft: String,
        val error: InputError? = null,
        val pending: Overflow? = null,
    ) : Prompt

    data class Overflow(val input: Edit, val quantity: Quantity, val total: BigDecimal) : Prompt
    data class SwitchUnit(val unit: MeasureUnit) : Prompt
    data object MenuReset : Prompt
}

internal data class EditorState(val mix: Mix = Mix(), val prompt: Prompt? = null)

internal sealed interface EditorAction {
    data class OpenField(val field: InputField) : EditorAction
    data class Type(val value: String) : EditorAction
    data class SelectCount(val count: Int) : EditorAction
    data class SelectUnit(val unit: MeasureUnit) : EditorAction
    data class Slide(val fraction: Float) : EditorAction
    data class Step(val increase: Boolean) : EditorAction
    data object OpenReset : EditorAction
    data object Confirm : EditorAction
    data object Cancel : EditorAction
    data object CorrectInput : EditorAction
    data object RaiseLimit : EditorAction
}

internal object MixEditor {
    fun reduce(state: EditorState, action: EditorAction): EditorState {
        val mix = state.mix
        val prompt = state.prompt
        return when (action) {
            is EditorAction.OpenField -> if (prompt == null) {
                state.copy(prompt = edit(mix, action.field))
            } else state
            is EditorAction.Type -> if (prompt is Prompt.Edit) {
                state.copy(prompt = prompt.copy(draft = action.value.take(64), error = null))
            } else state
            is EditorAction.SelectCount -> if (prompt == null && action.count in MIN_COMPONENTS..MAX_COMPONENTS) {
                state.copy(mix = mix.changeCount(action.count))
            } else state
            is EditorAction.SelectUnit -> if (prompt == null && action.unit != mix.unit) {
                state.copy(prompt = Prompt.SwitchUnit(action.unit))
            } else state
            is EditorAction.Slide -> if (prompt == null && action.fraction.isFinite()) {
                applySliderAmount(state, sliderAmount(action.fraction, mix.maximum))
            } else state
            is EditorAction.Step -> if (prompt == null) {
                val scale = sliderScale(mix.maximum)
                val current = mix.amounts.total
                val grid = current.setScale(scale, if (action.increase) RoundingMode.CEILING else RoundingMode.FLOOR)
                val step = BigDecimal.ONE.movePointLeft(scale)
                val target = if (grid.compareTo(current) != 0) grid
                else if (action.increase) grid + step else grid - step
                applySliderAmount(state, target.coerceIn(BigDecimal.ZERO, mix.maximum))
            } else state
            EditorAction.OpenReset -> if (prompt == null) state.copy(prompt = Prompt.MenuReset) else state
            EditorAction.Confirm -> when (prompt) {
                is Prompt.Edit -> submit(state, prompt)
                is Prompt.SwitchUnit -> EditorState(mix.copy(unit = prompt.unit))
                Prompt.MenuReset -> EditorState()
                else -> state
            }
            EditorAction.Cancel -> state.copy(prompt = (prompt as? Prompt.Edit)?.pending)
            EditorAction.CorrectInput -> if (prompt is Prompt.Overflow) state.copy(prompt = prompt.input) else state
            EditorAction.RaiseLimit -> if (prompt is Prompt.Overflow) {
                state.copy(prompt = edit(mix, InputField.Maximum).copy(pending = prompt))
            } else state
        }
    }

    private fun edit(mix: Mix, field: InputField): Prompt.Edit {
        val value = when (field) {
            is InputField.Ratio -> mix.ratios[field.index]
            is InputField.Component -> mix.amounts.components[field.index]
            InputField.Total -> mix.amounts.total
            InputField.Maximum -> mix.maximum
        }
        return Prompt.Edit(field, NumberText.edit(value))
    }

    private fun submit(state: EditorState, input: Prompt.Edit): EditorState {
        val mix = state.mix
        val field = input.field
        val value = NumberText.parse(input.draft, if (field is InputField.Ratio) 12 else 2)
        if (value == null || value.signum() == 0 && (field is InputField.Ratio || field == InputField.Maximum)) {
            val error = when (field) {
                is InputField.Ratio -> InputError.RATIO
                InputField.Maximum -> InputError.POSITIVE_AMOUNT
                else -> InputError.AMOUNT
            }
            return state.copy(prompt = input.copy(error = error))
        }
        return when (field) {
            is InputField.Ratio -> EditorState(mix.changeRatio(field.index, value))
            InputField.Maximum -> {
                val required = input.pending?.total ?: mix.amounts.total
                if (value < required) state.copy(prompt = input.copy(error = InputError.BELOW_TOTAL))
                else EditorState(mix.copy(maximum = value, quantity = input.pending?.quantity ?: mix.quantity))
            }
            InputField.Total -> setQuantity(state, input, Quantity.Total(value))
            is InputField.Component -> setQuantity(state, input, Quantity.Component(field.index, value))
        }
    }

    private fun setQuantity(state: EditorState, input: Prompt.Edit, quantity: Quantity): EditorState {
        val next = state.mix.copy(quantity = quantity)
        return if (next.amounts.total > next.maximum) {
            state.copy(prompt = Prompt.Overflow(input, quantity, next.amounts.total))
        } else EditorState(next)
    }

    private fun applySliderAmount(state: EditorState, amount: BigDecimal): EditorState =
        if (amount.compareTo(state.mix.amounts.total) == 0) state
        else state.copy(mix = state.mix.copy(quantity = Quantity.Total(amount)))

    private fun sliderScale(maximum: BigDecimal): Int = if (maximum < BigDecimal.ONE) 2 else 0

    fun sliderAmount(fraction: Float, maximum: BigDecimal): BigDecimal {
        val bounded = fraction.coerceIn(0f, 1f)
        if (bounded == 1f) return maximum
        val snapPoint = (1..4).firstOrNull { kotlin.math.abs(bounded - it * .25f) <= .025f }
        if (snapPoint != null) {
            return maximum.multiply(BigDecimal.valueOf(snapPoint.toLong()))
                .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP)
        }
        val position = maximum.multiply(BigDecimal(bounded.toString()))
        val step = position.setScale(sliderScale(maximum), RoundingMode.HALF_UP)
        // A fractional maximum is an additional endpoint after the whole-unit steps.
        return if (step > maximum || maximum.subtract(position) < position.subtract(step).abs()) maximum else step
    }
}
