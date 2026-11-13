package de.rodeprojects.liquidratios

import android.content.res.Configuration
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import de.rodeprojects.liquidratios.domain.EditorState
import de.rodeprojects.liquidratios.domain.MixEditor
import de.rodeprojects.liquidratios.ui.LiquidRatiosTheme
import de.rodeprojects.liquidratios.ui.MixScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MixScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun showApp() {
        val current = mutableStateOf(EditorState())
        compose.setContent {
            LiquidRatiosTheme {
                MixScreen(AppState(current.value)) { action ->
                    current.value = MixEditor.reduce(current.value, action)
                }
            }
        }
    }

    @Test
    fun defaultAndHiddenRatiosWhenChangingCount() {
        amount("total", "250 g")
        amount("component-0", "208,33 g")
        amount("component-1", "41,67 g")
        click("count-4")
        compose.onNodeWithTag("count-4").assertIsSelected()
        enter("ratio-2", "2,5")
        enter("ratio-3", "4")
        click("count-2")
        compose.onNodeWithTag("ratio-2").assertDoesNotExist()
        click("count-3")
        compose.onNodeWithTag("ratio-2").assertTextContains("2,5")
        click("count-4")
        compose.onNodeWithTag("ratio-3").assertTextContains("4")
        amount("total", "250 g")
    }

    @Test
    fun componentCountSelectorHasEqualVisibleGapsToLabelAndBurger() {
        val row = compose.onNodeWithTag("component-count-row").fetchSemanticsNode().boundsInRoot
        val labelNode = compose.onNodeWithTag("component-count-label").fetchSemanticsNode()
        val label = labelNode.boundsInRoot
        val labelText = labelNode.config[SemanticsProperties.Text].joinToString("") { it.text }
        val selector = compose.onNodeWithTag("component-count-control").fetchSemanticsNode().boundsInRoot
        val burger = compose.onNodeWithTag("burger-icon", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val menu = compose.onNodeWithTag("settings").fetchSemanticsNode().boundsInRoot
        val leftGap = selector.left - label.right
        val rightGap = burger.left - selector.right
        val rowCenteredLeft = row.center.x - selector.width / 2 - label.right
        val rowCenteredRight = burger.left - row.center.x - selector.width / 2
        val minimumGapPx = with(compose.density) { 8.dp.toPx() }
        val isLandscape = InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE
        val canCenterAcrossLandscapeRow = isLandscape && '\n' !in labelText &&
            rowCenteredLeft >= minimumGapPx && rowCenteredRight >= minimumGapPx
        android.util.Log.i(
            "HeaderSpacing",
            "row=${row.left}..${row.right} label=${label.left}..${label.right} selector=${selector.left}..${selector.right} " +
                "burger=${burger.left}..${burger.right} menu=${menu.left}..${menu.right} gaps=$leftGap/$rightGap",
        )
        if (canCenterAcrossLandscapeRow) {
            assertEquals(row.center.x, selector.center.x, 0.01f)
        } else {
            assertEquals(leftGap, rightGap, 0.01f)
        }
    }

    @Test
    fun aboutMenuShowsTheInstalledVersion() {
        compose.onNodeWithText("LiquidRatios").assertDoesNotExist()
        click("settings")
        compose.onNodeWithText("Über LiquidRatios").performClick()
        compose.onNodeWithText("Über LiquidRatios").assertIsDisplayed()
        compose.onNodeWithText("Version ${BuildConfig.VERSION_NAME}").assertIsDisplayed()
        compose.onNodeWithText("Schließen").performClick()
        compose.onNodeWithText("Über LiquidRatios").assertDoesNotExist()
    }

    @Test
    fun invalidDraftAndCancelPreserveCommittedValues() {
        click("ratio-0")
        type("")
        click("confirm")
        compose.onNodeWithText("Gib einen Verhältniswert größer als 0", substring = true).assertIsDisplayed()
        type("0")
        click("confirm")
        compose.onNodeWithText("Gib einen Verhältniswert größer als 0", substring = true).assertIsDisplayed()
        type("4,")
        click("confirm")
        compose.onNodeWithText("Gib einen Verhältniswert größer als 0", substring = true).assertIsDisplayed()
        click("cancel")
        compose.onNodeWithTag("ratio-0").assertTextContains("5")
        amount("total", "250 g")
    }

    @Test
    fun keyboardDoneCommitsAndComponentInputUpdatesTotal() {
        click("count-3")
        enter("ratio-0", "2")
        click("component-1")
        type("120")
        compose.onNodeWithTag("number-input").performImeAction()
        amount("total", "480 g")
        amount("component-0", "240 g")
        amount("component-1", "120 g")
        amount("component-2", "120 g")
    }

    @Test
    fun overflowCancelLeavesValuesUntouchedAndCorrectionReopensDraft() {
        enter("total", "700")
        compose.onNodeWithText("Berechnete Gesamtmenge: 700 g").assertIsDisplayed()
        compose.onNodeWithText("Bisherige Grenze: 500 g").assertIsDisplayed()
        click("cancel")
        amount("total", "250 g")
        enter("total", "620")
        click("correct-input")
        compose.onNodeWithTag("number-input").assertTextContains("620")
        type("420,25")
        click("confirm")
        amount("total", "420,25 g")
        amount("maximum", "500 g")
    }

    @Test
    fun overflowCanOnlyCommitTogetherWithSufficientLimit() {
        compose.onNodeWithTag("component-1").performScrollTo().performTouchInput {
            click(Offset(width - 18.dp.toPx(), centerY))
        }
        type("120")
        click("confirm")
        compose.onNodeWithText("Berechnete Gesamtmenge: 720 g").assertIsDisplayed()
        click("raise-limit")
        type("700")
        click("confirm")
        compose.onNodeWithText("Die Grenze muss mindestens 720 g betragen.").assertIsDisplayed()
        click("cancel")
        compose.onNodeWithText("Bisherige Grenze: 500 g").assertIsDisplayed()
        click("raise-limit")
        type("900")
        click("confirm")
        amount("total", "720 g")
        amount("maximum", "900 g")
        amount("component-1", "120 g")
    }

    @Test
    fun sliderUpdatesMixAndUsesFineStepsBelowOneUnit() {
        enter("total", "0")
        enter("maximum", "0,5")
        val slider = compose.onNodeWithTag("amount-slider").performScrollTo()
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(.34f) }
        amount("total", "0,17 g")
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        amount("total", "0,5 g")
    }

    @Test
    fun sliderSnapsToQuarterAmounts() {
        val slider = compose.onNodeWithTag("amount-slider").performScrollTo()
        listOf(.24f to "125 g", .51f to "250 g", .74f to "375 g", .99f to "500 g")
            .forEach { (fraction, expected) ->
                slider.performSemanticsAction(SemanticsActions.SetProgress) { it(fraction) }
                amount("total", expected)
            }
        enter("total", "123")
        amount("total", "123 g")
        val progress = slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
        assertEquals(.246f, progress, .001f)
    }

    @Test
    fun draggingCanEnterAndLeaveASnapPoint() {
        val slider = compose.onNodeWithTag("amount-slider")
        slider.performTouchInput {
            down(center)
            moveTo(Offset(width * .245f, centerY), delayMillis = 200)
        }
        amount("total", "125 g")
        slider.performTouchInput {
            moveTo(Offset(width * .29f, centerY), delayMillis = 200)
        }
        val value = slider.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            .substringBefore(' ').toInt()
        assertTrue(value in 130..150)
        slider.performTouchInput { up() }
        amount("total", "$value g")
    }

    @Test
    fun vesselPercentagesReflectDisplayedAmountsAndZeroTotal() {
        compose.onNodeWithText("83,33 %", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("16,67 %", substring = true).assertIsDisplayed()
        enter("total", "0")
        compose.onAllNodesWithText("0 %").assertCountEquals(2)
    }

    @Test
    fun touchDragUpdatesBeforeReleaseAndReachesTheMaximum() {
        val slider = compose.onNodeWithTag("amount-slider")
        slider.performTouchInput {
            down(center)
            moveTo(Offset(width * .8f, centerY), delayMillis = 120)
        }
        val duringDrag = slider.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            .substringBefore(' ').toInt()
        assertTrue(duringDrag in 350..450)
        amount("total", "$duringDrag g")
        slider.performTouchInput {
            moveTo(Offset(width - 1f, centerY), delayMillis = 120)
            up()
        }
        amount("total", "500 g")
    }

    @Test
    fun rapidTransitionsThenConfirmedResetReturnToDefaults() {
        for (count in listOf(4, 3, 2, 4, 2, 3)) click("count-$count")
        enter("total", "10")
        click("settings")
        compose.onNodeWithText("Standardwerte zurücksetzen").performClick()
        click("cancel")
        amount("total", "10 g")
        click("settings")
        compose.onNodeWithText("Standardwerte zurücksetzen").performClick()
        click("confirm")
        compose.onNodeWithTag("count-2").assertIsSelected()
        amount("total", "250 g")
    }

    private fun enter(tag: String, value: String) {
        click(tag)
        type(value)
        click("confirm")
    }

    private fun type(value: String) {
        compose.onNodeWithTag("number-input").performTextReplacement(value)
    }

    private fun click(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (tag in listOf("maximum", "total") || tag.startsWith("component-") || tag.startsWith("ratio-")) {
            node.performScrollTo()
        }
        node.performClick()
    }

    private fun amount(tag: String, expected: String) {
        compose.onNodeWithTag(tag).assertTextContains(expected)
    }
}
