package com.selfuse.timesquest.ui.battle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfuse.timesquest.data.ElementStyle
import com.selfuse.timesquest.data.HeroProgress
import com.selfuse.timesquest.data.IslandTheme
import com.selfuse.timesquest.engine.BattleSnapshot
import com.selfuse.timesquest.engine.BattleVisualEvent
import com.selfuse.timesquest.ui.components.HatChipLabel
import com.selfuse.timesquest.ui.components.SpriteImage
import com.selfuse.timesquest.ui.theme.QuestColors
import com.selfuse.timesquest.ui.theme.tabular
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Animated RPG Battle Stage with painted island backdrop, hero/pet/monster sprites,
 * curved elemental magic projectiles, particle bursts, screen shake, shield domes,
 * and floating crit/rhyme callouts.
 */
@Composable
fun BattleStageCanvas(
    snapshot: BattleSnapshot,
    hero: HeroProgress,
    wandElement: ElementStyle,
    petSpriteKey: String?,
    theme: IslandTheme,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "stageIdle")
    val heroBob by infinite.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "heroBob"
    )
    val monsterBob by infinite.animateFloat(
        initialValue = 5f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(tween(1350), RepeatMode.Reverse),
        label = "monsterBob"
    )
    val cloudShift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudShift"
    )
    val qtePulse by infinite.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "qtePulse"
    )

    // One-shot combat animations
    val projectileProgress = remember { Animatable(0f) }
    val burstProgress = remember { Animatable(0f) }
    val heroLunge = remember { Animatable(0f) }
    val monsterRecoil = remember { Animatable(0f) }
    val screenShake = remember { Animatable(0f) }
    val floatTextProgress = remember { Animatable(1f) }

    var projectileActive by remember { mutableStateOf(false) }
    var isMonsterProjectile by remember { mutableStateOf(false) }
    var isPetBoltActive by remember { mutableStateOf(false) }
    var isUltimateBeam by remember { mutableStateOf(false) }
    var calloutPrimary by remember { mutableStateOf("") }
    var calloutSub by remember { mutableStateOf("") }
    var calloutColor by remember { mutableStateOf(QuestColors.Gold) }

    LaunchedEffect(snapshot.eventSeq) {
        val ev = snapshot.lastEvent ?: return@LaunchedEffect
        when (ev) {
            is BattleVisualEvent.HeroAttack -> {
                isMonsterProjectile = false
                isUltimateBeam = ev.isUltimate
                isPetBoltActive = ev.petBonusDamage > 0
                calloutPrimary = when {
                    ev.isUltimate -> "⚡ 奥义 -${ev.damage}!"
                    ev.isParry -> "🛡️ 弹反 -${ev.damage}!"
                    ev.isShieldBreak -> "💥 破盾 -${ev.damage}!"
                    ev.isCrit -> "🔥 暴击 -${ev.damage}!"
                    else -> "-${ev.damage}"
                }
                calloutSub = buildString {
                    append(ev.rhymeText)
                    if (ev.isRevenge) append(" · 复仇成功!")
                    if (ev.petBonusDamage > 0) append(" · 萌宠 +${ev.petBonusDamage}!")
                }
                calloutColor = when {
                    ev.isUltimate -> Color(0xFFFFD600)
                    ev.isParry -> Color(0xFF40C4FF)
                    ev.isCrit -> Color(0xFFFF6D00)
                    else -> Color.White
                }

                launch {
                    heroLunge.snapTo(0f)
                    heroLunge.animateTo(24f, tween(110))
                    heroLunge.animateTo(0f, tween(180))
                }
                projectileActive = true
                projectileProgress.snapTo(0f)
                projectileProgress.animateTo(1f, tween(if (ev.isUltimate) 290 else 220, easing = FastOutLinearInEasing))
                projectileActive = false

                // Impact burst + recoil + shake
                launch {
                    burstProgress.snapTo(0f)
                    burstProgress.animateTo(1f, tween(340))
                }
                launch {
                    monsterRecoil.snapTo(0f)
                    monsterRecoil.animateTo(18f, tween(80))
                    monsterRecoil.animateTo(-10f, tween(70))
                    monsterRecoil.animateTo(0f, tween(120))
                }
                if (ev.isCrit || ev.isUltimate || ev.isShieldBreak || ev.isParry) {
                    launch {
                        val amp = if (ev.isUltimate) 14f else 8f
                        screenShake.snapTo(amp)
                        screenShake.animateTo(-amp * 0.7f, tween(55))
                        screenShake.animateTo(amp * 0.4f, tween(55))
                        screenShake.animateTo(0f, tween(70))
                    }
                }
                launch {
                    floatTextProgress.snapTo(0f)
                    floatTextProgress.animateTo(1f, tween(900))
                }
            }

            is BattleVisualEvent.MonsterHit -> {
                isMonsterProjectile = true
                isUltimateBeam = false
                isPetBoltActive = false
                calloutPrimary = if (ev.wasTimeout) "⏰ 没挡住!" else "呀，偏了一点"
                calloutSub = ev.rhymeText
                calloutColor = Color(0xFFFFCDD2)

                projectileActive = true
                projectileProgress.snapTo(0f)
                projectileProgress.animateTo(1f, tween(220))
                projectileActive = false

                launch {
                    heroLunge.snapTo(-18f)
                    heroLunge.animateTo(0f, tween(180))
                }
                launch {
                    screenShake.snapTo(9f)
                    screenShake.animateTo(-6f, tween(60))
                    screenShake.animateTo(0f, tween(70))
                }
                launch {
                    floatTextProgress.snapTo(0f)
                    floatTextProgress.animateTo(1f, tween(850))
                }
            }

            is BattleVisualEvent.BubblePop -> {
                calloutPrimary = "🫧 啵! ${ev.label}"
                calloutSub = "继续点出相等的泡泡！"
                calloutColor = Color(0xFF80DEEA)
                launch {
                    floatTextProgress.snapTo(0f)
                    floatTextProgress.animateTo(1f, tween(650))
                }
            }

            is BattleVisualEvent.PotionRevive -> {
                calloutPrimary = "🍓 魔法药水满血复活！"
                calloutSub = "继续战斗！"
                calloutColor = Color(0xFFFF80AB)
                launch {
                    floatTextProgress.snapTo(0f)
                    floatTextProgress.animateTo(1f, tween(900))
                }
            }

            is BattleVisualEvent.EndlessWaveCleared -> {
                calloutPrimary = "🏆 第 ${ev.nextWave} 波来袭！"
                calloutSub = ev.nextMonsterName
                calloutColor = QuestColors.Gold
                launch {
                    floatTextProgress.snapTo(0f)
                    floatTextProgress.animateTo(1f, tween(950))
                }
            }
        }
    }

    val stageShape = RoundedCornerShape(26.dp)
    val feverBorder = when {
        snapshot.isBossQteActive -> QuestColors.DangerRed.copy(alpha = qtePulse + 0.25f)
        snapshot.isFever -> QuestColors.Gold
        else -> Color.White.copy(alpha = 0.65f)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(screenShake.value.roundToInt(), (screenShake.value * 0.4f).roundToInt()) }
            .clip(stageShape)
            .border(
                width = if (snapshot.isFever || snapshot.isBossQteActive) 3.5.dp else 2.dp,
                color = feverBorder,
                shape = stageShape
            )
    ) {
        val stageW = maxWidth
        val stageH = maxHeight

        // 1. Painted Fantasy Island Backdrop Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Sky gradient
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(theme.skyTopHex), Color(theme.skyBottomHex))
                )
            )

            // Soft clouds drifting gently
            val cloudAlpha = 0.42f
            val c1x = ((w * 0.2f) + cloudShift * w) % (w + 180f) - 90f
            val c2x = ((w * 0.72f) + cloudShift * w * 0.65f) % (w + 200f) - 100f
            drawCircle(Color.White.copy(alpha = cloudAlpha), radius = 34.dp.toPx(), center = Offset(c1x, h * 0.22f))
            drawCircle(Color.White.copy(alpha = cloudAlpha), radius = 24.dp.toPx(), center = Offset(c1x + 26.dp.toPx(), h * 0.24f))
            drawCircle(Color.White.copy(alpha = cloudAlpha * 0.85f), radius = 28.dp.toPx(), center = Offset(c2x, h * 0.16f))
            drawCircle(Color.White.copy(alpha = cloudAlpha * 0.85f), radius = 20.dp.toPx(), center = Offset(c2x - 22.dp.toPx(), h * 0.18f))

            // Rolling distant hills
            val hillPath = Path().apply {
                moveTo(0f, h * 0.72f)
                quadraticTo(w * 0.28f, h * 0.56f, w * 0.55f, h * 0.69f)
                quadraticTo(w * 0.82f, h * 0.80f, w, h * 0.64f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(hillPath, color = Color(theme.hillHex))

            // Foreground battle meadow / ground
            val groundPath = Path().apply {
                moveTo(0f, h * 0.76f)
                quadraticTo(w * 0.5f, h * 0.70f, w, h * 0.77f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(groundPath, color = Color(theme.groundHex))

            // Ground contact ellipses under Hero & Monster
            drawOval(
                color = Color.Black.copy(alpha = 0.16f),
                topLeft = Offset(w * 0.10f, h * 0.84f),
                size = androidx.compose.ui.geometry.Size(w * 0.26f, h * 0.08f)
            )
            drawOval(
                color = Color.Black.copy(alpha = 0.18f),
                topLeft = Offset(w * 0.61f, h * 0.84f),
                size = androidx.compose.ui.geometry.Size(w * 0.30f, h * 0.09f)
            )

            // Fever golden aura glow around hero
            if (snapshot.isFever) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(QuestColors.Gold.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(w * 0.23f, h * 0.64f),
                        radius = h * 0.36f
                    ),
                    radius = h * 0.36f,
                    center = Offset(w * 0.23f, h * 0.64f)
                )
            }

            // Boss Enrage Red Warning Glow
            if (snapshot.isBossQteActive) {
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(Color.Transparent, QuestColors.DangerRed.copy(alpha = qtePulse * 0.5f)),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.65f
                    )
                )
            }
        }

        // 2. Top HUD inside stage: Hero Hearts + Combo/Ult + Monster HP Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Hero hearts & ultimate bar
            Column(modifier = Modifier.width(stageW * 0.43f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(snapshot.heroMaxHp) { idx ->
                        Text(
                            text = if (idx < snapshot.heroHp) "❤️" else "🤍",
                            fontSize = 15.sp
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = HatChipLabel(hero.equippedHatId),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.32f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(5.dp))
                // Ultimate Energy Bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (snapshot.ultimateCharge >= 100) "⚡大招满!" else "⚡奥义",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (snapshot.ultimateCharge >= 100) Color(0xFFFFEE58) else Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    LinearProgressIndicator(
                        progress = { snapshot.ultimateCharge / 100f },
                        color = if (snapshot.ultimateCharge >= 100) QuestColors.Gold else Color(0xFF40C4FF),
                        trackColor = Color.Black.copy(alpha = 0.28f),
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(99.dp))
                    )
                }
            }

            // Right: Monster Name & HP Bar
            Column(
                modifier = Modifier.width(stageW * 0.48f),
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.38f)
                ) {
                    Text(
                        text = buildString {
                            if (snapshot.monster.isBoss) append("👑 ")
                            append(snapshot.monster.name)
                            if (snapshot.monsterStunned) append(" 💫")
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(99.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(snapshot.monsterHpFraction.coerceIn(0f, 1f))
                            .height(14.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF5252), Color(0xFFFFAB40))
                                )
                            )
                    )
                    Text(
                        text = "${snapshot.monsterHp} / ${snapshot.monsterMaxHp}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.tabular(),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        // 3. Combo Flame Badge (Top Center when combo >= 2)
        if (snapshot.combo >= 2) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (snapshot.isFever) QuestColors.CoralAccent else QuestColors.Gold,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            ) {
                Text(
                    text = if (snapshot.isFever) "🔥 ${snapshot.combo} 连击 FEVER!" else "✨ ${snapshot.combo} 连击",
                    color = if (snapshot.isFever) Color.White else QuestColors.InkPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelLarge.tabular(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // 4. Hero + Pet Sprites (Left)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 10.dp)
        ) {
            SpriteImage(
                spriteKey = "hero_${hero.avatar.id}",
                contentDescription = hero.name,
                modifier = Modifier
                    .size((stageH * 0.54f).coerceIn(100.dp, 134.dp))
                    .offset(x = heroLunge.value.dp, y = heroBob.dp)
            )
            if (petSpriteKey != null) {
                SpriteImage(
                    spriteKey = petSpriteKey,
                    contentDescription = "助战萌宠",
                    modifier = Modifier
                        .size((stageH * 0.28f).coerceIn(52.dp, 68.dp))
                        .align(Alignment.BottomEnd)
                        .offset(x = 24.dp, y = (-heroBob * 1.15f).dp)
                )
            }
        }

        // 5. Monster / Boss Sprite + Shield Dome (Right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val monsterSize = if (snapshot.monster.isBoss) {
                (stageH * 0.62f).coerceIn(118.dp, 152.dp)
            } else {
                (stageH * 0.52f).coerceIn(96.dp, 128.dp)
            }
            SpriteImage(
                spriteKey = snapshot.monster.spriteKey,
                contentDescription = snapshot.monster.name,
                modifier = Modifier
                    .size(monsterSize)
                    .offset(x = monsterRecoil.value.dp, y = monsterBob.dp)
                    .scale(if (monsterRecoil.value > 5f) 0.94f else 1f)
            )
            if (snapshot.bossShieldActive) {
                Canvas(modifier = Modifier.size(monsterSize + 16.dp)) {
                    val r = size.minDimension * 0.48f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0x2240C4FF), Color(0x8829B6F6))
                        ),
                        radius = r
                    )
                    drawCircle(
                        color = Color(0xFFE1F5FE),
                        radius = r,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
            if (snapshot.monsterStunned) {
                Text(
                    text = "💫 眩晕中!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .clip(RoundedCornerShape(99.dp))
                        .background(QuestColors.Purple)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        // 6. Elemental Magic Projectile + Impact Particle Burst Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val heroOrigin = Offset(w * 0.28f, h * 0.58f)
            val monsterTarget = Offset(w * 0.74f, h * 0.56f)
            val primaryColor = Color(wandElement.primaryHex)
            val secondaryColor = Color(wandElement.secondaryHex)

            if (projectileActive) {
                val t = projectileProgress.value.coerceIn(0f, 1f)
                val start = if (isMonsterProjectile) monsterTarget else heroOrigin
                val end = if (isMonsterProjectile) heroOrigin else monsterTarget
                val ctrl = Offset((start.x + end.x) / 2f, h * if (isUltimateBeam) 0.14f else 0.28f)
                val u = 1f - t
                val pos = Offset(
                    u * u * start.x + 2 * u * t * ctrl.x + t * t * end.x,
                    u * u * start.y + 2 * u * t * ctrl.y + t * t * end.y
                )
                val boltColor = if (isMonsterProjectile) QuestColors.DangerRed else primaryColor
                val orbRadius = if (isUltimateBeam) 24.dp.toPx() else 14.dp.toPx()

                // Trailing comet sparks
                for (step in 1..4) {
                    val bt = (t - step * 0.06f).coerceAtLeast(0f)
                    val bu = 1f - bt
                    val bp = Offset(
                        bu * bu * start.x + 2 * bu * bt * ctrl.x + bt * bt * end.x,
                        bu * bu * start.y + 2 * bu * bt * ctrl.y + bt * bt * end.y
                    )
                    drawCircle(
                        color = secondaryColor.copy(alpha = 0.65f - step * 0.13f),
                        radius = orbRadius * (1f - step * 0.18f),
                        center = bp
                    )
                }
                drawCircle(color = boltColor, radius = orbRadius, center = pos)
                drawCircle(color = Color.White, radius = orbRadius * 0.48f, center = pos)

                // Secondary Pet Assist Bolt
                if (isPetBoltActive && !isMonsterProjectile) {
                    val petStart = Offset(w * 0.34f, h * 0.76f)
                    val petCtrl = Offset(w * 0.56f, h * 0.45f)
                    val petPos = Offset(
                        u * u * petStart.x + 2 * u * t * petCtrl.x + t * t * monsterTarget.x,
                        u * u * petStart.y + 2 * u * t * petCtrl.y + t * t * monsterTarget.y
                    )
                    drawCircle(color = QuestColors.Gold, radius = 10.dp.toPx(), center = petPos)
                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = petPos)
                }
            }

            // Impact Explosion Burst
            val bt = burstProgress.value
            if (bt > 0f && bt < 1f) {
                val center = monsterTarget
                val alpha = (1f - bt).coerceIn(0f, 1f)
                val maxR = (if (isUltimateBeam) 78.dp else 46.dp).toPx()
                drawCircle(
                    color = secondaryColor.copy(alpha = alpha * 0.55f),
                    radius = maxR * bt,
                    center = center,
                    style = Stroke(width = 4.dp.toPx())
                )
                val sparkCount = if (isUltimateBeam) 12 else 8
                for (i in 0 until sparkCount) {
                    val angle = (i * (360.0 / sparkCount)) * PI / 180.0
                    val dist = maxR * 1.15f * bt
                    val sp = Offset(
                        center.x + (cos(angle) * dist).toFloat(),
                        center.y + (sin(angle) * dist).toFloat()
                    )
                    drawCircle(
                        color = if (i % 2 == 0) primaryColor.copy(alpha = alpha) else QuestColors.Gold.copy(alpha = alpha),
                        radius = (7.dp.toPx()) * (1f - bt * 0.6f),
                        center = sp
                    )
                }
            }
        }

        // 7. Floating Damage & Chinese Rhyme Callout
        val fp = floatTextProgress.value
        if (fp < 1f && calloutPrimary.isNotBlank()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-12f - fp * 36f).dp)
                    .alpha((1f - (fp - 0.6f).coerceAtLeast(0f) / 0.4f).coerceIn(0f, 1f)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.66f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = calloutPrimary,
                            color = calloutColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (calloutSub.isNotBlank()) {
                            Text(
                                text = calloutSub,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
