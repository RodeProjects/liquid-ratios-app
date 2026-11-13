package de.rodeprojects.liquidratios.domain

import java.math.BigDecimal
import java.math.RoundingMode

internal const val MIN_COMPONENTS = 2
internal const val MAX_COMPONENTS = 4

internal sealed interface Quantity {
    val amount: BigDecimal

    data class Total(override val amount: BigDecimal) : Quantity
    data class Component(val index: Int, override val amount: BigDecimal) : Quantity
}

internal data class Mix(
    val count: Int = MIN_COMPONENTS,
    val ratios: List<BigDecimal> = listOf(BigDecimal("5"), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE),
    val quantity: Quantity = Quantity.Total(BigDecimal("250")),
    val maximum: BigDecimal = BigDecimal("500"),
) {
    val activeRatios: List<BigDecimal> get() = ratios.take(count)
    val amounts: MixtureAmounts by lazy(LazyThreadSafetyMode.NONE) {
        MixCalculator.calculate(activeRatios, quantity)
    }

    fun changeCount(value: Int): Mix = if (value == count) this else copy(
        count = value,
        quantity = Quantity.Total(amounts.total),
    )

    fun changeRatio(index: Int, value: BigDecimal): Mix = if (ratios[index].compareTo(value) == 0) this else copy(
        ratios = ratios.mapIndexed { i, ratio -> if (i == index) value else ratio },
        quantity = Quantity.Total(amounts.total),
    )
}

internal data class MixtureAmounts(val total: BigDecimal, val components: List<BigDecimal>)

internal object MixCalculator {
    fun calculate(ratios: List<BigDecimal>, quantity: Quantity): MixtureAmounts {
        val ratioSum = ratios.reduce(BigDecimal::add)
        return when (quantity) {
            is Quantity.Total -> MixtureAmounts(quantity.amount, distribute(quantity.amount, ratios))
            is Quantity.Component -> {
                val total = quantity.amount.multiply(ratioSum)
                    .divide(ratios[quantity.index], 2, RoundingMode.HALF_UP)
                val remaining = total.subtract(quantity.amount)
                val others = distribute(remaining, ratios.filterIndexed { i, _ -> i != quantity.index })
                val amounts = others.toMutableList().apply { add(quantity.index, quantity.amount) }
                MixtureAmounts(total, amounts)
            }
        }
    }

    private fun distribute(amount: BigDecimal, ratios: List<BigDecimal>): List<BigDecimal> {
        val cents = amount.movePointRight(2)
        val sum = ratios.reduce(BigDecimal::add)
        // The remainder denominators are identical, so the exact remainders can be compared directly.
        val allocations = ratios.map { cents.multiply(it).divideAndRemainder(sum) }
        val missing = cents.subtract(allocations.sumOf { it[0] }).intValueExact()
        val extra = allocations.indices.sortedWith(
            compareByDescending<Int> { allocations[it][1] }.thenBy { it },
        ).take(missing).toSet()
        return allocations.mapIndexed { index, allocation ->
            val share = if (index in extra) allocation[0].add(BigDecimal.ONE) else allocation[0]
            share.movePointLeft(2)
        }
    }
}
