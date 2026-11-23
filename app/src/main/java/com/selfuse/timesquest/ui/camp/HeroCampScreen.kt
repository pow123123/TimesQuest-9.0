package com.selfuse.timesquest.ui.camp

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfuse.timesquest.audio.SoundEffectEngine
import com.selfuse.timesquest.data.CurriculumCatalog
import com.selfuse.timesquest.data.EquipSlot
import com.selfuse.timesquest.data.EquipmentDef
import com.selfuse.timesquest.data.GameStateStore
import com.selfuse.timesquest.data.GameUiState
import com.selfuse.timesquest.ui.components.HatChipLabel
import com.selfuse.timesquest.ui.components.SpriteImage
import com.selfuse.timesquest.ui.components.StatPill
import com.selfuse.timesquest.ui.components.TactileRpgButton
import com.selfuse.timesquest.ui.theme.QuestColors

@Composable
fun HeroCampScreen(
    state: GameUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hero = state.hero
    val sfx = state.settings.sfxEnabled
    var selectedSlot by rememberSaveable { mutableStateOf(EquipSlot.WAND) }

    val activeWand = CurriculumCatalog.wand(hero.equippedWandId)
    val activePet = CurriculumCatalog.equipment(hero.equippedPetId)
    val itemsForSlot = CurriculumCatalog.equipments.filter { it.slot == selectedSlot }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(QuestColors.CreamBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Live Hero Showcase Card
        item(key = "hero_showcase") {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                shadowElevation = 5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFD6F1FF), Color(0xFFFFFBF2))
                            )
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("🏕️ 勇者魔法营地", style = MaterialTheme.typography.titleLarge, color = QuestColors.InkPrimary)
                            Text("Lv.${hero.level} · ${hero.titleText}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = QuestColors.SkyDark)
                        }
                        StatPill("🪙", "${hero.coins}", bgColor = Color(0xFFFFF8E1))
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SpriteImage(
                            spriteKey = "hero_${hero.avatar.id}",
                            contentDescription = hero.name,
                            modifier = Modifier.size(126.dp)
                        )
                        if (activePet?.spriteKey != null) {
                            Spacer(Modifier.width(6.dp))
                            SpriteImage(
                                spriteKey = activePet.spriteKey,
                                contentDescription = activePet.name,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatPill("🪄", activeWand.name, bgColor = Color.White)
                        StatPill("", HatChipLabel(hero.equippedHatId), bgColor = Color.White)
                    }
                    if (activePet != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "🐾 助战萌宠：${activePet.name}（每 4 连击自动协击！）",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuestColors.CoralDark
                        )
                    }
                }
            }
        }

        // 2. Strawberry Potion Quick Refill Bar
        item(key = "potion_shop") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFFF0F3),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFFFFC2D1), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🍓", fontSize = 30.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "魔法草莓药水（背包：${hero.potions} 瓶）",
                            style = MaterialTheme.typography.titleMedium,
                            color = QuestColors.InkPrimary
                        )
                        Text(
                            text = "血量归零时喝一瓶即可满血续战",
                            fontSize = 12.sp,
                            color = QuestColors.InkSecondary
                        )
                    }
                    TactileRpgButton(
                        onClick = {
                            if (GameStateStore.buyPotion(35)) {
                                SoundEffectEngine.playChestOpen(sfx)
                                Toast.makeText(context, "买到 1 瓶草莓药水！", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, if (hero.potions >= 9) "背包药水满啦" else "金币还不够哦", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = hero.coins >= 35 && hero.potions < 9,
                        containerColor = QuestColors.CoralAccent,
                        ledgeColor = QuestColors.CoralDark,
                        cornerRadius = 12.dp,
                        ledgeHeight = 3.dp,
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text("🪙 35 买1瓶", fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // 3. Category Tabs
        item(key = "slot_tabs") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EquipSlot.entries.forEach { slot ->
                    FilterChip(
                        selected = selectedSlot == slot,
                        onClick = {
                            SoundEffectEngine.playButtonTap(sfx)
                            selectedSlot = slot
                        },
                        label = {
                            Text(
                                text = when (slot) {
                                    EquipSlot.WAND -> "🪄 元素法杖"
                                    EquipSlot.HAT -> "👑 勇者装扮"
                                    EquipSlot.PET -> "🐾 助战萌宠"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Equipment Cards
        items(itemsForSlot, key = { it.id }) { item ->
            val owned = item.id in hero.ownedEquipIds
            val equipped = when (item.slot) {
                EquipSlot.WAND -> hero.equippedWandId == item.id
                EquipSlot.HAT -> hero.equippedHatId == item.id
                EquipSlot.PET -> hero.equippedPetId == item.id
            }
            EquipmentShopCard(
                item = item,
                owned = owned,
                equipped = equipped,
                canAfford = hero.coins >= item.priceCoins,
                onAction = {
                    val ok = GameStateStore.buyOrEquip(item)
                    if (ok) {
                        if (owned) SoundEffectEngine.playButtonTap(sfx) else SoundEffectEngine.playChestOpen(sfx)
                        Toast.makeText(
                            context,
                            if (owned) "已装备「${item.name}」！" else "解锁并装备了「${item.name}」！",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(context, "再打几关攒够 🪙${item.priceCoins} 金币吧！", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
private fun EquipmentShopCard(
    item: EquipmentDef,
    owned: Boolean,
    equipped: Boolean,
    canAfford: Boolean,
    onAction: () -> Unit
) {
    val accent = Color(item.accentHex)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = if (equipped) 4.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (equipped) 2.5.dp else 1.dp,
                color = if (equipped) QuestColors.Emerald else QuestColors.BorderSoft,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.spriteKey != null) {
                    SpriteImage(
                        spriteKey = item.spriteKey,
                        contentDescription = item.name,
                        modifier = Modifier.size(56.dp)
                    )
                } else {
                    Text(
                        text = when {
                            item.slot == EquipSlot.WAND -> "🪄"
                            item.id == "hat_dino" -> "🦖"
                            item.id == "hat_astro" -> "🧑‍🚀"
                            item.id == "hat_crown" -> "👑"
                            else -> "⭐"
                        },
                        fontSize = 30.sp
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, color = QuestColors.InkPrimary)
                Spacer(Modifier.height(2.dp))
                Text(item.description, fontSize = 12.sp, color = QuestColors.InkSecondary)
                if (!owned && item.streakUnlockDays != null) {
                    Text(
                        text = "🎁 连续打卡 ${item.streakUnlockDays} 天可免费获赠",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuestColors.CoralDark
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            when {
                equipped -> Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = QuestColors.Emerald.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "已装备",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = QuestColors.EmeraldDark,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                owned -> TactileRpgButton(
                    onClick = onAction,
                    containerColor = QuestColors.SkyPrimary,
                    ledgeColor = QuestColors.SkyDark,
                    cornerRadius = 12.dp,
                    ledgeHeight = 3.dp,
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("穿戴", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }

                else -> TactileRpgButton(
                    onClick = onAction,
                    enabled = canAfford,
                    containerColor = QuestColors.Gold,
                    ledgeColor = QuestColors.GoldDark,
                    contentColor = QuestColors.InkPrimary,
                    cornerRadius = 12.dp,
                    ledgeHeight = 3.dp,
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("🪙 ${item.priceCoins}", fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
