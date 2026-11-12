package de.rodeprojects.liquidratios.domain

import de.rodeprojects.liquidratios.domain.EditorAction.Cancel
import de.rodeprojects.liquidratios.domain.EditorAction.Confirm
import de.rodeprojects.liquidratios.domain.EditorAction.CorrectInput
import de.rodeprojects.liquidratios.domain.EditorAction.OpenField
import de.rodeprojects.liquidratios.domain.EditorAction.RaiseLimit
import de.rodeprojects.liquidratios.domain.EditorAction.SelectCount
import de.rodeprojects.liquidratios.domain.EditorAction.SelectUnit
import de.rodeprojects.liquidratios.domain.EditorAction.Slide
import de.rodeprojects.liquidratios.domain.EditorAction.Type
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MixEditorTest {
    private val initial = EditorState()

    @Test
    fun manualChangesOnlyApplyAfterConfirmation() {
        val draft = initial.act(OpenField(InputField.Total), Type("321,27"))
        assertEquals(initial.mix, draft.mix)
        val updated = draft.act(Confirm)
        assertEquals("321,27", NumberText.display(updated.mix.amounts.total))
        assertEquals(listOf("267,73", "53,54"), updated.mix.amounts.components.map(NumberText::display))
        assertNull(updated.prompt)
    }

    @Test
    fun emptyIncompleteAndInvalidDraftsKeepCommittedMix() {
        val editing = initial.act(OpenField(InputField.Ratio(0)))
        listOf("", "1,", "0", "-1", "abc").forEach { draft ->
            val invalid = editing.act(Type(draft), Confirm)
            assertEquals(initial.mix, invalid.mix)
            assertEquals(InputError.RATIO, (invalid.prompt as Prompt.Edit).error)
            assertEquals(initial, invalid.act(Cancel))
        }
    }

    @Test
    fun hiddenRatiosSurviveCountAndRatioChangesKeepTotal() {
        val two = initial.act(
            SelectCount(4),
            OpenField(InputField.Ratio(2)), Type("3,25"), Confirm,
            OpenField(InputField.Ratio(3)), Type("4"), Confirm,
            SelectCount(2),
        )
        assertEquals(2, two.mix.amounts.components.size)
        val restored = two.act(SelectCount(4)).mix
        assertEquals(listOf("5", "1", "3,25", "4"), restored.activeRatios.map(NumberText::display))
        assertEquals("250", NumberText.display(restored.amounts.total))
    }

    @Test
    fun switchUnitRequiresConfirmationAndKeepsAmountsAndAnchor() {
        val anchored = initial.act(OpenField(InputField.Component(1)), Type("50"), Confirm)
        val pending = anchored.act(SelectUnit(MeasureUnit.MILLILITERS))
        assertEquals(anchored.mix, pending.mix)
        assertEquals(anchored, pending.act(Cancel))
        val switched = pending.act(Confirm).mix
        assertEquals(MeasureUnit.MILLILITERS, switched.unit)
        assertEquals(anchored.mix.amounts, switched.amounts)
        assertEquals(anchored.mix.quantity, switched.quantity)
        assertEquals(anchored.mix.maximum, switched.maximum)
    }

    @Test
    fun overflowCanBeCancelledOrCorrectedInOriginalEditor() {
        val overflow = initial.act(OpenField(InputField.Total), Type("600"), Confirm)
        val prompt = overflow.prompt as Prompt.Overflow
        assertEquals("600", NumberText.display(prompt.total))
        assertEquals(initial, overflow.act(Cancel))
        val editing = overflow.act(CorrectInput)
        assertEquals("600", (editing.prompt as Prompt.Edit).draft)
        assertEquals("450", NumberText.display(editing.act(Type("450"), Confirm).mix.amounts.total))
    }

    @Test
    fun overflowAppliesNewLimitAndAnchoredInputAtomically() {
        val overflow = initial.act(OpenField(InputField.Component(1)), Type("100"), Confirm)
        assertEquals(initial.mix, overflow.mix)
        assertEquals("600", NumberText.display((overflow.prompt as Prompt.Overflow).total))
        val tooLow = overflow.act(RaiseLimit, Type("599,99"), Confirm)
        assertEquals(InputError.BELOW_TOTAL, (tooLow.prompt as Prompt.Edit).error)
        assertEquals(initial.mix, tooLow.mix)
        assertEquals(overflow, tooLow.act(Cancel))
        val valid = tooLow.act(Type("625"), Confirm).mix
        assertEquals("625", NumberText.display(valid.maximum))
        assertEquals("600", NumberText.display(valid.amounts.total))
        assertEquals("100", NumberText.display(valid.amounts.components[1]))
    }

    @Test
    fun standaloneLimitCannotGoBelowMixAndValidLimitKeepsMix() {
        val invalid = initial.act(OpenField(InputField.Maximum), Type("249,99"), Confirm)
        assertEquals(InputError.BELOW_TOTAL, (invalid.prompt as Prompt.Edit).error)
        assertEquals(initial.mix, invalid.mix)
        val valid = invalid.act(Type("250"), Confirm)
        assertEquals("250", NumberText.display(valid.mix.maximum))
        assertEquals(initial.mix.amounts, valid.mix.amounts)
    }

    @Test
    fun sliderUpdatesLiveAndSupportsFractionalEndpointsAndSubunitScale() {
        assertEquals("260", NumberText.display(initial.act(Slide(.52f)).mix.amounts.total))
        val small = initial.act(OpenField(InputField.Total), Type("0"), Confirm, OpenField(InputField.Maximum), Type("0,5"), Confirm)
        assertEquals("0,17", NumberText.display(small.act(Slide(.34f)).mix.amounts.total))
        assertEquals("0,5", NumberText.display(small.act(Slide(1f)).mix.amounts.total))
        assertEquals("1", NumberText.display(MixEditor.sliderAmount(.8f, BigDecimal("1.24"))))
        assertEquals("1,24", NumberText.display(MixEditor.sliderAmount(.97f, BigDecimal("1.24"))))
    }

    @Test
    fun sliderSnapsToQuarterPointsWithFractionalLimits() {
        val maximum = BigDecimal("501.01")
        listOf("125,25", "250,51", "375,76", "501,01").forEachIndexed { index, expected ->
            val point = (index + 1) * .25f
            assertEquals(expected, NumberText.display(MixEditor.sliderAmount(point - .01f, maximum)))
            assertEquals(expected, NumberText.display(MixEditor.sliderAmount(point, maximum)))
        }
        assertEquals("125,25", NumberText.display(MixEditor.sliderAmount(.232f, maximum)))
        assertEquals("0,13", NumberText.display(MixEditor.sliderAmount(.25f, BigDecimal("0.5"))))
        assertEquals("0,5", NumberText.display(MixEditor.sliderAmount(1f, BigDecimal("0.5"))))
    }

    @Test
    fun rapidChangesDoNotAccumulateRoundingError() {
        var state = initial.act(OpenField(InputField.Total), Type("0,05"), Confirm)
        repeat(100) {
            state = state.act(SelectCount(3), SelectCount(4), SelectCount(2))
        }
        assertEquals("0,05", NumberText.display(state.mix.amounts.total))
        assertEquals(listOf("0,04", "0,01"), state.mix.amounts.components.map(NumberText::display))
        assertTrue(state.prompt == null)
    }

    @Test
    fun keyboardStepsFromFractionalInputAndRespectsBothBounds() {
        val manual = initial.act(OpenField(InputField.Total), Type("250,45"), Confirm)
        assertEquals("250", NumberText.display(manual.act(EditorAction.Step(false)).mix.amounts.total))
        assertEquals("251", NumberText.display(manual.act(EditorAction.Step(true)).mix.amounts.total))
        val small = initial.act(OpenField(InputField.Total), Type("0"), Confirm, OpenField(InputField.Maximum), Type("0,02"), Confirm)
        assertEquals(small, small.act(EditorAction.Step(false)))
        val actions = List(3) { EditorAction.Step(true) }.toTypedArray()
        assertEquals("0,02", NumberText.display(small.act(*actions).mix.amounts.total))
    }

    private fun EditorState.act(vararg actions: EditorAction): EditorState =
        actions.fold(this, MixEditor::reduce)
}
