package com.selfuse.timesquest.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfuse.timesquest.audio.SoundEffectEngine
import com.selfuse.timesquest.data.CurriculumCatalog
import com.selfuse.timesquest.data.GameStateStore
import com.selfuse.timesquest.data.HeroAvatar
import com.selfuse.timesquest.engine.AdaptiveFactScheduler
import com.selfuse.timesquest.ui.components.SpriteImage
import com.selfuse.timesquest.ui.components.TactileRpgButton
import com.selfuse.timesquest.ui.theme.QuestColors
import com.selfuse.timesquest.ui.theme.tabular

private enum class OnboardStage { PICK_HERO, PICK_START, PLACEMENT_QUIZ, PLACEMENT_RESULT }

@Composable
fun OnboardingScreen(sfxEnabled: Boolean) {
    var stage by rememberSaveable { mutableStateOf(OnboardStage.PICK_HERO) }
    var selectedAvatar by rememberSaveable { mutableStateOf(HeroAvatar.BOY) }
    var heroName by rememberSaveable { mutableStateOf(HeroAvatar.BOY.defaultName) }

    val placementQuiz = remember { AdaptiveFactScheduler.generatePlacementQuiz() }
    var quizIdx by remember { mutableIntStateOf(0) }
    var easyCorrect by remember { mutableIntStateOf(0) }
    var hardCorrect by remember { mutableIntStateOf(0) }
    var totalMs by remember { mutableLongStateOf(0L) }
    var questionStartMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var recommendedIsland by remember { mutableIntStateOf(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFD6F1FF), QuestColors.CreamBg, Color(0xFFFFF2D6))
                )
            )
            .safeDrawingPadding()
    ) {
        AnimatedContent(targetState = stage, label = "onboardingStage") { current ->
            when (current) {
                OnboardStage.PICK_HERO -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "欢迎来到口诀大冒险！",
                            style = MaterialTheme.typography.headlineMedium,
                            color = QuestColors.InkPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "用乘法口诀释放魔法，一起打败奇幻群岛上的调皮怪兽吧！",
                            style = MaterialTheme.typography.bodyLarge,
                            color = QuestColors.InkSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = "第 1 步 · 选择你的小勇者",
                            style = MaterialTheme.typography.titleMedium,
                            color = QuestColors.SkyDark
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            HeroAvatar.entries.forEach { avatar ->
                                val chosen = selectedAvatar == avatar
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = if (chosen) Color.White else Color.White.copy(alpha = 0.72f),
                                    shadowElevation = if (chosen) 6.dp else 1.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            width = if (chosen) 3.dp else 1.dp,
                                            color = if (chosen) QuestColors.CoralAccent else QuestColors.BorderSoft,
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .clip(RoundedCornerShape(24.dp))
                                        .clickable {
                                            SoundEffectEngine.playButtonTap(sfxEnabled)
                                            selectedAvatar = avatar
                                            if (heroName == HeroAvatar.BOY.defaultName || heroName == HeroAvatar.GIRL.defaultName) {
                                                heroName = avatar.defaultName
                                            }
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        SpriteImage(
                                            spriteKey = "hero_${avatar.id}",
                                            contentDescription = avatar.label,
                                            modifier = Modifier.size(128.dp)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = avatar.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (chosen) QuestColors.CoralDark else QuestColors.InkPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        OutlinedTextField(
                            value = heroName,
                            onValueChange = { heroName = it.take(10) },
                            label = { Text("给小勇者起个名字") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(28.dp))
                        TactileRpgButton(
                            onClick = {
                                SoundEffectEngine.playButtonTap(sfxEnabled)
                                stage = OnboardStage.PICK_START
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            Text("选好啦，下一步！", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                OnboardStage.PICK_START -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SpriteImage(
                            spriteKey = "hero_${selectedAvatar.id}",
                            contentDescription = null,
                            modifier = Modifier.size(126.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "你好，${heroName.ifBlank { selectedAvatar.defaultName }}！",
                            style = MaterialTheme.typography.headlineMedium,
                            color = QuestColors.InkPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "你想从哪里开始冒险呢？",
                            style = MaterialTheme.typography.bodyLarge,
                            color = QuestColors.InkSecondary
                        )
                        Spacer(Modifier.height(28.dp))

                        TactileRpgButton(
                            onClick = {
                                SoundEffectEngine.playChestOpen(sfxEnabled)
                                GameStateStore.completeOnboarding(heroName, selectedAvatar, startIsland = 1)
                            },
                            containerColor = QuestColors.Emerald,
                            ledgeColor = QuestColors.EmeraldDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🏝️ 从第 1 岛轻松出发", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                Text("先从 1、2、5 的口诀热身打怪", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        TactileRpgButton(
                            onClick = {
                                SoundEffectEngine.playButtonTap(sfxEnabled)
                                quizIdx = 0
                                easyCorrect = 0
                                hardCorrect = 0
                                totalMs = 0L
                                questionStartMs = System.currentTimeMillis()
                                stage = OnboardStage.PLACEMENT_QUIZ
                            },
                            containerColor = QuestColors.SkyPrimary,
                            ledgeColor = QuestColors.SkyDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⚡ 我都学过啦！6 题小测试定级", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                Text("30 秒测出水平，直接解锁适合你的岛屿", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                            }
                        }
                    }
                }

                OnboardStage.PLACEMENT_QUIZ -> {
                    val q = placementQuiz[quizIdx]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "勇者摸底小试炼  ${quizIdx + 1} / ${placementQuiz.size}",
                            style = MaterialTheme.typography.titleMedium.tabular(),
                            color = QuestColors.SkyDark
                        )
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (quizIdx + 1).toFloat() / placementQuiz.size.toFloat() },
                            color = QuestColors.CoralAccent,
                            trackColor = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(99.dp))
                        )
                        Spacer(Modifier.height(28.dp))
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = Color.White,
                            shadowElevation = 6.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 32.dp, horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${q.a} × ${q.b} = ?",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Black,
                                    color = QuestColors.InkPrimary,
                                    style = MaterialTheme.typography.headlineLarge.tabular()
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "凭第一反应点出答案就行，不用紧张哦",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = QuestColors.InkSecondary
                                )
                            }
                        }

                        Spacer(Modifier.height(28.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            q.options.chunked(2).forEach { rowOpts ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    rowOpts.forEach { opt ->
                                        TactileRpgButton(
                                            onClick = {
                                                val elapsed = (System.currentTimeMillis() - questionStartMs).coerceIn(300L, 12_000L)
                                                totalMs += elapsed
                                                val ok = opt == q.a * q.b
                                                if (ok) {
                                                    SoundEffectEngine.playComboHit(sfxEnabled, quizIdx + 1, false)
                                                    if (q.isHardTier) hardCorrect++ else easyCorrect++
                                                } else {
                                                    SoundEffectEngine.playSoftMiss(sfxEnabled)
                                                }
                                                if (quizIdx + 1 < placementQuiz.size) {
                                                    quizIdx++
                                                    questionStartMs = System.currentTimeMillis()
                                                } else {
                                                    recommendedIsland = AdaptiveFactScheduler.evaluatePlacementIsland(
                                                        easyCorrect = easyCorrect,
                                                        hardCorrect = hardCorrect,
                                                        avgResponseMs = totalMs / placementQuiz.size
                                                    )
                                                    SoundEffectEngine.playVictoryFanfare(sfxEnabled)
                                                    stage = OnboardStage.PLACEMENT_RESULT
                                                }
                                            },
                                            containerColor = QuestColors.SkyPrimary,
                                            ledgeColor = QuestColors.SkyDark,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(72.dp)
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
                }

                OnboardStage.PLACEMENT_RESULT -> {
                    val island = CurriculumCatalog.island(recommendedIsland)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        SpriteImage(
                            spriteKey = CurriculumCatalog.monster(island.bossId).spriteKey,
                            contentDescription = island.name,
                            modifier = Modifier.size(160.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "哇，你好厉害！",
                            style = MaterialTheme.typography.headlineMedium,
                            color = QuestColors.CoralDark
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "已为你直接解锁到第 $recommendedIsland 岛\n「${island.name}」（${island.badgeText}）",
                            style = MaterialTheme.typography.titleLarge,
                            color = QuestColors.InkPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "前面的岛屿也可以随时进去赚金币和点亮图鉴哦！",
                            style = MaterialTheme.typography.bodyMedium,
                            color = QuestColors.InkSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(28.dp))
                        TactileRpgButton(
                            onClick = {
                                SoundEffectEngine.playChestOpen(sfxEnabled)
                                GameStateStore.completeOnboarding(heroName, selectedAvatar, startIsland = recommendedIsland)
                            },
                            containerColor = QuestColors.CoralAccent,
                            ledgeColor = QuestColors.CoralDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        ) {
                            Text("🚀 立刻踏上冒险之旅！", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}
