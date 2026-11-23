package com.selfuse.timesquest

import com.selfuse.timesquest.data.BattleQuestion
import com.selfuse.timesquest.data.CurriculumCatalog
import com.selfuse.timesquest.data.StageType
import com.selfuse.timesquest.engine.BattleEngine
import com.selfuse.timesquest.engine.BattlePhase
import com.selfuse.timesquest.engine.BattleVisualEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BattleEngineTest {

    private fun answerCorrectly(engine: BattleEngine, responseMs: Long = 1600L) {
        val q = engine.state.currentQuestion ?: return
        when (q) {
            is BattleQuestion.MultipleChoice -> engine.submitNumericAnswer(q.product, responseMs)
            is BattleQuestion.MissingFactorShield -> engine.submitNumericAnswer(q.expectedFactor, responseMs)
            is BattleQuestion.KeypadUltimate -> engine.submitNumericAnswer(q.product, responseMs)
            is BattleQuestion.BossCounterQte -> engine.submitNumericAnswer(q.product, responseMs)
            is BattleQuestion.CompareDuel -> engine.submitCompareChoice(q.leftIsBigger, responseMs)
            is BattleQuestion.BubbleMatchSet -> {
                q.bubbles.filter { it.isMatch }.forEach { b ->
                    engine.tapBubble(b.id, responseMs)
                }
            }
        }
    }

    @Test
    fun answeringFastClearsScoutStageWithThreeStarsAndCoins() {
        val level = CurriculumCatalog.island(1).levels.first()
        val engine = BattleEngine(
            level = level,
            heroLevel = 1,
            hasCompanionPet = true,
            masterySnapshot = emptyMap(),
            random = Random(11)
        )

        var steps = 0
        while (engine.state.phase == BattlePhase.PLAYER_TURN && steps < 30) {
            answerCorrectly(engine, responseMs = 1500L)
            steps++
        }

        assertEquals(BattlePhase.VICTORY, engine.state.phase)
        assertEquals(3, engine.state.rewardStars)
        assertTrue(engine.state.rewardCoins > level.baseCoins)
        assertTrue(engine.state.petAssistCount >= 1)
    }

    @Test
    fun mistakeShowsDotMatrixCoachAndQueuesRevengeQuestion() {
        val level = CurriculumCatalog.island(2).levels.first()
        val engine = BattleEngine(
            level = level,
            heroLevel = 1,
            hasCompanionPet = false,
            masterySnapshot = emptyMap(),
            random = Random(22)
        )

        val firstQ = engine.state.currentQuestion as BattleQuestion.MultipleChoice
        val wrongOption = firstQ.options.first { it != firstQ.product }
        engine.submitNumericAnswer(wrongOption, 3200L)

        assertEquals(BattlePhase.COACH_CORRECTION, engine.state.phase)
        assertNotNull(engine.state.activeCoach)
        assertEquals(firstQ.product, engine.state.activeCoach?.product)
        assertEquals(BattleEngine.HERO_MAX_HP - 1, engine.state.heroHp)

        engine.acknowledgeCorrection()
        assertEquals(BattlePhase.PLAYER_TURN, engine.state.phase)

        // Clear the current question and next few turns: a revenge shot for firstQ.factKey should appear!
        var sawRevenge = false
        var guard = 0
        while (engine.state.phase == BattlePhase.PLAYER_TURN && guard < 6) {
            if (engine.state.currentQuestion?.isRevenge == true &&
                engine.state.currentQuestion?.factKey == firstQ.factKey
            ) {
                sawRevenge = true
                break
            }
            answerCorrectly(engine, 2000L)
            guard++
        }
        assertTrue("Missed fact should reappear as a revenge shot", sawRevenge)
    }

    @Test
    fun bossBattleTriggersCounterQteAndUltimate() {
        val bossLevel = CurriculumCatalog.island(5).levels.first { it.stageType == StageType.BOSS }
        val engine = BattleEngine(
            level = bossLevel,
            heroLevel = 3,
            hasCompanionPet = true,
            masterySnapshot = emptyMap(),
            random = Random(33)
        )

        var sawQte = false
        var sawUltimate = false
        var steps = 0
        while (engine.state.phase == BattlePhase.PLAYER_TURN && steps < 40) {
            val q = engine.state.currentQuestion
            if (q is BattleQuestion.BossCounterQte) sawQte = true
            if (q is BattleQuestion.KeypadUltimate) sawUltimate = true
            answerCorrectly(engine, 1400L)
            steps++
        }

        assertEquals(BattlePhase.VICTORY, engine.state.phase)
        assertTrue("Boss battle should trigger at least one counter QTE", sawQte)
        assertTrue("Boss battle should trigger ultimate keypad cast", sawUltimate)
    }

    @Test
    fun potionRevivesHeroAfterDefeat() {
        val level = CurriculumCatalog.island(1).levels.first()
        val engine = BattleEngine(level, 1, false, emptyMap(), Random(55))

        repeat(BattleEngine.HERO_MAX_HP) {
            val q = engine.state.currentQuestion as BattleQuestion.MultipleChoice
            val wrong = q.options.first { it != q.product }
            engine.submitNumericAnswer(wrong, 3000L)
            engine.acknowledgeCorrection()
        }

        assertEquals(BattlePhase.DEFEAT, engine.state.phase)
        engine.reviveWithPotion()
        assertEquals(BattlePhase.PLAYER_TURN, engine.state.phase)
        assertEquals(BattleEngine.HERO_MAX_HP, engine.state.heroHp)
        assertTrue(engine.state.lastEvent is BattleVisualEvent.PotionRevive)
    }
}
