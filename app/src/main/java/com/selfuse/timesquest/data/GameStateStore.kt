package com.selfuse.timesquest.data

import android.content.Context
import android.content.SharedPreferences
import com.selfuse.timesquest.engine.AnswerOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class BattleSettlement(
    val newLevelReached: Int?,
    val newlyUnlockedIsland: Int?,
    val newlyDiscoveredMonsterId: String?,
    val streakBonusEquipName: String?
)

/**
 * Single source of truth for TimesQuest. Thread-safe, offline-only, backed by SharedPreferences JSON.
 */
object GameStateStore {
    private const val PREFS_NAME = "times_quest_prefs_v1"
    private const val KEY_HERO = "hero_json"
    private const val KEY_MASTERY = "mastery_json"
    private const val KEY_DAILY = "daily_usage_json"
    private const val KEY_SETTINGS = "parent_settings_json"

    @Volatile
    private var initialized = false
    private lateinit var prefs: SharedPreferences

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    @Synchronized
    fun ensureInitialized(context: Context) {
        if (initialized) {
            rollOverDayIfNeeded()
            return
        }
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val hero = loadHero()
        val mastery = loadMastery()
        val settings = loadSettings()
        val daily = loadDaily(today)

        _uiState.value = GameUiState(
            initialized = true,
            hero = hero,
            mastery = mastery,
            dailyUsage = daily,
            settings = settings
        )
        initialized = true
    }

    @Synchronized
    fun rollOverDayIfNeeded() {
        if (!initialized) return
        val today = LocalDate.now().toString()
        if (_uiState.value.dailyUsage.dateIso == today) return
        val freshDaily = DailyUsage(
            dateIso = today,
            quests = CurriculumCatalog.createDefaultDailyQuests()
        )
        saveDaily(freshDaily)
        _uiState.update { it.copy(dailyUsage = freshDaily) }
    }

    @Synchronized
    fun completeOnboarding(name: String, avatar: HeroAvatar, startIsland: Int) {
        val cleanName = name.trim().ifEmpty { avatar.defaultName }.take(10)
        val hero = _uiState.value.hero.copy(
            name = cleanName,
            avatar = avatar,
            onboardingDone = true,
            unlockedIslandMax = maxOf(_uiState.value.hero.unlockedIslandMax, startIsland.coerceIn(1, 9))
        )
        saveHero(hero)
        _uiState.update { it.copy(hero = hero) }
    }

    /**
     * Records battle completion (victory or defeat), updates 9x9 mastery, coins, XP, streaks, quests,
     * bestiary, and daily eye-care counters.
     */
    @Synchronized
    fun commitBattleResult(
        level: LevelDef,
        monsterId: String,
        isVictory: Boolean,
        stars: Int,
        coinsEarned: Int,
        xpEarned: Int,
        maxCombo: Int,
        totalCorrect: Int,
        durationSeconds: Int,
        endlessWave: Int,
        outcomes: List<AnswerOutcome>,
        nowMs: Long = System.currentTimeMillis()
    ): BattleSettlement {
        rollOverDayIfNeeded()
        val state = _uiState.value
        val today = LocalDate.now().toString()
        val yesterday = LocalDate.now().minusDays(1).toString()

        // 1. Update 9x9 fact mastery
        val updatedMastery = state.mastery.toMutableMap()
        for (o in outcomes) {
            val prev = updatedMastery[o.factKey.id] ?: FactMastery(key = o.factKey)
            val newAttempts = prev.attempts + 1
            val newCorrect = prev.correct + if (o.isCorrect) 1 else 0
            val newAvgMs = if (prev.attempts == 0) {
                o.responseMs
            } else {
                ((prev.avgResponseMs * 0.70) + (o.responseMs * 0.30)).toLong()
            }
            val newStreak = if (o.isCorrect) prev.streak + 1 else 0
            updatedMastery[o.factKey.id] = prev.copy(
                attempts = newAttempts,
                correct = newCorrect,
                avgResponseMs = newAvgMs,
                streak = newStreak,
                lastSeenAtMs = nowMs
            )
        }

        // 2. Update hero progression, stars, bestiary & login streak
        val oldHero = state.hero
        val oldLevel = oldHero.level
        val newStreakDays = when (oldHero.lastPlayDateIso) {
            today -> maxOf(1, oldHero.streakDays)
            yesterday -> oldHero.streakDays + 1
            else -> 1
        }

        val updatedStars = oldHero.levelStars.toMutableMap()
        val updatedMonsters = oldHero.defeatedMonsters.toMutableMap()
        var newlyDiscoveredMonster: String? = null
        var nextUnlockedIsland = oldHero.unlockedIslandMax

        if (isVictory && level.stageType != StageType.ENDLESS) {
            val prevStar = updatedStars[level.id] ?: 0
            updatedStars[level.id] = maxOf(prevStar, stars)
            val prevDefeats = updatedMonsters[monsterId] ?: 0
            if (prevDefeats == 0) newlyDiscoveredMonster = monsterId
            updatedMonsters[monsterId] = prevDefeats + 1

            // Unlock next island after defeating the Boss (stage 4)
            if (level.stageType == StageType.BOSS && level.islandId >= nextUnlockedIsland && level.islandId < 9) {
                nextUnlockedIsland = level.islandId + 1
            }
        } else if (isVictory && level.stageType == StageType.ENDLESS) {
            val prevDefeats = updatedMonsters[monsterId] ?: 0
            updatedMonsters[monsterId] = prevDefeats + 1
        }

        // Check streak milestone free gifts (Day 3: Bubble Otter pet, Day 7: Rainbow Wand)
        val ownedSet = oldHero.ownedEquipIds.toMutableSet()
        var streakBonusName: String? = null
        for (eq in CurriculumCatalog.equipments) {
            val req = eq.streakUnlockDays
            if (req != null && newStreakDays >= req && eq.id !in ownedSet) {
                ownedSet += eq.id
                streakBonusName = eq.name
            }
        }

        val newHero = oldHero.copy(
            coins = oldHero.coins + coinsEarned,
            xp = oldHero.xp + xpEarned,
            streakDays = newStreakDays,
            lastPlayDateIso = today,
            ownedEquipIds = ownedSet,
            unlockedIslandMax = nextUnlockedIsland,
            levelStars = updatedStars,
            defeatedMonsters = updatedMonsters,
            endlessBestScore = if (level.stageType == StageType.ENDLESS) {
                maxOf(oldHero.endlessBestScore, endlessWave)
            } else {
                oldHero.endlessBestScore
            }
        )

        // 3. Update daily quests & eye-care usage
        val oldDaily = state.dailyUsage
        val updatedQuests = oldDaily.quests.map { q ->
            val add = when (q.id) {
                "q_clear_1" -> if (isVictory) 1 else 0
                "q_combo_5" -> if (maxCombo >= 5) 1 else 0
                "q_correct_15" -> totalCorrect
                else -> 0
            }
            q.copy(progress = (q.progress + add).coerceAtMost(q.target))
        }
        val newDaily = oldDaily.copy(
            activeBattleSeconds = oldDaily.activeBattleSeconds + durationSeconds.coerceAtLeast(1),
            stagesCompleted = oldDaily.stagesCompleted + if (isVictory) 1 else 0,
            quests = updatedQuests
        )

        saveHero(newHero)
        saveMastery(updatedMastery)
        saveDaily(newDaily)
        _uiState.update {
            it.copy(
                hero = newHero,
                mastery = updatedMastery,
                dailyUsage = newDaily
            )
        }

        return BattleSettlement(
            newLevelReached = if (newHero.level > oldLevel) newHero.level else null,
            newlyUnlockedIsland = if (nextUnlockedIsland > oldHero.unlockedIslandMax) nextUnlockedIsland else null,
            newlyDiscoveredMonsterId = newlyDiscoveredMonster,
            streakBonusEquipName = streakBonusName
        )
    }

    @Synchronized
    fun consumePotion(): Boolean {
        val h = _uiState.value.hero
        if (h.potions <= 0) return false
        val updated = h.copy(potions = h.potions - 1)
        saveHero(updated)
        _uiState.update { it.copy(hero = updated) }
        return true
    }

    @Synchronized
    fun buyOrEquip(item: EquipmentDef): Boolean {
        val h = _uiState.value.hero
        val owned = item.id in h.ownedEquipIds
        if (!owned && h.coins < item.priceCoins) return false

        val nextOwned = if (owned) h.ownedEquipIds else h.ownedEquipIds + item.id
        val nextCoins = if (owned) h.coins else h.coins - item.priceCoins
        val updated = when (item.slot) {
            EquipSlot.WAND -> h.copy(coins = nextCoins, ownedEquipIds = nextOwned, equippedWandId = item.id)
            EquipSlot.HAT -> h.copy(coins = nextCoins, ownedEquipIds = nextOwned, equippedHatId = item.id)
            EquipSlot.PET -> h.copy(coins = nextCoins, ownedEquipIds = nextOwned, equippedPetId = item.id)
        }
        saveHero(updated)
        _uiState.update { it.copy(hero = updated) }
        return true
    }

    @Synchronized
    fun buyPotion(price: Int = 35): Boolean {
        val h = _uiState.value.hero
        if (h.coins < price || h.potions >= 9) return false
        val updated = h.copy(coins = h.coins - price, potions = h.potions + 1)
        saveHero(updated)
        _uiState.update { it.copy(hero = updated) }
        return true
    }

    @Synchronized
    fun claimDailyQuest(questId: String): Int {
        rollOverDayIfNeeded()
        val daily = _uiState.value.dailyUsage
        val target = daily.quests.firstOrNull { it.id == questId } ?: return 0
        if (!target.isCompleted || target.claimed) return 0

        val updatedQuests = daily.quests.map { if (it.id == questId) it.copy(claimed = true) else it }
        val updatedDaily = daily.copy(quests = updatedQuests)
        val updatedHero = _uiState.value.hero.copy(coins = _uiState.value.hero.coins + target.rewardCoins)

        saveDaily(updatedDaily)
        saveHero(updatedHero)
        _uiState.update { it.copy(hero = updatedHero, dailyUsage = updatedDaily) }
        return target.rewardCoins
    }

    /** Daily treasure chest opens once the child clears at least 1 stage today. */
    @Synchronized
    fun claimDailyChest(): Pair<Int, Int>? {
        rollOverDayIfNeeded()
        val daily = _uiState.value.dailyUsage
        if (daily.chestClaimed || daily.stagesCompleted < 1) return null
        val bonusCoins = 55 + (_uiState.value.hero.streakDays.coerceAtMost(7) * 5)
        val bonusPotions = 1
        val updatedDaily = daily.copy(chestClaimed = true)
        val updatedHero = _uiState.value.hero.copy(
            coins = _uiState.value.hero.coins + bonusCoins,
            potions = (_uiState.value.hero.potions + bonusPotions).coerceAtMost(9)
        )
        saveDaily(updatedDaily)
        saveHero(updatedHero)
        _uiState.update { it.copy(hero = updatedHero, dailyUsage = updatedDaily) }
        return bonusCoins to bonusPotions
    }

    @Synchronized
    fun togglePinnedFact(key: FactKey) {
        val map = _uiState.value.mastery.toMutableMap()
        val cur = map[key.id] ?: FactMastery(key = key)
        map[key.id] = cur.copy(pinnedForFocus = !cur.pinnedForFocus)
        saveMastery(map)
        _uiState.update { it.copy(mastery = map) }
    }

    @Synchronized
    fun updateParentSettings(transform: (ParentSettings) -> ParentSettings) {
        val updated = transform(_uiState.value.settings)
        saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    @Synchronized
    fun grantParentBonusTime(extraMinutes: Int = 5, extraStages: Int = 1) {
        rollOverDayIfNeeded()
        val updated = _uiState.value.dailyUsage.copy(
            bonusMinutesGranted = _uiState.value.dailyUsage.bonusMinutesGranted + extraMinutes,
            bonusStagesGranted = _uiState.value.dailyUsage.bonusStagesGranted + extraStages
        )
        saveDaily(updated)
        _uiState.update { it.copy(dailyUsage = updated) }
    }

    @Synchronized
    fun resetAllProgress() {
        val freshHero = HeroProgress()
        val freshDaily = DailyUsage(
            dateIso = LocalDate.now().toString(),
            quests = CurriculumCatalog.createDefaultDailyQuests()
        )
        saveHero(freshHero)
        saveMastery(emptyMap())
        saveDaily(freshDaily)
        _uiState.update {
            it.copy(
                hero = freshHero,
                mastery = emptyMap(),
                dailyUsage = freshDaily
            )
        }
    }

    // -------------------------------------------------------------------------
    // Persistence helpers
    // -------------------------------------------------------------------------

    private fun loadHero(): HeroProgress {
        val raw = prefs.getString(KEY_HERO, null) ?: return HeroProgress()
        return try {
            val o = JSONObject(raw)
            val owned = mutableSetOf("wand_wood", "hat_apprentice")
            val ownedArr = o.optJSONArray("ownedEquipIds")
            if (ownedArr != null) {
                for (i in 0 until ownedArr.length()) {
                    owned += ownedArr.optString(i)
                }
            }
            val starsMap = mutableMapOf<String, Int>()
            o.optJSONObject("levelStars")?.let { obj ->
                val it = obj.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    starsMap[k] = obj.optInt(k, 0)
                }
            }
            val monstersMap = mutableMapOf<String, Int>()
            o.optJSONObject("defeatedMonsters")?.let { obj ->
                val it = obj.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    monstersMap[k] = obj.optInt(k, 0)
                }
            }
            val petRaw = o.optString("equippedPetId", "")
            HeroProgress(
                name = o.optString("name", "小朗勇者"),
                avatar = HeroAvatar.fromId(o.optString("avatar", "boy")),
                onboardingDone = o.optBoolean("onboardingDone", false),
                coins = o.optInt("coins", 80),
                xp = o.optInt("xp", 0),
                potions = o.optInt("potions", 2),
                streakDays = o.optInt("streakDays", 0),
                lastPlayDateIso = o.optString("lastPlayDateIso", ""),
                equippedWandId = o.optString("equippedWandId", "wand_wood"),
                equippedHatId = o.optString("equippedHatId", "hat_apprentice"),
                equippedPetId = petRaw.ifBlank { null },
                ownedEquipIds = owned,
                unlockedIslandMax = o.optInt("unlockedIslandMax", 1).coerceIn(1, 9),
                levelStars = starsMap,
                defeatedMonsters = monstersMap,
                endlessBestScore = o.optInt("endlessBestScore", 0)
            )
        } catch (_: Exception) {
            HeroProgress()
        }
    }

    private fun saveHero(h: HeroProgress) {
        val o = JSONObject().apply {
            put("name", h.name)
            put("avatar", h.avatar.id)
            put("onboardingDone", h.onboardingDone)
            put("coins", h.coins)
            put("xp", h.xp)
            put("potions", h.potions)
            put("streakDays", h.streakDays)
            put("lastPlayDateIso", h.lastPlayDateIso)
            put("equippedWandId", h.equippedWandId)
            put("equippedHatId", h.equippedHatId)
            put("equippedPetId", h.equippedPetId ?: "")
            put("ownedEquipIds", JSONArray(h.ownedEquipIds.toList()))
            put("unlockedIslandMax", h.unlockedIslandMax)
            put("levelStars", JSONObject().apply { h.levelStars.forEach { (k, v) -> put(k, v) } })
            put("defeatedMonsters", JSONObject().apply { h.defeatedMonsters.forEach { (k, v) -> put(k, v) } })
            put("endlessBestScore", h.endlessBestScore)
        }
        prefs.edit().putString(KEY_HERO, o.toString()).apply()
    }

    private fun loadMastery(): Map<String, FactMastery> {
        val raw = prefs.getString(KEY_MASTERY, null) ?: return emptyMap()
        return try {
            val arr = JSONArray(raw)
            val map = LinkedHashMap<String, FactMastery>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val key = FactKey.parse(o.optString("id")) ?: continue
                map[key.id] = FactMastery(
                    key = key,
                    attempts = o.optInt("attempts", 0),
                    correct = o.optInt("correct", 0),
                    avgResponseMs = o.optLong("avgResponseMs", 0L),
                    streak = o.optInt("streak", 0),
                    lastSeenAtMs = o.optLong("lastSeenAtMs", 0L),
                    pinnedForFocus = o.optBoolean("pinnedForFocus", false)
                )
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveMastery(map: Map<String, FactMastery>) {
        val arr = JSONArray()
        map.values.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.key.id)
                put("attempts", m.attempts)
                put("correct", m.correct)
                put("avgResponseMs", m.avgResponseMs)
                put("streak", m.streak)
                put("lastSeenAtMs", m.lastSeenAtMs)
                put("pinnedForFocus", m.pinnedForFocus)
            })
        }
        prefs.edit().putString(KEY_MASTERY, arr.toString()).apply()
    }

    private fun loadDaily(today: String): DailyUsage {
        val raw = prefs.getString(KEY_DAILY, null)
        if (raw.isNullOrBlank()) {
            return DailyUsage(dateIso = today, quests = CurriculumCatalog.createDefaultDailyQuests())
        }
        return try {
            val o = JSONObject(raw)
            if (o.optString("dateIso") != today) {
                return DailyUsage(dateIso = today, quests = CurriculumCatalog.createDefaultDailyQuests())
            }
            val qArr = o.optJSONArray("quests")
            val quests = if (qArr == null || qArr.length() == 0) {
                CurriculumCatalog.createDefaultDailyQuests()
            } else {
                (0 until qArr.length()).map { i ->
                    val q = qArr.getJSONObject(i)
                    DailyQuestItem(
                        id = q.optString("id"),
                        title = q.optString("title"),
                        target = q.optInt("target", 1),
                        progress = q.optInt("progress", 0),
                        rewardCoins = q.optInt("rewardCoins", 40),
                        claimed = q.optBoolean("claimed", false)
                    )
                }
            }
            DailyUsage(
                dateIso = today,
                activeBattleSeconds = o.optInt("activeBattleSeconds", 0),
                stagesCompleted = o.optInt("stagesCompleted", 0),
                bonusMinutesGranted = o.optInt("bonusMinutesGranted", 0),
                bonusStagesGranted = o.optInt("bonusStagesGranted", 0),
                chestClaimed = o.optBoolean("chestClaimed", false),
                quests = quests
            )
        } catch (_: Exception) {
            DailyUsage(dateIso = today, quests = CurriculumCatalog.createDefaultDailyQuests())
        }
    }

    private fun saveDaily(d: DailyUsage) {
        val qArr = JSONArray()
        d.quests.forEach { q ->
            qArr.put(JSONObject().apply {
                put("id", q.id)
                put("title", q.title)
                put("target", q.target)
                put("progress", q.progress)
                put("rewardCoins", q.rewardCoins)
                put("claimed", q.claimed)
            })
        }
        val o = JSONObject().apply {
            put("dateIso", d.dateIso)
            put("activeBattleSeconds", d.activeBattleSeconds)
            put("stagesCompleted", d.stagesCompleted)
            put("bonusMinutesGranted", d.bonusMinutesGranted)
            put("bonusStagesGranted", d.bonusStagesGranted)
            put("chestClaimed", d.chestClaimed)
            put("quests", qArr)
        }
        prefs.edit().putString(KEY_DAILY, o.toString()).apply()
    }

    private fun loadSettings(): ParentSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return ParentSettings()
        return try {
            val o = JSONObject(raw)
            ParentSettings(
                dailyMinutesLimit = o.optInt("dailyMinutesLimit", 15).coerceIn(0, 120),
                dailyStagesLimit = o.optInt("dailyStagesLimit", 3).coerceIn(0, 30),
                ttsEnabled = o.optBoolean("ttsEnabled", true),
                sfxEnabled = o.optBoolean("sfxEnabled", true),
                hapticsEnabled = o.optBoolean("hapticsEnabled", true),
                unlockAllIslands = o.optBoolean("unlockAllIslands", false)
            )
        } catch (_: Exception) {
            ParentSettings()
        }
    }

    private fun saveSettings(s: ParentSettings) {
        val o = JSONObject().apply {
            put("dailyMinutesLimit", s.dailyMinutesLimit)
            put("dailyStagesLimit", s.dailyStagesLimit)
            put("ttsEnabled", s.ttsEnabled)
            put("sfxEnabled", s.sfxEnabled)
            put("hapticsEnabled", s.hapticsEnabled)
            put("unlockAllIslands", s.unlockAllIslands)
        }
        prefs.edit().putString(KEY_SETTINGS, o.toString()).apply()
    }
}
