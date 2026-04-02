package com.gymtracker.viewmodel

import com.gymtracker.data.MonthCount
import com.gymtracker.data.PtCarrySeed
import com.gymtracker.data.PtPurchase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PtCarryoverTest {

    @Test
    fun `builds timeline through current month with carryover`() {
        val breakdown = computePtCarryover(
            purchases = listOf(
                PtPurchase(month = "2026-01", count = 8),
                PtPurchase(month = "2026-03", count = 4)
            ),
            usedByMonth = listOf(
                MonthCount(month = "2026-01", count = 5),
                MonthCount(month = "2026-03", count = 3)
            ),
            carrySeeds = emptyList(),
            selectedMonth = "2026-04",
            currentMonth = "2026-04"
        )

        assertEquals(listOf("2026-01", "2026-02", "2026-03", "2026-04"), breakdown.map { it.month })

        assertEquals(8, breakdown[0].available)
        assertEquals(3, breakdown[0].carriedOut)

        assertEquals(3, breakdown[1].carriedIn)
        assertEquals(3, breakdown[1].available)
        assertEquals(3, breakdown[1].carriedOut)

        assertEquals(4, breakdown[2].purchased)
        assertEquals(3, breakdown[2].carriedIn)
        assertEquals(7, breakdown[2].available)
        assertEquals(3, breakdown[2].used)
        assertEquals(4, breakdown[2].carriedOut)

        assertEquals(4, breakdown[3].available)
        assertEquals(4, breakdown[3].carriedOut)
    }

    @Test
    fun `usage without purchases still creates editable visible months`() {
        val breakdown = computePtCarryover(
            purchases = emptyList(),
            usedByMonth = listOf(MonthCount(month = "2026-01", count = 1)),
            carrySeeds = emptyList(),
            selectedMonth = "2026-04",
            currentMonth = "2026-04"
        )

        assertEquals(listOf("2026-01", "2026-02", "2026-03", "2026-04"), breakdown.map { it.month })
        assertEquals(1, breakdown.first().overused)
        assertEquals(0, breakdown.first().carriedOut)
        assertTrue(breakdown.drop(1).all { it.available == 0 && it.used == 0 && it.carriedOut == 0 })
    }

    @Test
    fun `overuse is surfaced and does not create negative carryover`() {
        val breakdown = computePtCarryover(
            purchases = listOf(PtPurchase(month = "2026-02", count = 2)),
            usedByMonth = listOf(MonthCount(month = "2026-02", count = 5)),
            carrySeeds = emptyList(),
            selectedMonth = "2026-02",
            currentMonth = "2026-02"
        )

        assertEquals(1, breakdown.size)
        assertEquals(2, breakdown[0].available)
        assertEquals(5, breakdown[0].used)
        assertEquals(3, breakdown[0].overused)
        assertEquals(0, breakdown[0].carriedOut)
    }

    @Test
    fun `manual carry seed applies when earlier history is missing`() {
        val breakdown = computePtCarryover(
            purchases = listOf(PtPurchase(month = "2026-04", count = 2)),
            usedByMonth = listOf(MonthCount(month = "2026-04", count = 1)),
            carrySeeds = listOf(PtCarrySeed(month = "2026-03", count = 4)),
            selectedMonth = "2026-03",
            currentMonth = "2026-04"
        )

        assertEquals("2026-03", breakdown[0].month)
        assertEquals(4, breakdown[0].carriedIn)
        assertEquals(PtCarrySource.Manual, breakdown[0].carrySource)
        assertEquals(4, breakdown[0].carriedOut)

        assertEquals(4, breakdown[1].carriedIn)
        assertEquals(6, breakdown[1].available)
        assertEquals(5, breakdown[1].carriedOut)
    }

    @Test
    fun `manual carry seed is ignored when earlier history exists`() {
        val breakdown = computePtCarryover(
            purchases = listOf(PtPurchase(month = "2026-02", count = 3)),
            usedByMonth = listOf(MonthCount(month = "2026-02", count = 1)),
            carrySeeds = listOf(PtCarrySeed(month = "2026-03", count = 9)),
            selectedMonth = "2026-03",
            currentMonth = "2026-03"
        )

        assertEquals(2, breakdown.last().carriedIn)
        assertEquals(PtCarrySource.Derived, breakdown.last().carrySource)
        assertTrue(breakdown.last().manualCarryIgnored)
    }
}
