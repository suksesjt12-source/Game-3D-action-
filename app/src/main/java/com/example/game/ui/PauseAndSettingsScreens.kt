package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.game.model.GraphicsQuality
import com.example.game.model.ScreenMode
import com.example.ui.theme.*

@Composable
fun PauseMenuScreen(
    viewModel: GameViewModel,
    state: HudUiState
) {
    var showSaveSlots by remember { mutableStateOf(false) }
    var saveFeedback by remember { mutableStateOf<String?>(null) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid.copy(alpha = 0.78f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, RunicGold.copy(alpha = 0.7f)),
                modifier = Modifier.width(340.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ADVENTURE PAUSED", style = MaterialTheme.typography.headlineMedium, color = RunicGoldLight)

                    PauseButton("Resume", primary = true, tag = "pause_resume_btn") { viewModel.resumeGame() }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PauseButton("Inventory", Modifier.weight(1f), tag = "pause_inv_btn") {
                            viewModel.openScreen(ScreenMode.INVENTORY)
                        }
                        PauseButton("Equipment", Modifier.weight(1f), tag = "pause_eq_btn") {
                            viewModel.openScreen(ScreenMode.EQUIPMENT)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PauseButton("Quests", Modifier.weight(1f), tag = "pause_quest_btn") {
                            viewModel.openScreen(ScreenMode.QUESTS)
                        }
                        PauseButton("World Map", Modifier.weight(1f), tag = "pause_map_btn") {
                            viewModel.openScreen(ScreenMode.WORLD_MAP)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PauseButton("Skills", Modifier.weight(1f), tag = "pause_skills_btn") {
                            viewModel.openScreen(ScreenMode.SKILLS)
                        }
                        PauseButton("Settings", Modifier.weight(1f), tag = "pause_settings_btn") {
                            viewModel.openScreen(ScreenMode.SETTINGS)
                        }
                    }

                    PauseButton("Save Game", tag = "pause_save_btn") {
                        viewModel.refreshSlots()
                        showSaveSlots = !showSaveSlots
                        saveFeedback = null
                    }

                    PauseButton("Main Menu", tag = "pause_main_menu_btn") {
                        viewModel.saveToSlot(0)
                        viewModel.openScreen(ScreenMode.MAIN_MENU)
                    }
                }
            }

            if (showSaveSlots) {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, AetherCyan),
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("SAVE TO SLOT", style = MaterialTheme.typography.titleMedium, color = AetherCyanGlow)
                        for (slotIdx in 1..3) {
                            val summary = state.saveSlots.find { it.slotIndex == slotIdx }
                            Surface(
                                color = ObsidianElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val ok = viewModel.saveToSlot(slotIdx)
                                        saveFeedback = if (ok) "Saved to Slot $slotIdx!" else "Save failed."
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Slot $slotIdx", style = MaterialTheme.typography.labelLarge, color = RunicGoldLight)
                                    Text(
                                        if (summary?.exists == true) "Lv.${summary.playerLevel} • ${summary.regionName}" else "Empty Slot — Tap to Save",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ParchmentText
                                    )
                                }
                            }
                        }
                        if (saveFeedback != null) {
                            Text(saveFeedback!!, style = MaterialTheme.typography.labelLarge, color = EmeraldStamina)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PauseButton(
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    tag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) RunicGold else ObsidianElevated,
            contentColor = if (primary) ObsidianVoid else ParchmentText
        ),
        modifier = modifier.fillMaxWidth().height(48.dp).testTag(tag)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SettingsScreen(
    viewModel: GameViewModel,
    state: HudUiState,
    returnTo: ScreenMode
) {
    val s = state.settings

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid.copy(alpha = 0.90f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, ObsidianBorder),
            modifier = Modifier.widthIn(max = 660.dp).fillMaxHeight(0.92f)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SETTINGS", style = MaterialTheme.typography.headlineMedium, color = RunicGoldLight)
                    Button(onClick = { viewModel.openScreen(returnTo) }, modifier = Modifier.height(48.dp).testTag("settings_close_btn")) {
                        Text("Done")
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text("Graphics & Performance (Mali GPU Optimization)", style = MaterialTheme.typography.titleMedium, color = AetherCyan)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GraphicsQuality.entries.forEach { preset ->
                                FilterChip(
                                    selected = s.graphicsQuality == preset,
                                    onClick = { viewModel.updateSettings(s.copy(graphicsQuality = preset)) },
                                    label = { Text(preset.label) },
                                    modifier = Modifier.height(48.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            listOf(30, 60).forEach { fps ->
                                FilterChip(
                                    selected = s.targetFps == fps,
                                    onClick = { viewModel.updateSettings(s.copy(targetFps = fps)) },
                                    label = { Text("$fps FPS") },
                                    modifier = Modifier.height(48.dp)
                                )
                            }
                        }
                        SettingSwitchRow("Weather Particles & Wetness", s.weatherEffectsEnabled) {
                            viewModel.updateSettings(s.copy(weatherEffectsEnabled = it))
                        }
                    }

                    item {
                        HorizontalDivider(color = ObsidianBorder)
                        Text("Audio Channels", style = MaterialTheme.typography.titleMedium, color = AetherCyan)
                        SettingSliderRow("Master Volume", s.masterVolume) { viewModel.updateSettings(s.copy(masterVolume = it)) }
                        SettingSliderRow("Music Volume", s.musicVolume) { viewModel.updateSettings(s.copy(musicVolume = it)) }
                        SettingSliderRow("SFX Volume", s.sfxVolume) { viewModel.updateSettings(s.copy(sfxVolume = it)) }
                        SettingSliderRow("Ambient Volume", s.ambientVolume) { viewModel.updateSettings(s.copy(ambientVolume = it)) }
                    }

                    item {
                        HorizontalDivider(color = ObsidianBorder)
                        Text("Touch Controls & Accessibility", style = MaterialTheme.typography.titleMedium, color = AetherCyan)
                        SettingSliderRow("Virtual Gamepad Scale", s.uiScale, 0.75f..1.35f) {
                            viewModel.updateSettings(s.copy(uiScale = it))
                        }
                        SettingSliderRow("Joystick Sensitivity", s.touchSensitivity, 0.5f..1.8f) {
                            viewModel.updateSettings(s.copy(touchSensitivity = it))
                        }
                        SettingSliderRow("Camera Look Sensitivity", s.cameraSensitivity, 0.4f..2.0f) {
                            viewModel.updateSettings(s.copy(cameraSensitivity = it))
                        }
                        SettingSwitchRow("Combat Haptic Vibration", s.vibrationEnabled) {
                            viewModel.updateSettings(s.copy(vibrationEnabled = it))
                        }
                        SettingSwitchRow("Compact Thumb Button Layout", s.compactButtonLayout) {
                            viewModel.updateSettings(s.copy(compactButtonLayout = it))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = ParchmentText, modifier = Modifier.width(190.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f)
        )
        Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MutedSlateText, modifier = Modifier.width(46.dp))
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = ParchmentText)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun EndStateOverlay(
    isVictory: Boolean,
    level: Int,
    onPrimaryAction: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid.copy(alpha = 0.88f))
            .padding(24.dp)
    ) {
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, if (isVictory) RunicGold else CrimsonHealth),
            modifier = Modifier.widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isVictory) "THE FRACTURE SEALED" else "FALLEN IN BATTLE",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isVictory) RunicGoldLight else CrimsonHealth
                )
                Text(
                    text = if (isVictory)
                        "You conquered The Ancient Colossus and restored light across Aetherfall at Level $level!"
                    else
                        "Your strength faded in the wilds, yet the Dawnreach Shrine recalls your spirit.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ParchmentText
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onPrimaryAction, modifier = Modifier.height(48.dp)) {
                        Text(if (isVictory) "Continue Exploring" else "Awaken at Dawnreach")
                    }
                    OutlinedButton(onClick = onMainMenu, modifier = Modifier.height(48.dp)) {
                        Text("Main Menu")
                    }
                }
            }
        }
    }
}
