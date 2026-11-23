package com.selfuse.timesquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.selfuse.timesquest.audio.SoundEffectEngine
import com.selfuse.timesquest.audio.SpeechCoachEngine
import com.selfuse.timesquest.data.GameStateStore
import com.selfuse.timesquest.data.LevelDef
import com.selfuse.timesquest.engine.DailyLimitGuard
import com.selfuse.timesquest.ui.battle.BattleScreen
import com.selfuse.timesquest.ui.bestiary.BestiaryScreen
import com.selfuse.timesquest.ui.camp.HeroCampScreen
import com.selfuse.timesquest.ui.components.QuestIcons
import com.selfuse.timesquest.ui.map.WorldMapScreen
import com.selfuse.timesquest.ui.onboarding.OnboardingScreen
import com.selfuse.timesquest.ui.parent.EyeCareRestScreen
import com.selfuse.timesquest.ui.parent.ParentDashboardScreen
import com.selfuse.timesquest.ui.parent.ParentGateDialog
import com.selfuse.timesquest.ui.theme.QuestColors
import com.selfuse.timesquest.ui.theme.TimesQuestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameStateStore.ensureInitialized(applicationContext)
        enableEdgeToEdge()

        setContent {
            TimesQuestTheme {
                TimesQuestApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        GameStateStore.rollOverDayIfNeeded()
    }
}

private enum class HomeTab { MAP, CAMP, BESTIARY }

private enum class GatePurpose { OPEN_PARENT_DASHBOARD, GRANT_BONUS_TIME }

@Composable
private fun TimesQuestApp() {
    val context = LocalContext.current
    val state by GameStateStore.uiState.collectAsStateWithLifecycle()

    val speechCoach = remember(context) { SpeechCoachEngine(context) }
    DisposableEffect(speechCoach) {
        onDispose { speechCoach.shutdown() }
    }

    if (!state.initialized) {
        Surface(modifier = Modifier.fillMaxSize(), color = QuestColors.CreamBg) {}
        return
    }

    if (!state.hero.onboardingDone) {
        OnboardingScreen(sfxEnabled = state.settings.sfxEnabled)
        return
    }

    var currentTab by rememberSaveable { mutableStateOf(HomeTab.MAP) }
    var activeBattleLevel by remember { mutableStateOf<LevelDef?>(null) }
    var showParentDashboard by rememberSaveable { mutableStateOf(false) }
    var showRestCamp by rememberSaveable { mutableStateOf(false) }
    var gatePurpose by remember { mutableStateOf<GatePurpose?>(null) }

    val limitStatus = remember(state.dailyUsage, state.settings) {
        DailyLimitGuard.evaluate(state.dailyUsage, state.settings)
    }

    when {
        activeBattleLevel != null -> {
            BattleScreen(
                level = activeBattleLevel!!,
                gameState = state,
                speechCoach = speechCoach,
                onExitBattle = {
                    activeBattleLevel = null
                    val latestLimit = DailyLimitGuard.evaluate(
                        GameStateStore.uiState.value.dailyUsage,
                        GameStateStore.uiState.value.settings
                    )
                    if (latestLimit.isLocked) {
                        showRestCamp = true
                    }
                }
            )
        }

        showParentDashboard -> {
            ParentDashboardScreen(
                state = state,
                onBack = { showParentDashboard = false }
            )
        }

        showRestCamp && limitStatus.isLocked -> {
            EyeCareRestScreen(
                state = state,
                limitStatus = limitStatus,
                onRequestParentBonus = { gatePurpose = GatePurpose.GRANT_BONUS_TIME },
                onBackToCamp = {
                    showRestCamp = false
                    currentTab = HomeTab.CAMP
                }
            )
        }

        else -> {
            Scaffold(
                containerColor = QuestColors.CreamBg,
                bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        NavigationBarItem(
                            selected = currentTab == HomeTab.MAP,
                            onClick = {
                                SoundEffectEngine.playButtonTap(state.settings.sfxEnabled)
                                currentTab = HomeTab.MAP
                            },
                            icon = { Icon(QuestIcons.MapCompass, contentDescription = null) },
                            label = { Text("冒险地图", fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = QuestColors.CoralAccent,
                                selectedTextColor = QuestColors.CoralDark,
                                indicatorColor = Color(0xFFFFE5DA)
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == HomeTab.CAMP,
                            onClick = {
                                SoundEffectEngine.playButtonTap(state.settings.sfxEnabled)
                                currentTab = HomeTab.CAMP
                            },
                            icon = { Icon(QuestIcons.WandStar, contentDescription = null) },
                            label = { Text("勇者营地", fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = QuestColors.SkyPrimary,
                                selectedTextColor = QuestColors.SkyDark,
                                indicatorColor = Color(0xFFD9EEFF)
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == HomeTab.BESTIARY,
                            onClick = {
                                SoundEffectEngine.playButtonTap(state.settings.sfxEnabled)
                                currentTab = HomeTab.BESTIARY
                            },
                            icon = { Icon(QuestIcons.BestiaryBook, contentDescription = null) },
                            label = { Text("怪物图鉴", fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = QuestColors.Purple,
                                selectedTextColor = QuestColors.PurpleDark,
                                indicatorColor = Color(0xFFEDE7F6)
                            )
                        )
                    }
                }
            ) { padding ->
                when (currentTab) {
                    HomeTab.MAP -> WorldMapScreen(
                        state = state,
                        onStartLevel = { level ->
                            if (limitStatus.isLocked) {
                                showRestCamp = true
                            } else {
                                activeBattleLevel = level
                            }
                        },
                        onOpenParentGate = { gatePurpose = GatePurpose.OPEN_PARENT_DASHBOARD },
                        modifier = Modifier.padding(padding)
                    )

                    HomeTab.CAMP -> HeroCampScreen(
                        state = state,
                        modifier = Modifier.padding(padding)
                    )

                    HomeTab.BESTIARY -> BestiaryScreen(
                        state = state,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }

    val purpose = gatePurpose
    if (purpose != null) {
        ParentGateDialog(
            title = if (purpose == GatePurpose.GRANT_BONUS_TIME) "家长验证 · 奖励加时" else "家长守护中心",
            subtitle = if (purpose == GatePurpose.GRANT_BONUS_TIME) {
                "验证通过后将为今天增加 5 分钟与 1 关冒险时间："
            } else {
                "请家长算一下这道题，进入学习报告与护眼设置："
            },
            onVerified = {
                gatePurpose = null
                when (purpose) {
                    GatePurpose.OPEN_PARENT_DASHBOARD -> showParentDashboard = true
                    GatePurpose.GRANT_BONUS_TIME -> {
                        GameStateStore.grantParentBonusTime(extraMinutes = 5, extraStages = 1)
                        showRestCamp = false
                    }
                }
            },
            onDismiss = { gatePurpose = null }
        )
    }
}
