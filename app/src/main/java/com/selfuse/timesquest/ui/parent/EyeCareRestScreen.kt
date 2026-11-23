package com.selfuse.timesquest.ui.parent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfuse.timesquest.data.GameUiState
import com.selfuse.timesquest.engine.LimitStatus
import com.selfuse.timesquest.ui.components.SpriteImage
import com.selfuse.timesquest.ui.components.TactileRpgButton
import com.selfuse.timesquest.ui.theme.QuestColors

/**
 * Cozy campfire rest screen shown when daily eye-care limit is reached.
 */
@Composable
fun EyeCareRestScreen(
    state: GameUiState,
    limitStatus: LimitStatus,
    onRequestParentBonus: () -> Unit,
    onBackToCamp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A2347), Color(0xFF2E3B6E), Color(0xFF47578A))
                )
            )
            .safeDrawingPadding()
    ) {
        // Twinkling starry night background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stars = listOf(
                0.15f to 0.12f, 0.35f to 0.08f, 0.78f to 0.14f, 0.88f to 0.24f,
                0.22f to 0.26f, 0.62f to 0.10f, 0.48f to 0.20f, 0.10f to 0.34f
            )
            stars.forEachIndexed { idx, (rx, ry) ->
                drawCircle(
                    color = Color(0xFFFFF59D).copy(alpha = if (idx % 2 == 0) 0.85f else 0.55f),
                    radius = (if (idx % 3 == 0) 4.dp else 2.5.dp).toPx(),
                    center = Offset(size.width * rx, size.height * ry)
                )
            }
            // Warm glowing campfire moon
            drawCircle(
                color = Color(0xFFFFF176),
                radius = 28.dp.toPx(),
                center = Offset(size.width * 0.82f, size.height * 0.12f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SpriteImage(
                spriteKey = "hero_${state.hero.avatar.id}",
                contentDescription = state.hero.name,
                modifier = Modifier.size(156.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "⛺ 魔法休息营地",
                style = MaterialTheme.typography.headlineMedium,
                color = QuestColors.Gold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = limitStatus.reasonText,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "望望窗外远处的风景，喝口水活动一下，明天魔法能量就会全部充满哦！",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            TactileRpgButton(
                onClick = onBackToCamp,
                containerColor = QuestColors.Gold,
                ledgeColor = QuestColors.GoldDark,
                contentColor = QuestColors.InkPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Text("🏕️ 回营地看看装扮和图鉴", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(14.dp))
            TextButton(onClick = onRequestParentBonus) {
                Text(
                    text = "家长验证：奖励加时 5 分钟 / 再玩 1 关",
                    color = Color(0xFFB3E5FC),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
