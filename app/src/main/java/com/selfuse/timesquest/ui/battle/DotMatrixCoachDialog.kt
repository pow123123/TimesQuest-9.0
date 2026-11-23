package com.selfuse.timesquest.ui.battle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfuse.timesquest.engine.CorrectionCoach
import com.selfuse.timesquest.ui.components.TactileRpgButton
import com.selfuse.timesquest.ui.theme.QuestColors
import com.selfuse.timesquest.ui.theme.tabular

/**
 * Encouraging mistake-teaching dialog with a visual magic gem dot-matrix array (rows x cols),
 * golden Chinese rhyme callout, TTS replay button, and "我记住啦" confirmation.
 */
@Composable
fun DotMatrixCoachDialog(
    coach: CorrectionCoach,
    onReplayRhyme: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = QuestColors.CreamBg,
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, QuestColors.Gold, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (coach.isTimeout) "⏰ 没关系，我们记住它！" else "✨ 口诀魔法卷轴来帮忙",
                    style = MaterialTheme.typography.titleLarge,
                    color = QuestColors.CoralDark
                )
                Spacer(Modifier.height(10.dp))

                // Big golden rhyme banner
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFFFF3CD),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, QuestColors.Gold, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${coach.leftFactor} × ${coach.rightFactor} = ${coach.product}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = QuestColors.InkPrimary,
                            style = MaterialTheme.typography.headlineLarge.tabular()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "「${coach.rhyme}」",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = QuestColors.CoralDark
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = coach.detailHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = QuestColors.InkSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))

                // Visual Magic Gem Dot Matrix (rows = leftFactor, cols = rightFactor)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, QuestColors.BorderSoft, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${coach.leftFactor} 排魔法宝石，每排 ${coach.rightFactor} 颗，一共 ${coach.product} 颗",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuestColors.SkyDark
                        )
                        Spacer(Modifier.height(8.dp))
                        MagicGemMatrix(
                            rows = coach.leftFactor.coerceIn(1, 9),
                            cols = coach.rightFactor.coerceIn(1, 9),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((coach.leftFactor * 16 + 18).coerceIn(58, 160).dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TactileRpgButton(
                        onClick = onReplayRhyme,
                        containerColor = QuestColors.SkyPrimary,
                        ledgeColor = QuestColors.SkyDark,
                        cornerRadius = 16.dp,
                        ledgeHeight = 4.dp,
                        modifier = Modifier
                            .weight(0.95f)
                            .height(54.dp)
                    ) {
                        Text("🔊 听一遍", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    TactileRpgButton(
                        onClick = onConfirm,
                        containerColor = QuestColors.Emerald,
                        ledgeColor = QuestColors.EmeraldDark,
                        cornerRadius = 16.dp,
                        ledgeHeight = 4.dp,
                        modifier = Modifier
                            .weight(1.45f)
                            .height(54.dp)
                    ) {
                        Text("我记住啦 (${coach.product})", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MagicGemMatrix(rows: Int, cols: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val padX = 12.dp.toPx()
        val padY = 6.dp.toPx()
        val availW = (size.width - padX * 2).coerceAtLeast(20f)
        val availH = (size.height - padY * 2).coerceAtLeast(20f)

        // Add subtle gap after column 5 when cols > 5 so kids can subitize 5 + remainder!
        val extraGapCols = if (cols > 5) 0.45f else 0f
        val cellW = availW / (cols + extraGapCols)
        val cellH = availH / rows
        val radius = (minOf(cellW, cellH) * 0.36f).coerceIn(3.5.dp.toPx(), 10.dp.toPx())

        val totalGridW = cols * cellW + extraGapCols * cellW
        val startX = (size.width - totalGridW) / 2f + cellW / 2f
        val startY = (size.height - rows * cellH) / 2f + cellH / 2f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val groupOffset = if (cols > 5 && c >= 5) cellW * extraGapCols else 0f
                val cx = startX + c * cellW + groupOffset
                val cy = startY + r * cellH
                val gemColor = if (c < 5) QuestColors.SkyPrimary else QuestColors.CoralAccent
                drawCircle(color = gemColor, radius = radius, center = Offset(cx, cy))
                drawCircle(
                    color = Color.White.copy(alpha = 0.65f),
                    radius = radius * 0.35f,
                    center = Offset(cx - radius * 0.25f, cy - radius * 0.25f)
                )
            }
        }
    }
}
