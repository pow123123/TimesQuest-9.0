package com.selfuse.timesquest.ui.bestiary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfuse.timesquest.data.CurriculumCatalog
import com.selfuse.timesquest.data.GameUiState
import com.selfuse.timesquest.data.MonsterDef
import com.selfuse.timesquest.ui.components.SpriteImage
import com.selfuse.timesquest.ui.theme.QuestColors
import com.selfuse.timesquest.ui.theme.tabular

@Composable
fun BestiaryScreen(
    state: GameUiState,
    modifier: Modifier = Modifier
) {
    val defeated = state.hero.defeatedMonsters
    val allMonsters = CurriculumCatalog.monsters
    val discoveredCount = allMonsters.count { (defeated[it.id] ?: 0) > 0 }
    var inspecting by remember { mutableStateOf<MonsterDef?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(QuestColors.CreamBg),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("📖 奇幻怪物图鉴", style = MaterialTheme.typography.titleLarge, color = QuestColors.InkPrimary)
                        Text("每打败一只新怪兽就会点亮它的彩色卡片！", fontSize = 12.sp, color = QuestColors.InkSecondary)
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF3CD)
                    ) {
                        Text(
                            text = "$discoveredCount / ${allMonsters.size}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = QuestColors.CoralDark,
                            style = MaterialTheme.typography.titleMedium.tabular(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        items(allMonsters, key = { it.id }) { monster ->
            val count = defeated[monster.id] ?: 0
            val discovered = count > 0
            val accent = Color(monster.accentHex)

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (discovered) Color.White else Color(0xFFF2ECE0),
                shadowElevation = if (discovered) 3.dp else 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (discovered && monster.isBoss) 2.dp else 1.dp,
                        color = if (discovered) accent.copy(alpha = 0.45f) else QuestColors.BorderSoft,
                        shape = RoundedCornerShape(22.dp)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(enabled = discovered) { inspecting = monster }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = if (monster.isBoss) QuestColors.CoralAccent.copy(alpha = 0.16f) else Color(0xFFE8EEF5)
                    ) {
                        Text(
                            text = if (monster.isBoss) "👑 第${monster.islandId}岛 Boss" else "第${monster.islandId}岛小怪",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (monster.isBoss) QuestColors.CoralDark else QuestColors.SkyDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    SpriteImage(
                        spriteKey = monster.spriteKey,
                        contentDescription = if (discovered) monster.name else "未发现怪物",
                        silhouette = !discovered,
                        modifier = Modifier.size(88.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (discovered) monster.name else "？？？",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (discovered) QuestColors.InkPrimary else QuestColors.InkSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    Text(
                        text = if (discovered) "已击败 $count 次" else "去第 ${monster.islandId} 岛遇见它",
                        fontSize = 11.sp,
                        color = QuestColors.InkSecondary,
                        style = MaterialTheme.typography.bodySmall.tabular()
                    )
                }
            }
        }
    }

    val detail = inspecting
    if (detail != null) {
        val count = defeated[detail.id] ?: 0
        AlertDialog(
            onDismissRequest = { inspecting = null },
            title = {
                Text(
                    text = if (detail.isBoss) "👑 ${detail.name}" else detail.name,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    SpriteImage(
                        spriteKey = detail.spriteKey,
                        contentDescription = detail.name,
                        modifier = Modifier.size(130.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(detail.title, fontWeight = FontWeight.Bold, color = Color(detail.accentHex))
                    Spacer(Modifier.height(6.dp))
                    Text(detail.flavorText, textAlign = TextAlign.Center, color = QuestColors.InkPrimary)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "弱点口诀：${detail.weakFactHint} · 已击败 $count 次",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuestColors.CoralDark
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { inspecting = null }) {
                    Text("关闭", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
