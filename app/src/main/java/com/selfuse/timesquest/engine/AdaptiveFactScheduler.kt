package com.selfuse.timesquest.engine

import com.selfuse.timesquest.data.BattleQuestion
import com.selfuse.timesquest.data.BubbleOption
import com.selfuse.timesquest.data.CurriculumCatalog
import com.selfuse.timesquest.data.FactKey
import com.selfuse.timesquest.data.FactMastery
import com.selfuse.timesquest.data.LevelDef
import com.selfuse.timesquest.data.MasteryLevel
import com.selfuse.timesquest.data.PlacementQuestion
import com.selfuse.timesquest.data.StageType
import kotlin.math.abs
import kotlin.random.Random

/**
 * Pure-Kotlin adaptive question generator and spaced-repetition scheduler.
 * Blends 60% current-island target facts, 25% weak/pinned facts, and 15% confidence review facts.
 */
object AdaptiveFactScheduler {

    /**
     * Generates a curated deck of RPG battle questions tailored to the level type and mastery map.
     */
    fun generateStageDeck(
        level: LevelDef,
        mastery: Map<String, FactMastery>,
        random: Random = Random.Default
    ): List<BattleQuestion> {
        val island = CurriculumCatalog.island(level.islandId)
        val targetPool = factsForTables(island.targetTables)
        val unlockedTables = CurriculumCatalog.islands
            .filter { it.id <= level.islandId }
            .flatMap { it.targetTables }
            .distinct()
        val reviewPool = factsForTables(unlockedTables)

        val total = level.questionCount
        val targetQuota = ((total * 0.60f).toInt()).coerceAtLeast(1)
        val weakQuota = ((total * 0.25f).toInt()).coerceAtLeast(1)
        val chosenKeys = mutableListOf<FactKey>()

        // 1. Target island facts (weighted toward unseen/weak inside this island)
        repeat(targetQuota) {
            chosenKeys += pickWeighted(targetPool, mastery, chosenKeys.takeLast(2), preferWeak = true, random = random)
        }

        // 2. Weak & parent-pinned facts across unlocked tables (secret adaptive reinforcement)
        val weakCandidates = reviewPool.filter { key ->
            val m = mastery[key.id]
            m != null && (m.pinnedForFocus || m.masteryLevel == MasteryLevel.NEEDS_FOCUS)
        }.ifEmpty { targetPool }

        repeat(weakQuota) {
            chosenKeys += pickWeighted(weakCandidates, mastery, chosenKeys.takeLast(2), preferWeak = true, random = random)
        }

        // 3. Confidence / fluency facts to keep combo energy high
        val confidenceCandidates = reviewPool.filter { key ->
            val m = mastery[key.id]
            m == null || m.masteryLevel != MasteryLevel.NEEDS_FOCUS
        }.ifEmpty { reviewPool }

        while (chosenKeys.size < total) {
            chosenKeys += pickWeighted(confidenceCandidates, mastery, chosenKeys.takeLast(2), preferWeak = false, random = random)
        }

        // Shuffle gently while keeping the opener approachable
        val shuffled = chosenKeys.shuffled(random).toMutableList()
        val easyIdx = shuffled.indexOfFirst { key ->
            val m = mastery[key.id]
            m == null || m.masteryLevel != MasteryLevel.NEEDS_FOCUS
        }
        if (easyIdx > 0) {
            val first = shuffled.removeAt(easyIdx)
            shuffled.add(0, first)
        }

        return shuffled.mapIndexed { idx, key ->
            buildQuestionForSlot(
                key = key,
                slotIndex = idx,
                totalSlots = shuffled.size,
                stageType = level.stageType,
                islandId = level.islandId,
                candidatePool = reviewPool,
                random = random
            )
        }
    }

    fun factsForTables(tables: List<Int>): List<FactKey> {
        val set = LinkedHashSet<FactKey>()
        for (t in tables) {
            for (other in 1..9) {
                set += FactKey.of(t, other)
            }
        }
        return set.toList()
    }

    /**
     * Generates a revenge question (swapping factor order when possible) for a missed fact.
     */
    fun buildRevengeQuestion(
        key: FactKey,
        preferShield: Boolean = false,
        random: Random = Random.Default
    ): BattleQuestion {
        val (a, b) = orientFactors(key, random)
        return if (preferShield && a > 1 && b > 1) {
            val hideLeft = random.nextBoolean()
            val expected = if (hideLeft) a else b
            BattleQuestion.MissingFactorShield(
                factKey = key,
                leftFactor = a,
                rightFactor = b,
                hideLeft = hideLeft,
                options = factorDistractors(expected, random),
                isRevenge = true
            )
        } else {
            BattleQuestion.MultipleChoice(
                factKey = key,
                leftFactor = a,
                rightFactor = b,
                options = productDistractors(a, b, random),
                isRevenge = true
            )
        }
    }

    /**
     * Generates a Boss Enrage Counter QTE question (4-choice with tight timer).
     */
    fun buildBossCounterQte(
        islandId: Int,
        mastery: Map<String, FactMastery>,
        random: Random = Random.Default
    ): BattleQuestion.BossCounterQte {
        val island = CurriculumCatalog.island(islandId)
        val pool = factsForTables(island.targetTables)
        val key = pickWeighted(pool, mastery, emptyList(), preferWeak = true, random = random)
        val (a, b) = orientFactors(key, random)
        return BattleQuestion.BossCounterQte(
            factKey = key,
            leftFactor = a,
            rightFactor = b,
            options = productDistractors(a, b, random),
            timeLimitMs = 4200L
        )
    }

    /**
     * Generates an Ultimate Keypad question when hero's energy reaches 100%.
     */
    fun buildUltimateKeypad(
        key: FactKey,
        random: Random = Random.Default
    ): BattleQuestion.KeypadUltimate {
        val (a, b) = orientFactors(key, random)
        return BattleQuestion.KeypadUltimate(
            factKey = key,
            leftFactor = a,
            rightFactor = b,
            isUltimateCast = true
        )
    }

    /**
     * Generates 6 onboarding placement questions: 3 easy (2~5) + 3 harder (6~9).
     */
    fun generatePlacementQuiz(random: Random = Random.Default): List<PlacementQuestion> {
        val easyPairs = listOf(2 to 6, 3 to 5, 4 to 7, 3 to 8, 5 to 6, 4 to 4).shuffled(random).take(3)
        val hardPairs = listOf(6 to 7, 7 to 8, 6 to 8, 7 to 9, 8 to 9, 9 to 6).shuffled(random).take(3)
        val easyQs = easyPairs.map { (a, b) ->
            PlacementQuestion(a, b, productDistractors(a, b, random), isHardTier = false)
        }
        val hardQs = hardPairs.map { (a, b) ->
            PlacementQuestion(a, b, productDistractors(a, b, random), isHardTier = true)
        }
        return easyQs + hardQs
    }

    /**
     * Maps 6-question placement test results to a recommended starting unlocked island (1..7).
     */
    fun evaluatePlacementIsland(
        easyCorrect: Int,
        hardCorrect: Int,
        avgResponseMs: Long
    ): Int {
        return when {
            easyCorrect <= 1 -> 1
            easyCorrect == 2 && hardCorrect == 0 -> 2
            hardCorrect == 0 -> 4
            hardCorrect == 1 -> 5
            hardCorrect == 2 -> 6
            hardCorrect >= 3 && avgResponseMs <= 3500L -> 7
            else -> 6
        }
    }

    /**
     * Generates 4 distinct positive product options including the true answer and plausible nearby mistakes.
     */
    fun productDistractors(a: Int, b: Int, random: Random = Random.Default): List<Int> {
        val answer = a * b
        val candidates = LinkedHashSet<Int>()

        // Plausible nearby table mistakes (off by one step in either factor)
        val nearby = listOf(
            (a + 1) * b,
            (a - 1) * b,
            a * (b + 1),
            a * (b - 1),
            answer + 2,
            answer - 2,
            answer + 10,
            answer - 10,
            a + b // Classic addition slip!
        ).filter { it in 1..81 && it != answer }.shuffled(random)

        for (v in nearby) {
            if (candidates.size < 3) candidates += v
        }
        var delta = 1
        while (candidates.size < 3) {
            val up = answer + delta
            val down = answer - delta
            if (up in 1..81 && up != answer) candidates += up
            if (candidates.size < 3 && down in 1..81 && down != answer) candidates += down
            delta++
        }
        return (candidates.take(3) + answer).shuffled(random)
    }

    /**
     * Generates 4 distinct factor options (1..9) for missing-factor shield questions.
     */
    fun factorDistractors(expected: Int, random: Random = Random.Default): List<Int> {
        val near = listOf(expected - 1, expected + 1, expected - 2, expected + 2)
            .filter { it in 1..9 && it != expected }
            .shuffled(random)
        val out = LinkedHashSet<Int>()
        for (v in near) {
            if (out.size < 3) out += v
        }
        val fallback = (1..9).filter { it != expected }.shuffled(random)
        for (v in fallback) {
            if (out.size < 3) out += v
        }
        return (out.take(3) + expected).shuffled(random)
    }

    private fun buildQuestionForSlot(
        key: FactKey,
        slotIndex: Int,
        totalSlots: Int,
        stageType: StageType,
        islandId: Int,
        candidatePool: List<FactKey>,
        random: Random
    ): BattleQuestion {
        val (a, b) = orientFactors(key, random)
        return when (stageType) {
            StageType.SCOUT -> {
                if (islandId >= 3 && slotIndex == totalSlots - 1) {
                    BattleQuestion.KeypadUltimate(key, a, b, isUltimateCast = false)
                } else {
                    BattleQuestion.MultipleChoice(key, a, b, productDistractors(a, b, random))
                }
            }

            StageType.SHIELD_ELITE -> {
                if (slotIndex % 2 == 1) {
                    val hideLeft = random.nextBoolean()
                    val expected = if (hideLeft) a else b
                    BattleQuestion.MissingFactorShield(
                        factKey = key,
                        leftFactor = a,
                        rightFactor = b,
                        hideLeft = hideLeft,
                        options = factorDistractors(expected, random)
                    )
                } else {
                    BattleQuestion.MultipleChoice(key, a, b, productDistractors(a, b, random))
                }
            }

            StageType.MAGIC_TRIAL -> {
                when (slotIndex % 3) {
                    0 -> buildBubbleMatch(key, candidatePool, random)
                    1 -> buildCompareDuel(key, candidatePool, random)
                    else -> BattleQuestion.KeypadUltimate(key, a, b, isUltimateCast = false)
                }
            }

            StageType.BOSS -> {
                when {
                    slotIndex == totalSlots - 1 -> BattleQuestion.KeypadUltimate(key, a, b, isUltimateCast = true)
                    slotIndex % 3 == 1 -> {
                        val hideLeft = random.nextBoolean()
                        val expected = if (hideLeft) a else b
                        BattleQuestion.MissingFactorShield(
                            factKey = key,
                            leftFactor = a,
                            rightFactor = b,
                            hideLeft = hideLeft,
                            options = factorDistractors(expected, random)
                        )
                    }
                    else -> BattleQuestion.MultipleChoice(key, a, b, productDistractors(a, b, random))
                }
            }

            StageType.ENDLESS -> {
                when (slotIndex % 5) {
                    1 -> {
                        val hideLeft = random.nextBoolean()
                        val expected = if (hideLeft) a else b
                        BattleQuestion.MissingFactorShield(key, a, b, hideLeft, factorDistractors(expected, random))
                    }
                    3 -> buildCompareDuel(key, candidatePool, random)
                    4 -> BattleQuestion.KeypadUltimate(key, a, b, isUltimateCast = false)
                    else -> BattleQuestion.MultipleChoice(key, a, b, productDistractors(a, b, random))
                }
            }
        }
    }

    private fun buildBubbleMatch(
        anchor: FactKey,
        pool: List<FactKey>,
        random: Random
    ): BattleQuestion.BubbleMatchSet {
        // Prefer products that have multiple factor pairs or clean commutative pair (a != b)
        val preferredAnchor = if (anchor.minFactor == anchor.maxFactor) {
            pool.firstOrNull { it.minFactor != it.maxFactor } ?: anchor
        } else {
            anchor
        }
        val target = preferredAnchor.product

        val matchingPairs = mutableListOf<Pair<Int, Int>>()
        for (x in 1..9) {
            if (target % x == 0) {
                val y = target / x
                if (y in 1..9) {
                    matchingPairs += x to y
                }
            }
        }
        val chosenMatches = matchingPairs.shuffled(random).take(minOf(3, matchingPairs.size)).toMutableList()
        if (chosenMatches.size < 2) {
            chosenMatches += preferredAnchor.maxFactor to preferredAnchor.minFactor
        }

        val distractors = mutableListOf<Pair<Int, Int>>()
        val candidateDistractors = pool.shuffled(random)
        for (fk in candidateDistractors) {
            if (fk.product != target && abs(fk.product - target) <= 18) {
                val (da, db) = orientFactors(fk, random)
                if (distractors.none { it.first == da && it.second == db }) {
                    distractors += da to db
                }
            }
            if (chosenMatches.size + distractors.size >= 6) break
        }
        while (chosenMatches.size + distractors.size < 6) {
            val da = random.nextInt(2, 10)
            val db = random.nextInt(2, 10)
            if (da * db != target && distractors.none { it.first == da && it.second == db }) {
                distractors += da to db
            }
        }

        val bubbles = (chosenMatches.map { (x, y) -> x to y to true } +
            distractors.take(6 - chosenMatches.size).map { (x, y) -> x to y to false })
            .shuffled(random)
            .mapIndexed { idx, pair ->
                BubbleOption(
                    id = idx,
                    a = pair.first.first,
                    b = pair.first.second,
                    isMatch = pair.second
                )
            }

        return BattleQuestion.BubbleMatchSet(
            factKey = preferredAnchor,
            leftFactor = preferredAnchor.minFactor,
            rightFactor = preferredAnchor.maxFactor,
            targetProduct = target,
            bubbles = bubbles
        )
    }

    private fun buildCompareDuel(
        anchor: FactKey,
        pool: List<FactKey>,
        random: Random
    ): BattleQuestion.CompareDuel {
        val (a1, b1) = orientFactors(anchor, random)
        val p1 = a1 * b1

        val opponent = pool
            .filter { it.product != p1 && abs(it.product - p1) in 1..14 }
            .ifEmpty { pool.filter { it.product != p1 } }
            .randomOrNull(random)
            ?: FactKey.of((a1 % 9) + 1, b1)

        val (a2, b2) = orientFactors(opponent, random)
        val p2 = a2 * b2

        // Randomly decide whether anchor goes on left or right
        return if (random.nextBoolean()) {
            BattleQuestion.CompareDuel(
                factKey = anchor,
                leftFactor = a1,
                rightFactor = b1,
                altLeft = a2,
                altRight = b2,
                leftIsBigger = p1 > p2
            )
        } else {
            BattleQuestion.CompareDuel(
                factKey = opponent,
                leftFactor = a2,
                rightFactor = b2,
                altLeft = a1,
                altRight = b1,
                leftIsBigger = p2 > p1
            )
        }
    }

    private fun pickWeighted(
        pool: List<FactKey>,
        mastery: Map<String, FactMastery>,
        recentExclude: List<FactKey>,
        preferWeak: Boolean,
        random: Random
    ): FactKey {
        val filtered = pool.filterNot { it in recentExclude }.ifEmpty { pool }
        val weights = filtered.map { key ->
            val m = mastery[key.id]
            if (preferWeak) {
                var w = when (m?.masteryLevel) {
                    MasteryLevel.NEEDS_FOCUS -> 6.0
                    null, MasteryLevel.UNSEEN -> 3.5
                    MasteryLevel.LEARNING -> 2.4
                    MasteryLevel.MASTERED -> 0.8
                }
                if (m?.pinnedForFocus == true) w += 4.5
                w
            } else {
                when (m?.masteryLevel) {
                    MasteryLevel.MASTERED -> 3.0
                    MasteryLevel.LEARNING -> 2.5
                    null, MasteryLevel.UNSEEN -> 1.5
                    MasteryLevel.NEEDS_FOCUS -> 0.7
                }
            }
        }
        val total = weights.sum()
        var r = random.nextDouble() * total
        for (i in filtered.indices) {
            r -= weights[i]
            if (r <= 0.0) return filtered[i]
        }
        return filtered.last()
    }

    private fun orientFactors(key: FactKey, random: Random): Pair<Int, Int> {
        return if (key.minFactor != key.maxFactor && random.nextBoolean()) {
            key.maxFactor to key.minFactor
        } else {
            key.minFactor to key.maxFactor
        }
    }
}
