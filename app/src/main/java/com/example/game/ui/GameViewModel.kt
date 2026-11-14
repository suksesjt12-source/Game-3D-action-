package com.example.game.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.audio.AudioEngine
import com.example.game.audio.MusicTheme
import com.example.game.audio.SfxType
import com.example.game.math.TerrainMath
import com.example.game.model.*
import com.example.game.render.AetherfallRenderer
import com.example.game.render.HudRenderBridge
import com.example.game.render.ProjectedDamageHud
import com.example.game.save.SaveManager
import com.example.game.world.GameSimulation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MinimapBlip(
    val dx: Float,
    val dz: Float,
    val kind: Int // 0=enemy, 1=boss, 2=npc, 3=waypoint, 4=chest
)

data class HudUiState(
    val screenMode: ScreenMode = ScreenMode.MAIN_MENU,
    val loadingProgress: Float = 0f,
    val hp: Float = 160f,
    val maxHp: Float = 160f,
    val stamina: Float = 110f,
    val maxStamina: Float = 110f,
    val level: Int = 1,
    val exp: Int = 0,
    val expToNext: Int = 120,
    val skillPoints: Int = 1,
    val gold: Int = 85,
    val attackPower: Float = 24f,
    val defense: Float = 11f,
    val critRate: Float = 0.12f,
    val weaponName: String = "Dawnreach Iron Sword",
    val potionCount: Int = 4,
    val playerX: Float = 0f,
    val playerZ: Float = 16f,
    val playerYaw: Float = 0f,
    val region: RegionId = RegionId.DAWNREACH_VILLAGE,
    val timeOfDay: TimeOfDay = TimeOfDay.MORNING,
    val weather: WeatherType = WeatherType.CLEAR,
    val inBoat: Boolean = false,
    val inDungeon: Boolean = false,
    val interactPrompt: String? = null,
    val activeQuestTitle: String? = null,
    val activeQuestObjective: String? = null,
    val bossName: String? = null,
    val bossHp: Float = 0f,
    val bossMaxHp: Float = 1f,
    val bossPhase: Int = 1,
    val lootNotifications: List<LootNotification> = emptyList(),
    val projectedDamage: List<ProjectedDamageHud> = emptyList(),
    val minimapBlips: List<MinimapBlip> = emptyList(),
    val measuredFps: Int = 30,
    val settings: GameSettings = GameSettings(),
    val saveSlots: List<SaveSlotSummary> = emptyList(),
    val hasContinueSave: Boolean = false,
    val activeNpc: NpcDef? = null,
    val activeDialogue: DialogueNode? = null,
    val shopFeedback: String? = null,
    val lorePopup: Pair<String, String>? = null,
    val uiTick: Int = 0
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val audioEngine = AudioEngine()
    val saveManager = SaveManager(application)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    val simulation = GameSimulation(audioEngine) { ms -> triggerHaptic(ms) }

    @Volatile private var latestBridge = HudRenderBridge()

    val renderer = AetherfallRenderer(simulation) { bridge ->
        latestBridge = bridge
    }

    private val _uiState = MutableStateFlow(HudUiState())
    val uiState: StateFlow<HudUiState> = _uiState.asStateFlow()

    var onResolutionScaleChanged: ((Float) -> Unit)? = null
    private var autoSaveCounter = 0f

    init {
        val loadedSettings = saveManager.loadSettings()
        simulation.settings = loadedSettings
        audioEngine.applySettings(loadedSettings)
        audioEngine.start()
        refreshSlots()
        startHudSyncLoop()
    }

    private fun startHudSyncLoop() {
        viewModelScope.launch {
            while (true) {
                delay(50L) // 20 Hz UI state sync for crisp HUD bars & minimap without Compose GC pressure
                syncSnapshot()
            }
        }
    }

    private fun syncSnapshot() {
        val cur = _uiState.value
        val mode = cur.screenMode

        if (mode == ScreenMode.PLAYING) {
            autoSaveCounter += 0.05f
            if (autoSaveCounter >= 60f) {
                autoSaveCounter = 0f
                saveToSlot(0) // Background periodic Auto-Save
            }
        }

        synchronized(simulation.stateLock) {
            val px = simulation.stats.position.x
            val pz = simulation.stats.position.z
            val activeQ = simulation.questList.firstOrNull { simulation.questStatuses[it.id] == QuestStatus.ACTIVE }
            val activeObj = activeQ?.objectives?.firstOrNull { !it.isComplete }

            val blips = ArrayList<MinimapBlip>(24)
            for (en in simulation.enemies) {
                if (!en.isAlive) continue
                val dx = en.position.x - px
                val dz = en.position.z - pz
                if (dx * dx + dz * dz < 85f * 85f) {
                    blips.add(MinimapBlip(dx, dz, if (en.type.isBoss) 1 else 0))
                }
            }
            for (npc in simulation.npcs) {
                val dx = npc.currentPos.x - px
                val dz = npc.currentPos.z - pz
                if (dx * dx + dz * dz < 85f * 85f) {
                    blips.add(MinimapBlip(dx, dz, 2))
                }
            }
            for (obj in simulation.interactables) {
                if (obj.type == InteractableType.WAYPOINT_SHRINE) {
                    val dx = obj.position.x - px
                    val dz = obj.position.z - pz
                    if (dx * dx + dz * dz < 140f * 140f) {
                        blips.add(MinimapBlip(dx, dz, 3))
                    }
                } else if (obj.type == InteractableType.TREASURE_CHEST && !obj.activated) {
                    val dx = obj.position.x - px
                    val dz = obj.position.z - pz
                    if (dx * dx + dz * dz < 85f * 85f) {
                        blips.add(MinimapBlip(dx, dz, 4))
                    }
                }
            }

            val nextMode = when {
                mode == ScreenMode.PLAYING && simulation.victoryTriggered -> {
                    renderer.isSimulationPaused = true
                    ScreenMode.VICTORY
                }
                mode == ScreenMode.PLAYING && simulation.stats.hp <= 0f -> {
                    ScreenMode.GAME_OVER
                }
                else -> mode
            }

            val boss = simulation.activeBoss
            val potionTotal = simulation.inventory
                .filter { it.itemId == "con_health_potion" || it.itemId == "con_greater_potion" }
                .sumOf { it.quantity }

            _uiState.value = cur.copy(
                screenMode = nextMode,
                hp = simulation.stats.hp,
                maxHp = simulation.getMaxHp(),
                stamina = simulation.stats.stamina,
                maxStamina = simulation.getMaxStamina(),
                level = simulation.stats.level,
                exp = simulation.stats.exp,
                expToNext = simulation.stats.expToNext,
                skillPoints = simulation.stats.skillPoints,
                gold = simulation.stats.gold,
                attackPower = simulation.getAttackPower(),
                defense = simulation.getDefense(),
                critRate = simulation.getCritChance(),
                weaponName = GameCatalog.items[simulation.equipment.weaponId]?.name ?: "Sword",
                potionCount = potionTotal,
                playerX = px,
                playerZ = pz,
                playerYaw = simulation.stats.yaw,
                region = TerrainMath.getRegionAt(px, pz),
                timeOfDay = simulation.getTimeOfDay(),
                weather = simulation.currentWeather,
                inBoat = simulation.stats.inBoat,
                inDungeon = simulation.stats.inDungeon,
                interactPrompt = simulation.nearestInteractPrompt,
                activeQuestTitle = activeQ?.title,
                activeQuestObjective = activeObj?.let { "${it.description} (${it.currentCount}/${it.requiredCount})" },
                bossName = boss?.type?.displayName,
                bossHp = boss?.hp ?: 0f,
                bossMaxHp = boss?.maxHp ?: 1f,
                bossPhase = boss?.phase ?: 1,
                lootNotifications = simulation.lootNotifications.toList(),
                projectedDamage = latestBridge.projectedDamage,
                minimapBlips = blips,
                measuredFps = latestBridge.measuredFps,
                lorePopup = simulation.lorePopupTitle?.let { it to (simulation.lorePopupText ?: "") },
                uiTick = cur.uiTick + 1
            )
        }
    }

    fun startNewGame() {
        audioEngine.playSfx(SfxType.UI_CLICK)
        launchIntoWorld {
            simulation.startNewGame()
        }
    }

    fun continueMostRecentGame() {
        val recent = saveManager.getMostRecentSlot()
        if (recent != null) {
            loadFromSlot(recent)
        } else {
            startNewGame()
        }
    }

    fun loadFromSlot(slotIndex: Int) {
        val payload = saveManager.loadGame(slotIndex) ?: return
        audioEngine.playSfx(SfxType.UI_CLICK)
        launchIntoWorld {
            simulation.applyLoadedPayload(payload)
        }
    }

    fun saveToSlot(slotIndex: Int): Boolean {
        val payload = simulation.exportSavePayload()
        val ok = saveManager.saveGame(slotIndex, payload)
        refreshSlots()
        if (slotIndex > 0 && ok) {
            audioEngine.playSfx(SfxType.QUEST_COMPLETE, 0.7f)
        }
        return ok
    }

    private fun launchIntoWorld(prepare: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(screenMode = ScreenMode.LOADING, loadingProgress = 0.15f)
            delay(120L)
            prepare()
            _uiState.value = _uiState.value.copy(loadingProgress = 0.65f)
            delay(180L)
            _uiState.value = _uiState.value.copy(loadingProgress = 1.0f)
            delay(120L)
            renderer.isMenuPanoramaMode = false
            renderer.isSimulationPaused = false
            _uiState.value = _uiState.value.copy(screenMode = ScreenMode.PLAYING)
        }
    }

    fun openScreen(mode: ScreenMode) {
        audioEngine.playSfx(SfxType.UI_CLICK)
        val pauseSim = mode != ScreenMode.PLAYING && mode != ScreenMode.MAIN_MENU
        renderer.isSimulationPaused = pauseSim
        if (mode == ScreenMode.MAIN_MENU) {
            renderer.isMenuPanoramaMode = true
            audioEngine.updateEnvironment(MusicTheme.MAIN_MENU, RegionId.DAWNREACH_VILLAGE, false, false)
            refreshSlots()
        }
        _uiState.value = _uiState.value.copy(screenMode = mode, shopFeedback = null)
    }

    fun resumeGame() {
        audioEngine.playSfx(SfxType.UI_CLICK)
        renderer.isSimulationPaused = false
        _uiState.value = _uiState.value.copy(screenMode = ScreenMode.PLAYING, activeNpc = null, activeDialogue = null)
    }

    fun onInteractPressed() {
        val npcId = simulation.triggerInteract()
        if (npcId != null) {
            val npc = simulation.npcs.find { it.id == npcId } ?: return
            val activeIds = simulation.questStatuses.filterValues { it == QuestStatus.ACTIVE }.keys
            val completedIds = simulation.questStatuses.filterValues { it == QuestStatus.COMPLETED }.keys
            val dlg = GameCatalog.getDialogueForNpc(npc, activeIds, completedIds)
            renderer.isSimulationPaused = true
            _uiState.value = _uiState.value.copy(
                screenMode = ScreenMode.DIALOGUE,
                activeNpc = npc,
                activeDialogue = dlg,
                shopFeedback = null
            )
        }
    }

    fun handleDialogueChoice(choice: DialogueChoice) {
        audioEngine.playSfx(SfxType.UI_CLICK)
        choice.acceptQuestId?.let { simulation.acceptQuest(it) }
        if (choice.openShop) {
            _uiState.value = _uiState.value.copy(screenMode = ScreenMode.SHOP, shopFeedback = null)
            return
        }
        if (choice.openUpgrade) {
            val msg = simulation.upgradeEquippedWeapon()
            _uiState.value = _uiState.value.copy(shopFeedback = msg)
            return
        }
        resumeGame()
    }

    fun buyItem(itemId: String) {
        val msg = simulation.buyShopItem(itemId)
        _uiState.value = _uiState.value.copy(shopFeedback = msg)
    }

    fun dismissLorePopup() {
        simulation.lorePopupTitle = null
        simulation.lorePopupText = null
    }

    fun respawnPlayer() {
        simulation.respawnAfterDeath()
        resumeGame()
    }

    fun updateSettings(updated: GameSettings) {
        simulation.settings = updated
        saveManager.saveSettings(updated)
        audioEngine.applySettings(updated)
        onResolutionScaleChanged?.invoke(updated.graphicsQuality.resolutionScale)
        _uiState.value = _uiState.value.copy(settings = updated)
    }

    fun refreshSlots() {
        val slots = saveManager.getSlotSummaries()
        _uiState.value = _uiState.value.copy(
            saveSlots = slots,
            hasContinueSave = slots.any { it.exists },
            settings = simulation.settings
        )
    }

    // Hardware Gamepad Support (Left/Right Analog Sticks + Action Buttons)
    fun handleGamepadKey(keyCode: Int, isDown: Boolean): Boolean {
        if (_uiState.value.screenMode != ScreenMode.PLAYING) return false
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> if (isDown) simulation.triggerJump()
            KeyEvent.KEYCODE_BUTTON_X -> if (isDown) simulation.triggerAttack(heavy = false)
            KeyEvent.KEYCODE_BUTTON_Y -> if (isDown) simulation.triggerAttack(heavy = true)
            KeyEvent.KEYCODE_BUTTON_B -> if (isDown) simulation.triggerDodge()
            KeyEvent.KEYCODE_BUTTON_L1 -> simulation.setBlocking(isDown)
            KeyEvent.KEYCODE_BUTTON_R1 -> if (isDown) onInteractPressed()
            KeyEvent.KEYCODE_BUTTON_START -> if (isDown) openScreen(ScreenMode.PAUSED)
            else -> return false
        }
        return true
    }

    fun handleGamepadMotion(event: MotionEvent): Boolean {
        if (_uiState.value.screenMode != ScreenMode.PLAYING) return false
        val lx = event.getAxisValue(MotionEvent.AXIS_X)
        val ly = event.getAxisValue(MotionEvent.AXIS_Y)
        val rx = event.getAxisValue(MotionEvent.AXIS_Z)
        val ry = event.getAxisValue(MotionEvent.AXIS_RZ)
        simulation.moveInputX = if (kotlin.math.abs(lx) > 0.14f) lx else 0f
        simulation.moveInputY = if (kotlin.math.abs(ly) > 0.14f) ly else 0f
        if (kotlin.math.abs(rx) > 0.14f) simulation.cameraYaw -= rx * 0.045f * simulation.settings.cameraSensitivity
        if (kotlin.math.abs(ry) > 0.14f) {
            simulation.cameraPitch = (simulation.cameraPitch + ry * 0.035f * simulation.settings.cameraSensitivity).coerceIn(-0.15f, 1.15f)
        }
        return true
    }

    fun onAppPause() {
        audioEngine.setPaused(true)
        if (_uiState.value.screenMode == ScreenMode.PLAYING) {
            saveToSlot(0)
        }
    }

    fun onAppResume() {
        audioEngine.setPaused(false)
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
        renderer.release()
    }

    private fun triggerHaptic(ms: Long) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(ms)
            }
        } catch (_: Exception) {
        }
    }
}
