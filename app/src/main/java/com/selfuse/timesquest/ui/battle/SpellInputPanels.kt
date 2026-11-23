package com.selfuse.timesquest.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.selfuse.timesquest.data.BattleQuestion
import com.selfuse.timesquest.ui.components.TactileRpgButton
import com.selfuse.timesquest.ui.theme.QuestColors
import com.selfuse.timesquest.ui.theme.tabular

/**
 * Renders the active spell input UI for all 5 RPG combat mechanics.
 */
@Composable
fun SpellInputPanel(
    question: BattleQuestion,
    poppedBubbleIds: Set<Int>,
    elapsedQuestionMs: Long,
    onSubmitNumber: (Int) -> Unit,
    onTapBubble: (Int) -> Unit,
    onSubmitCompare: (Boolean) -> Unit,
    onReplayVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (question) {
        is BattleQuestion.MultipleChoice -> MultipleChoiceSpellPanel(
            q = question,
            elapsedMs = elapsedQuestionMs,
            onSelect = onSubmitNumber,
            onReplayVoice = onReplayVoice,
            modifier = modifier
        )

        is BattleQuestion.MissingFactorShield -> MissingFactorShieldPanel(
            q = question,
            onSelect = onSubmitNumber,
            onReplayVoice = onReplayVoice,
            modifier = modifier
        )

        is BattleQuestion.KeypadUltimate -> KeypadUltimatePanel(
            q = question,
            onSubmit = onSubmitNumber,
            onReplayVoice = onReplayVoice,
            modifier = modifier
        )

        is BattleQuestion.BubbleMatchSet -> BubbleMatchPanel(
            q = question,
            poppedIds = poppedBubbleIds,
            onTapBubble = onTapBubble,
            modifier = modifier
        )

        is BattleQuestion.CompareDuel -> CompareDuelPanel(
            q = question,
            onPickLeft = onSubmitCompare,
            modifier = modifier
        )

        is BattleQuestion.BossCounterQte -> BossCounterQtePanel(
            q = question,
            elapsedMs = elapsedQuestionMs,
            onSelect = onSubmitNumber,
            modifier = modifier
        )
    }
}

@Composable
private fun MultipleChoiceSpellPanel(
    q: BattleQuestion.MultipleChoice,
    elapsedMs: Long,
    onSelect: (Int) -> Unit,
    onReplayVoice: () -> Unit,
    modifier: Modifier
) {
    val critRatio = (1f - elapsedMs.toFloat() / q.fastBonusMs.toFloat()).coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Fast Crit Speed Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (q.isRevenge) "🔥 复仇时刻！再战这句口诀" else if (critRatio > 0f) "⚡ 极速暴击充能中！" else "✨ 稳稳选出魔法咒语",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (q.isRevenge) QuestColors.CoralDark else QuestColors.SkyDark
            )
            TactileRpgButton(
                onClick = onReplayVoice,
                containerColor = Color.White,
                ledgeColor = QuestColors.BorderSoft,
                contentColor = QuestColors.SkyDark,
                cornerRadius = 12.dp,
                ledgeHeight = 3.dp,
                modifier = Modifier.height(34.dp)
            ) {
                Text("🔊 读题", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { critRatio },
            color = if (critRatio > 0.35f) QuestColors.Gold else QuestColors.CoralAccent,
            trackColor = QuestColors.BorderSoft,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(99.dp))
        )
        Spacer(Modifier.height(12.dp))

        EquationPromptCard(
            equation = "${q.leftFactor} × ${q.rightFactor} = ?",
            borderColor = if (q.isRevenge) QuestColors.CoralAccent else QuestColors.SkyPrimary
        )

        Spacer(Modifier.height(14.dp))
        FourChoiceGrid(
            options = q.options,
            containerColor = QuestColors.SkyPrimary,
            ledgeColor = QuestColors.SkyDark,
            onSelect = onSelect
        )
    }
}

@Composable
private fun MissingFactorShieldPanel(
    q: BattleQuestion.MissingFactorShield,
    onSelect: (Int) -> Unit,
    onReplayVoice: () -> Unit,
    modifier: Modifier
) {
    val eqText = if (q.hideLeft) {
        "□ × ${q.rightFactor} = ${q.product}"
    } else {
        "${q.leftFactor} × □ = ${q.product}"
    }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🛡️ 破盾逆向咒！找出藏在 □ 里的数",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = QuestColors.PurpleDark
            )
            TactileRpgButton(
                onClick = onReplayVoice,
                containerColor = Color.White,
                ledgeColor = QuestColors.BorderSoft,
                contentColor = QuestColors.PurpleDark,
                cornerRadius = 12.dp,
                ledgeHeight = 3.dp,
                modifier = Modifier.height(34.dp)
            ) {
                Text("🔊 读题", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))

        EquationPromptCard(
            equation = eqText,
            borderColor = QuestColors.Purple,
            badgeText = "破盾后怪物眩晕 · 下回合伤害翻倍！"
        )

        Spacer(Modifier.height(14.dp))
        FourChoiceGrid(
            options = q.options,
            containerColor = QuestColors.Purple,
            ledgeColor = QuestColors.PurpleDark,
            onSelect = onSelect
        )
    }
}

@Composable
private fun KeypadUltimatePanel(
    q: BattleQuestion.KeypadUltimate,
    onSubmit: (Int) -> Unit,
    onReplayVoice: () -> Unit,
    modifier: Modifier
) {
    var typed by remember(q) { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (q.isUltimateCast) "⚡ 终极奥义就绪！亲手敲出答案放绝招！" else "🔮 符文精准施法！直接输入答案",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = QuestColors.CoralDark
            )
            TactileRpgButton(
                onClick = onReplayVoice,
                containerColor = Color.White,
                ledgeColor = QuestColors.BorderSoft,
                contentColor = QuestColors.CoralDark,
                cornerRadius = 12.dp,
                ledgeHeight = 3.dp,
                modifier = Modifier.height(34.dp)
            ) {
                Text("🔊", fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (q.isUltimateCast) Color(0xFFFFF8E1) else Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(2.5.dp, if (q.isUltimateCast) QuestColors.Gold else QuestColors.CoralAccent, RoundedCornerShape(20.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${q.leftFactor} × ${q.rightFactor} = ",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = QuestColors.InkPrimary,
                    style = MaterialTheme.typography.headlineLarge.tabular()
                )
                Box(
                    modifier = Modifier
                        .width(86.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(2.5.dp, QuestColors.CoralAccent, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = typed.ifEmpty { "?" },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = if (typed.isEmpty()) Color(0xFFBDB5A6) else QuestColors.CoralDark,
                        style = MaterialTheme.typography.headlineLarge.tabular()
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Chunky 3x4 Rune Number Keypad (large hit targets for 8-year-old fingers)
        val rows = listOf(
            listOf("1", "2", "3", "4", "5"),
            listOf("6", "7", "8", "9", "0")
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { digit ->
                        TactileRpgButton(
                            onClick = {
                                if (typed.length < 2) {
                                    typed = if (typed == "0") digit else typed + digit
                                }
                            },
                            containerColor = Color.White,
                            ledgeColor = Color(0xFFD7CEBC),
                            contentColor = QuestColors.InkPrimary,
                            cornerRadius = 14.dp,
                            ledgeHeight = 4.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        ) {
                            Text(digit, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TactileRpgButton(
                    onClick = { typed = "" },
                    containerColor = Color(0xFFECE5D8),
                    ledgeColor = Color(0xFFCBBFA9),
                    contentColor = QuestColors.InkPrimary,
                    cornerRadius = 16.dp,
                    ledgeHeight = 4.dp,
                    modifier = Modifier
                        .weight(0.9f)
                        .height(54.dp)
                ) {
                    Text("重输", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                TactileRpgButton(
                    onClick = {
                        val v = typed.toIntOrNull()
                        if (v != null) {
                            typed = ""
                            onSubmit(v)
                        }
                    },
                    enabled = typed.isNotEmpty(),
                    containerColor = if (q.isUltimateCast) QuestColors.CoralAccent else QuestColors.Emerald,
                    ledgeColor = if (q.isUltimateCast) QuestColors.CoralDark else QuestColors.EmeraldDark,
                    cornerRadius = 16.dp,
                    ledgeHeight = 4.dp,
                    modifier = Modifier
                        .weight(2.1f)
                        .height(54.dp)
                ) {
                    Text(
                        text = if (q.isUltimateCast) "⚡ 释放终极奥义！" else "✨ 发射魔法！",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun BubbleMatchPanel(
    q: BattleQuestion.BubbleMatchSet,
    poppedIds: Set<Int>,
    onTapBubble: (Int) -> Unit,
    modifier: Modifier
) {
    val totalMatches = q.bubbles.count { it.isMatch }
    val remaining = (totalMatches - poppedIds.size).coerceAtLeast(0)

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFE0F7FA),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color(0xFF00ACC1), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🫧 找出所有等于 ${q.targetProduct} 的魔法泡泡！",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF006064)
                )
                Text(
                    text = "还差 $remaining 个正确泡泡就能发动大水花攻击！",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00838F)
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            q.bubbles.chunked(3).forEach { rowBubbles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowBubbles.forEach { b ->
                        val popped = b.id in poppedIds
                        TactileRpgButton(
                            onClick = { onTapBubble(b.id) },
                            enabled = !popped,
                            containerColor = if (popped) QuestColors.Gold else Color(0xFF26C6DA),
                            ledgeColor = if (popped) QuestColors.GoldDark else Color(0xFF00838F),
                            contentColor = if (popped) QuestColors.InkPrimary else Color.White,
                            cornerRadius = 22.dp,
                            ledgeHeight = 5.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(68.dp)
                        ) {
                            Text(
                                text = if (popped) "⭐ ${b.label}" else b.label,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareDuelPanel(
    q: BattleQuestion.CompareDuel,
    onPickLeft: (Boolean) -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFFFF3E0),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, QuestColors.Gold, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚔️ 魔法比大小！点出得数更大的咒语！",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = QuestColors.CoralDark
                )
                Text(
                    text = "口算两边的结果，点中更大的就能先手暴击！",
                    fontSize = 12.sp,
                    color = QuestColors.InkSecondary
                )
            }
        }
        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TactileRpgButton(
                onClick = { onPickLeft(true) },
                containerColor = QuestColors.SkyPrimary,
                ledgeColor = QuestColors.SkyDark,
                cornerRadius = 22.dp,
                ledgeHeight = 6.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(112.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("蓝色咒语", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                    Spacer(Modifier.height(4.dp))
                    Text("${q.leftFactor} × ${q.rightFactor}", fontSize = 30.sp, fontWeight = FontWeight.Black)
                }
            }

            Text(
                text = "VS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = QuestColors.CoralAccent
            )

            TactileRpgButton(
                onClick = { onPickLeft(false) },
                containerColor = QuestColors.CoralAccent,
                ledgeColor = QuestColors.CoralDark,
                cornerRadius = 22.dp,
                ledgeHeight = 6.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(112.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("火焰咒语", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                    Spacer(Modifier.height(4.dp))
                    Text("${q.altLeft} × ${q.altRight}", fontSize = 30.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun BossCounterQtePanel(
    q: BattleQuestion.BossCounterQte,
    elapsedMs: Long,
    onSelect: (Int) -> Unit,
    modifier: Modifier
) {
    val remainRatio = (1f - elapsedMs.toFloat() / q.timeLimitMs.toFloat()).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = QuestColors.DangerRed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "⚠️ Boss 正在放大招！快答对触发【完美弹反】！",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { remainRatio },
            color = QuestColors.DangerRed,
            trackColor = Color(0xFFFFCDD2),
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(99.dp))
        )
        Spacer(Modifier.height(10.dp))
        EquationPromptCard(
            equation = "${q.leftFactor} × ${q.rightFactor} = ?",
            borderColor = QuestColors.DangerRed
        )
        Spacer(Modifier.height(12.dp))
        FourChoiceGrid(
            options = q.options,
            containerColor = QuestColors.DangerRed,
            ledgeColor = QuestColors.DangerDark,
            onSelect = onSelect
        )
    }
}

@Composable
private fun EquationPromptCard(
    equation: String,
    borderColor: Color,
    badgeText: String? = null
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(2.5.dp, borderColor, RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = equation,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = QuestColors.InkPrimary,
                style = MaterialTheme.typography.headlineLarge.tabular()
            )
            if (badgeText != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = badgeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = borderColor
                )
            }
        }
    }
}

@Composable
private fun FourChoiceGrid(
    options: List<Int>,
    containerColor: Color,
    ledgeColor: Color,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { opt ->
                    TactileRpgButton(
                        onClick = { onSelect(opt) },
                        containerColor = containerColor,
                        ledgeColor = ledgeColor,
                        cornerRadius = 18.dp,
                        ledgeHeight = 5.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                    ) {
                        Text(
                            text = opt.toString(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}
