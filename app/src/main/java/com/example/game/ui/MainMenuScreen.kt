package com.example.game.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.ScreenMode
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    state: HudUiState
) {
    val activity = LocalContext.current as? Activity
    var showSlotPicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        ObsidianVoid.copy(alpha = 0.88f),
                        ObsidianVoid.copy(alpha = 0.45f),
                        Color.Transparent
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 36.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Title & Navigation Stack
            Column(
                modifier = Modifier.widthIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "AETHERFALL",
                    style = MaterialTheme.typography.displayLarge,
                    color = ParchmentText,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "THE LOST FRONTIER",
                    style = MaterialTheme.typography.titleMedium,
                    color = RunicGoldLight,
                    letterSpacing = 3.sp
                )
                Text(
                    text = "Open World 3D Action RPG • Offline Native Android",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSlateText
                )

                Spacer(Modifier.height(6.dp))

                MenuActionButton(
                    label = if (state.hasContinueSave) "Continue Adventure" else "Start Game",
                    primary = true,
                    testTag = "start_continue_button"
                ) {
                    if (state.hasContinueSave) viewModel.continueMostRecentGame() else viewModel.startNewGame()
                }

                MenuActionButton(
                    label = "New Game",
                    testTag = "new_game_button"
                ) {
                    viewModel.startNewGame()
                }

                MenuActionButton(
                    label = "Load Save Slot",
                    testTag = "load_slots_button"
                ) {
                    viewModel.refreshSlots()
                    showSlotPicker = !showSlotPicker
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MenuActionButton(
                        label = "Settings",
                        modifier = Modifier.weight(1f),
                        testTag = "settings_button"
                    ) {
                        viewModel.openScreen(ScreenMode.SETTINGS)
                    }
                    MenuActionButton(
                        label = "Credits",
                        modifier = Modifier.weight(1f),
                        testTag = "credits_button"
                    ) {
                        viewModel.openScreen(ScreenMode.CREDITS)
                    }
                }

                MenuActionButton(
                    label = "Exit",
                    testTag = "exit_button"
                ) {
                    activity?.finish()
                }
            }

            // Right Save Slot Picker Panel (when toggled)
            if (showSlotPicker) {
                Surface(
                    color = ObsidianSurface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, RunicGold.copy(alpha = 0.6f)),
                    modifier = Modifier.width(340.dp).fillMaxHeight(0.86f)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("SELECT SAVE SLOT", style = MaterialTheme.typography.titleMedium, color = RunicGoldLight)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.saveSlots) { slot ->
                                val title = if (slot.slotIndex == 0) "Auto-Save Slot" else "Save Slot ${slot.slotIndex}"
                                Surface(
                                    color = ObsidianElevated,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = slot.exists) { viewModel.loadFromSlot(slot.slotIndex) }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(title, style = MaterialTheme.typography.labelLarge, color = AetherCyanGlow)
                                        if (slot.exists) {
                                            val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                                .format(Date(slot.savedTimestamp))
                                            Text(
                                                "Lv.${slot.playerLevel} • ${slot.regionName} • ${slot.completedQuests} Quests",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = ParchmentText
                                            )
                                            Text("Saved $dateStr", style = MaterialTheme.typography.labelSmall, color = MutedSlateText)
                                        } else {
                                            Text("Empty Slot", style = MaterialTheme.typography.bodyMedium, color = MutedSlateText)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuActionButton(
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) RunicGold else ObsidianElevated.copy(alpha = 0.90f),
            contentColor = if (primary) ObsidianVoid else ParchmentText
        ),
        border = if (primary) null else androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LoadingScreenOverlay(progress: Float) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .padding(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Text("AETHERFALL", style = MaterialTheme.typography.displayLarge, color = RunicGoldLight)
            Text("Forging the 2km x 2km Lost Frontier...", style = MaterialTheme.typography.bodyLarge, color = ParchmentText)
            LinearProgressIndicator(
                progress = { progress },
                color = AetherCyan,
                trackColor = ObsidianElevated,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            )
            Text(
                text = "Tip: Time your Block right as an enemy strikes to trigger a Perfect Parry!",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSlateText
            )
        }
    }
}

@Composable
fun CreditsScreen(onBack: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid.copy(alpha = 0.92f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
    ) {
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, RunicGold),
            modifier = Modifier.widthIn(max = 540.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("AETHERFALL: THE LOST FRONTIER", style = MaterialTheme.typography.headlineMedium, color = RunicGoldLight)
                Text("Native Android 3D Open World Action Adventure", style = MaterialTheme.typography.labelLarge, color = AetherCyan)
                HorizontalDivider(color = ObsidianBorder)
                Text(
                    text = "Engine: Native Android OpenGL ES 3.0 + Jetpack Compose\n" +
                        "Audio: Real-time Procedural PCM Music & Biome Ambience\n" +
                        "World: 5 Regions • Dynamic Day/Night & Weather • 23 Quests • 2 Multi-Phase Bosses",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ParchmentText
                )
                Spacer(Modifier.height(6.dp))
                Button(onClick = onBack, modifier = Modifier.height(48.dp).testTag("credits_back_btn")) {
                    Text("Back to Main Menu")
                }
            }
        }
    }
}
