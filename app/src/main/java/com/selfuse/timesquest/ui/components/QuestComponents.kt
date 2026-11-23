package com.selfuse.timesquest.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfuse.timesquest.ui.theme.QuestColors
import com.selfuse.timesquest.ui.theme.SpriteRegistry
import com.selfuse.timesquest.ui.theme.tabular
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Nintendo / Duolingo-style chunky 3D pressable RPG button with tactile bottom ledge.
 */
@Composable
fun TactileRpgButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = QuestColors.CoralAccent,
    ledgeColor: Color = QuestColors.CoralDark,
    contentColor: Color = Color.White,
    enabled: Boolean = true,
    cornerRadius: Dp = 18.dp,
    ledgeHeight: Dp = 5.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressOffset by animateDpAsState(
        targetValue = if (pressed && enabled) ledgeHeight - 1.dp else 0.dp,
        label = "rpgPressOffset"
    )
    val bg = if (enabled) containerColor else Color(0xFFD5D0C5)
    val ledge = if (enabled) ledgeColor else Color(0xFFB5AFA2)
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .padding(bottom = ledgeHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        // Bottom 3D ledge
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = ledgeHeight)
                .clip(shape)
                .background(ledge)
        )
        // Top pressable face
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = pressOffset)
                .clip(shape)
                .background(bg)
                .border(1.5.dp, Color.White.copy(alpha = 0.25f), shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides contentColor
            ) {
                content()
            }
        }
    }
}

@Composable
fun SpriteImage(
    spriteKey: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    silhouette: Boolean = false
) {
    Image(
        painter = painterResource(id = SpriteRegistry.drawableFor(spriteKey)),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        colorFilter = if (silhouette) ColorFilter.tint(Color(0xFF394256)) else null,
        modifier = modifier
    )
}

@Composable
fun StarRow(
    earned: Int,
    maxStars: Int = 3,
    starSize: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..maxStars) {
            StarGlyph(
                filled = i <= earned,
                size = starSize
            )
        }
    }
}

@Composable
fun StarGlyph(filled: Boolean, size: Dp = 18.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val outerR = this.size.minDimension * 0.47f
        val innerR = outerR * 0.46f
        val path = Path()
        for (i in 0 until 10) {
            val angle = (-90.0 + i * 36.0) * PI / 180.0
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        if (filled) {
            drawPath(path, color = QuestColors.Gold)
            drawPath(path, color = QuestColors.GoldDark, style = Stroke(width = 1.4.dp.toPx()))
        } else {
            drawPath(path, color = Color(0xFFE0D7C6))
        }
    }
}

@Composable
fun StatPill(
    emoji: String,
    value: String,
    bgColor: Color = Color.White,
    textColor: Color = QuestColors.InkPrimary,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bgColor,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.tabular(),
                color = textColor,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun HatChipLabel(hatId: String): String = when (hatId) {
    "hat_dino" -> "🦖 恐龙兜"
    "hat_astro" -> "🧑‍🚀 宇航盔"
    "hat_crown" -> "👑 黄金冠"
    else -> "⭐ 星光帽"
}

/** Custom lightweight vector icons (since we depend only on material-icons-core). */
object QuestIcons {
    val MapCompass: ImageVector by lazy {
        ImageVector.Builder("MapCompass", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveTo(6.48f, 22f, 12f, 22f)
                reflectiveCurveTo(22f, 17.52f, 22f, 12f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                moveTo(14.19f, 14.19f)
                lineTo(6f, 18f)
                lineTo(9.81f, 9.81f)
                lineTo(18f, 6f)
                lineTo(14.19f, 14.19f)
                close()
            }
        }.build()
    }

    val WandStar: ImageVector by lazy {
        ImageVector.Builder("WandStar", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                lineTo(14.5f, 8.2f)
                lineTo(21f, 8.8f)
                lineTo(16f, 13.1f)
                lineTo(17.5f, 19.6f)
                lineTo(12f, 16.1f)
                lineTo(6.5f, 19.6f)
                lineTo(8f, 13.1f)
                lineTo(3f, 8.8f)
                lineTo(9.5f, 8.2f)
                close()
            }
        }.build()
    }

    val BestiaryBook: ImageVector by lazy {
        ImageVector.Builder("BestiaryBook", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18f, 2f)
                horizontalLineTo(6f)
                curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
                verticalLineTo(20f)
                curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
                horizontalLineTo(18f)
                curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
                verticalLineTo(4f)
                curveTo(20f, 2.9f, 19.1f, 2f, 18f, 2f)
                close()
                moveTo(9f, 4f)
                horizontalLineTo(11f)
                verticalLineTo(9f)
                lineTo(10f, 8.25f)
                lineTo(9f, 9f)
                verticalLineTo(4f)
                close()
            }
        }.build()
    }
}
