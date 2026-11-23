package com.selfuse.timesquest.engine

import com.selfuse.timesquest.data.BattleQuestion
import com.selfuse.timesquest.data.CurriculumCatalog
import com.selfuse.timesquest.data.FactKey
import com.selfuse.timesquest.data.FactMastery
import com.selfuse.timesquest.data.LevelDef
import com.selfuse.timesquest.data.MonsterDef
import com.selfuse.timesquest.data.StageType
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.random.Random

enum class BattlePhase {
    PLAYER_TURN,
    COACH_CORRECTION,
    VICTORY,
    DEFEAT
}

sealed class BattleVisualEvent {
    data class HeroAttack(
        val damage: Int,
        val isCrit: Boolean,
        val isUltimate: Boolean,
        val isParry: Boolean,
        val isShieldBreak: Boolean,
        val isRevenge: Boolean,
        val petBonusDamage: Int,
        val rhymeText: String
    ) : BattleVisualEvent()

    data class MonsterHit(
        val rhymeText: String,
        val wasTimeout: Boolean
    ) : BattleVisualEvent()

    data class BubblePop(val label: String) : BattleVisualEvent()

    data object PotionRevive : BattleVisualEvent()

    data class EndlessWaveCleared(val nextWave: Int, val nextMonsterName: String) : BattleVisualEvent()
}

data class AnswerOutcome(
    val factKey: FactKey,
    val isCorrect: Boolean,
    val responseMs: Long
)

data class CorrectionCoach(
    val leftFactor: Int,
    val rightFactor: Int,
    val product: Int,
    val rhyme: String,
    val detailHint: String,
    val isTimeout: Boolean = false
)

data class BattleSnapshot(
    val level: LevelDef,
    val monster: MonsterDef,
    val phase: BattlePhase,
    val heroHp: Int,
    val heroMaxHp: Int,
    val monsterHp: Int,
    val monsterMaxHp: Int,
    val combo: Int,
    val maxCombo: Int,
    val ultimateCharge: Int,
    val monsterStunned: Boolean,
    val currentQuestion: BattleQuestion?,
    val poppedBubbleIds: Set<Int>,
    val answeredCount: Int,
    val totalCorrect: Int,
    val totalWrong: Int,
    val fastCritCount: Int,
    val revengeSuccessCount: Int,
    val petAssistCount: Int,
    val endlessWave: Int,
    val activeCoach: CorrectionCoach?,
    val eventSeq: Long,
    val lastEvent: BattleVisualEvent?,
    val rewardStars: Int,
    val rewardCoins: Int,
    val rewardXp: Int,
    val outcomes: List<AnswerOutcome>
) {
    val isFever: Boolean get() = combo >= 5
    val bossShieldActive: Boolean get() = currentQuestion is BattleQuestion.MissingFactorShield
    val isBossQteActive: Boolean get() = currentQuestion is BattleQuestion.BossCounterQte
    val heroHpFraction: Float get() = if (heroMaxHp <= 0) 0f else heroHp.toFloat() / heroMaxHp
    val monsterHpFraction: Float get() = if (monsterMaxHp <= 0) 0f else monsterHp.toFloat() / monsterMaxHp
}

/**
 * Pure-Kotlin RPG battle state machine powering all 5 spell & boss mechanics.
 */
class BattleEngine(
    private val level: LevelDef,
    private val heroLevel: Int,
    private val hasCompanionPet: Boolean,
    private val masterySnapshot: Map<String, FactMastery>,
    private val random: Random = Random.Default
) {
    companion object {
        const val HERO_MAX_HP = 4
        const val FAST_CRIT_MS = 2800L
        const val PET_ASSIST_COMBO_STEP = 4
    }

    private val deck = ArrayDeque<BattleQuestion>()
    private val outcomes = mutableListOf<AnswerOutcome>()
    private var eventSeq = 0L
    private var bossQtePhase1Triggered = false
    private var bossQtePhase2Triggered = false
    private var currentMonster: MonsterDef = CurriculumCatalog.monster(level.monsterId)
    private var currentMonsterMaxHp: Int = level.monsterMaxHp
    private var pendingAdvanceAfterCoach = false

    private val baseDamage: Int = run {
        val raw = ceil(level.monsterMaxHp.toDouble() / (level.questionCount * 1.15)).toInt()
        (raw + (heroLevel - 1).coerceIn(0, 10)).coerceAtLeast(16)
    }

    private var _state: BattleSnapshot
    val state: BattleSnapshot get() = _state

    init {
        deck.addAll(AdaptiveFactScheduler.generateStageDeck(level, masterySnapshot, random))
        val firstQ = nextQuestionFromDeck()
        _state = BattleSnapshot(
            level = level,
            monster = currentMonster,
            phase = BattlePhase.PLAYER_TURN,
            heroHp = HERO_MAX_HP,
            heroMaxHp = HERO_MAX_HP,
            monsterHp = currentMonsterMaxHp,
            monsterMaxHp = currentMonsterMaxHp,
            combo = 0,
            maxCombo = 0,
            ultimateCharge = 0,
            monsterStunned = false,
            currentQuestion = firstQ,
            poppedBubbleIds = emptySet(),
            answeredCount = 0,
            totalCorrect = 0,
            totalWrong = 0,
            fastCritCount = 0,
            revengeSuccessCount = 0,
            petAssistCount = 0,
            endlessWave = 1,
            activeCoach = null,
            eventSeq = 0L,
            lastEvent = null,
            rewardStars = 0,
            rewardCoins = 0,
            rewardXp = 0,
            outcomes = emptyList()
        )
    }

    /**
     * Submits a numeric answer for MultipleChoice, MissingFactorShield, KeypadUltimate, or BossCounterQte.
     */
    fun submitNumericAnswer(value: Int, responseMs: Long): BattleSnapshot {
        if (_state.phase != BattlePhase.PLAYER_TURN) return _state
        val q = _state.currentQuestion ?: return _state

        val isCorrect = when (q) {
            is BattleQuestion.MultipleChoice -> value == q.product
            is BattleQuestion.MissingFactorShield -> value == q.expectedFactor
            is BattleQuestion.KeypadUltimate -> value == q.product
            is BattleQuestion.BossCounterQte -> value == q.product
            is BattleQuestion.BubbleMatchSet,
            is BattleQuestion.CompareDuel -> return _state
        }

        val clampedMs = responseMs.coerceIn(150L, 20_000L)
        outcomes += AnswerOutcome(q.factKey, isCorrect, clampedMs)

        return if (isCorrect) {
            applyCorrectAttack(q, clampedMs)
        } else {
            applyMistake(
                factKey = q.factKey,
                a = q.leftFactor,
                b = q.rightFactor,
                detailHint = buildCorrectionHint(q),
                wasTimeout = false,
                advanceAfterCoach = q is BattleQuestion.BossCounterQte
            )
        }
    }

    /**
     * Submits a bubble tap for BubbleMatchSet.
     */
    fun tapBubble(bubbleId: Int, responseMs: Long): BattleSnapshot {
        if (_state.phase != BattlePhase.PLAYER_TURN) return _state
        val q = _state.currentQuestion as? BattleQuestion.BubbleMatchSet ?: return _state
        if (bubbleId in _state.poppedBubbleIds) return _state
        val bubble = q.bubbles.firstOrNull { it.id == bubbleId } ?: return _state

        val clampedMs = responseMs.coerceIn(150L, 20_000L)
        val bubbleKey = FactKey.of(bubble.a, bubble.b)

        return if (bubble.isMatch) {
            outcomes += AnswerOutcome(bubbleKey, true, clampedMs)
            val updatedPopped = _state.poppedBubbleIds + bubbleId
            val allMatchesPopped = q.bubbles.filter { it.isMatch }.all { it.id in updatedPopped }
            if (allMatchesPopped) {
                _state = _state.copy(poppedBubbleIds = updatedPopped)
                applyCorrectAttack(q, clampedMs)
            } else {
                eventSeq++
                _state = _state.copy(
                    poppedBubbleIds = updatedPopped,
                    eventSeq = eventSeq,
                    lastEvent = BattleVisualEvent.BubblePop(bubble.label),
                    outcomes = outcomes.toList()
                )
                _state
            }
        } else {
            outcomes += AnswerOutcome(bubbleKey, false, clampedMs)
            applyMistake(
                factKey = bubbleKey,
                a = bubble.a,
                b = bubble.b,
                detailHint = "${bubble.a} × ${bubble.b} = ${bubble.product}，不是 ${q.targetProduct} 哦！",
                wasTimeout = false,
                advanceAfterCoach = false
            )
        }
    }

    /**
     * Submits choice for CompareDuel (pickedLeft == true means user tapped the left monster's expression).
     */
    fun submitCompareChoice(pickedLeft: Boolean, responseMs: Long): BattleSnapshot {
        if (_state.phase != BattlePhase.PLAYER_TURN) return _state
        val q = _state.currentQuestion as? BattleQuestion.CompareDuel ?: return _state
        val clampedMs = responseMs.coerceIn(150L, 20_000L)
        val isCorrect = pickedLeft == q.leftIsBigger
        outcomes += AnswerOutcome(q.factKey, isCorrect, clampedMs)

        return if (isCorrect) {
            applyCorrectAttack(q, clampedMs)
        } else {
            val leftProd = q.leftFactor * q.rightFactor
            val rightProd = q.altLeft * q.altRight
            val bigA = if (q.leftIsBigger) q.leftFactor else q.altLeft
            val bigB = if (q.leftIsBigger) q.rightFactor else q.altRight
            applyMistake(
                factKey = FactKey.of(bigA, bigB),
                a = bigA,
                b = bigB,
                detailHint = "${q.leftFactor}×${q.rightFactor}=$leftProd，${q.altLeft}×${q.altRight}=$rightProd，更大的积是 ${maxOf(leftProd, rightProd)}！",
                wasTimeout = false,
                advanceAfterCoach = true
            )
        }
    }

    /**
     * Called when the Boss Enrage QTE timer expires before the child answers.
     */
    fun onBossQteTimeout(): BattleSnapshot {
        if (_state.phase != BattlePhase.PLAYER_TURN) return _state
        val q = _state.currentQuestion as? BattleQuestion.BossCounterQte ?: return _state
        outcomes += AnswerOutcome(q.factKey, false, q.timeLimitMs)
        return applyMistake(
            factKey = q.factKey,
            a = q.leftFactor,
            b = q.rightFactor,
            detailHint = "Boss 蓄力太快啦！记住这句口诀，下次一定能弹反！",
            wasTimeout = true,
            advanceAfterCoach = true
        )
    }

    /**
     * Dismisses the DotMatrixCoachDialog after the child taps "我记住啦！".
     */
    fun acknowledgeCorrection(): BattleSnapshot {
        if (_state.phase != BattlePhase.COACH_CORRECTION) return _state
        if (_state.heroHp <= 0) {
            _state = _state.copy(
                phase = BattlePhase.DEFEAT,
                activeCoach = null,
                rewardCoins = (_state.totalCorrect * 3).coerceAtLeast(10),
                rewardXp = (_state.totalCorrect * 4).coerceAtLeast(15)
            )
            return _state
        }

        val nextQ = if (pendingAdvanceAfterCoach) {
            prepareNextTurnQuestion(currentMonsterHp = _state.monsterHp, nextUltCharge = _state.ultimateCharge)
        } else {
            _state.currentQuestion
        }
        pendingAdvanceAfterCoach = false

        _state = _state.copy(
            phase = BattlePhase.PLAYER_TURN,
            activeCoach = null,
            currentQuestion = nextQ,
            poppedBubbleIds = if (nextQ is BattleQuestion.BubbleMatchSet && nextQ === _state.currentQuestion) {
                _state.poppedBubbleIds
            } else {
                emptySet()
            }
        )
        return _state
    }

    /**
     * Uses a Strawberry Potion on defeat to revive with full HP and keep fighting.
     */
    fun reviveWithPotion(): BattleSnapshot {
        if (_state.phase != BattlePhase.DEFEAT) return _state
        eventSeq++
        val nextQ = _state.currentQuestion ?: prepareNextTurnQuestion(_state.monsterHp, _state.ultimateCharge)
        _state = _state.copy(
            phase = BattlePhase.PLAYER_TURN,
            heroHp = HERO_MAX_HP,
            activeCoach = null,
            currentQuestion = nextQ,
            poppedBubbleIds = emptySet(),
            eventSeq = eventSeq,
            lastEvent = BattleVisualEvent.PotionRevive
        )
        return _state
    }

    /**
     * Allows graceful finish in Endless Tower once at least 1 question was answered.
     */
    fun finishEndlessRun(): BattleSnapshot {
        if (level.stageType != StageType.ENDLESS) return _state
        val coins = 20 + _state.totalCorrect * 4 + (_state.endlessWave - 1) * 18
        val xp = 25 + _state.totalCorrect * 5 + (_state.endlessWave - 1) * 20
        val stars = when {
            _state.endlessWave >= 4 -> 3
            _state.endlessWave >= 2 -> 2
            else -> 1
        }
        _state = _state.copy(
            phase = BattlePhase.VICTORY,
            activeCoach = null,
            rewardStars = stars,
            rewardCoins = coins,
            rewardXp = xp,
            outcomes = outcomes.toList()
        )
        return _state
    }

    private fun applyCorrectAttack(q: BattleQuestion, responseMs: Long): BattleSnapshot {
        val nextCombo = _state.combo + 1
        val nextMaxCombo = maxOf(_state.maxCombo, nextCombo)
        val isFastCrit = q is BattleQuestion.MultipleChoice && responseMs <= FAST_CRIT_MS
        val isUltimate = q is BattleQuestion.KeypadUltimate && q.isUltimateCast
        val isParry = q is BattleQuestion.BossCounterQte
        val isShieldBreak = q is BattleQuestion.MissingFactorShield

        var mult = 1.0f
        if (isFastCrit) mult *= 1.45f
        if (nextCombo >= 5) mult *= 1.25f
        if (_state.monsterStunned) mult *= 1.4f
        if (isShieldBreak) mult *= 1.3f
        if (isParry) mult *= 1.65f
        if (isUltimate) mult *= 2.1f
        if (q.isRevenge) mult *= 1.2f

        val heroDamage = (baseDamage * mult).roundToInt().coerceAtLeast(15)
        val triggersPetAssist = hasCompanionPet && nextCombo > 0 && nextCombo % PET_ASSIST_COMBO_STEP == 0
        val petBonus = if (triggersPetAssist) (baseDamage * 0.65f).roundToInt().coerceAtLeast(12) else 0
        val totalDamage = heroDamage + petBonus

        val nextMonsterHp = (_state.monsterHp - totalDamage).coerceAtLeast(0)
        val nextUltCharge = if (isUltimate) {
            0
        } else {
            val gain = when {
                isFastCrit || isParry -> 35
                isShieldBreak -> 30
                else -> 25
            }
            (_state.ultimateCharge + gain).coerceAtMost(100)
        }

        val rhymeText = CurriculumCatalog.chineseRhyme(q.leftFactor, q.rightFactor)
        eventSeq++
        val attackEvent = BattleVisualEvent.HeroAttack(
            damage = totalDamage,
            isCrit = isFastCrit || isUltimate || isParry,
            isUltimate = isUltimate,
            isParry = isParry,
            isShieldBreak = isShieldBreak,
            isRevenge = q.isRevenge,
            petBonusDamage = petBonus,
            rhymeText = rhymeText
        )

        val totalCorrect = _state.totalCorrect + 1
        val answered = _state.answeredCount + 1
        val fastCrits = _state.fastCritCount + if (isFastCrit) 1 else 0
        val revengeOk = _state.revengeSuccessCount + if (q.isRevenge) 1 else 0
        val petAssists = _state.petAssistCount + if (triggersPetAssist) 1 else 0

        if (nextMonsterHp <= 0) {
            if (level.stageType == StageType.ENDLESS) {
                // Endless Tower: spawn next wave monster with fresh HP!
                val nextWave = _state.endlessWave + 1
                val allBosses = CurriculumCatalog.monsters.filter { it.isBoss }
                currentMonster = allBosses[(nextWave - 1) % allBosses.size]
                currentMonsterMaxHp = 150 + nextWave * 30
                val nextQ = prepareNextTurnQuestion(currentMonsterMaxHp, nextUltCharge)
                eventSeq++
                _state = _state.copy(
                    monster = currentMonster,
                    monsterHp = currentMonsterMaxHp,
                    monsterMaxHp = currentMonsterMaxHp,
                    combo = nextCombo,
                    maxCombo = nextMaxCombo,
                    ultimateCharge = nextUltCharge,
                    monsterStunned = false,
                    currentQuestion = nextQ,
                    poppedBubbleIds = emptySet(),
                    answeredCount = answered,
                    totalCorrect = totalCorrect,
                    fastCritCount = fastCrits,
                    revengeSuccessCount = revengeOk,
                    petAssistCount = petAssists,
                    endlessWave = nextWave,
                    eventSeq = eventSeq,
                    lastEvent = BattleVisualEvent.EndlessWaveCleared(nextWave, currentMonster.name),
                    outcomes = outcomes.toList()
                )
                return _state
            }

            // Stage Victory!
            val accuracy = totalCorrect.toFloat() / maxOf(1, totalCorrect + _state.totalWrong)
            val stars = when {
                _state.totalWrong == 0 || (accuracy >= 0.90f && _state.totalWrong <= 1) -> 3
                accuracy >= 0.70f -> 2
                else -> 1
            }
            val coins = level.baseCoins + stars * 10 + fastCrits * 3 + revengeOk * 6 + if (nextMaxCombo >= 5) 12 else 0
            val xp = level.baseXp + stars * 12 + nextMaxCombo * 2

            _state = _state.copy(
                phase = BattlePhase.VICTORY,
                monsterHp = 0,
                combo = nextCombo,
                maxCombo = nextMaxCombo,
                ultimateCharge = nextUltCharge,
                monsterStunned = false,
                currentQuestion = null,
                poppedBubbleIds = emptySet(),
                answeredCount = answered,
                totalCorrect = totalCorrect,
                fastCritCount = fastCrits,
                revengeSuccessCount = revengeOk,
                petAssistCount = petAssists,
                eventSeq = eventSeq,
                lastEvent = attackEvent,
                rewardStars = stars,
                rewardCoins = coins,
                rewardXp = xp,
                outcomes = outcomes.toList()
            )
            return _state
        }

        val nextQ = prepareNextTurnQuestion(nextMonsterHp, nextUltCharge)
        _state = _state.copy(
            monsterHp = nextMonsterHp,
            combo = nextCombo,
            maxCombo = nextMaxCombo,
            ultimateCharge = nextUltCharge,
            monsterStunned = isShieldBreak,
            currentQuestion = nextQ,
            poppedBubbleIds = emptySet(),
            answeredCount = answered,
            totalCorrect = totalCorrect,
            fastCritCount = fastCrits,
            revengeSuccessCount = revengeOk,
            petAssistCount = petAssists,
            eventSeq = eventSeq,
            lastEvent = attackEvent,
            outcomes = outcomes.toList()
        )
        return _state
    }

    private fun applyMistake(
        factKey: FactKey,
        a: Int,
        b: Int,
        detailHint: String,
        wasTimeout: Boolean,
        advanceAfterCoach: Boolean
    ): BattleSnapshot {
        val rhyme = CurriculumCatalog.chineseRhyme(a, b)
        val nextHeroHp = (_state.heroHp - 1).coerceAtLeast(0)
        pendingAdvanceAfterCoach = advanceAfterCoach

        // Insert a revenge question 2..3 turns later so the child masters it!
        val revenge = AdaptiveFactScheduler.buildRevengeQuestion(
            key = factKey,
            preferShield = level.stageType == StageType.SHIELD_ELITE || level.stageType == StageType.BOSS,
            random = random
        )
        val insertPos = minOf(2, deck.size)
        val list = deck.toMutableList()
        list.add(insertPos, revenge)
        deck.clear()
        deck.addAll(list)

        eventSeq++
        _state = _state.copy(
            phase = BattlePhase.COACH_CORRECTION,
            heroHp = nextHeroHp,
            combo = 0,
            monsterStunned = false,
            totalWrong = _state.totalWrong + 1,
            answeredCount = _state.answeredCount + if (advanceAfterCoach) 1 else 0,
            activeCoach = CorrectionCoach(
                leftFactor = a,
                rightFactor = b,
                product = a * b,
                rhyme = rhyme,
                detailHint = detailHint,
                isTimeout = wasTimeout
            ),
            eventSeq = eventSeq,
            lastEvent = BattleVisualEvent.MonsterHit(rhymeText = rhyme, wasTimeout = wasTimeout),
            outcomes = outcomes.toList()
        )
        return _state
    }

    private fun prepareNextTurnQuestion(currentMonsterHp: Int, nextUltCharge: Int): BattleQuestion {
        // Boss Enrage Counter QTE #1 triggers around 58% HP
        if (level.stageType == StageType.BOSS && currentMonsterMaxHp > 0) {
            val ratio = currentMonsterHp.toFloat() / currentMonsterMaxHp.toFloat()
            if (!bossQtePhase1Triggered && ratio <= 0.58f) {
                bossQtePhase1Triggered = true
                return AdaptiveFactScheduler.buildBossCounterQte(level.islandId, masterySnapshot, random)
            }
        }

        val upcoming = nextQuestionFromDeck()

        // When Ultimate Energy hits 100%, immediately let the hero cast their Rune Keypad Ultimate!
        if (nextUltCharge >= 100 && upcoming !is BattleQuestion.KeypadUltimate) {
            return AdaptiveFactScheduler.buildUltimateKeypad(
                key = upcoming.factKey,
                isRevenge = upcoming.isRevenge,
                random = random
            )
        }

        // Boss Enrage Counter QTE #2 triggers around 28% HP (after Ultimate has fired)
        if (level.stageType == StageType.BOSS && currentMonsterMaxHp > 0) {
            val ratio = currentMonsterHp.toFloat() / currentMonsterMaxHp.toFloat()
            if (!bossQtePhase2Triggered && ratio <= 0.28f) {
                bossQtePhase2Triggered = true
                return AdaptiveFactScheduler.buildBossCounterQte(level.islandId, masterySnapshot, random)
            }
        }
        return upcoming
    }

    private fun nextQuestionFromDeck(): BattleQuestion {
        if (deck.isEmpty()) {
            deck.addAll(AdaptiveFactScheduler.generateStageDeck(level, masterySnapshot, random))
        }
        return deck.removeFirst()
    }

    private fun buildCorrectionHint(q: BattleQuestion): String = when (q) {
        is BattleQuestion.MissingFactorShield ->
            if (q.hideLeft) "想一想：几乘 ${q.rightFactor} 等于 ${q.product}？答案是 ${q.leftFactor}！"
            else "想一想：${q.leftFactor} 乘几等于 ${q.product}？答案是 ${q.rightFactor}！"
        else -> "${q.leftFactor} 个 ${q.rightFactor} 合在一起，就是 ${q.product}！"
    }
}
