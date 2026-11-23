package com.selfuse.timesquest

import com.selfuse.timesquest.data.BattleQuestion
import com.selfuse.timesquest.data.CurriculumCatalog
import com.selfuse.timesquest.data.FactKey
import com.selfuse.timesquest.data.FactMastery
import com.selfuse.timesquest.data.StageType
import com.selfuse.timesquest.engine.AdaptiveFactScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AdaptiveFactSchedulerTest {

    @Test
    fun canonicalChineseRhymesFollowStandardRules() {
        assertEquals("一一得一", CurriculumCatalog.chineseRhyme(1, 1))
        assertEquals("二三得六", CurriculumCatalog.chineseRhyme(3, 2))
        assertEquals("二五一十", CurriculumCatalog.chineseRhyme(5, 2))
        assertEquals("三四十二", CurriculumCatalog.chineseRhyme(4, 3))
        assertEquals("二九十八", CurriculumCatalog.chineseRhyme(9, 2))
        assertEquals("三七二十一", CurriculumCatalog.chineseRhyme(7, 3))
        assertEquals("六八四十八", CurriculumCatalog.chineseRhyme(8, 6))
        assertEquals("七八五十六", CurriculumCatalog.chineseRhyme(7, 8))
        assertEquals("九九八十一", CurriculumCatalog.chineseRhyme(9, 9))
    }

    @Test
    fun allFacts45AreUniqueAndOrdered() {
        val all = CurriculumCatalog.allFacts45
        assertEquals(45, all.size)
        assertEquals(45, all.map { it.id }.toSet().size)
        assertTrue(all.all { it.minFactor <= it.maxFactor })
    }

    @Test
    fun distractorsAlwaysHave4UniquePositiveOptionsIncludingAnswer() {
        val rng = Random(42)
        for (a in 1..9) {
            for (b in 1..9) {
                val opts = AdaptiveFactScheduler.productDistractors(a, b, rng)
                assertEquals(4, opts.size)
                assertEquals(4, opts.toSet().size)
                assertTrue(opts.contains(a * b))
                assertTrue(opts.all { it in 1..81 })
            }
        }
    }

    @Test
    fun factorDistractorsAlwaysStayWithin1To9() {
        val rng = Random(7)
        for (f in 1..9) {
            val opts = AdaptiveFactScheduler.factorDistractors(f, rng)
            assertEquals(4, opts.size)
            assertEquals(4, opts.toSet().size)
            assertTrue(opts.contains(f))
            assertTrue(opts.all { it in 1..9 })
        }
    }

    @Test
    fun weakFactsAppearMoreFrequentlyThanMasteredFacts() {
        val island6 = CurriculumCatalog.island(6)
        val level = island6.levels.first()
        val weakKey = FactKey.of(7, 8)
        val strongKey = FactKey.of(7, 2)
        val mastery = mapOf(
            weakKey.id to FactMastery(weakKey, attempts = 5, correct = 1, avgResponseMs = 5500L, streak = 0, pinnedForFocus = true),
            strongKey.id to FactMastery(strongKey, attempts = 10, correct = 10, avgResponseMs = 1500L, streak = 8)
        )

        var weakHits = 0
        var strongHits = 0
        val rng = Random(1234)
        repeat(40) {
            val deck = AdaptiveFactScheduler.generateStageDeck(level, mastery, rng)
            weakHits += deck.count { it.factKey == weakKey }
            strongHits += deck.count { it.factKey == strongKey }
        }
        assertTrue("Expected weak fact ($weakHits) > mastered fact ($strongHits)", weakHits > strongHits * 2)
    }

    @Test
    fun magicTrialStageContainsBubbleCompareAndKeypadMechanics() {
        val level = CurriculumCatalog.island(4).levels.first { it.stageType == StageType.MAGIC_TRIAL }
        val deck = AdaptiveFactScheduler.generateStageDeck(level, emptyMap(), Random(99))
        assertTrue(deck.any { it is BattleQuestion.BubbleMatchSet })
        assertTrue(deck.any { it is BattleQuestion.CompareDuel })
        assertTrue(deck.any { it is BattleQuestion.KeypadUltimate })
    }

    @Test
    fun placementEvaluatorMapsSkillLevelsSensibly() {
        assertEquals(1, AdaptiveFactScheduler.evaluatePlacementIsland(1, 0, 4000L))
        assertEquals(4, AdaptiveFactScheduler.evaluatePlacementIsland(3, 0, 2500L))
        assertEquals(5, AdaptiveFactScheduler.evaluatePlacementIsland(3, 1, 3000L))
        assertEquals(7, AdaptiveFactScheduler.evaluatePlacementIsland(3, 3, 2200L))
    }
}
