package com.example.game.save

import android.content.Context
import com.example.game.math.TerrainMath
import com.example.game.model.EquipmentLoadout
import com.example.game.model.GameSettings
import com.example.game.model.GraphicsQuality
import com.example.game.model.InventoryEntry
import com.example.game.model.PlayerStatsState
import com.example.game.model.QuestStatus
import com.example.game.model.SaveSlotSummary
import com.example.game.model.Vec3
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class LoadedGamePayload(
    val stats: PlayerStatsState,
    val inventory: List<InventoryEntry>,
    val equipment: EquipmentLoadout,
    val skillRanks: Map<String, Int>,
    val questStates: Map<String, Pair<QuestStatus, Map<String, Int>>>,
    val activatedInteractables: Set<String>,
    val defeatedBosses: Set<String>,
    val discoveredRegions: Set<String>,
    val worldTimeMinutes: Float,
    val playTimeSeconds: Int
)

class SaveManager(private val context: Context) {

    private val saveDir: File get() = File(context.filesDir, "aetherfall_saves").apply { mkdirs() }
    private val settingsFile: File get() = File(saveDir, "settings.json")

    private fun slotFile(slotIndex: Int): File = File(saveDir, "save_slot_$slotIndex.json")
    private fun slotBackupFile(slotIndex: Int): File = File(saveDir, "save_slot_$slotIndex.json.bak")
    private fun slotTempFile(slotIndex: Int): File = File(saveDir, "save_slot_$slotIndex.json.tmp")

    fun saveSettings(settings: GameSettings) {
        val json = JSONObject().apply {
            put("graphicsQuality", settings.graphicsQuality.name)
            put("targetFps", settings.targetFps)
            put("masterVolume", settings.masterVolume.toDouble())
            put("musicVolume", settings.musicVolume.toDouble())
            put("sfxVolume", settings.sfxVolume.toDouble())
            put("ambientVolume", settings.ambientVolume.toDouble())
            put("uiScale", settings.uiScale.toDouble())
            put("touchSensitivity", settings.touchSensitivity.toDouble())
            put("cameraSensitivity", settings.cameraSensitivity.toDouble())
            put("vibrationEnabled", settings.vibrationEnabled)
            put("compactButtonLayout", settings.compactButtonLayout)
            put("weatherEffectsEnabled", settings.weatherEffectsEnabled)
        }
        writeAtomic(settingsFile, File(saveDir, "settings.json.tmp"), File(saveDir, "settings.json.bak"), json.toString())
    }

    fun loadSettings(): GameSettings {
        val text = readWithFallback(settingsFile, File(saveDir, "settings.json.bak")) ?: return GameSettings()
        return try {
            val j = JSONObject(text)
            GameSettings(
                graphicsQuality = runCatching {
                    GraphicsQuality.valueOf(j.optString("graphicsQuality", GraphicsQuality.MEDIUM.name))
                }.getOrDefault(GraphicsQuality.MEDIUM),
                targetFps = j.optInt("targetFps", 30).let { if (it == 60) 60 else 30 },
                masterVolume = j.optDouble("masterVolume", 0.85).toFloat().coerceIn(0f, 1f),
                musicVolume = j.optDouble("musicVolume", 0.70).toFloat().coerceIn(0f, 1f),
                sfxVolume = j.optDouble("sfxVolume", 0.90).toFloat().coerceIn(0f, 1f),
                ambientVolume = j.optDouble("ambientVolume", 0.75).toFloat().coerceIn(0f, 1f),
                uiScale = j.optDouble("uiScale", 1.0).toFloat().coerceIn(0.75f, 1.35f),
                touchSensitivity = j.optDouble("touchSensitivity", 1.0).toFloat().coerceIn(0.5f, 2.0f),
                cameraSensitivity = j.optDouble("cameraSensitivity", 1.0).toFloat().coerceIn(0.4f, 2.2f),
                vibrationEnabled = j.optBoolean("vibrationEnabled", true),
                compactButtonLayout = j.optBoolean("compactButtonLayout", false),
                weatherEffectsEnabled = j.optBoolean("weatherEffectsEnabled", true)
            )
        } catch (_: Exception) {
            GameSettings()
        }
    }

    fun saveGame(slotIndex: Int, payload: LoadedGamePayload): Boolean {
        return try {
            val root = JSONObject().apply {
                put("version", 1)
                put("timestamp", System.currentTimeMillis())
                put("playTimeSeconds", payload.playTimeSeconds)
                put("worldTimeMinutes", payload.worldTimeMinutes.toDouble())
                put("regionName", TerrainMath.getRegionAt(payload.stats.position.x, payload.stats.position.z).displayName)

                put("stats", JSONObject().apply {
                    put("level", payload.stats.level)
                    put("exp", payload.stats.exp)
                    put("expToNext", payload.stats.expToNext)
                    put("skillPoints", payload.stats.skillPoints)
                    put("gold", payload.stats.gold)
                    put("baseMaxHp", payload.stats.baseMaxHp.toDouble())
                    put("baseMaxStamina", payload.stats.baseMaxStamina.toDouble())
                    put("hp", payload.stats.hp.toDouble())
                    put("stamina", payload.stats.stamina.toDouble())
                    put("px", payload.stats.position.x.toDouble())
                    put("py", payload.stats.position.y.toDouble())
                    put("pz", payload.stats.position.z.toDouble())
                    put("yaw", payload.stats.yaw.toDouble())
                    put("inBoat", payload.stats.inBoat)
                    put("inDungeon", payload.stats.inDungeon)
                })

                put("inventory", JSONArray().apply {
                    payload.inventory.forEach { entry ->
                        put(JSONObject().apply {
                            put("itemId", entry.itemId)
                            put("quantity", entry.quantity)
                            put("upgradeLevel", entry.upgradeLevel)
                        })
                    }
                })

                put("equipment", JSONObject().apply {
                    put("weaponId", payload.equipment.weaponId)
                    put("helmetId", payload.equipment.helmetId ?: "")
                    put("chestId", payload.equipment.chestId ?: "")
                    put("glovesId", payload.equipment.glovesId ?: "")
                    put("bootsId", payload.equipment.bootsId ?: "")
                    put("accessoryId", payload.equipment.accessoryId ?: "")
                })

                put("skills", JSONObject().apply {
                    payload.skillRanks.forEach { (id, rank) -> put(id, rank) }
                })

                put("quests", JSONObject().apply {
                    payload.questStates.forEach { (qId, state) ->
                        put(qId, JSONObject().apply {
                            put("status", state.first.name)
                            put("objectives", JSONObject().apply {
                                state.second.forEach { (objId, cnt) -> put(objId, cnt) }
                            })
                        })
                    }
                })

                put("activatedInteractables", JSONArray(payload.activatedInteractables.toList()))
                put("defeatedBosses", JSONArray(payload.defeatedBosses.toList()))
                put("discoveredRegions", JSONArray(payload.discoveredRegions.toList()))
            }

            writeAtomic(slotFile(slotIndex), slotTempFile(slotIndex), slotBackupFile(slotIndex), root.toString())
            true
        } catch (_: Exception) {
            false
        }
    }

    fun loadGame(slotIndex: Int): LoadedGamePayload? {
        val raw = readWithFallback(slotFile(slotIndex), slotBackupFile(slotIndex)) ?: return null
        return try {
            val root = JSONObject(raw)
            val s = root.getJSONObject("stats")
            val stats = PlayerStatsState(
                level = s.optInt("level", 1),
                exp = s.optInt("exp", 0),
                expToNext = s.optInt("expToNext", 120),
                skillPoints = s.optInt("skillPoints", 0),
                gold = s.optInt("gold", 50),
                baseMaxHp = s.optDouble("baseMaxHp", 160.0).toFloat(),
                baseMaxStamina = s.optDouble("baseMaxStamina", 110.0).toFloat(),
                hp = s.optDouble("hp", 160.0).toFloat(),
                stamina = s.optDouble("stamina", 110.0).toFloat(),
                position = Vec3(
                    s.optDouble("px", 0.0).toFloat(),
                    s.optDouble("py", 3.0).toFloat(),
                    s.optDouble("pz", 6.0).toFloat()
                ),
                yaw = s.optDouble("yaw", 0.0).toFloat(),
                inBoat = s.optBoolean("inBoat", false),
                inDungeon = s.optBoolean("inDungeon", false)
            )

            val invArr = root.getJSONArray("inventory")
            val inv = mutableListOf<InventoryEntry>()
            for (i in 0 until invArr.length()) {
                val obj = invArr.getJSONObject(i)
                inv.add(
                    InventoryEntry(
                        itemId = obj.getString("itemId"),
                        quantity = obj.getInt("quantity"),
                        upgradeLevel = obj.optInt("upgradeLevel", 0)
                    )
                )
            }

            val eqObj = root.getJSONObject("equipment")
            val equipment = EquipmentLoadout(
                weaponId = eqObj.optString("weaponId", "wpn_iron_sword").ifEmpty { "wpn_iron_sword" },
                helmetId = eqObj.optString("helmetId", "").ifEmpty { null },
                chestId = eqObj.optString("chestId", "").ifEmpty { null },
                glovesId = eqObj.optString("glovesId", "").ifEmpty { null },
                bootsId = eqObj.optString("bootsId", "").ifEmpty { null },
                accessoryId = eqObj.optString("accessoryId", "").ifEmpty { null }
            )

            val skillsObj = root.optJSONObject("skills")
            val skills = mutableMapOf<String, Int>()
            skillsObj?.keys()?.forEach { k -> skills[k] = skillsObj.optInt(k, 0) }

            val questsObj = root.optJSONObject("quests")
            val quests = mutableMapOf<String, Pair<QuestStatus, Map<String, Int>>>()
            questsObj?.keys()?.forEach { qId ->
                val qj = questsObj.getJSONObject(qId)
                val status = runCatching { QuestStatus.valueOf(qj.getString("status")) }.getOrDefault(QuestStatus.AVAILABLE)
                val objMap = mutableMapOf<String, Int>()
                val objs = qj.optJSONObject("objectives")
                objs?.keys()?.forEach { oId -> objMap[oId] = objs.optInt(oId, 0) }
                quests[qId] = status to objMap
            }

            val activated = mutableSetOf<String>()
            root.optJSONArray("activatedInteractables")?.let { arr ->
                for (i in 0 until arr.length()) activated.add(arr.getString(i))
            }

            val bosses = mutableSetOf<String>()
            root.optJSONArray("defeatedBosses")?.let { arr ->
                for (i in 0 until arr.length()) bosses.add(arr.getString(i))
            }

            val regions = mutableSetOf<String>()
            root.optJSONArray("discoveredRegions")?.let { arr ->
                for (i in 0 until arr.length()) regions.add(arr.getString(i))
            }

            LoadedGamePayload(
                stats = stats,
                inventory = inv,
                equipment = equipment,
                skillRanks = skills,
                questStates = quests,
                activatedInteractables = activated,
                defeatedBosses = bosses,
                discoveredRegions = regions,
                worldTimeMinutes = root.optDouble("worldTimeMinutes", 480.0).toFloat(),
                playTimeSeconds = root.optInt("playTimeSeconds", 0)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun getSlotSummaries(): List<SaveSlotSummary> {
        return (0..3).map { slot ->
            val raw = readWithFallback(slotFile(slot), slotBackupFile(slot))
            if (raw == null) {
                SaveSlotSummary(slotIndex = slot, exists = false)
            } else {
                try {
                    val root = JSONObject(raw)
                    val s = root.getJSONObject("stats")
                    var completed = 0
                    val qObj = root.optJSONObject("quests")
                    qObj?.keys()?.forEach { k ->
                        if (qObj.getJSONObject(k).optString("status") == QuestStatus.COMPLETED.name) completed++
                    }
                    SaveSlotSummary(
                        slotIndex = slot,
                        exists = true,
                        playerLevel = s.optInt("level", 1),
                        regionName = root.optString("regionName", "Dawnreach Village"),
                        playTimeSeconds = root.optInt("playTimeSeconds", 0),
                        completedQuests = completed,
                        savedTimestamp = root.optLong("timestamp", 0L)
                    )
                } catch (_: Exception) {
                    SaveSlotSummary(slotIndex = slot, exists = false)
                }
            }
        }
    }

    fun hasAnySave(): Boolean = getSlotSummaries().any { it.exists }

    fun getMostRecentSlot(): Int? =
        getSlotSummaries().filter { it.exists }.maxByOrNull { it.savedTimestamp }?.slotIndex

    private fun writeAtomic(target: File, temp: File, backup: File, content: String) {
        temp.writeText(content, Charsets.UTF_8)
        if (target.exists()) {
            target.copyTo(backup, overwrite = true)
        }
        if (!temp.renameTo(target)) {
            target.writeText(content, Charsets.UTF_8)
            temp.delete()
        }
    }

    private fun readWithFallback(primary: File, backup: File): String? {
        if (primary.exists()) {
            val txt = runCatching { primary.readText(Charsets.UTF_8) }.getOrNull()
            if (!txt.isNullOrBlank()) return txt
        }
        if (backup.exists()) {
            val txt = runCatching { backup.readText(Charsets.UTF_8) }.getOrNull()
            if (!txt.isNullOrBlank()) return txt
        }
        return null
    }
}
