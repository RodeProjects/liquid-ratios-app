package de.rodeprojects.liquidratios.ui

import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Test

class VesselLabelsTest {
    @Test
    fun targetsStayWithinAvailableWidthForCloselyPackedLayers() {
        val anchors = listOf(100f, 100.2f, 100.4f, 100.6f)
        val labels = listOf(100f, 120f, 140f, 160f)
        val bases = listOf(22f, 22f, 22f, 22f)
        val maxima = List(4) { 45f }
        val routes = arrangeLabelRoutes(anchors, labels, bases, maxima)
        routes.lengths.forEachIndexed { index, length ->
            assertTrue("Point exceeds the vessel center", length <= maxima[index] + .0001f)
        }
    }

    @Test
    fun angledRoutesStaySeparateAndInsideTheVessel() {
        val anchors = listOf(90f, 91f, 92f, 180f)
        val labels = listOf(90f, 110f, 130f, 180f)
        val bases = listOf(20f, 20f, 20f, 21f)
        val maxima = List(4) { 100f }
        val routes = arrangeLabelRoutes(anchors, labels, bases, maxima)
        assertTrue(routes.angled)
        routes.lengths.forEachIndexed { index, length ->
            assertTrue(length <= maxima[index])
            val delta = abs(anchors[index] - labels[index])
            assertTrue(length >= delta)
        }
        for (first in 0 until anchors.lastIndex) {
            for (second in first + 1 until anchors.size) {
                val end = minOf(routes.lengths[first], routes.lengths[second])
                fun y(row: Int, x: Float): Float {
                    val delta = anchors[row] - labels[row]
                    val bend = (routes.lengths[row] - abs(delta)).coerceAtLeast(0f)
                    if (x <= bend || delta == 0f) return labels[row]
                    return labels[row] + delta * (x - bend) / (routes.lengths[row] - bend)
                }
                val bend1 = (routes.lengths[first] - abs(anchors[first] - labels[first])).coerceIn(0f, end)
                val bend2 = (routes.lengths[second] - abs(anchors[second] - labels[second])).coerceIn(0f, end)
                listOf(0f, bend1, bend2, end).forEach { x ->
                    assertTrue("Angled lines cross", y(first, x) < y(second, x))
                }
            }
        }
    }

    @Test
    fun pointsKeepConnectingLinesSeparateWhenLabelsMoveUpOrDown() {
        val cases = listOf(
            listOf(30.52f, 32.88f, 34.82f, 44.39f) to listOf(30.52f, 50.52f, 70.52f, 90.52f),
            listOf(235f, 236f, 237f, 238f) to listOf(187f, 207f, 227f, 247f),
            listOf(15f, 16f, 17f, 18f) to listOf(16f, 36f, 56f, 76f),
        )
        cases.forEach { (anchors, labels) ->
            val baseLengths = anchors.map { 20f + it * .006f }
            val offsets = arrangePointOffsets(anchors, labels, baseLengths)
            for (first in 0 until anchors.lastIndex) {
                for (second in first + 1 until anchors.size) {
                    val length1 = baseLengths[first] + offsets[first]
                    val length2 = baseLengths[second] + offsets[second]
                    val x = minOf(length1, length2)
                    val y1 = labels[first] + (anchors[first] - labels[first]) * x / length1
                    val y2 = labels[second] + (anchors[second] - labels[second]) * x / length2
                    assertTrue("Connecting lines cross", y1 <= y2 + .0001f)
                    if (anchors[second] - anchors[first] < 6f) {
                        assertTrue("Points overlap", abs(length1 - length2) >= 4f)
                    }
                }
            }
        }
    }
}
