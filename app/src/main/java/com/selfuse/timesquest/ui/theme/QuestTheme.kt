package com.selfuse.timesquest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.selfuse.timesquest.R

object QuestColors {
    val CreamBg = Color(0xFFFFFBF2)
    val CardWhite = Color(0xFFFFFFFF)
    val CardWarm = Color(0xFFFFF4DE)
    val SkyPrimary = Color(0xFF2B8AE2)
    val SkyDark = Color(0xFF1A64B0)
    val CoralAccent = Color(0xFFFF6B3D)
    val CoralDark = Color(0xFFD94B20)
    val Emerald = Color(0xFF28B463)
    val EmeraldDark = Color(0xFF1B8A49)
    val Gold = Color(0xFFFFB703)
    val GoldDark = Color(0xFFD48F00)
    val Purple = Color(0xFF7B5CE6)
    val PurpleDark = Color(0xFF5638BF)
    val DangerRed = Color(0xFFE53935)
    val DangerDark = Color(0xFFB71C1C)
    val InkPrimary = Color(0xFF242938)
    val InkSecondary = Color(0xFF5F687B)
    val BorderSoft = Color(0xFFE8DFCE)
}

private val QuestLightScheme = lightColorScheme(
    primary = QuestColors.SkyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EEFF),
    onPrimaryContainer = Color(0xFF0A335C),
    secondary = QuestColors.CoralAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE2D6),
    onSecondaryContainer = Color(0xFF5C1900),
    tertiary = QuestColors.Purple,
    onTertiary = Color.White,
    background = QuestColors.CreamBg,
    onBackground = QuestColors.InkPrimary,
    surface = QuestColors.CardWhite,
    onSurface = QuestColors.InkPrimary,
    surfaceVariant = Color(0xFFF5EDE0),
    onSurfaceVariant = QuestColors.InkSecondary,
    outline = Color(0xFFCBBFA9),
    outlineVariant = QuestColors.BorderSoft,
    error = QuestColors.DangerRed
)

@Composable
fun TimesQuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = QuestLightScheme,
        typography = Typography(
            headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold),
            headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold),
            headlineSmall = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
            titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
            titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
            bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
            labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
        ),
        content = content
    )
}

fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

/**
 * Maps string sprite keys in CurriculumCatalog to drawable resource IDs.
 */
object SpriteRegistry {
    fun drawableFor(key: String?): Int = when (key) {
        "hero_boy" -> R.drawable.hero_boy
        "hero_girl" -> R.drawable.hero_girl
        "pet_dragon" -> R.drawable.pet_dragon
        "pet_otter" -> R.drawable.pet_otter
        "pet_cat" -> R.drawable.pet_cat
        "pet_owl" -> R.drawable.pet_owl
        "boss_slime" -> R.drawable.boss_slime
        "boss_mushroom" -> R.drawable.boss_mushroom
        "boss_golem" -> R.drawable.boss_golem
        "boss_octopus" -> R.drawable.boss_octopus
        "boss_crystal" -> R.drawable.boss_crystal
        "boss_yeti" -> R.drawable.boss_yeti
        "boss_crab" -> R.drawable.boss_crab
        "boss_cat" -> R.drawable.boss_cat
        "boss_dragon" -> R.drawable.boss_dragon
        else -> R.drawable.ic_launcher_art
    }
}
