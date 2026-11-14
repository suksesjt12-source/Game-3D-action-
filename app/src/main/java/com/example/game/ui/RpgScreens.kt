package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.math.TerrainMath
import com.example.game.model.*
import com.example.ui.theme.*

@Composable
private fun RpgPanelFrame(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid.copy(alpha = 0.86f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(14.dp)
    ) {
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, RunicGold.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxSize(0.95f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(title, style = MaterialTheme.typography.headlineMedium, color = RunicGoldLight)
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MutedSlateText)
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(48.dp).testTag("rpg_panel_close")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = ParchmentText)
                    }
                }
                HorizontalDivider(color = ObsidianBorder, modifier = Modifier.padding(vertical = 8.dp))
                content()
            }
        }
    }
}

@Composable
fun InventoryScreen(viewModel: GameViewModel, state: HudUiState) {
    var selectedCategory by remember { mutableStateOf<ItemCategory?>(null) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }

    val entries = remember(state.uiTick, selectedCategory) {
        viewModel.simulation.inventory.filter { entry ->
            val cat = GameCatalog.items[entry.itemId]?.category
            selectedCategory == null || cat == selectedCategory
        }
    }

    RpgPanelFrame(
        title = "INVENTORY",
        subtitle = "Gold: ${state.gold}G  •  Equipped: ${state.weaponName}",
        onClose = { viewModel.resumeGame() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All") },
                    modifier = Modifier.height(42.dp)
                )
                ItemCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.displayName) },
                        modifier = Modifier.height(42.dp)
                    )
                }
            }
            OutlinedButton(
                onClick = { viewModel.simulation.sortInventory() },
                modifier = Modifier.height(48.dp).testTag("sort_inventory_btn")
            ) {
                Text("Sort")
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyColumn(
                modifier = Modifier.weight(1.3f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(entries) { entry ->
                    val def = GameCatalog.items[entry.itemId] ?: return@items
                    val isSelected = selectedItemId == entry.itemId
                    Surface(
                        color = if (isSelected) ObsidianElevated else ObsidianVoid.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AetherCyan else ObsidianBorder),
                        modifier = Modifier.fillMaxWidth().clickable { selectedItemId = entry.itemId }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val upg = if (entry.upgradeLevel > 0) " +${entry.upgradeLevel}" else ""
                                Text("${def.name}$upg", style = MaterialTheme.typography.labelLarge, color = ParchmentText)
                                Text(def.category.displayName, style = MaterialTheme.typography.bodyMedium, color = MutedSlateText)
                            }
                            Text("x${entry.quantity}", style = MaterialTheme.typography.labelSmall, color = RunicGoldLight)
                        }
                    }
                }
            }

            // Item Details & Actions
            Surface(
                color = ObsidianElevated,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                val selDef = selectedItemId?.let { GameCatalog.items[it] }
                if (selDef == null) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text("Select an item to inspect, use, equip, or drop.", color = MutedSlateText)
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(selDef.name, style = MaterialTheme.typography.titleMedium, color = RunicGoldLight)
                            Text(selDef.description, style = MaterialTheme.typography.bodyLarge, color = ParchmentText)
                            if (selDef.attackBonus > 0f) Text("Attack +${selDef.attackBonus.toInt()}", color = AetherCyan)
                            if (selDef.defenseBonus > 0f) Text("Defense +${selDef.defenseBonus.toInt()}", color = EmeraldStamina)
                            if (selDef.healthBonus > 0f) Text("Max HP +${selDef.healthBonus.toInt()}", color = CrimsonHealth)
                            if (selDef.healAmount > 0f) Text("Restores ${selDef.healAmount.toInt()} HP", color = EmeraldStamina)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selDef.category == ItemCategory.CONSUMABLES || selDef.equipSlot != null) {
                                Button(
                                    onClick = { viewModel.simulation.useInventoryItem(selDef.id) },
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text(if (selDef.equipSlot != null) "Equip" else "Use")
                                }
                            }
                            if (selDef.category != ItemCategory.QUEST_ITEMS && selDef.id != viewModel.simulation.equipment.weaponId) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.simulation.dropItem(selDef.id)
                                        selectedItemId = null
                                    },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("Drop")
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
fun EquipmentScreen(viewModel: GameViewModel, state: HudUiState) {
    val eq = viewModel.simulation.equipment
    val slots = listOf(
        EquipSlot.WEAPON to eq.weaponId,
        EquipSlot.HELMET to eq.helmetId,
        EquipSlot.CHEST to eq.chestId,
        EquipSlot.GLOVES to eq.glovesId,
        EquipSlot.BOOTS to eq.bootsId,
        EquipSlot.ACCESSORY to eq.accessoryId
    )

    RpgPanelFrame(
        title = "EQUIPMENT & STATUS",
        subtitle = "Level ${state.level} Adventurer",
        onClose = { viewModel.resumeGame() }
    ) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Left: Character Stat Sheet
            Surface(
                color = ObsidianElevated,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.weight(0.9f).fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CHARACTER ATTRIBUTES", style = MaterialTheme.typography.titleMedium, color = AetherCyan)
                    HorizontalDivider(color = ObsidianBorder)
                    StatLine("Level", "${state.level}")
                    StatLine("EXP", "${state.exp} / ${state.expToNext}")
                    StatLine("Health", "${state.hp.toInt()} / ${state.maxHp.toInt()}")
                    StatLine("Stamina", "${state.stamina.toInt()} / ${state.maxStamina.toInt()}")
                    StatLine("Attack Power", "${state.attackPower.toInt()}")
                    StatLine("Defense", "${state.defense.toInt()}")
                    StatLine("Critical Rate", "${(state.critRate * 100).toInt()}%")
                    StatLine("Gold", "${state.gold} G")
                }
            }

            // Right: 6 Equipment Slots
            LazyColumn(
                modifier = Modifier.weight(1.3f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(slots) { (slot, itemId) ->
                    val def = itemId?.let { GameCatalog.items[it] }
                    Surface(
                        color = ObsidianVoid.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(slot.displayName.uppercase(), style = MaterialTheme.typography.labelSmall, color = MutedSlateText)
                                Text(def?.name ?: "Empty Slot", style = MaterialTheme.typography.labelLarge, color = if (def != null) RunicGoldLight else MutedSlateText)
                            }
                            if (def != null && slot != EquipSlot.WEAPON) {
                                OutlinedButton(
                                    onClick = { viewModel.simulation.unequipSlot(slot) },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("Unequip")
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
private fun StatLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MutedSlateText)
        Text(value, style = MaterialTheme.typography.labelLarge, color = ParchmentText)
    }
}

@Composable
fun SkillTreeScreen(viewModel: GameViewModel, state: HudUiState) {
    RpgPanelFrame(
        title = "SKILL TREE",
        subtitle = "Available Skill Points: ${state.skillPoints}",
        onClose = { viewModel.resumeGame() }
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GameCatalog.skills) { skill ->
                val rank = viewModel.simulation.skillRanks[skill.id] ?: 0
                Surface(
                    color = ObsidianElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (rank > 0) AetherCyan else ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("${skill.branch} • ${skill.name} (Rank $rank/${skill.maxRank})", style = MaterialTheme.typography.titleMedium, color = RunicGoldLight)
                            Text(skill.description, style = MaterialTheme.typography.bodyMedium, color = ParchmentText)
                            Text(skill.bonusPerRankLabel, style = MaterialTheme.typography.labelSmall, color = EmeraldStamina)
                        }
                        Button(
                            enabled = state.skillPoints > 0 && rank < skill.maxRank,
                            onClick = { viewModel.simulation.upgradeSkill(skill.id) },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(if (rank >= skill.maxRank) "Maxed" else "Learn (1 SP)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestJournalScreen(viewModel: GameViewModel, state: HudUiState) {
    var selectedType by remember { mutableStateOf(QuestType.MAIN) }
    val quests = remember(state.uiTick, selectedType) {
        viewModel.simulation.questList.filter { it.type == selectedType }
    }

    RpgPanelFrame(
        title = "QUEST JOURNAL",
        subtitle = "8 Main Quests • 10 Side Quests • 5 Exploration Objectives",
        onClose = { viewModel.resumeGame() }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuestType.entries.forEach { qt ->
                FilterChip(
                    selected = selectedType == qt,
                    onClick = { selectedType = qt },
                    label = { Text(qt.label) },
                    modifier = Modifier.height(48.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quests) { q ->
                val status = viewModel.simulation.questStatuses[q.id] ?: QuestStatus.AVAILABLE
                val borderCol = when (status) {
                    QuestStatus.COMPLETED -> EmeraldStamina
                    QuestStatus.ACTIVE -> RunicGold
                    QuestStatus.AVAILABLE -> ObsidianBorder
                }
                Surface(
                    color = ObsidianElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(q.title, style = MaterialTheme.typography.titleMedium, color = ParchmentText)
                            Text(status.name, style = MaterialTheme.typography.labelSmall, color = borderCol)
                        }
                        Text(q.description, style = MaterialTheme.typography.bodyMedium, color = MutedSlateText)
                        q.objectives.forEach { obj ->
                            Text("• ${obj.description} (${obj.currentCount}/${obj.requiredCount})", style = MaterialTheme.typography.bodyMedium, color = AetherCyanGlow)
                        }
                        Text("Reward: ${q.rewardExp} EXP • ${q.rewardGold} Gold", style = MaterialTheme.typography.labelSmall, color = RunicGoldLight)
                        if (status == QuestStatus.AVAILABLE && q.prerequisiteQuestId == null) {
                            Button(
                                onClick = { viewModel.simulation.acceptQuest(q.id) },
                                modifier = Modifier.height(48.dp).align(Alignment.End)
                            ) {
                                Text("Track Quest")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorldMapScreen(viewModel: GameViewModel, state: HudUiState) {
    val waypoints = remember(state.uiTick) {
        viewModel.simulation.interactables.filter { it.type == InteractableType.WAYPOINT_SHRINE }
    }

    RpgPanelFrame(
        title = "WORLD MAP • 2km x 2km FRONTIER",
        subtitle = "Current Region: ${state.region.displayName} (${state.playerX.toInt()}m, ${state.playerZ.toInt()}m)",
        onClose = { viewModel.resumeGame() }
    ) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            // 2D World Map Canvas
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0E1726))
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    fun toCanvas(wx: Float, wz: Float): Offset {
                        val nx = (wx + TerrainMath.WORLD_HALF_SIZE) / (TerrainMath.WORLD_HALF_SIZE * 2f)
                        val nz = (wz + TerrainMath.WORLD_HALF_SIZE) / (TerrainMath.WORLD_HALF_SIZE * 2f)
                        return Offset(nx * size.width, nz * size.height)
                    }

                    // 5 Region zone circles
                    drawCircle(Color(0xFF1E4D2B).copy(alpha = 0.45f), size.minDimension * 0.25f, toCanvas(-420f, -80f))
                    drawCircle(Color(0xFF6B2D1E).copy(alpha = 0.45f), size.minDimension * 0.22f, toCanvas(480f, -480f))
                    drawCircle(Color(0xFF0D4A6B).copy(alpha = 0.55f), size.minDimension * 0.22f, toCanvas(440f, 440f))
                    drawCircle(Color(0xFF332D52).copy(alpha = 0.50f), size.minDimension * 0.22f, toCanvas(0f, -640f))
                    drawCircle(Color(0xFF475530).copy(alpha = 0.55f), size.minDimension * 0.09f, toCanvas(0f, 0f))

                    // Waypoint Shrines
                    for (wp in waypoints) {
                        drawCircle(
                            color = if (wp.activated) AetherCyan else MutedSlateText,
                            radius = 9f,
                            center = toCanvas(wp.position.x, wp.position.z)
                        )
                    }

                    // Player marker
                    drawCircle(RunicGoldLight, radius = 10f, center = toCanvas(state.playerX, state.playerZ))
                }

                // Region labels
                Text("Forgotten Kingdom", color = ParchmentText, fontSize = 11.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp))
                Text("Greenveil Forest", color = EmeraldStamina, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp))
                Text("Ashen Highlands", color = RunicGold, fontSize = 11.sp, modifier = Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 12.dp))
                Text("Silverlake Valley", color = AetherCyan, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 14.dp, end = 14.dp))
                Text("Dawnreach", color = RunicGoldLight, fontSize = 11.sp, modifier = Modifier.align(Alignment.Center))
            }

            // Fast Travel Waypoints List
            LazyColumn(
                modifier = Modifier.weight(0.9f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("FAST TRAVEL WAYPOINTS", style = MaterialTheme.typography.titleMedium, color = AetherCyan)
                }
                items(waypoints) { wp ->
                    Surface(
                        color = ObsidianElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (wp.activated) AetherCyan else ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(wp.title, style = MaterialTheme.typography.labelLarge, color = ParchmentText)
                                Text(wp.region.displayName, style = MaterialTheme.typography.bodyMedium, color = MutedSlateText)
                            }
                            Button(
                                enabled = wp.activated,
                                onClick = {
                                    if (viewModel.simulation.fastTravelTo(wp.id)) {
                                        viewModel.resumeGame()
                                    }
                                },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(if (wp.activated) "Travel" else "Locked")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogueOverlay(viewModel: GameViewModel, state: HudUiState) {
    val dlg = state.activeDialogue ?: return
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid.copy(alpha = 0.45f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
    ) {
        Surface(
            color = ObsidianSurface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, RunicGold),
            modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(dlg.speakerName, style = MaterialTheme.typography.titleMedium, color = RunicGoldLight)
                Text(dlg.text, style = MaterialTheme.typography.bodyLarge, color = ParchmentText)
                if (state.shopFeedback != null) {
                    Text(state.shopFeedback, style = MaterialTheme.typography.labelLarge, color = EmeraldStamina)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dlg.choices.forEach { ch ->
                        Button(
                            onClick = { viewModel.handleDialogueChoice(ch) },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(ch.label)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShopOverlay(viewModel: GameViewModel, state: HudUiState) {
    val npc = state.activeNpc ?: return
    RpgPanelFrame(
        title = "${npc.name.uppercase()} • SHOP",
        subtitle = "Your Gold: ${state.gold}G",
        onClose = { viewModel.resumeGame() }
    ) {
        if (state.shopFeedback != null) {
            Text(state.shopFeedback, style = MaterialTheme.typography.labelLarge, color = EmeraldStamina)
            Spacer(Modifier.height(6.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(npc.shopItemIds) { itemId ->
                val def = GameCatalog.items[itemId] ?: return@items
                Surface(
                    color = ObsidianElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(def.name, style = MaterialTheme.typography.labelLarge, color = RunicGoldLight)
                            Text(def.description, style = MaterialTheme.typography.bodyMedium, color = ParchmentText)
                        }
                        Button(
                            enabled = state.gold >= def.priceGold,
                            onClick = { viewModel.buyItem(def.id) },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Buy (${def.priceGold}G)")
                        }
                    }
                }
            }
        }
    }
}
