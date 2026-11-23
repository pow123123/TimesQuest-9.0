package com.selfuse.timesquest

import com.selfuse.timesquest.data.DailyUsage
import com.selfuse.timesquest.data.ParentSettings
import com.selfuse.timesquest.engine.DailyLimitGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyLimitGuardTest {

    @Test
    fun locksWhenDailyStagesOrMinutesReachedAndUnlocksWithParentBonus() {
        val settings = ParentSettings(dailyMinutesLimit = 15, dailyStagesLimit = 3)

        val under = DailyUsage(dateIso = "2026-11-23", activeBattleSeconds = 600, stagesCompleted = 2)
        assertFalse(DailyLimitGuard.evaluate(under, settings).isLocked)

        val hitStages = under.copy(stagesCompleted = 3)
        assertTrue(DailyLimitGuard.evaluate(hitStages, settings).isLocked)

        val granted = hitStages.copy(bonusStagesGranted = 1, bonusMinutesGranted = 5)
        assertFalse(DailyLimitGuard.evaluate(granted, settings).isLocked)

        val hitMinutes = DailyUsage(dateIso = "2026-11-23", activeBattleSeconds = 15 * 60, stagesCompleted = 1)
        assertTrue(DailyLimitGuard.evaluate(hitMinutes, settings).isLocked)
    }

    @Test
    fun zeroLimitsDisableLock() {
        val unlimited = ParentSettings(dailyMinutesLimit = 0, dailyStagesLimit = 0)
        val heavy = DailyUsage(dateIso = "2026-11-23", activeBattleSeconds = 3600, stagesCompleted = 20)
        assertFalse(DailyLimitGuard.evaluate(heavy, unlimited).isLocked)
    }
}
