package de.rodeprojects.liquidratios.domain

import java.math.BigDecimal
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MixCalculatorTest {
    @Test
    fun defaultMix() {
        val mix = Mix()
        assertEquals("250", NumberText.display(mix.amounts.total))
        assertEquals(listOf("208,33", "41,67"), mix.amounts.components.map(NumberText::display))
        assertEquals("500", NumberText.display(mix.maximum))
    }

    @Test
    fun threeAndFourComponentMixes() {
        assertMix("400", listOf("200", "100", "100"), listOf("2", "1", "1"))
        assertMix("100", listOf("10", "20", "30", "40"), listOf("1", "2", "3", "4"))
    }

    @Test
    fun distributesHundredthsByRemainderAndBreaksTiesInComponentOrder() {
        assertMix("0.05", listOf("0,03", "0,02"), listOf("1", "1"))
        assertMix("0.02", listOf("0,01", "0,01", "0", "0"), listOf("1", "1", "1", "1"))
        assertMix("0.05", listOf("0,01", "0,01", "0,02", "0,01"), listOf("1", "2", "4", "3"))
    }

    @Test
    fun componentInputAnchorsTheExactValue() {
        val result = MixCalculator.calculate(ratios("2", "1", "1"), Quantity.Component(1, BigDecimal("120")))
        assertEquals("480", NumberText.display(result.total))
        assertEquals(listOf("240", "120", "120"), result.components.map(NumberText::display))
    }

    @Test
    fun componentInputRoundsTotalHalfUpAndAllocatesOnlyTheRemainder() {
        val result = MixCalculator.calculate(ratios("1", "1", "2"), Quantity.Component(2, BigDecimal("0.03")))
        assertEquals("0,06", NumberText.display(result.total))
        assertEquals(listOf("0,02", "0,01", "0,03"), result.components.map(NumberText::display))
        val tie = MixCalculator.calculate(ratios("1", "2"), Quantity.Component(1, BigDecimal("0.03")))
        assertEquals("0,05", NumberText.display(tie.total))
        assertEquals(listOf("0,02", "0,03"), tie.components.map(NumberText::display))
    }

    @Test
    fun zeroTotalAndZeroComponentBothYieldZero() {
        assertMix("0", listOf("0", "0", "0"), listOf("3", "1", "1"))
        val result = MixCalculator.calculate(ratios("4", "2"), Quantity.Component(0, BigDecimal.ZERO))
        assertEquals(listOf("0", "0"), result.components.map(NumberText::display))
    }

    @Test
    fun decimalRatiosWithWidelyDifferentScalesStayExact() {
        assertMix("0.01", listOf("0", "0", "0,01"), listOf("0.000000000001", "0.000000000001", "999999999999"))
    }

    @Test
    fun manyValidInputsPreserveNonnegativeAmountsAndExactSum() {
        val random = Random(13)
        repeat(250) {
            val ratios = List(random.nextInt(2, 5)) {
                BigDecimal.valueOf(random.nextLong(1, 1_000_000), random.nextInt(0, 7))
            }
            val amount = BigDecimal.valueOf(random.nextLong(0, 99_999_999), 2)
            val index = random.nextInt(ratios.size)
            val anchored = Quantity.Component(index, amount)
            listOf(Quantity.Total(amount), anchored).forEach { input ->
                val result = MixCalculator.calculate(ratios, input)
                assertEquals(0, result.total.compareTo(result.components.reduce(BigDecimal::add)))
                assertTrue(result.components.all { it.signum() >= 0 })
                if (input == anchored) assertEquals(amount, result.components[index])
            }
        }
    }

    @Test
    fun parsesGermanDecimalsAndFormatsWithoutTrailingZeroes() {
        assertEquals(BigDecimal("0.01"), NumberText.parse(" ,01 ", 2))
        assertEquals(BigDecimal("0.01"), NumberText.parse(".01", 2))
        assertEquals(listOf("150", "150,5", "150,26"), ratios("150.000", "150.500", "150.256").map(NumberText::display))
        assertEquals("<0,01", NumberText.ratio(BigDecimal("0.00001")))
        listOf("", "1,", "1.234", "-1", "1e3", "1 000", "1,2,3").forEach {
            assertEquals(null, NumberText.parse(it, 2))
        }
    }

    private fun assertMix(total: String, expected: List<String>, ratios: List<String>) {
        val result = MixCalculator.calculate(ratios.map(::BigDecimal), Quantity.Total(BigDecimal(total)))
        assertEquals(expected, result.components.map(NumberText::display))
        assertEquals(0, result.total.compareTo(result.components.reduce(BigDecimal::add)))
    }

    private fun ratios(vararg values: String): List<BigDecimal> = values.map(::BigDecimal)
}
