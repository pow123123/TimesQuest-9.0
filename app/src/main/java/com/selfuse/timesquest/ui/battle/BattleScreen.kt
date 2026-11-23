package com.selfuse.timesquest.ui.battle

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfuse.timesquest.audio.SoundEffectEngine
import com.selfuse.timesquest.audio.SpeechCoachEngine
import com.selfuse.timesquest.data.BattleQuestion
import com.selfuse.timesquest.data.BattleSettlement
import com.selfuse.timesquest.data.CurriculumCatalog
import com.selfuse.timesquest.data.GameStateStore
import com.selfuse.timesquest.data.GameUiState
import com.selfuse.timesquest.data.LevelDef
import com.selfuse.timesquest.data.StageType
import com.selfuse.timesquest.engine.BattleEngine
import com.selfuse.timesquest.engine.BattlePhase
import com.selfuse.timesquest.engine.BattleSnapshot
import com.selfuse.timesquest.engine.BattleVisualEvent
import com.selfuse.timesquest.ui.components.SpriteImage
import com.selfuse.timesquest.ui.components.StarRow
import com.selfuse.timesquest.ui.components.TactileRpgButton
import com.selfuse.timesquest.ui.theme.QuestColors
import com.selfuse.timesquest.ui.theme.tabular
import kotlinx.coroutines.delay

@Composable
fun BattleScreen(
    level: LevelDef,
    gameState: GameUiState,
    speechCoach: SpeechCoachEngine,
    onExitBattle: () -> Unit
) {
    val context = LocalContext.current
    val settings = gameState.settings
    val hero = gameState.hero
    val island = remember(level.islandId) { CurriculumCatalog.island(level.islandId) }
    val wand = remember(hero.equippedWandId) { CurriculumCatalog.wand(hero.equippedWandId) }
    val pet = remember(hero.equippedPetId) { CurriculumCatalog.equipment(hero.equippedPetId) }

    val engine = remember(level.id) {
        BattleEngine(
            level = level,
            heroLevel = hero.level,
            hasCompanionPet = pet != null,
            masterySnapshot = gameState.mastery
        )
    }

    var snapshot by remember(engine) { mutableStateOf(engine.state) }
    var battleStartedAtMs by remember(engine) { mutableLongStateOf(System.currentTimeMillis()) }
    var questionShownAtMs by remember(engine) { mutableLongStateOf(System.currentTimeMillis()) }
    var elapsedQuestionMs by remember(engine) { mutableLongStateOf(0L) }
    var confirmExit by remember { mutableStateOf(false) }
    var committed by remember(engine) { mutableStateOf(false) }
    var settlement by remember(engine) { mutableStateOf<BattleSettlement?>(null) }

    BackHandler { confirmExit = true }

    // Speak new questions via TTS & reset per-question stopwatch
    val currentQuestion = snapshot.currentQuestion
    LaunchedEffect(currentQuestion, snapshot.phase) {
        if (snapshot.phase == BattlePhase.PLAYER_TURN && currentQuestion != null) {
            questionShownAtMs = System.currentTimeMillis()
            elapsedQuestionMs = 0L
            val spoken = spokenPromptFor(currentQuestion)
            if (spoken != null) {
                speechCoach.speakQuestion(spoken, settings.ttsEnabled)
            }
        }
    }

    // Live 50ms ticker for speed crit bar & Boss QTE countdown
    LaunchedEffect(currentQuestion, snapshot.phase) {
        if (snapshot.phase != BattlePhase.PLAYER_TURN || currentQuestion == null) return@LaunchedEffect
        while (true) {
            delay(50L)
            val elapsed = System.currentTimeMillis() - questionShownAtMs
            elapsedQuestionMs = elapsed
            if (currentQuestion is BattleQuestion.BossCounterQte && elapsed >= currentQuestion.timeLimitMs) {
                snapshot = engine.onBossQteTimeout()
                break
            }
        }
    }

    // Play SFX, Haptics & Voice Celebrations on combat visual events
    LaunchedEffect(snapshot.eventSeq) {
        val ev = snapshot.lastEvent ?: return@LaunchedEffect
        when (ev) {
            is BattleVisualEvent.HeroAttack -> {
                when {
                    ev.isUltimate -> SoundEffectEngine.playUltimateBlast(settings.sfxEnabled)
                    ev.isShieldBreak -> SoundEffectEngine.playShieldBreak(settings.sfxEnabled)
                    else -> SoundEffectEngine.playComboHit(settings.sfxEnabled, snapshot.combo, ev.isCrit || ev.isParry)
                }
                SoundEffectEngine.hapticTap(context, settings.hapticsEnabled, strong = ev.isCrit || ev.isUltimate || ev.isParry)
                speechCoach.speakRhymeCelebration(ev.rhymeText, settings.ttsEnabled)
            }

            is BattleVisualEvent.MonsterHit -> {
                SoundEffectEngine.playSoftMiss(settings.sfxEnabled)
                SoundEffectEngine.hapticTap(context, settings.hapticsEnabled, strong = true)
                speechCoach.speakSlowCorrection(ev.rhymeText, settings.ttsEnabled)
            }

            is BattleVisualEvent.BubblePop -> {
                SoundEffectEngine.playBubblePop(settings.sfxEnabled)
                SoundEffectEngine.hapticTap(context, settings.hapticsEnabled, strong = false)
            }

            is BattleVisualEvent.PotionRevive -> {
                SoundEffectEngine.playChestOpen(settings.sfxEnabled)
            }

            is BattleVisualEvent.EndlessWaveCleared -> {
                SoundEffectEngine.playVictoryFanfare(settings.sfxEnabled)
            }
        }
    }

    // Commit victory once to persistent store
    LaunchedEffect(snapshot.phase) {
        if (snapshot.phase == BattlePhase.VICTORY && !committed) {
            committed = true
            SoundEffectEngine.playVictoryFanfare(settings.sfxEnabled)
            val sec = ((System.currentTimeMillis() - battleStartedAtMs) / 1000L).toInt().coerceAtLeast(1)
            settlement = GameStateStore.commitBattleResult(
                level = level,
                monsterId = snapshot.monster.id,
                isVictory = true,
                stars = snapshot.rewardStars,
                coinsEarned = snapshot.rewardCoins,
                xpEarned = snapshot.rewardXp,
                maxCombo = snapshot.maxCombo,
                totalCorrect = snapshot.totalCorrect,
                durationSeconds = sec,
                endlessWave = snapshot.endlessWave,
                outcomes = snapshot.outcomes
            )
        }
    }

    fun commitDefeatAndExit() {
        if (!committed && snapshot.outcomes.isNotEmpty()) {
            committed = true
            val sec = ((System.currentTimeMillis() - battleStartedAtMs) / 1000L).toInt().coerceAtLeast(1)
            GameStateStore.commitBattleResult(
                level = level,
                monsterId = snapshot.monster.id,
                isVictory = false,
                stars = 0,
                coinsEarned = snapshot.rewardCoins,
                xpEarned = snapshot.rewardXp,
                maxCombo = snapshot.maxCombo,
                totalCorrect = snapshot.totalCorrect,
                durationSeconds = sec,
                endlessWave = snapshot.endlessWave,
                outcomes = snapshot.outcomes
            )
        }
        onExitBattle()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QuestColors.CreamBg)
            .safeDrawingPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Top Battle Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { confirmExit = true }) {
                Icon(Icons.Filled.Close, contentDescription = "退出战斗", tint = QuestColors.InkPrimary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(
                    text = if (level.stageType == StageType.ENDLESS) {
                        "无尽勇者塔 · 第 ${snapshot.endlessWave} 波"
                    } else {
                        "${island.name} · ${level.title}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = QuestColors.InkPrimary,
                    maxLines = 1
                )
                Text(
                    text = "已答对 ${snapshot.totalCorrect} 题 · 最高 ${snapshot.maxCombo} 连击",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = QuestColors.InkSecondary
                )
            }
            if (level.stageType == StageType.ENDLESS && snapshot.totalCorrect >= 3) {
                TextButton(onClick = { snapshot = engine.finishEndlessRun() }) {
                    Text("结算", fontWeight = FontWeight.ExtraBold, color = QuestColors.CoralDark)
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
        }

        Spacer(Modifier.height(6.dp))

        // Upper Half: Animated RPG Battle Stage
        BattleStageCanvas(
            snapshot = snapshot,
            hero = hero,
            wandElement = wand.element,
            petSpriteKey = pet?.spriteKey,
            theme = island.theme,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.44f)
        )

        Spacer(Modifier.height(12.dp))

        // Lower Half: Active Spell Input Mechanics
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.56f)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            val q = snapshot.currentQuestion
            if (q != null && snapshot.phase == BattlePhase.PLAYER_TURN) {
                SpellInputPanel(
                    question = q,
                    poppedBubbleIds = snapshot.poppedBubbleIds,
                    elapsedQuestionMs = elapsedQuestionMs,
                    onSubmitNumber = { value ->
                        val ms = System.currentTimeMillis() - questionShownAtMs
                        snapshot = engine.submitNumericAnswer(value, ms)
                    },
                    onTapBubble = { bubbleId ->
                        val ms = System.currentTimeMillis() - questionShownAtMs
                        snapshot = engine.tapBubble(bubbleId, ms)
                    },
                    onSubmitCompare = { pickedLeft ->
                        val ms = System.currentTimeMillis() - questionShownAtMs
                        snapshot = engine.submitCompareChoice(pickedLeft, ms)
                    },
                    onReplayVoice = {
                        spokenPromptFor(q)?.let { speechCoach.speakQuestion(it, settings.ttsEnabled) }
                    }
                )
            }
        }
    }

    // 1. Mistake Teaching Dialog (Gem Dot Matrix + Slow Rhyme)
    val activeCoach = snapshot.activeCoach
    if (snapshot.phase == BattlePhase.COACH_CORRECTION && activeCoach != null) {
        DotMatrixCoachDialog(
            coach = activeCoach,
            onReplayRhyme = { speechCoach.speakSlowCorrection(activeCoach.rhyme, settings.ttsEnabled) },
            onConfirm = {
                SoundEffectEngine.playButtonTap(settings.sfxEnabled)
                snapshot = engine.acknowledgeCorrection()
            }
        )
    }

    // 2. Defeat Revive with Strawberry Potion Dialog
    if (snapshot.phase == BattlePhase.DEFEAT) {
        DefeatPotionDialog(
            potions = gameState.hero.potions,
            onUsePotion = {
                if (GameStateStore.consumePotion()) {
                    snapshot = engine.reviveWithPotion()
                }
            },
            onRetreat = { commitDefeatAndExit() }
        )
    }

    // 3. Celebratory Victory Dialog
    if (snapshot.phase == BattlePhase.VICTORY) {
        VictoryDialog(
            snapshot = snapshot,
            settlement = settlement,
            onContinue = onExitBattle
        )
    }

    // 4. Confirm Early Exit
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("先回奇幻地图吗？") },
            text = { Text("本场战斗还没打完哦，现在离开本次关卡进度不会保存。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    onExitBattle()
                }) { Text("先回去", color = QuestColors.CoralDark) }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text("继续打怪！") }
            }
        )
    }
}

private fun spokenPromptFor(q: BattleQuestion): String? = when (q) {
    is BattleQuestion.MultipleChoice -> CurriculumCatalog.spokenQuestion(q.leftFactor, q.rightFactor)
    is BattleQuestion.KeypadUltimate -> CurriculumCatalog.spokenQuestion(q.leftFactor, q.rightFactor)
    is BattleQuestion.BossCounterQte -> CurriculumCatalog.spokenQuestion(q.leftFactor, q.rightFactor)
    is BattleQuestion.MissingFactorShield -> {
        val known = if (q.hideLeft) q.rightFactor else q.leftFactor
        CurriculumCatalog.spokenMissingFactor(known, q.product, q.hideLeft)
    }
    is BattleQuestion.BubbleMatchSet -> "找出所有等于${CurriculumCatalog.spokenNumber(q.targetProduct)}的泡泡"
    is BattleQuestion.CompareDuel -> "哪一个得数更大？"
}

@Composable
private fun DefeatPotionDialog(
    potions: Int,
    onUsePotion: () -> Unit,
    onRetreat: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = QuestColors.CreamBg,
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, QuestColors.CoralAccent, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🍓 小勇者累啦！", style = MaterialTheme.typography.headlineSmall, color = QuestColors.CoralDark)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (potions > 0) {
                        "喝一瓶魔法草莓药水就能满血复活，继续打败这只怪兽！（背包剩余 $potions 瓶）"
                    } else {
                        "没关系！你刚才已经记住了好多口诀，回营地休息一下再来挑战吧！"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = QuestColors.InkSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                if (potions > 0) {
                    TactileRpgButton(
                        onClick = onUsePotion,
                        containerColor = QuestColors.Emerald,
                        ledgeColor = QuestColors.EmeraldDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                    ) {
                        Text("🍓 喝魔法药水复活！", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.height(10.dp))
                }
                TextButton(onClick = onRetreat) {
                    Text("先回地图休息", fontSize = 15.sp, color = QuestColors.InkSecondary)
                }
            }
        }
    }
}

@Composable
private fun VictoryDialog(
    snapshot: BattleSnapshot,
    settlement: BattleSettlement?,
    onContinue: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = QuestColors.CreamBg,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, QuestColors.Gold, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉 冒险大胜利！", style = MaterialTheme.typography.headlineMedium, color = QuestColors.CoralDark)
                Spacer(Modifier.height(10.dp))
                StarRow(earned = snapshot.rewardStars, starSize = 36.dp)
                Spacer(Modifier.height(12.dp))
                SpriteImage(
                    spriteKey = snapshot.monster.spriteKey,
                    contentDescription = snapshot.monster.name,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "成功收服了「${snapshot.monster.name}」！",
                    style = MaterialTheme.typography.titleMedium,
                    color = QuestColors.InkPrimary
                )
                Spacer(Modifier.height(14.dp))

                // Rewards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RewardBadge("🪙 金币", "+${snapshot.rewardCoins}")
                    RewardBadge("✨ 经验", "+${snapshot.rewardXp}")
                    RewardBadge("🔥 最高连击", "${snapshot.maxCombo}")
                }

                // Special unlocks
                val highlights = buildList {
                    settlement?.newLevelReached?.let { add("🆙 勇者升级到 Lv.$it！") }
                    settlement?.newlyUnlockedIsland?.let { id ->
                        add("🏝️ 解锁新岛屿：${CurriculumCatalog.island(id).name}！")
                    }
                    settlement?.newlyDiscoveredMonsterId?.let { mid ->
                        add("📖 点亮新图鉴：${CurriculumCatalog.monster(mid).name}！")
                    }
                    settlement?.streakBonusEquipName?.let { eq ->
                        add("🎁 连续签到礼物：获得「$eq」！")
                    }
                    if (snapshot.revengeSuccessCount > 0) {
                        add("🔥 错题复仇成功 ${snapshot.revengeSuccessCount} 次！")
                    }
                }
                if (highlights.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFF3CD),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            highlights.forEach { line ->
                                Text(
                                    text = line,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = QuestColors.CoralDark
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                TactileRpgButton(
                    onClick = onContinue,
                    containerColor = QuestColors.CoralAccent,
                    ledgeColor = QuestColors.CoralDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Text("太棒啦，继续冒险！", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun RewardBadge(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 12.sp, color = QuestColors.InkSecondary)
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = QuestColors.InkPrimary,
                style = MaterialTheme.typography.titleMedium.tabular()
            )
        }
    }
}
