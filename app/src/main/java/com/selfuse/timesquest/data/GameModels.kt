package com.selfuse.timesquest.data

import kotlin.math.max
import kotlin.math.min

enum class HeroAvatar(val id: String, val defaultName: String, val label: String) {
    BOY("boy", "小朗勇者", "男孩小勇者"),
    GIRL("girl", "小星勇者", "女孩小勇者");

    companion object {
        fun fromId(id: String?): HeroAvatar = entries.firstOrNull { it.id == id } ?: BOY
    }
}

enum class ElementStyle(val label: String, val primaryHex: Long, val secondaryHex: Long) {
    WOOD("森叶魔力", 0xFF34C759, 0xFFB8F29A),
    FIRE("烈焰陨石", 0xFFFF5E3A, 0xFFFFD166),
    ICE("极光寒冰", 0xFF29B6F6, 0xFFE1F5FE),
    LIGHTNING("雷霆电光", 0xFFFFC107, 0xFFFFF59D),
    RAINBOW("星辰彩虹", 0xFFAB47BC, 0xFFFFCA28)
}

enum class StageType(val badge: String) {
    SCOUT("侦察战"),
    SHIELD_ELITE("破盾精英"),
    MAGIC_TRIAL("魔法特训"),
    BOSS("岛屿 Boss"),
    ENDLESS("无尽勇者塔")
}

enum class MasteryLevel(val label: String) {
    UNSEEN("未遇到"),
    NEEDS_FOCUS("重点攻坚"),
    LEARNING("熟练中"),
    MASTERED("已精通")
}

enum class EquipSlot(val label: String) {
    WAND("元素法杖"),
    HAT("勇者装扮"),
    PET("助战萌宠")
}

/**
 * Canonical unordered multiplication fact key (minFactor <= maxFactor), 45 unique facts for 1..9.
 */
data class FactKey(val minFactor: Int, val maxFactor: Int) {
    val product: Int get() = minFactor * maxFactor
    val id: String get() = "${minFactor}x${maxFactor}"

    companion object {
        fun of(a: Int, b: Int): FactKey = FactKey(min(a, b), max(a, b))

        fun parse(id: String): FactKey? {
            val parts = id.split("x")
            if (parts.size != 2) return null
            val a = parts[0].toIntOrNull() ?: return null
            val b = parts[1].toIntOrNull() ?: return null
            if (a !in 1..9 || b !in 1..9) return null
            return of(a, b)
        }
    }
}

data class FactMastery(
    val key: FactKey,
    val attempts: Int = 0,
    val correct: Int = 0,
    val avgResponseMs: Long = 0L,
    val streak: Int = 0,
    val lastSeenAtMs: Long = 0L,
    val pinnedForFocus: Boolean = false
) {
    val accuracy: Float get() = if (attempts <= 0) 0f else correct.toFloat() / attempts.toFloat()

    val masteryLevel: MasteryLevel
        get() = when {
            attempts == 0 -> MasteryLevel.UNSEEN
            accuracy < 0.75f || (streak == 0 && attempts >= 1) -> MasteryLevel.NEEDS_FOCUS
            accuracy >= 0.90f && attempts >= 3 && avgResponseMs in 1L..3500L && streak >= 2 -> MasteryLevel.MASTERED
            else -> MasteryLevel.LEARNING
        }
}

data class IslandTheme(
    val skyTopHex: Long,
    val skyBottomHex: Long,
    val hillHex: Long,
    val groundHex: Long,
    val accentHex: Long
)

data class MonsterDef(
    val id: String,
    val name: String,
    val title: String,
    val islandId: Int,
    val isBoss: Boolean,
    val spriteKey: String,
    val accentHex: Long,
    val flavorText: String,
    val weakFactHint: String
)

data class LevelDef(
    val id: String,
    val islandId: Int,
    val stageIndex: Int,
    val title: String,
    val subtitle: String,
    val stageType: StageType,
    val monsterId: String,
    val questionCount: Int,
    val monsterMaxHp: Int,
    val baseCoins: Int,
    val baseXp: Int
)

data class IslandDef(
    val id: Int,
    val name: String,
    val subtitle: String,
    val badgeText: String,
    val targetTables: List<Int>,
    val bossId: String,
    val theme: IslandTheme,
    val levels: List<LevelDef>
)

data class EquipmentDef(
    val id: String,
    val slot: EquipSlot,
    val name: String,
    val description: String,
    val priceCoins: Int,
    val accentHex: Long,
    val element: ElementStyle = ElementStyle.WOOD,
    val spriteKey: String? = null,
    val streakUnlockDays: Int? = null
)

data class DailyQuestItem(
    val id: String,
    val title: String,
    val target: Int,
    val progress: Int = 0,
    val rewardCoins: Int,
    val claimed: Boolean = false
) {
    val isCompleted: Boolean get() = progress >= target
}

data class ParentSettings(
    val dailyMinutesLimit: Int = 15,
    val dailyStagesLimit: Int = 3,
    val ttsEnabled: Boolean = true,
    val sfxEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val unlockAllIslands: Boolean = false
)

data class DailyUsage(
    val dateIso: String,
    val activeBattleSeconds: Int = 0,
    val stagesCompleted: Int = 0,
    val bonusMinutesGranted: Int = 0,
    val bonusStagesGranted: Int = 0,
    val chestClaimed: Boolean = false,
    val quests: List<DailyQuestItem> = emptyList()
)

data class HeroProgress(
    val name: String = "小朗勇者",
    val avatar: HeroAvatar = HeroAvatar.BOY,
    val onboardingDone: Boolean = false,
    val coins: Int = 80,
    val xp: Int = 0,
    val potions: Int = 2,
    val streakDays: Int = 0,
    val lastPlayDateIso: String = "",
    val equippedWandId: String = "wand_wood",
    val equippedHatId: String = "hat_apprentice",
    val equippedPetId: String? = null,
    val ownedEquipIds: Set<String> = setOf("wand_wood", "hat_apprentice"),
    val unlockedIslandMax: Int = 1,
    val levelStars: Map<String, Int> = emptyMap(),
    val defeatedMonsters: Map<String, Int> = emptyMap(),
    val endlessBestScore: Int = 0
) {
    val level: Int
        get() {
            var lv = 1
            var remaining = xp
            while (remaining >= xpNeededForLevel(lv)) {
                remaining -= xpNeededForLevel(lv)
                lv++
            }
            return lv
        }

    val xpInCurrentLevel: Int
        get() {
            var lv = 1
            var remaining = xp
            while (remaining >= xpNeededForLevel(lv)) {
                remaining -= xpNeededForLevel(lv)
                lv++
            }
            return remaining
        }

    val xpForNextLevel: Int get() = xpNeededForLevel(level)

    val titleText: String
        get() = when {
            level <= 2 -> "见习小学徒"
            level <= 5 -> "口诀魔法师"
            level <= 9 -> "疾风咒术师"
            level <= 14 -> "星光魔导师"
            level <= 19 -> "圣盾大骑士"
            else -> "九九传奇大勇者"
        }

    val totalStars: Int get() = levelStars.values.sum()

    companion object {
        fun xpNeededForLevel(lv: Int): Int = 80 + (lv - 1) * 25
    }
}

data class GameUiState(
    val initialized: Boolean = false,
    val hero: HeroProgress = HeroProgress(),
    val mastery: Map<String, FactMastery> = emptyMap(),
    val dailyUsage: DailyUsage = DailyUsage(""),
    val settings: ParentSettings = ParentSettings()
)

/**
 * Interactive battle question types powering the 5 RPG mechanics.
 */
sealed class BattleQuestion {
    abstract val factKey: FactKey
    abstract val leftFactor: Int
    abstract val rightFactor: Int
    abstract val isRevenge: Boolean

    val product: Int get() = leftFactor * rightFactor

    /** 1. Fast 4-choice magic spell cast */
    data class MultipleChoice(
        override val factKey: FactKey,
        override val leftFactor: Int,
        override val rightFactor: Int,
        val options: List<Int>,
        val fastBonusMs: Long = 2800L,
        override val isRevenge: Boolean = false
    ) : BattleQuestion()

    /** 2. Reverse shield breaker: [] x b = product or a x [] = product */
    data class MissingFactorShield(
        override val factKey: FactKey,
        override val leftFactor: Int,
        override val rightFactor: Int,
        val hideLeft: Boolean,
        val options: List<Int>,
        override val isRevenge: Boolean = false
    ) : BattleQuestion() {
        val expectedFactor: Int get() = if (hideLeft) leftFactor else rightFactor
    }

    /** 3. Direct rune keypad input (no guessing!) for ultimate attacks & precision trials */
    data class KeypadUltimate(
        override val factKey: FactKey,
        override val leftFactor: Int,
        override val rightFactor: Int,
        val isUltimateCast: Boolean = true,
        override val isRevenge: Boolean = false
    ) : BattleQuestion()

    /** 4A. Pop all bubbles whose expression equals targetProduct */
    data class BubbleMatchSet(
        override val factKey: FactKey,
        override val leftFactor: Int,
        override val rightFactor: Int,
        val targetProduct: Int,
        val bubbles: List<BubbleOption>,
        override val isRevenge: Boolean = false
    ) : BattleQuestion()

    /** 4B. Dual monster duel: pick whichever expression has the larger product */
    data class CompareDuel(
        override val factKey: FactKey,
        override val leftFactor: Int,
        override val rightFactor: Int,
        val altLeft: Int,
        val altRight: Int,
        val leftIsBigger: Boolean,
        override val isRevenge: Boolean = false
    ) : BattleQuestion()

    /** 5. Boss enrage counter QTE: answer within timeLimitMs to parry and reflect damage */
    data class BossCounterQte(
        override val factKey: FactKey,
        override val leftFactor: Int,
        override val rightFactor: Int,
        val options: List<Int>,
        val timeLimitMs: Long = 4200L,
        override val isRevenge: Boolean = false
    ) : BattleQuestion()
}

data class BubbleOption(
    val id: Int,
    val a: Int,
    val b: Int,
    val isMatch: Boolean
) {
    val label: String get() = "$a × $b"
    val product: Int get() = a * b
}

data class PlacementQuestion(
    val a: Int,
    val b: Int,
    val options: List<Int>,
    val isHardTier: Boolean
)
