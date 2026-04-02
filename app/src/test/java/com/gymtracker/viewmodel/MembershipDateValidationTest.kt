package com.gymtracker.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MembershipDateValidationTest {

    @Test
    fun `allows past membership dates`() {
        assertTrue(isAllowedMembershipStartDate("2026-03-31", today = LocalDate.parse("2026-04-02")))
    }

    @Test
    fun `allows todays membership date`() {
        assertTrue(isAllowedMembershipStartDate("2026-04-02", today = LocalDate.parse("2026-04-02")))
    }

    @Test
    fun `rejects future membership dates`() {
        assertFalse(isAllowedMembershipStartDate("2026-04-03", today = LocalDate.parse("2026-04-02")))
    }

    @Test
    fun `rejects invalid membership dates`() {
        assertFalse(isAllowedMembershipStartDate("not-a-date", today = LocalDate.parse("2026-04-02")))
    }
}
