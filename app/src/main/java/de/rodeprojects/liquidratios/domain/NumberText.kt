package de.rodeprojects.liquidratios.domain

import java.math.BigDecimal
import java.math.RoundingMode

internal object NumberText {
    private val decimal = Regex("[0-9]{0,12}(?:[,.][0-9]{1,12})?")

    fun parse(input: String, maxDecimals: Int): BigDecimal? {
        val trimmed = input.trim().replace(',', '.')
        if (!decimal.matches(trimmed) || trimmed.isEmpty()) return null
        val number = trimmed.toBigDecimalOrNull() ?: return null
        return number.takeIf { it.scale() <= maxDecimals }?.stripTrailingZeros()
    }

    fun display(value: BigDecimal): String = edit(value.setScale(2, RoundingMode.HALF_UP))

    fun edit(value: BigDecimal): String = value.stripTrailingZeros().toPlainString().replace('.', ',')

    fun ratio(value: BigDecimal): String {
        val display = display(value)
        return if (display == "0") "<0,01" else display
    }
}
