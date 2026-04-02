package com.gymtracker.viewmodel

import com.gymtracker.data.PtCarrySeed
import com.gymtracker.data.PtPurchase
import com.gymtracker.data.TrainingSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCsvTest {

    @Test
    fun `backup csv round trips purchases carry seeds and membership`() {
        val payload = BackupPayload(
            sessions = listOf(
                TrainingSession(date = "2026-04-01", isPersonalTraining = false),
                TrainingSession(date = "2026-04-02", isPersonalTraining = true)
            ),
            purchases = listOf(PtPurchase(month = "2026-04", count = 5)),
            carrySeeds = listOf(PtCarrySeed(month = "2026-03", count = 2)),
            membershipStart = "2026-04-01"
        )

        val parsed = parseBackupCsv(buildBackupCsv(payload))

        assertEquals(payload.membershipStart, parsed.membershipStart)
        assertEquals(payload.sessions, parsed.sessions)
        assertEquals(payload.purchases, parsed.purchases)
        assertEquals(payload.carrySeeds, parsed.carrySeeds)
    }

    @Test
    fun `old backup without carry seed section still parses`() {
        val csv = """
            # GymTracker Backup
            # membershipStartDate=

            [sessions]
            date,isPersonalTraining
            2026-04-01,false

            [ptPurchases]
            month,count
            2026-04,3
        """.trimIndent()

        val parsed = parseBackupCsv(csv)

        assertNull(parsed.membershipStart)
        assertEquals(1, parsed.sessions.size)
        assertEquals(1, parsed.purchases.size)
        assertTrue(parsed.carrySeeds.isEmpty())
    }

    @Test
    fun `range backup injects effective carry for first exported month`() {
        val payload = buildRangeBackupPayload(
            allSessions = listOf(
                TrainingSession(date = "2026-01-10", isPersonalTraining = true),
                TrainingSession(date = "2026-03-12", isPersonalTraining = true)
            ),
            allPurchases = listOf(
                PtPurchase(month = "2026-01", count = 5),
                PtPurchase(month = "2026-03", count = 2)
            ),
            allCarrySeeds = emptyList(),
            membershipStart = "2026-01-01",
            fromDate = "2026-03-01",
            toDate = "2026-03-31"
        )

        assertEquals(listOf(TrainingSession(date = "2026-03-12", isPersonalTraining = true)), payload.sessions)
        assertEquals(listOf(PtPurchase(month = "2026-03", count = 2)), payload.purchases)
        assertEquals(listOf(PtCarrySeed(month = "2026-03", count = 4)), payload.carrySeeds)
    }

    @Test
    fun `range backup excludes ignored manual carry seeds from later months`() {
        val payload = buildRangeBackupPayload(
            allSessions = listOf(TrainingSession(date = "2026-02-15", isPersonalTraining = false)),
            allPurchases = listOf(PtPurchase(month = "2026-02", count = 3)),
            allCarrySeeds = listOf(PtCarrySeed(month = "2026-03", count = 9)),
            membershipStart = null,
            fromDate = "2026-02-01",
            toDate = "2026-03-31"
        )

        assertEquals(listOf(PtPurchase(month = "2026-02", count = 3)), payload.purchases)
        assertEquals(listOf(TrainingSession(date = "2026-02-15", isPersonalTraining = false)), payload.sessions)
        assertTrue(payload.carrySeeds.isEmpty())
    }
}
