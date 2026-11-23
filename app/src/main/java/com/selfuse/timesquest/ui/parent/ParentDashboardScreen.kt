@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.selfuse.timesquest.ui.parent

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfuse.timesquest.data.CurriculumCatalog
import com.selfuse.timesquest.data.FactKey
import com.selfuse.timesquest.data.FactMastery
import com.selfuse.timesquest.data.GameStateStore
import com.selfuse.timesquest.data.GameUiState
import com.selfuse.timesquest.data.MasteryLevel
import com.selfuse.timesquest.ui.theme.QuestColors
import com.selfuse.timesquest.ui.theme.tabular
import java.util.Locale

private val MINUTE_OPTIONS = listOf(10, 15, 20, 30, 0)
private val STAGE_OPTIONS = listOf(2, 3, 5, 0)

@Composable
fun ParentDashboardScreen(
    state: GameUiState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings = state.settings
    val mastery = state.mastery
    val daily = state.dailyUsage

    var selectedFact by remember { mutableStateOf(FactKey.of(7, 8)) }
    var confirmReset by remember { mutableStateOf(false) }

    val masteredCount = CurriculumCatalog.allFacts45.count { mastery[it.id]?.masteryLevel == MasteryLevel.MASTERED }
    val weakCount = CurriculumCatalog.allFacts45.count { mastery[it.id]?.masteryLevel == MasteryLevel.NEEDS_FOCUS }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("家长守护与学习报告", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        containerColor = QuestColors.CreamBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Summary Overview
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SummaryStat("已精通口诀", "$masteredCount / 45", QuestColors.EmeraldDark)
                        SummaryStat("重点攻坚", "$weakCount 句", QuestColors.DangerRed)
                        SummaryStat("今日已玩", "${daily.activeBattleSeconds / 60}分 · ${daily.stagesCompleted}关", QuestColors.SkyDark)
                    }
                }
            }

            // 2. Interactive 9x9 Triangle Mastery Heatmap
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "9×9 乘法口诀掌握热力图",
                            style = MaterialTheme.typography.titleLarge,
                            color = QuestColors.InkPrimary
                        )
                        Text(
                            text = "点击任意口诀格可查看正确率与速度，也可设为「重点特训」让关卡悄悄多出。",
                            fontSize = 12.sp,
                            color = QuestColors.InkSecondary
                        )
                        Spacer(Modifier.height(10.dp))

                        // Legend
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendDot("未遇到", Color(0xFFECE7DC))
                            LegendDot("需加强", Color(0xFFEF5350))
                            LegendDot("熟练中", Color(0xFFFFCA28))
                            LegendDot("已精通", Color(0xFF43A047))
                        }

                        Spacer(Modifier.height(12.dp))

                        // 9x9 Lower Triangle Grid (row = 1..9, col = 1..row)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (row in 1..9) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (col in 1..9) {
                                        if (col <= row) {
                                            val fk = FactKey.of(col, row)
                                            val m = mastery[fk.id]
                                            val isSelected = selectedFact == fk
                                            val bg = when (m?.masteryLevel) {
                                                MasteryLevel.NEEDS_FOCUS -> Color(0xFFEF5350)
                                                MasteryLevel.LEARNING -> Color(0xFFFFCA28)
                                                MasteryLevel.MASTERED -> Color(0xFF43A047)
                                                null, MasteryLevel.UNSEEN -> Color(0xFFECE7DC)
                                            }
                                            val fg = when (m?.masteryLevel) {
                                                MasteryLevel.NEEDS_FOCUS, MasteryLevel.MASTERED -> Color.White
                                                else -> QuestColors.InkPrimary
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(bg)
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else 0.dp,
                                                        color = if (isSelected) QuestColors.InkPrimary else Color.Transparent,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { selectedFact = fk },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${col}×${row}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = fg,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1
                                                )
                                                if (m?.pinnedForFocus == true) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(2.dp)
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(QuestColors.Purple)
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        // Selected Cell Detail Strip
                        FactDetailBox(
                            key = selectedFact,
                            mastery = mastery[selectedFact.id],
                            onTogglePin = { GameStateStore.togglePinnedFact(selectedFact) }
                        )
                    }
                }
            }

            // 3. Eye-care & Anti-addiction Settings
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("👀 护眼与防沉迷限额", style = MaterialTheme.typography.titleLarge, color = QuestColors.InkPrimary)
                        Text(
                            text = "到达上限后会在当前关卡结束时弹出温馨的「休息营地」，不会中途打断战斗。",
                            fontSize = 12.sp,
                            color = QuestColors.InkSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("每日冒险时长上限", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = QuestColors.InkPrimary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MINUTE_OPTIONS.forEach { m ->
                                FilterChip(
                                    selected = settings.dailyMinutesLimit == m,
                                    onClick = { GameStateStore.updateParentSettings { it.copy(dailyMinutesLimit = m) } },
                                    label = { Text(if (m == 0) "不限" else "$m 分钟") }
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("每日通关数上限", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = QuestColors.InkPrimary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            STAGE_OPTIONS.forEach { s ->
                                FilterChip(
                                    selected = settings.dailyStagesLimit == s,
                                    onClick = { GameStateStore.updateParentSettings { it.copy(dailyStagesLimit = s) } },
                                    label = { Text(if (s == 0) "不限" else "$s 关") }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            GameStateStore.grantParentBonusTime(extraMinutes = 5, extraStages = 1)
                            Toast.makeText(context, "已给今天增加 5 分钟与 1 关奖励！", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("🎁 临时奖励今天 +5 分钟 / +1 关", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // 4. Audio & Progression Controls
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("口诀语音朗读 (TTS)", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("自动读题并在答题后朗读「三七二十一」") },
                            trailingContent = {
                                Switch(
                                    checked = settings.ttsEnabled,
                                    onCheckedChange = { v -> GameStateStore.updateParentSettings { it.copy(ttsEnabled = v) } }
                                )
                            }
                        )
                        ListItem(
                            headlineContent = { Text("魔法与连击音效", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("施法、连击升调与胜利号角音效") },
                            trailingContent = {
                                Switch(
                                    checked = settings.sfxEnabled,
                                    onCheckedChange = { v -> GameStateStore.updateParentSettings { it.copy(sfxEnabled = v) } }
                                )
                            }
                        )
                        ListItem(
                            headlineContent = { Text("震动反馈", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("暴击与破盾时给予轻微手感震动") },
                            trailingContent = {
                                Switch(
                                    checked = settings.hapticsEnabled,
                                    onCheckedChange = { v -> GameStateStore.updateParentSettings { it.copy(hapticsEnabled = v) } }
                                )
                            }
                        )
                        ListItem(
                            headlineContent = { Text("自由开放全部 9 座岛屿", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("打开后孩子可直接挑 6、7、8、9 岛练习，无需逐关解锁") },
                            trailingContent = {
                                Switch(
                                    checked = settings.unlockAllIslands,
                                    onCheckedChange = { v -> GameStateStore.updateParentSettings { it.copy(unlockAllIslands = v) } }
                                )
                            }
                        )
                    }
                }
            }

            // 5. Reset Progress
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text("重置全部游戏与学习记录", color = QuestColors.DangerRed, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("清空勇者等级、装备、图鉴与 9×9 熟练度数据") },
                        modifier = Modifier.clickable { confirmReset = true }
                    )
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("确定要重置全部进度吗？") },
            text = { Text("重置后孩子需要重新选择勇者并从头收集装备和图鉴，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    GameStateStore.resetAllProgress()
                    confirmReset = false
                    Toast.makeText(context, "已重置", Toast.LENGTH_SHORT).show()
                    onBack()
                }) { Text("确定重置", color = QuestColors.DangerRed, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SummaryStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.tabular(),
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(label, fontSize = 12.sp, color = QuestColors.InkSecondary)
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = QuestColors.InkSecondary)
    }
}

@Composable
private fun FactDetailBox(
    key: FactKey,
    mastery: FactMastery?,
    onTogglePin: () -> Unit
) {
    val rhyme = CurriculumCatalog.chineseRhyme(key.minFactor, key.maxFactor)
    val pinned = mastery?.pinnedForFocus == true

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = QuestColors.CardWarm,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${key.minFactor} × ${key.maxFactor} = ${key.product}（$rhyme）",
                    style = MaterialTheme.typography.titleMedium.tabular(),
                    color = QuestColors.InkPrimary
                )
                val info = if (mastery == null || mastery.attempts == 0) {
                    "尚未练习到这句口诀"
                } else {
                    val pct = (mastery.accuracy * 100f).toInt()
                    val sec = String.format(Locale.ROOT, "%.1f", mastery.avgResponseMs / 1000f)
                    "练习 ${mastery.attempts} 次 · 正确率 $pct% · 平均 ${sec} 秒"
                }
                Text(info, fontSize = 12.sp, color = QuestColors.InkSecondary)
            }
            TextButton(onClick = onTogglePin) {
                Text(
                    text = if (pinned) "★ 已重点特训" else "☆ 加入特训",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (pinned) QuestColors.PurpleDark else QuestColors.SkyDark
                )
            }
        }
    }
}
