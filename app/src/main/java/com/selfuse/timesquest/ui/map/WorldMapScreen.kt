@file:OptIn(ExperimentalMaterial3Api::class)

package com.selfuse.timesquest.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfuse.timesquest.audio.SoundEffectEngine
import com.selfuse.timesquest.data.CurriculumCatalog
import com.selfuse.timesquest.data.GameStateStore
import com.selfuse.timesquest.data.GameUiState
import com.selfuse.timesquest.data.IslandDef
import com.selfuse.timesquest.data.LevelDef
import com.selfuse.timesquest.data.StageType
import com.selfuse.timesquest.engine.DailyLimitGuard
import com.selfuse.timesquest.ui.components.SpriteImage
import com.selfuse.timesquest.ui.components.StarRow
import com.selfuse.timesquest.ui.components.StatPill
import com.selfuse.timesquest.ui.components.TactileRpgButton
import com.selfuse.timesquest.ui.theme.QuestColors
import com.selfuse.timesquest.ui.theme.tabular

@Composable
fun WorldMapScreen(
    state: GameUiState,
    onStartLevel: (LevelDef) -> Unit,
    onOpenParentGate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hero = state.hero
    val daily = state.dailyUsage
    val settings = state.settings
    val limitStatus = remember(daily, settings) { DailyLimitGuard.evaluate(daily, settings) }

    var selectedLevel by remember { mutableStateOf<LevelDef?>(null) }
    var chestRewardPopup by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(QuestColors.CreamBg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Hero RPG Top Banner
        item(key = "hero_header") {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE3F2FD))
                                .border(2.dp, QuestColors.SkyPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            SpriteImage(
                                spriteKey = "hero_${hero.avatar.id}",
                                contentDescription = hero.name,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = hero.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = QuestColors.InkPrimary
                                )
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFF3CD)
                                ) {
                                    Text(
                                        text = "Lv.${hero.level} ${hero.titleText}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = QuestColors.CoralDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            LinearProgressIndicator(
                                progress = { hero.xpInCurrentLevel.toFloat() / hero.xpForNextLevel.coerceAtLeast(1) },
                                color = QuestColors.Emerald,
                                trackColor = Color(0xFFECE5D8),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(99.dp))
                            )
                        }
                        IconButton(onClick = onOpenParentGate) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "家长守护中心",
                                tint = QuestColors.InkSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatPill("🪙", "${hero.coins}", bgColor = Color(0xFFFFF8E1))
                        StatPill("⭐", "${hero.totalStars}", bgColor = Color(0xFFE3F2FD))
                        StatPill("🔥", "${hero.streakDays}天", bgColor = Color(0xFFFFEBEE))
                        Spacer(Modifier.weight(1f))
                        val eyeText = buildString {
                            if (limitStatus.stagesAllowed > 0) {
                                append("今日 ${limitStatus.stagesUsed}/${limitStatus.stagesAllowed} 关")
                            } else {
                                append("今日 ${limitStatus.stagesUsed} 关")
                            }
                        }
                        Text(
                            text = eyeText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuestColors.InkSecondary,
                            style = MaterialTheme.typography.bodySmall.tabular()
                        )
                    }
                }
            }
        }

        // 2. Daily Quests + Daily Treasure Chest Strip
        item(key = "daily_quests") {
            DailyQuestCard(
                state = state,
                onClaimQuest = { qid ->
                    val reward = GameStateStore.claimDailyQuest(qid)
                    if (reward > 0) SoundEffectEngine.playChestOpen(settings.sfxEnabled)
                },
                onOpenChest = {
                    val res = GameStateStore.claimDailyChest()
                    if (res != null) {
                        SoundEffectEngine.playChestOpen(settings.sfxEnabled)
                        chestRewardPopup = res
                    }
                }
            )
        }

        // 3. Section Header
        item(key = "map_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🗺️ 奇幻口诀群岛",
                    style = MaterialTheme.typography.headlineSmall,
                    color = QuestColors.InkPrimary
                )
                Text(
                    text = "共 9 座岛 · 36 关",
                    style = MaterialTheme.typography.bodySmall,
                    color = QuestColors.InkSecondary
                )
            }
        }

        // 4. Nine Fantasy Islands connected by winding adventure dots
        itemsIndexed(CurriculumCatalog.islands, key = { _, isl -> isl.id }) { idx, island ->
            val islandUnlocked = settings.unlockAllIslands || island.id <= hero.unlockedIslandMax
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IslandMapCard(
                    island = island,
                    unlocked = islandUnlocked,
                    levelStars = hero.levelStars,
                    unlockAll = settings.unlockAllIslands,
                    onSelectLevel = { lv ->
                        SoundEffectEngine.playButtonTap(settings.sfxEnabled)
                        selectedLevel = lv
                    }
                )
                if (idx < CurriculumCatalog.islands.lastIndex) {
                    AdventureTrailConnector(
                        unlocked = settings.unlockAllIslands || (island.id + 1) <= hero.unlockedIslandMax
                    )
                }
            }
        }

        // 5. Endless Tower Special Banner
        item(key = "endless_tower") {
            val endlessUnlocked = settings.unlockAllIslands || hero.unlockedIslandMax >= 4
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (endlessUnlocked) Color(0xFF311B92) else Color(0xFFE8E2D6),
                shadowElevation = if (endlessUnlocked) 6.dp else 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpriteImage(
                        spriteKey = "boss_dragon",
                        contentDescription = "无尽勇者塔",
                        silhouette = !endlessUnlocked,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🏰 无尽勇者塔",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (endlessUnlocked) QuestColors.Gold else QuestColors.InkSecondary
                        )
                        Text(
                            text = if (endlessUnlocked) {
                                "全口诀车轮大战！最高纪录：第 ${hero.endlessBestScore} 波"
                            } else {
                                "解锁第 4 岛「珊瑚泡泡海湾」后开放"
                            },
                            fontSize = 13.sp,
                            color = if (endlessUnlocked) Color.White.copy(alpha = 0.88f) else QuestColors.InkSecondary
                        )
                        if (endlessUnlocked) {
                            Spacer(Modifier.height(10.dp))
                            TactileRpgButton(
                                onClick = {
                                    SoundEffectEngine.playButtonTap(settings.sfxEnabled)
                                    selectedLevel = CurriculumCatalog.endlessTowerLevel
                                },
                                containerColor = QuestColors.Gold,
                                ledgeColor = QuestColors.GoldDark,
                                contentColor = QuestColors.InkPrimary,
                                cornerRadius = 14.dp,
                                ledgeHeight = 4.dp,
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text("⚔️ 挑战最高波次", fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }

    // Level Brief Bottom Sheet
    val lv = selectedLevel
    if (lv != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val monster = CurriculumCatalog.monster(lv.monsterId)
        val earnedStars = hero.levelStars[lv.id] ?: 0
        ModalBottomSheet(
            onDismissRequest = { selectedLevel = null },
            sheetState = sheetState,
            containerColor = QuestColors.CreamBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Color(monster.accentHex).copy(alpha = 0.16f)
                ) {
                    Text(
                        text = lv.stageType.badge,
                        color = Color(monster.accentHex),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(lv.title, style = MaterialTheme.typography.headlineSmall, color = QuestColors.InkPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = lv.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = QuestColors.InkSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                SpriteImage(
                    spriteKey = monster.spriteKey,
                    contentDescription = monster.name,
                    modifier = Modifier.size(124.dp)
                )
                Text(
                    text = "${monster.name}（${monster.title}）",
                    style = MaterialTheme.typography.titleMedium,
                    color = QuestColors.InkPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "弱点提示：${monster.weakFactHint}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuestColors.CoralDark
                )
                if (lv.stageType != StageType.ENDLESS) {
                    Spacer(Modifier.height(8.dp))
                    StarRow(earned = earnedStars, starSize = 24.dp)
                }
                Spacer(Modifier.height(18.dp))
                TactileRpgButton(
                    onClick = {
                        val toPlay = lv
                        selectedLevel = null
                        onStartLevel(toPlay)
                    },
                    containerColor = QuestColors.CoralAccent,
                    ledgeColor = QuestColors.CoralDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Text("⚔️ 出发打怪！", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }

    // Daily Chest Popup
    val chestReward = chestRewardPopup
    if (chestReward != null) {
        AlertDialog(
            onDismissRequest = { chestRewardPopup = null },
            title = { Text("🎁 开启每日大宝箱！") },
            text = {
                Text(
                    "恭喜获得 🪙 +${chestReward.first} 金币 和 🍓 +${chestReward.second} 瓶魔法草莓药水！\n" +
                        "连续冒险 3 天送泡泡水獭，7 天送传说彩虹魔杖哦！"
                )
            },
            confirmButton = {
                TextButton(onClick = { chestRewardPopup = null }) {
                    Text("收进背包！", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }
}

@Composable
private fun DailyQuestCard(
    state: GameUiState,
    onClaimQuest: (String) -> Unit,
    onOpenChest: () -> Unit
) {
    val daily = state.dailyUsage
    val chestReady = !daily.chestClaimed && daily.stagesCompleted >= 1

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFFFF4DE),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFFFFDDA0), RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🎯 今日冒险悬赏",
                    style = MaterialTheme.typography.titleMedium,
                    color = QuestColors.CoralDark
                )
                TactileRpgButton(
                    onClick = onOpenChest,
                    enabled = chestReady,
                    containerColor = if (chestReady) QuestColors.Gold else Color(0xFFE6DEC9),
                    ledgeColor = if (chestReady) QuestColors.GoldDark else Color(0xFFCBBFA9),
                    contentColor = if (chestReady) QuestColors.InkPrimary else QuestColors.InkSecondary,
                    cornerRadius = 12.dp,
                    ledgeHeight = 3.dp,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = when {
                            daily.chestClaimed -> "🎁 宝箱已领"
                            chestReady -> "🎁 开每日宝箱!"
                            else -> "🎁 打赢1关开箱"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                daily.quests.forEach { q ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(q.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = QuestColors.InkPrimary)
                            Text(
                                text = "进度 ${q.progress}/${q.target} · 奖励 🪙${q.rewardCoins}",
                                fontSize = 11.sp,
                                color = QuestColors.InkSecondary,
                                style = MaterialTheme.typography.bodySmall.tabular()
                            )
                        }
                        when {
                            q.claimed -> Text("✅ 已领", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = QuestColors.EmeraldDark)
                            q.isCompleted -> TactileRpgButton(
                                onClick = { onClaimQuest(q.id) },
                                containerColor = QuestColors.Emerald,
                                ledgeColor = QuestColors.EmeraldDark,
                                cornerRadius = 10.dp,
                                ledgeHeight = 3.dp,
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("领 +${q.rewardCoins}", fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                            else -> Text("${q.progress}/${q.target}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = QuestColors.InkSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IslandMapCard(
    island: IslandDef,
    unlocked: Boolean,
    levelStars: Map<String, Int>,
    unlockAll: Boolean,
    onSelectLevel: (LevelDef) -> Unit
) {
    val boss = CurriculumCatalog.monster(island.bossId)
    val islandStars = island.levels.sumOf { levelStars[it.id] ?: 0 }
    val accent = Color(island.theme.accentHex)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = if (unlocked) 4.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (unlocked) 1f else 0.72f)
            .border(
                width = if (unlocked) 2.dp else 1.dp,
                color = if (unlocked) accent.copy(alpha = 0.45f) else QuestColors.BorderSoft,
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(Color(island.theme.skyBottomHex).copy(alpha = 0.55f), Color.White)
                    )
                )
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SpriteImage(
                    spriteKey = boss.spriteKey,
                    contentDescription = island.name,
                    silhouette = !unlocked,
                    modifier = Modifier.size(66.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (unlocked) accent else Color(0xFFB5AFA2)
                        ) {
                            Text(
                                text = "第 ${island.id} 岛 · ${island.badgeText}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "⭐ $islandStars/12",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = QuestColors.GoldDark,
                            style = MaterialTheme.typography.bodySmall.tabular()
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(island.name, style = MaterialTheme.typography.titleLarge, color = QuestColors.InkPrimary)
                    Text(island.subtitle, fontSize = 12.sp, color = QuestColors.InkSecondary)
                }
            }

            Spacer(Modifier.height(12.dp))

            // 4 Chunky Stage Nodes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                island.levels.forEachIndexed { idx, lv ->
                    val prevCleared = idx == 0 || (levelStars[island.levels[idx - 1].id] ?: 0) > 0
                    val stageOpen = unlocked && (unlockAll || prevCleared)
                    val stars = levelStars[lv.id] ?: 0
                    val isBoss = lv.stageType == StageType.BOSS
                    val nodeBg = when {
                        !stageOpen -> Color(0xFFECE7DC)
                        isBoss -> QuestColors.CoralAccent
                        stars > 0 -> QuestColors.Emerald
                        else -> accent
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = nodeBg,
                        shadowElevation = if (stageOpen) 3.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = stageOpen) { onSelectLevel(lv) }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!stageOpen) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = "未解锁",
                                    tint = QuestColors.InkSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = if (isBoss) "👑 Boss" else "第${lv.stageIndex}关",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = when (lv.stageType) {
                                    StageType.SCOUT -> "侦察"
                                    StageType.SHIELD_ELITE -> "破盾"
                                    StageType.MAGIC_TRIAL -> "特训"
                                    StageType.BOSS -> "决战"
                                    StageType.ENDLESS -> "无尽"
                                },
                                fontSize = 11.sp,
                                color = if (stageOpen) Color.White.copy(alpha = 0.92f) else QuestColors.InkSecondary
                            )
                            Spacer(Modifier.height(4.dp))
                            StarRow(earned = stars, starSize = 12.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdventureTrailConnector(unlocked: Boolean) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        val cx = size.width / 2f
        drawLine(
            color = if (unlocked) QuestColors.Gold else Color(0xFFD5CCB9),
            start = Offset(cx, 2f),
            end = Offset(cx, size.height - 2f),
            strokeWidth = 4.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        )
    }
}
