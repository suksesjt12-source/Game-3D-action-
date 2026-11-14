package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.render.ProjectedDamageHud
import com.example.ui.theme.*
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun VirtualGamepadOverlay(
    viewModel: GameViewModel,
    state: HudUiState
) {
    val scale = state.settings.uiScale
    val compact = state.settings.compactButtonLayout
    val touchSens = state.settings.touchSensitivity

    Box(modifier = Modifier.fillMaxSize()) {
        // Floating 3D->2D Projected Damage Numbers
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wPx = constraints.maxWidth.toFloat()
            val hPx = constraints.maxHeight.toFloat()
            state.projectedDamage.forEach { dmg: ProjectedDamageHud ->
                val textColor = when {
                    dmg.isHeal -> EmeraldStamina
                    dmg.isPlayerHit -> CrimsonHealth
                    dmg.isCrit -> RunicGoldLight
                    else -> ParchmentText
                }
                Text(
                    text = when {
                        dmg.isHeal -> "+${dmg.amount}"
                        dmg.amount == 0 -> "BLOCKED"
                        dmg.isCrit -> "${dmg.amount}!"
                        else -> "${dmg.amount}"
                    },
                    color = textColor.copy(alpha = dmg.alpha),
                    fontSize = if (dmg.isCrit) 20.sp else 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.offset {
                        IntOffset((dmg.normX * wPx).roundToInt(), (dmg.normY * hPx).roundToInt())
                    }
                )
            }
        }

        // Bottom-Left: Multi-Touch 360-degree Virtual Movement Joystick
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 24.dp, bottom = if (compact) 14.dp else 22.dp)
        ) {
            VirtualJoystick(
                size = (132f * scale).dp,
                sensitivity = touchSens,
                onMove = { dx, dy ->
                    viewModel.simulation.moveInputX = dx
                    viewModel.simulation.moveInputY = dy
                    viewModel.simulation.sprintHeld = sqrt(dx * dx + dy * dy) > 0.88f
                }
            )
        }

        // Bottom-Right: Ergonomic Action Button Cluster
        val btnPrimary = (72f * scale).dp
        val btnSec = (56f * scale).dp
        val spacing = if (compact) 8.dp else 12.dp

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 22.dp, bottom = if (compact) 14.dp else 22.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                // Upper row: Weapon Switch, Quick Potion, Interact (if near)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.interactPrompt != null) {
                        ActionCircleButton(
                            icon = Icons.Default.TouchApp,
                            label = "ACT",
                            size = btnSec,
                            accent = RunicGold,
                            testTag = "interact_button",
                            onClick = { viewModel.onInteractPressed() }
                        )
                    }
                    ActionCircleButton(
                        icon = Icons.Default.LocalDrink,
                        label = "${state.potionCount}",
                        size = (50f * scale).dp,
                        accent = CrimsonHealth,
                        testTag = "potion_button",
                        onClick = { viewModel.simulation.useQuickPotion() }
                    )
                    ActionCircleButton(
                        icon = Icons.Default.SwapHoriz,
                        label = "WPN",
                        size = (50f * scale).dp,
                        accent = AetherCyan,
                        testTag = "switch_weapon_button",
                        onClick = { viewModel.simulation.switchNextWeapon() }
                    )
                }

                // Middle row: Block/Parry, Heavy Attack, Jump
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HoldCircleButton(
                        icon = Icons.Default.Shield,
                        label = "BLK",
                        size = btnSec,
                        accent = AetherCyan,
                        testTag = "block_button",
                        onHoldChanged = { held -> viewModel.simulation.setBlocking(held) }
                    )
                    ActionCircleButton(
                        icon = Icons.Default.FlashOn,
                        label = "HVY",
                        size = btnSec,
                        accent = RunicGold,
                        testTag = "heavy_attack_button",
                        onClick = { viewModel.simulation.triggerAttack(heavy = true) }
                    )
                    ActionCircleButton(
                        icon = Icons.Default.ArrowUpward,
                        label = "JMP",
                        size = btnSec,
                        accent = EmeraldStamina,
                        testTag = "jump_button",
                        onClick = { viewModel.simulation.triggerJump() }
                    )
                }

                // Lower thumb arc: Dodge/Roll + Primary Light Attack
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing + 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionCircleButton(
                        icon = Icons.Default.DirectionsRun,
                        label = "DODGE",
                        size = btnSec,
                        accent = EmeraldStamina,
                        testTag = "dodge_button",
                        onClick = { viewModel.simulation.triggerDodge() }
                    )
                    ActionCircleButton(
                        icon = Icons.Default.Gavel,
                        label = "ATK",
                        size = btnPrimary,
                        accent = AetherCyanGlow,
                        testTag = "attack_button",
                        onClick = { viewModel.simulation.triggerAttack(heavy = false) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VirtualJoystick(
    size: Dp,
    sensitivity: Float,
    onMove: (Float, Float) -> Unit
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var activePointerId by remember { mutableStateOf<Long?>(null) }

    Box(
        modifier = Modifier
            .size(size)
            .testTag("movement_joystick")
            .pointerInput(sensitivity) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val maxRadius = (min(this.size.width, this.size.height) * 0.38f).coerceAtLeast(1f)
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Press -> {
                                val ch = event.changes.firstOrNull { it.pressed }
                                if (ch != null && activePointerId == null) {
                                    activePointerId = ch.id.value
                                }
                            }
                            PointerEventType.Move -> {
                                val ch = event.changes.firstOrNull { it.id.value == activePointerId }
                                if (ch != null) {
                                    ch.consume()
                                    val raw = ch.position - center
                                    val dist = raw.getDistance()
                                    val clamped = if (dist > maxRadius) raw * (maxRadius / dist) else raw
                                    knobOffset = clamped
                                    val nx = ((clamped.x / maxRadius) * sensitivity).coerceIn(-1f, 1f)
                                    val ny = ((clamped.y / maxRadius) * sensitivity).coerceIn(-1f, 1f)
                                    onMove(nx, ny)
                                }
                            }
                            PointerEventType.Release -> {
                                val released = event.changes.any { it.id.value == activePointerId && !it.pressed }
                                if (released) {
                                    activePointerId = null
                                    knobOffset = Offset.Zero
                                    onMove(0f, 0f)
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerR = this.size.minDimension * 0.46f
            drawCircle(
                color = ObsidianSurface.copy(alpha = 0.55f),
                radius = outerR,
                center = c
            )
            drawCircle(
                color = AetherCyan.copy(alpha = 0.45f),
                radius = outerR,
                center = c,
                style = Stroke(width = 2.5.dp.toPx())
            )
            // Inner direction ring
            drawCircle(
                color = ObsidianBorder.copy(alpha = 0.5f),
                radius = outerR * 0.48f,
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )
            val knobCenter = c + knobOffset
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AetherCyanGlow, AetherCyan.copy(alpha = 0.75f)),
                    center = knobCenter,
                    radius = outerR * 0.36f
                ),
                radius = outerR * 0.34f,
                center = knobCenter
            )
        }
    }
}

@Composable
private fun ActionCircleButton(
    icon: ImageVector,
    label: String,
    size: Dp,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.coerceAtLeast(48.dp))
            .testTag(testTag)
            .clip(CircleShape)
            .background(
                if (pressed) accent.copy(alpha = 0.45f)
                else ObsidianSurface.copy(alpha = 0.76f)
            )
            .border(2.dp, accent.copy(alpha = 0.85f), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onClick()
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = ParchmentText,
                modifier = Modifier.size((size.value * 0.36f).dp.coerceAtLeast(18.dp))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun HoldCircleButton(
    icon: ImageVector,
    label: String,
    size: Dp,
    accent: Color,
    testTag: String,
    onHoldChanged: (Boolean) -> Unit
) {
    var held by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.coerceAtLeast(48.dp))
            .testTag(testTag)
            .clip(CircleShape)
            .background(if (held) accent.copy(alpha = 0.48f) else ObsidianSurface.copy(alpha = 0.76f))
            .border(2.dp, accent.copy(alpha = 0.85f), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        held = true
                        onHoldChanged(true)
                        tryAwaitRelease()
                        held = false
                        onHoldChanged(false)
                    }
                )
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = ParchmentText,
                modifier = Modifier.size((size.value * 0.36f).dp.coerceAtLeast(18.dp))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontSize = 9.sp
            )
        }
    }
}
