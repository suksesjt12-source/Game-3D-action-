package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.RegionId
import com.example.game.model.ScreenMode
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HudOverlay(
    viewModel: GameViewModel,
    state: HudUiState
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // TOP-LEFT: Player Level, HP, Stamina, EXP, Weapon & Quest Tracker
        Column(
            modifier = Modifier.align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianSurface.copy(alpha = 0.78f))
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                // Level Medallion
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(RunicGold, Color(0xFFB45309))))
                        .border(1.5.dp, RunicGoldLight, CircleShape)
                ) {
                    Text(
                        text = "${state.level}",
                        style = MaterialTheme.typography.titleMedium,
                        color = ObsidianVoid,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // HP Bar
                    StatProgressBar(
                        value = state.hp,
                        maxValue = state.maxHp,
                        fillColor = CrimsonHealth,
                        trackColor = CrimsonDark.copy(alpha = 0.7f),
                        label = "HP ${state.hp.toInt()}/${state.maxHp.toInt()}",
                        width = 165.dp,
                        height = 12.dp
                    )
                    // Stamina Bar
                    StatProgressBar(
                        value = state.stamina,
                        maxValue = state.maxStamina,
                        fillColor = EmeraldStamina,
                        trackColor = EmeraldDark.copy(alpha = 0.7f),
                        label = "ST ${state.stamina.toInt()}/${state.maxStamina.toInt()}",
                        width = 150.dp,
                        height = 9.dp
                    )
                    // EXP Bar
                    StatProgressBar(
                        value = state.exp.toFloat(),
                        maxValue = state.expToNext.toFloat().coerceAtLeast(1f),
                        fillColor = AetherCyan,
                        trackColor = ObsidianVoid,
                        label = "EXP ${state.exp}/${state.expToNext}",
                        width = 150.dp,
                        height = 6.dp
                    )
                }
            }

            // Weapon & Region / Weather Pill
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ObsidianSurface.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = RunicGold, modifier = Modifier.size(13.dp))
                        Text(
                            text = state.weaponName,
                            style = MaterialTheme.typography.labelSmall,
                            color = ParchmentText,
                            maxLines = 1
                        )
                        Text(
                            text = "• ${state.gold}G",
                            style = MaterialTheme.typography.labelSmall,
                            color = RunicGoldLight
                        )
                    }
                }
            }

            // Active Quest Tracker
            if (state.activeQuestTitle != null) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianSurface.copy(alpha = 0.72f))
                        .border(1.dp, RunicGold.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .clickable { viewModel.openScreen(ScreenMode.QUESTS) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = state.activeQuestTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = RunicGoldLight,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (state.activeQuestObjective != null) {
                        Text(
                            text = "• ${state.activeQuestObjective}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ParchmentText,
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // TOP-CENTER: Boss Health Bar + Region Banner
        Column(
            modifier = Modifier.align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = ObsidianSurface.copy(alpha = 0.72f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Text(
                    text = "${if (state.inDungeon) "Dungeon Vault" else state.region.displayName}  •  ${state.timeOfDay.label}  •  ${state.weather.label}  •  ${state.measuredFps} FPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlateText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            if (state.bossName != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(300.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianVoid.copy(alpha = 0.85f))
                        .border(1.5.dp, CrimsonHealth, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = state.bossName,
                            style = MaterialTheme.typography.titleMedium,
                            color = RunicGoldLight,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Phase ${state.bossPhase}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrimsonHealth
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    StatProgressBar(
                        value = state.bossHp,
                        maxValue = state.bossMaxHp,
                        fillColor = CrimsonHealth,
                        trackColor = CrimsonDark,
                        label = "${state.bossHp.toInt()} / ${state.bossMaxHp.toInt()}",
                        width = 276.dp,
                        height = 12.dp
                    )
                }
            }
        }

        // TOP-RIGHT: Mini-Map & RPG Quick Menu Bar
        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Quick RPG Action Icons (48dp touch targets)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    HudIconButton(Icons.Default.Backpack, "Inventory", "hud_inventory_btn", state.skillPoints > 0 && false) {
                        viewModel.openScreen(ScreenMode.INVENTORY)
                    }
                    HudIconButton(Icons.Default.Security, "Equipment", "hud_equipment_btn") {
                        viewModel.openScreen(ScreenMode.EQUIPMENT)
                    }
                    HudIconButton(Icons.Default.AutoAwesome, "Skills", "hud_skills_btn", badge = state.skillPoints > 0) {
                        viewModel.openScreen(ScreenMode.SKILLS)
                    }
                    HudIconButton(Icons.Default.MenuBook, "Quests", "hud_quests_btn") {
                        viewModel.openScreen(ScreenMode.QUESTS)
                    }
                    HudIconButton(Icons.Default.Map, "World Map", "hud_map_btn") {
                        viewModel.openScreen(ScreenMode.WORLD_MAP)
                    }
                    HudIconButton(Icons.Default.Pause, "Pause", "hud_pause_btn") {
                        viewModel.openScreen(ScreenMode.PAUSED)
                    }
                }

                // Circular Mini-Map
                MiniMapWidget(state = state) {
                    viewModel.openScreen(ScreenMode.WORLD_MAP)
                }
            }

            // Loot / Quest Notifications
            state.lootNotifications.forEach { note ->
                Surface(
                    color = ObsidianSurface.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (note.isQuest) RunicGold else AetherCyan
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (note.isQuest) RunicGoldLight else AetherCyanGlow,
                            fontSize = 11.sp
                        )
                        Text(
                            text = note.detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ParchmentText,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // BOTTOM-CENTER: Contextual Interaction Prompt Banner
        if (state.interactPrompt != null) {
            Surface(
                color = ObsidianVoid.copy(alpha = 0.86f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, RunicGold),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
                    .testTag("hud_interact_prompt")
                    .clickable { viewModel.onInteractPressed() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = RunicGold, modifier = Modifier.size(18.dp))
                    Text(
                        text = state.interactPrompt,
                        style = MaterialTheme.typography.labelLarge,
                        color = ParchmentText
                    )
                }
            }
        }

        // Lore Tablet Popup Modal
        if (state.lorePopup != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianVoid.copy(alpha = 0.65f))
                    .clickable { viewModel.dismissLorePopup() }
            ) {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, RunicGold),
                    modifier = Modifier.widthIn(max = 440.dp).padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(state.lorePopup.first, style = MaterialTheme.typography.headlineMedium, color = RunicGoldLight)
                        Text(state.lorePopup.second, style = MaterialTheme.typography.bodyLarge, color = ParchmentText)
                        Button(onClick = { viewModel.dismissLorePopup() }, modifier = Modifier.align(Alignment.End)) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatProgressBar(
    value: Float,
    maxValue: Float,
    fillColor: Color,
    trackColor: Color,
    label: String,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp
) {
    val frac = (value / maxValue.coerceAtLeast(1f)).coerceIn(0f, 1f)
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(frac)
                .background(fillColor)
        )
        if (height >= 9.dp) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontSize = 8.sp,
                modifier = Modifier.padding(horizontal = 5.dp)
            )
        }
    }
}

@Composable
private fun HudIconButton(
    icon: ImageVector,
    description: String,
    tag: String,
    badge: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .testTag(tag)
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianSurface.copy(alpha = 0.80f))
            .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = description, tint = ParchmentText, modifier = Modifier.size(20.dp))
        if (badge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(RunicGold)
            )
        }
    }
}

@Composable
private fun MiniMapWidget(state: HudUiState, onClick: () -> Unit) {
    val bgTint = when (state.region) {
        RegionId.GREENVEIL_FOREST -> Color(0xFF143524)
        RegionId.ASHEN_HIGHLANDS -> Color(0xFF33201E)
        RegionId.SILVERLAKE_VALLEY -> Color(0xFF123247)
        RegionId.FORGOTTEN_KINGDOM -> Color(0xFF23253A)
        RegionId.DAWNREACH_VILLAGE -> Color(0xFF1E2D24)
    }

    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .background(bgTint.copy(alpha = 0.88f))
            .border(2.dp, RunicGold.copy(alpha = 0.8f), CircleShape)
            .clickable(onClick = onClick)
            .testTag("minimap_widget")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val mapRadius = size.minDimension * 0.45f
            val worldRange = 85f

            drawCircle(color = Color.White.copy(alpha = 0.14f), radius = mapRadius * 0.5f, center = c, style = Stroke(1f))

            for (blip in state.minimapBlips) {
                val bx = (blip.dx / worldRange) * mapRadius
                val bz = (blip.dz / worldRange) * mapRadius
                if (bx * bx + bz * bz <= mapRadius * mapRadius) {
                    val col = when (blip.kind) {
                        1 -> Color(0xFFFF2D55) // Boss
                        0 -> CrimsonHealth // Enemy
                        2 -> EmeraldStamina // NPC
                        3 -> AetherCyan // Waypoint
                        else -> RunicGold // Chest
                    }
                    drawCircle(color = col, radius = if (blip.kind == 1) 5f else 3.2f, center = c + Offset(bx, bz))
                }
            }

            // Player directional chevron
            val sinY = sin(state.playerYaw)
            val cosY = cos(state.playerYaw)
            val tip = c + Offset(sinY * 8f, cosY * 8f)
            val left = c + Offset(-cosY * 5f - sinY * 4f, sinY * 5f - cosY * 4f)
            val right = c + Offset(cosY * 5f - sinY * 4f, -sinY * 5f - cosY * 4f)
            val path = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(left.x, left.y)
                lineTo(right.x, right.y)
                close()
            }
            drawPath(path, color = Color.White)
        }
    }
}
