package com.example.game.world

import com.example.game.audio.AudioEngine
import com.example.game.audio.MusicTheme
import com.example.game.audio.SfxType
import com.example.game.math.Math3D
import com.example.game.math.TerrainMath
import com.example.game.model.*
import com.example.game.save.LoadedGamePayload
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Central real-time gameplay simulation for Aetherfall: The Lost Frontier.
 * Updates on the game loop thread and exposes immutable-style snapshots or synchronized state for UI & Renderer.
 */
class GameSimulation(
    private val audioEngine: AudioEngine,
    private val onVibrate: (Long) -> Unit
) {
    val stateLock = Any()

    // Core Player & World State
    var stats = PlayerStatsState()
    var equipment = EquipmentLoadout()
    val inventory = mutableListOf<InventoryEntry>()
    val skillRanks = mutableMapOf<String, Int>()
    val questList = GameCatalog.buildInitialQuests().toMutableList()
    val questStatuses = mutableMapOf<String, QuestStatus>()
    val npcs = GameCatalog.buildNpcs().toMutableList()
    val enemies = WorldEntities.createInitialEnemies()
    val interactables = WorldEntities.createInitialInteractables()
    val defeatedBosses = mutableSetOf<String>()
    val discoveredRegions = mutableSetOf(RegionId.DAWNREACH_VILLAGE.name)

    // VFX & Projectiles
    val arrows = mutableListOf<ArrowProjectile>()
    val particles = mutableListOf<ParticleVfx>()
    val damageIndicators = mutableListOf<DamageIndicator>()
    val lootNotifications = mutableListOf<LootNotification>()

    // Dynamic World Time & Weather
    var worldTimeMinutes = 520f // Starts ~08:40 Morning
    var currentWeather = WeatherType.CLEAR
    private var weatherTimer = 95f
    var playTimeSeconds = 0f

    // Player Movement & Animation State
    var playerVel = Vec3()
    var playerAnimState = AnimationState.IDLE
    var playerAnimTime = 0f
    var isGrounded = true
    var jumpsUsed = 0
    var isSwimming = false
    var isClimbing = false
    var footstepTimer = 0f

    // Combat State Machine
    var comboStep = 0
    var attackTimer = 0f
    var attackDuration = 0f
    var attackHasHit = false
    var isHeavySwing = false
    var comboResetTimer = 0f
    var dodgeTimer = 0f
    var invulnTimer = 0f
    var isBlocking = false
        private set
    var parryWindowTimer = 0f
    var staggerTimer = 0f
    var interactAnimTimer = 0f
    var hitStopTimer = 0f
    var cameraShake = 0f

    // Live Input State (thread-safe volatile)
    @Volatile var moveInputX = 0f
    @Volatile var moveInputY = 0f
    @Volatile var cameraYaw = 0f
    @Volatile var cameraPitch = 0.34f
    @Volatile var cameraDistance = 6.8f
    @Volatile var sprintHeld = false
    @Volatile var blockHeld = false
    @Volatile var settings = GameSettings()

    // Active Prompts & Boss Bar
    var nearestInteractPrompt: String? = null
    var nearestInteractTargetId: String? = null
    var activeBoss: EnemyInstance? = null
    var lorePopupTitle: String? = null
    var lorePopupText: String? = null
    var victoryTriggered = false

    private var nextId = 1L
    private val rng = Random(2026)
    private var lastRegion = RegionId.DAWNREACH_VILLAGE

    init {
        startNewGame()
    }

    fun startNewGame() {
        synchronized(stateLock) {
            stats = PlayerStatsState(
                level = 1,
                exp = 0,
                expToNext = 120,
                skillPoints = 1,
                gold = 85,
                baseMaxHp = 160f,
                baseMaxStamina = 110f,
                hp = 160f,
                stamina = 110f,
                position = Vec3(0f, TerrainMath.getHeight(0f, 16f), 16f),
                yaw = PI.toFloat()
            )
            equipment = EquipmentLoadout()
            inventory.clear()
            inventory.addAll(
                listOf(
                    InventoryEntry("wpn_iron_sword", 1),
                    InventoryEntry("wpn_windstrider_bow", 1),
                    InventoryEntry("arm_traveler_tunic", 1),
                    InventoryEntry("arm_leather_boots", 1),
                    InventoryEntry("con_health_potion", 4),
                    InventoryEntry("con_stamina_tonic", 2)
                )
            )
            skillRanks.clear()
            GameCatalog.skills.forEach { skillRanks[it.id] = 0 }

            questList.clear()
            questList.addAll(GameCatalog.buildInitialQuests())
            questStatuses.clear()
            questList.forEach { q ->
                questStatuses[q.id] = if (q.id == "mq_01") QuestStatus.ACTIVE else QuestStatus.AVAILABLE
            }

            npcs.clear()
            npcs.addAll(GameCatalog.buildNpcs())
            npcs.forEach { it.currentPos.y = TerrainMath.getHeight(it.currentPos.x, it.currentPos.z) }

            enemies.clear()
            enemies.addAll(WorldEntities.createInitialEnemies())
            interactables.clear()
            interactables.addAll(WorldEntities.createInitialInteractables())
            defeatedBosses.clear()
            discoveredRegions.clear()
            discoveredRegions.add(RegionId.DAWNREACH_VILLAGE.name)

            arrows.clear()
            particles.clear()
            damageIndicators.clear()
            lootNotifications.clear()

            worldTimeMinutes = 520f
            currentWeather = WeatherType.CLEAR
            playTimeSeconds = 0f
            playerVel = Vec3()
            playerAnimState = AnimationState.IDLE
            comboStep = 0
            attackTimer = 0f
            dodgeTimer = 0f
            invulnTimer = 0f
            staggerTimer = 0f
            activeBoss = null
            victoryTriggered = false
            cameraYaw = PI.toFloat()
        }
    }

    fun exportSavePayload(): LoadedGamePayload = synchronized(stateLock) {
        val qStates = mutableMapOf<String, Pair<QuestStatus, Map<String, Int>>>()
        questList.forEach { q ->
            val st = questStatuses[q.id] ?: QuestStatus.AVAILABLE
            val objMap = q.objectives.associate { it.id to it.currentCount }
            qStates[q.id] = st to objMap
        }
        LoadedGamePayload(
            stats = stats.copy(position = stats.position.copy()),
            inventory = inventory.map { it.copy() },
            equipment = equipment.copy(),
            skillRanks = skillRanks.toMap(),
            questStates = qStates,
            activatedInteractables = interactables.filter { it.activated }.map { it.id }.toSet(),
            defeatedBosses = defeatedBosses.toSet(),
            discoveredRegions = discoveredRegions.toSet(),
            worldTimeMinutes = worldTimeMinutes,
            playTimeSeconds = playTimeSeconds.toInt()
        )
    }

    fun applyLoadedPayload(payload: LoadedGamePayload) {
        synchronized(stateLock) {
            stats = payload.stats.copy(position = payload.stats.position.copy())
            equipment = payload.equipment.copy()
            inventory.clear()
            inventory.addAll(payload.inventory.map { it.copy() })

            skillRanks.clear()
            GameCatalog.skills.forEach { skillRanks[it.id] = payload.skillRanks[it.id] ?: 0 }

            questList.clear()
            questList.addAll(GameCatalog.buildInitialQuests())
            questStatuses.clear()
            questList.forEach { q ->
                val saved = payload.questStates[q.id]
                if (saved != null) {
                    questStatuses[q.id] = saved.first
                    q.objectives.forEach { o -> o.currentCount = saved.second[o.id] ?: 0 }
                } else {
                    questStatuses[q.id] = QuestStatus.AVAILABLE
                }
            }

            interactables.clear()
            interactables.addAll(WorldEntities.createInitialInteractables())
            interactables.forEach { item ->
                if (item.id in payload.activatedInteractables) item.activated = true
            }

            defeatedBosses.clear()
            defeatedBosses.addAll(payload.defeatedBosses)

            enemies.clear()
            enemies.addAll(WorldEntities.createInitialEnemies())
            enemies.forEach { en ->
                if (en.type.isBoss && en.id in defeatedBosses) {
                    en.hp = 0f
                    en.aiState = EnemyAiState.DEATH
                }
            }

            discoveredRegions.clear()
            discoveredRegions.addAll(payload.discoveredRegions)
            worldTimeMinutes = payload.worldTimeMinutes
            playTimeSeconds = payload.playTimeSeconds.toFloat()
            playerVel = Vec3()
            activeBoss = null
            victoryTriggered = false
        }
    }

    // Derived RPG Stats
    fun getMaxHp(): Float {
        var bonus = 0f
        listOfNotNull(equipment.helmetId, equipment.chestId, equipment.glovesId, equipment.bootsId, equipment.accessoryId)
            .forEach { id -> bonus += GameCatalog.items[id]?.healthBonus ?: 0f }
        return stats.baseMaxHp + bonus
    }

    fun getMaxStamina(): Float {
        var bonus = (skillRanks["sk_endurance"] ?: 0) * 25f
        listOfNotNull(equipment.bootsId, equipment.accessoryId)
            .forEach { id -> bonus += GameCatalog.items[id]?.staminaBonus ?: 0f }
        return stats.baseMaxStamina + bonus
    }

    fun getAttackPower(): Float {
        val wpnEntry = inventory.find { it.itemId == equipment.weaponId }
        val wpnItem = GameCatalog.items[equipment.weaponId]
        val base = (wpnItem?.attackBonus ?: 20f) + (wpnEntry?.upgradeLevel ?: 0) * 6f
        val gloveBonus = equipment.glovesId?.let { GameCatalog.items[it]?.attackBonus ?: 0f } ?: 0f
        val levelBonus = (stats.level - 1) * 4f
        val skillMult = 1f + (skillRanks["sk_sword_mastery"] ?: 0) * 0.12f
        return (base + gloveBonus + levelBonus) * skillMult
    }

    fun getDefense(): Float {
        var def = (stats.level - 1) * 1.5f + (skillRanks["sk_iron_ward"] ?: 0) * 5f
        listOfNotNull(equipment.helmetId, equipment.chestId, equipment.glovesId, equipment.bootsId)
            .forEach { id -> def += GameCatalog.items[id]?.defenseBonus ?: 0f }
        return def
    }

    fun getCritChance(): Float {
        var crit = (GameCatalog.items[equipment.weaponId]?.critBonus ?: 0.10f)
        crit += equipment.accessoryId?.let { GameCatalog.items[it]?.critBonus ?: 0f } ?: 0f
        crit += (skillRanks["sk_lethal_edge"] ?: 0) * 0.06f
        return crit.coerceIn(0.05f, 0.75f)
    }

    fun getEquippedWeaponType(): WeaponType =
        GameCatalog.items[equipment.weaponId]?.weaponType ?: WeaponType.SWORD

    fun getTimeOfDay(): TimeOfDay = when {
        worldTimeMinutes < 360f || worldTimeMinutes >= 1200f -> TimeOfDay.NIGHT
        worldTimeMinutes < 690f -> TimeOfDay.MORNING
        worldTimeMinutes < 1020f -> TimeOfDay.AFTERNOON
        else -> TimeOfDay.EVENING
    }

    // Player Actions Triggered from Virtual Gamepad / Gamepad
    fun triggerJump() {
        synchronized(stateLock) {
            if (stats.hp <= 0f || stats.inBoat || staggerTimer > 0f) return
            if (isGrounded) {
                playerVel.y = 10.6f
                isGrounded = false
                jumpsUsed = 1
                playerAnimState = AnimationState.JUMPING
                audioEngine.playSfx(SfxType.JUMP)
                spawnDust(stats.position, 6)
            } else if (jumpsUsed < 2 && stats.stamina >= 12f) {
                stats.stamina -= 12f
                playerVel.y = 9.6f
                jumpsUsed = 2
                playerAnimState = AnimationState.JUMPING
                audioEngine.playSfx(SfxType.JUMP, 0.85f)
                spawnBurst(stats.position, 0.25f, 0.8f, 1f, 8)
            }
        }
    }

    fun triggerDodge() {
        synchronized(stateLock) {
            if (stats.hp <= 0f || stats.inBoat || dodgeTimer > 0f || staggerTimer > 0f) return
            val costMult = 1f - (skillRanks["sk_phantom_step"] ?: 0) * 0.20f
            val cost = 20f * costMult
            if (stats.stamina < cost) return
            stats.stamina -= cost
            dodgeTimer = 0.42f
            invulnTimer = 0.36f + (skillRanks["sk_phantom_step"] ?: 0) * 0.06f
            attackTimer = 0f

            val moveLen = sqrt(moveInputX * moveInputX + moveInputY * moveInputY)
            if (moveLen > 0.15f) {
                val inputAngle = kotlin.math.atan2(moveInputX, -moveInputY)
                stats.yaw = cameraYaw + inputAngle
            }
            val dashSpeed = 14.5f
            playerVel.x = sin(stats.yaw) * dashSpeed
            playerVel.z = cos(stats.yaw) * dashSpeed
            playerAnimState = if (moveLen > 0.5f) AnimationState.ROLLING else AnimationState.DODGING
            playerAnimTime = 0f
            audioEngine.playSfx(SfxType.DODGE)
            spawnDust(stats.position, 7)
        }
    }

    fun triggerAttack(heavy: Boolean) {
        synchronized(stateLock) {
            if (stats.hp <= 0f || stats.inBoat || dodgeTimer > 0f || staggerTimer > 0f || attackTimer > 0.08f) return
            val wpn = getEquippedWeaponType()
            val cost = wpn.staminaCost * if (heavy) 1.75f else 1.0f
            if (stats.stamina < cost) return
            stats.stamina -= cost

            isHeavySwing = heavy
            attackHasHit = false

            if (wpn == WeaponType.BOW) {
                attackDuration = if (heavy) 0.48f else 0.34f
                attackTimer = attackDuration
                playerAnimState = if (heavy) AnimationState.HEAVY_ATTACK else AnimationState.LIGHT_ATTACK
                playerAnimTime = 0f
                firePlayerArrow(heavy)
                audioEngine.playSfx(SfxType.ARROW_SHOT)
                return
            }

            if (heavy) {
                comboStep = 3
                attackDuration = 0.56f / wpn.attackSpeed
                playerAnimState = AnimationState.HEAVY_ATTACK
                audioEngine.playSfx(SfxType.HEAVY_ATTACK)
            } else {
                comboStep = if (comboResetTimer > 0f) (comboStep % 3) + 1 else 1
                attackDuration = 0.38f / wpn.attackSpeed
                playerAnimState = if (comboStep == 3) AnimationState.COMBO_ATTACK else AnimationState.LIGHT_ATTACK
                audioEngine.playSfx(SfxType.SWORD_SWING)
            }
            attackTimer = attackDuration
            comboResetTimer = 1.15f
            playerAnimTime = 0f

            // Subtle forward lunge toward nearest enemy in front
            autoFaceNearestEnemy()
            playerVel.x = sin(stats.yaw) * 3.8f
            playerVel.z = cos(stats.yaw) * 3.8f
        }
    }

    fun setBlocking(held: Boolean) {
        synchronized(stateLock) {
            if (held && !blockHeld && !stats.inBoat && attackTimer <= 0f) {
                parryWindowTimer = 0.24f + (skillRanks["sk_iron_ward"] ?: 0) * 0.05f
            }
            blockHeld = held
            isBlocking = held && !stats.inBoat && attackTimer <= 0f && dodgeTimer <= 0f && stats.stamina > 4f
        }
    }

    fun switchNextWeapon() {
        synchronized(stateLock) {
            val ownedWeapons = inventory.mapNotNull { entry ->
                val def = GameCatalog.items[entry.itemId]
                if (def?.equipSlot == EquipSlot.WEAPON) entry.itemId else null
            }.distinct()
            if (ownedWeapons.size <= 1) return
            val idx = ownedWeapons.indexOf(equipment.weaponId)
            val next = ownedWeapons[(idx + 1) % ownedWeapons.size]
            equipment.weaponId = next
            audioEngine.playSfx(SfxType.UI_CLICK)
            val name = GameCatalog.items[next]?.name ?: "Weapon"
            pushLootNotification("Equipped $name", "Weapon switched", false)
        }
    }

    fun useQuickPotion() {
        synchronized(stateLock) {
            val potion = inventory.find { it.itemId == "con_greater_potion" && it.quantity > 0 }
                ?: inventory.find { it.itemId == "con_health_potion" && it.quantity > 0 }
                ?: return
            useInventoryItem(potion.itemId)
        }
    }

    fun useInventoryItem(itemId: String): Boolean {
        synchronized(stateLock) {
            val def = GameCatalog.items[itemId] ?: return false
            val entry = inventory.find { it.itemId == itemId } ?: return false
            if (entry.quantity <= 0) return false

            return when (def.category) {
                ItemCategory.CONSUMABLES -> {
                    val maxHp = getMaxHp()
                    val maxStam = getMaxStamina()
                    if (def.healAmount > 0f) {
                        val healed = min(def.healAmount, maxHp - stats.hp).toInt()
                        stats.hp = min(maxHp, stats.hp + def.healAmount)
                        if (healed > 0) spawnDamageIndicator(stats.position, healed, isCrit = false, isHeal = true)
                    }
                    if (def.staminaRestore > 0f) {
                        stats.stamina = min(maxStam, stats.stamina + def.staminaRestore)
                    }
                    entry.quantity--
                    if (entry.quantity <= 0) inventory.remove(entry)
                    audioEngine.playSfx(SfxType.ITEM_PICKUP)
                    true
                }
                ItemCategory.WEAPONS, ItemCategory.ARMOR -> {
                    when (def.equipSlot) {
                        EquipSlot.WEAPON -> equipment.weaponId = itemId
                        EquipSlot.HELMET -> equipment.helmetId = itemId
                        EquipSlot.CHEST -> equipment.chestId = itemId
                        EquipSlot.GLOVES -> equipment.glovesId = itemId
                        EquipSlot.BOOTS -> equipment.bootsId = itemId
                        EquipSlot.ACCESSORY -> equipment.accessoryId = itemId
                        null -> return false
                    }
                    stats.hp = min(stats.hp, getMaxHp())
                    stats.stamina = min(stats.stamina, getMaxStamina())
                    audioEngine.playSfx(SfxType.UI_CLICK)
                    true
                }
                else -> false
            }
        }
    }

    fun unequipSlot(slot: EquipSlot) {
        synchronized(stateLock) {
            when (slot) {
                EquipSlot.WEAPON -> Unit // Always keep a weapon equipped
                EquipSlot.HELMET -> equipment.helmetId = null
                EquipSlot.CHEST -> equipment.chestId = null
                EquipSlot.GLOVES -> equipment.glovesId = null
                EquipSlot.BOOTS -> equipment.bootsId = null
                EquipSlot.ACCESSORY -> equipment.accessoryId = null
            }
            stats.hp = min(stats.hp, getMaxHp())
            audioEngine.playSfx(SfxType.UI_CLICK)
        }
    }

    fun dropItem(itemId: String) {
        synchronized(stateLock) {
            if (itemId == equipment.weaponId) return
            val entry = inventory.find { it.itemId == itemId } ?: return
            if (GameCatalog.items[itemId]?.category == ItemCategory.QUEST_ITEMS) return
            entry.quantity--
            if (entry.quantity <= 0) inventory.remove(entry)
            audioEngine.playSfx(SfxType.UI_CLICK)
        }
    }

    fun sortInventory() {
        synchronized(stateLock) {
            inventory.sortWith(compareBy({ GameCatalog.items[it.itemId]?.category?.ordinal ?: 99 }, { GameCatalog.items[it.itemId]?.name ?: "" }))
            audioEngine.playSfx(SfxType.UI_CLICK)
        }
    }

    fun upgradeEquippedWeapon(): String {
        synchronized(stateLock) {
            val entry = inventory.find { it.itemId == equipment.weaponId } ?: return "No weapon equipped."
            val cost = 45 + entry.upgradeLevel * 35
            if (stats.gold < cost) return "Need $cost Gold to temper weapon."
            if (entry.upgradeLevel >= 5) return "Weapon is already Masterwork (+5)!"
            stats.gold -= cost
            entry.upgradeLevel++
            audioEngine.playSfx(SfxType.PARRY_CLANG)
            val name = GameCatalog.items[entry.itemId]?.name ?: "Weapon"
            pushLootNotification("$name +${entry.upgradeLevel}", "+6 Attack Power forged!", false)
            return "Forged $name to +${entry.upgradeLevel}!"
        }
    }

    fun buyShopItem(itemId: String): String {
        synchronized(stateLock) {
            val def = GameCatalog.items[itemId] ?: return "Item unavailable."
            if (stats.gold < def.priceGold) return "Not enough Gold (${def.priceGold}G required)."
            stats.gold -= def.priceGold
            addItemToInventory(itemId, 1)
            audioEngine.playSfx(SfxType.ITEM_PICKUP)
            return "Purchased ${def.name}."
        }
    }

    fun upgradeSkill(skillId: String): Boolean {
        synchronized(stateLock) {
            val def = GameCatalog.skills.find { it.id == skillId } ?: return false
            val rank = skillRanks[skillId] ?: 0
            if (stats.skillPoints <= 0 || rank >= def.maxRank) return false
            stats.skillPoints--
            skillRanks[skillId] = rank + 1
            stats.stamina = min(getMaxStamina(), stats.stamina + 25f)
            audioEngine.playSfx(SfxType.LEVEL_UP)
            return true
        }
    }

    fun acceptQuest(questId: String) {
        synchronized(stateLock) {
            if (questStatuses[questId] == QuestStatus.AVAILABLE) {
                questStatuses[questId] = QuestStatus.ACTIVE
                val q = questList.find { it.id == questId }
                if (q != null) {
                    pushLootNotification("Quest Accepted", q.title, true)
                    audioEngine.playSfx(SfxType.UI_CLICK)
                }
            }
        }
    }

    fun fastTravelTo(interactableId: String): Boolean {
        synchronized(stateLock) {
            val wp = interactables.find { it.id == interactableId && it.type == InteractableType.WAYPOINT_SHRINE && it.activated }
                ?: return false
            stats.inBoat = false
            stats.inDungeon = false
            stats.position.x = wp.position.x + 2.5f
            stats.position.z = wp.position.z + 2.5f
            stats.position.y = TerrainMath.getHeight(stats.position.x, stats.position.z) + 0.2f
            playerVel = Vec3()
            audioEngine.playSfx(SfxType.QUEST_COMPLETE)
            spawnBurst(stats.position, 0.2f, 0.85f, 1f, 22)
            pushLootNotification("Fast Traveled", wp.title, false)
            return true
        }
    }

    /**
     * Trigger primary contextual interaction (NPC, Chest, Collectible, Boat, Dungeon, Waypoint, Lore, Altar).
     * Returns an NPC id if dialogue UI should open.
     */
    fun triggerInteract(): String? {
        synchronized(stateLock) {
            // 1. Check nearby NPCs
            val nearNpc = npcs.minByOrNull { it.currentPos.horizontalDistanceTo(stats.position) }
            if (nearNpc != null && nearNpc.currentPos.horizontalDistanceTo(stats.position) < 4.5f) {
                interactAnimTimer = 0.55f
                playerAnimState = AnimationState.INTERACTING
                audioEngine.playSfx(SfxType.UI_CLICK)
                advanceQuestTag("talk_${nearNpc.id}")
                return nearNpc.id
            }

            // 2. If riding Silverlake boat, allow disembarking near shore
            if (stats.inBoat) {
                stats.inBoat = false
                val boat = interactables.find { it.type == InteractableType.BOAT_DOCK }
                boat?.position?.copyFrom(stats.position)
                stats.position.y = max(TerrainMath.getHeight(stats.position.x, stats.position.z), TerrainMath.WATER_LEVEL + 0.4f)
                audioEngine.playSfx(SfxType.LAND)
                pushLootNotification("Disembarked", "Left the Silverlake Rowboat", false)
                return null
            }

            // 3. Check nearby World Interactables
            val nearObj = interactables
                .filter { !(it.activated && (it.type == InteractableType.TREASURE_CHEST || it.type == InteractableType.COLLECTIBLE_ITEM)) }
                .minByOrNull { it.position.horizontalDistanceTo(stats.position) }

            if (nearObj != null && nearObj.position.horizontalDistanceTo(stats.position) < 5.0f) {
                interactAnimTimer = 0.65f
                playerAnimState = AnimationState.COLLECTING_ITEM
                when (nearObj.type) {
                    InteractableType.TREASURE_CHEST -> {
                        nearObj.activated = true
                        if (nearObj.rewardGold > 0) stats.gold += nearObj.rewardGold
                        nearObj.rewardItemId?.let { addItemToInventory(it, 1) }
                        grantExp(45)
                        audioEngine.playSfx(SfxType.CHEST_OPEN)
                        spawnBurst(nearObj.position, 1f, 0.85f, 0.25f, 20)
                        val itemName = nearObj.rewardItemId?.let { GameCatalog.items[it]?.name } ?: "Gold"
                        pushLootNotification("Chest Opened", "+${nearObj.rewardGold}G & $itemName", false)
                        if (nearObj.questTag.isNotEmpty()) advanceQuestTag(nearObj.questTag)
                    }
                    InteractableType.COLLECTIBLE_ITEM -> {
                        nearObj.activated = true
                        nearObj.rewardItemId?.let { addItemToInventory(it, 1) }
                        grantExp(25)
                        audioEngine.playSfx(SfxType.ITEM_PICKUP)
                        spawnBurst(nearObj.position, 0.3f, 0.9f, 0.5f, 14)
                        pushLootNotification("Collected", nearObj.title, false)
                        if (nearObj.questTag.isNotEmpty()) advanceQuestTag(nearObj.questTag)
                    }
                    InteractableType.WAYPOINT_SHRINE -> {
                        if (!nearObj.activated) {
                            nearObj.activated = true
                            grantExp(75)
                            audioEngine.playSfx(SfxType.QUEST_COMPLETE)
                            spawnBurst(nearObj.position, 0.25f, 0.85f, 1f, 26)
                            pushLootNotification("Waypoint Attuned", "${nearObj.title} unlocked for Fast Travel", true)
                        } else {
                            // Resting at shrine heals player
                            stats.hp = getMaxHp()
                            stats.stamina = getMaxStamina()
                            audioEngine.playSfx(SfxType.ITEM_PICKUP)
                            pushLootNotification("Shrine Rest", "Health & Stamina restored", false)
                        }
                        if (nearObj.questTag.isNotEmpty()) advanceQuestTag(nearObj.questTag)
                    }
                    InteractableType.BOAT_DOCK -> {
                        stats.inBoat = true
                        stats.position.x = nearObj.position.x
                        stats.position.z = nearObj.position.z
                        stats.position.y = TerrainMath.WATER_LEVEL + 0.35f
                        audioEngine.playSfx(SfxType.JUMP)
                        pushLootNotification("Rowboat Boarded", "Steer across Silverlake Valley", false)
                    }
                    InteractableType.DUNGEON_PORTAL -> {
                        stats.inDungeon = !stats.inDungeon
                        audioEngine.playSfx(SfxType.BOSS_INTRO)
                        spawnBurst(nearObj.position, 0.7f, 0.3f, 1f, 22)
                        if (stats.inDungeon) {
                            pushLootNotification("Entered Dungeon Vault", "Torchlight reveals the Royal Vault", true)
                            if (nearObj.questTag.isNotEmpty()) advanceQuestTag(nearObj.questTag)
                        } else {
                            pushLootNotification("Exited to Citadel", "Returned to Forgotten Kingdom", false)
                        }
                    }
                    InteractableType.LORE_TABLET -> {
                        nearObj.activated = true
                        lorePopupTitle = nearObj.title
                        lorePopupText = if (nearObj.id == "lore_greenveil") {
                            "\"Before The Fracture, the elders bound the forest spirit with living runes. Should the sky tear anew, only Aether steel may quiet the Tyrant.\""
                        } else {
                            "\"When the Citadel fell, the Ancient Colossus sealed the Fracture Altar behind itself. Strike its core only when its shockwave fades.\""
                        }
                        audioEngine.playSfx(SfxType.UI_CLICK)
                        if (nearObj.questTag.isNotEmpty()) advanceQuestTag(nearObj.questTag)
                    }
                    InteractableType.BEACON_BRAZIER -> {
                        if (!nearObj.activated) {
                            nearObj.activated = true
                            grantExp(120)
                            audioEngine.playSfx(SfxType.QUEST_COMPLETE)
                            spawnBurst(nearObj.position, 1f, 0.45f, 0.1f, 30)
                            pushLootNotification("Beacon Ignited", "Signal fire blazes across Ashen Highlands", true)
                            advanceQuestTag("activate_beacon")
                        }
                    }
                    InteractableType.FRACTURE_ALTAR -> {
                        if ("mq_07" in questStatuses.filterValues { it == QuestStatus.COMPLETED }.keys ||
                            "boss_ancient_colossus" in defeatedBosses
                        ) {
                            nearObj.activated = true
                            advanceQuestTag("activate_fracture_altar")
                            victoryTriggered = true
                            audioEngine.playSfx(SfxType.LEVEL_UP)
                            spawnBurst(nearObj.position, 0.3f, 0.9f, 1f, 45)
                        } else {
                            pushLootNotification("Sealed by Colossus", "Defeat The Ancient Colossus first!", true)
                        }
                    }
                }
            }
            return null
        }
    }

    fun respawnAfterDeath() {
        synchronized(stateLock) {
            stats.hp = getMaxHp()
            stats.stamina = getMaxStamina()
            stats.inBoat = false
            stats.inDungeon = false
            stats.position = Vec3(0f, TerrainMath.getHeight(0f, 12f), 12f)
            playerVel = Vec3()
            staggerTimer = 0f
            activeBoss = null
            enemies.forEach {
                if (it.isAlive && it.aiState != EnemyAiState.PATROL) {
                    it.aiState = EnemyAiState.RETURN
                }
            }
            pushLootNotification("Awakened at Dawnreach", "Recovered at the village shrine", false)
        }
    }

    fun step(dtRaw: Float) {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        synchronized(stateLock) {
            if (hitStopTimer > 0f) {
                hitStopTimer -= dt
                updateParticlesAndIndicators(dt * 0.25f)
                return
            }

            playTimeSeconds += dt
            updateDayNightAndWeather(dt)

            if (stats.hp <= 0f) {
                playerAnimState = AnimationState.DEATH
                playerAnimTime += dt
                updateParticlesAndIndicators(dt)
                return
            }

            updateTimers(dt)
            updatePlayerMovement(dt)
            updatePlayerCombatHitDetection()
            updateArrows(dt)
            updateNpcs(dt)
            updateEnemiesAndBosses(dt)
            updateParticlesAndIndicators(dt)
            updateNearestInteractablePrompt()
            updateRegionAndMusic()
        }
    }

    private fun updateDayNightAndWeather(dt: Float) {
        // 1 full 24h cycle = ~12 real minutes
        worldTimeMinutes = (worldTimeMinutes + dt * 2.0f) % 1440f
        weatherTimer -= dt
        if (weatherTimer <= 0f) {
            weatherTimer = rng.nextFloat() * 70f + 50f
            val roll = rng.nextFloat()
            currentWeather = when {
                roll < 0.48f -> WeatherType.CLEAR
                roll < 0.72f -> WeatherType.CLOUDY
                roll < 0.90f -> WeatherType.RAIN
                else -> WeatherType.FOG
            }
        }
        if (settings.weatherEffectsEnabled && currentWeather == WeatherType.RAIN && particles.size < settings.graphicsQuality.maxParticles) {
            repeat(3) {
                particles.add(
                    ParticleVfx(
                        position = Vec3(
                            stats.position.x + (rng.nextFloat() - 0.5f) * 32f,
                            stats.position.y + 14f + rng.nextFloat() * 4f,
                            stats.position.z + (rng.nextFloat() - 0.5f) * 32f
                        ),
                        velocity = Vec3(-1.2f, -18f, 0.4f),
                        r = 0.65f, g = 0.80f, b = 1.0f,
                        size = 0.14f,
                        life = 0.85f,
                        maxLife = 0.85f
                    )
                )
            }
        }
    }

    private fun updateTimers(dt: Float) {
        playerAnimTime += dt
        if (attackTimer > 0f) attackTimer = max(0f, attackTimer - dt)
        if (comboResetTimer > 0f) comboResetTimer = max(0f, comboResetTimer - dt)
        if (dodgeTimer > 0f) dodgeTimer = max(0f, dodgeTimer - dt)
        if (invulnTimer > 0f) invulnTimer = max(0f, invulnTimer - dt)
        if (parryWindowTimer > 0f) parryWindowTimer = max(0f, parryWindowTimer - dt)
        if (staggerTimer > 0f) staggerTimer = max(0f, staggerTimer - dt)
        if (interactAnimTimer > 0f) interactAnimTimer = max(0f, interactAnimTimer - dt)
        if (cameraShake > 0f) cameraShake = max(0f, cameraShake - dt * 2.8f)

        // Stamina regen
        val regenRate = if (isBlocking) 6f else 22f + (skillRanks["sk_endurance"] ?: 0) * 4f
        if (attackTimer <= 0f && dodgeTimer <= 0f && !sprintHeld) {
            stats.stamina = min(getMaxStamina(), stats.stamina + regenRate * dt)
        }
    }

    private fun updatePlayerMovement(dt: Float) {
        val maxStam = getMaxStamina()
        val inputMag = sqrt(moveInputX * moveInputX + moveInputY * moveInputY).coerceAtMost(1f)

        if (stats.inBoat) {
            val steer = moveInputX
            val thrust = -moveInputY
            stats.yaw -= steer * 1.9f * dt
            val speed = thrust * 11.5f
            val nextX = Math3D.clamp(stats.position.x + sin(stats.yaw) * speed * dt, 220f, 670f)
            val nextZ = Math3D.clamp(stats.position.z + cos(stats.yaw) * speed * dt, 220f, 670f)
            val groundH = TerrainMath.getHeight(nextX, nextZ)
            if (groundH < TerrainMath.WATER_LEVEL + 0.1f) {
                stats.position.x = nextX
                stats.position.z = nextZ
                stats.position.y = TerrainMath.WATER_LEVEL + 0.35f + sin(playTimeSeconds * 3f) * 0.06f
            }
            playerAnimState = AnimationState.INTERACTING
            return
        }

        if (dodgeTimer > 0f) {
            stats.position.x = Math3D.clamp(stats.position.x + playerVel.x * dt, -960f, 960f)
            stats.position.z = Math3D.clamp(stats.position.z + playerVel.z * dt, -960f, 960f)
        } else if (staggerTimer <= 0f && attackTimer <= 0.12f) {
            if (inputMag > 0.08f) {
                val inputAngle = kotlin.math.atan2(moveInputX, -moveInputY)
                val targetYaw = cameraYaw + inputAngle
                stats.yaw = Math3D.lerpAngleRad(stats.yaw, targetYaw, (dt * 12f).coerceAtMost(1f))

                val isSprinting = (sprintHeld || inputMag > 0.90f) && stats.stamina > 3f && !isBlocking
                if (isSprinting) {
                    stats.stamina = max(0f, stats.stamina - 11f * dt)
                }
                val baseSpeed = when {
                    isBlocking -> 3.4f
                    isSwimming -> 5.2f
                    isSprinting -> 11.4f
                    inputMag > 0.55f -> 8.2f
                    else -> 4.3f
                }

                val moveX = sin(targetYaw) * baseSpeed * inputMag
                val moveZ = cos(targetYaw) * baseSpeed * inputMag
                val nextX = Math3D.clamp(stats.position.x + moveX * dt, -960f, 960f)
                val nextZ = Math3D.clamp(stats.position.z + moveZ * dt, -960f, 960f)

                val currH = TerrainMath.getHeight(stats.position.x, stats.position.z)
                val nextH = TerrainMath.getHeight(nextX, nextZ)
                val slope = (nextH - currH) / max(0.05f, Math3D.length2D(nextX - stats.position.x, nextZ - stats.position.z))
                isClimbing = slope > 0.65f && slope < 2.2f

                if (slope < 2.2f) {
                    stats.position.x = nextX
                    stats.position.z = nextZ
                }

                if (isGrounded && !isSwimming) {
                    footstepTimer -= dt * (baseSpeed / 4.5f)
                    if (footstepTimer <= 0f) {
                        footstepTimer = 0.36f
                        audioEngine.playSfx(SfxType.FOOTSTEP, 0.55f)
                    }
                }
            } else {
                isClimbing = false
            }
        }

        // Vertical Gravity & Water Swimming
        val groundY = TerrainMath.getHeight(stats.position.x, stats.position.z)
        if (groundY < TerrainMath.WATER_LEVEL - 0.55f && stats.position.y <= TerrainMath.WATER_LEVEL + 0.1f) {
            isSwimming = true
            isGrounded = true
            jumpsUsed = 0
            playerVel.y = 0f
            stats.position.y = Math3D.lerp(stats.position.y, TerrainMath.WATER_LEVEL - 0.35f, dt * 8f)
        } else {
            isSwimming = false
            playerVel.y -= 26.0f * dt
            stats.position.y += playerVel.y * dt
            if (stats.position.y <= groundY) {
                if (!isGrounded && playerVel.y < -8f) {
                    audioEngine.playSfx(SfxType.LAND, 0.7f)
                    spawnDust(stats.position, 5)
                    playerAnimState = AnimationState.LANDING
                }
                stats.position.y = groundY
                playerVel.y = 0f
                isGrounded = true
                jumpsUsed = 0
            } else if (stats.position.y > groundY + 0.25f) {
                isGrounded = false
            }
        }

        // Resolve Character Animation State cleanly
        if (staggerTimer > 0f) {
            playerAnimState = AnimationState.STAGGER
        } else if (dodgeTimer > 0f) {
            // Keep ROLLING / DODGING
        } else if (attackTimer > 0f) {
            // Keep attack animation
        } else if (interactAnimTimer > 0f) {
            // Keep interacting/collecting
        } else if (isBlocking) {
            playerAnimState = if (parryWindowTimer > 0f) AnimationState.PARRY else AnimationState.BLOCK
        } else if (isSwimming) {
            playerAnimState = AnimationState.SWIMMING
        } else if (isClimbing && inputMag > 0.15f) {
            playerAnimState = AnimationState.CLIMBING
        } else if (!isGrounded) {
            playerAnimState = if (playerVel.y > 0.5f) AnimationState.JUMPING else AnimationState.FALLING
        } else if (inputMag > 0.90f || (sprintHeld && inputMag > 0.2f)) {
            playerAnimState = AnimationState.SPRINTING
        } else if (inputMag > 0.50f) {
            playerAnimState = AnimationState.RUNNING
        } else if (inputMag > 0.08f) {
            playerAnimState = AnimationState.WALKING
        } else {
            playerAnimState = AnimationState.IDLE
        }

        // Check Quest Region Visits
        val region = TerrainMath.getRegionAt(stats.position.x, stats.position.z)
        if (discoveredRegions.add(region.name)) {
            grantExp(60)
            pushLootNotification("Region Discovered", region.displayName, true)
        }
        when (region) {
            RegionId.GREENVEIL_FOREST -> advanceQuestTag("visit_greenveil")
            RegionId.ASHEN_HIGHLANDS -> advanceQuestTag("visit_ashen")
            RegionId.SILVERLAKE_VALLEY -> advanceQuestTag("visit_silverlake")
            RegionId.FORGOTTEN_KINGDOM -> advanceQuestTag("visit_forgotten")
            RegionId.DAWNREACH_VILLAGE -> Unit
        }
        if (Math3D.length2D(stats.position.x + 460f, stats.position.z + 180f) < 38f) {
            advanceQuestTag("visit_waterfall")
        }
        if (Math3D.length2D(stats.position.x - 520f, stats.position.z + 500f) < 60f) {
            advanceQuestTag("visit_volcano")
        }
    }

    private fun autoFaceNearestEnemy() {
        val target = enemies.filter { it.isAlive }
            .minByOrNull { it.position.horizontalDistanceTo(stats.position) }
        if (target != null && target.position.horizontalDistanceTo(stats.position) < 6.5f) {
            stats.yaw = Math3D.yawFromVector(target.position.x - stats.position.x, target.position.z - stats.position.z)
        }
    }

    private fun updatePlayerCombatHitDetection() {
        if (attackTimer <= 0f || attackHasHit) return
        val progress = 1f - (attackTimer / attackDuration.coerceAtLeast(0.05f))
        if (progress < 0.35f) return

        val wpn = getEquippedWeaponType()
        if (wpn == WeaponType.BOW) {
            attackHasHit = true
            return
        }

        attackHasHit = true
        val reach = wpn.range + 0.6f
        val fwdX = sin(stats.yaw)
        val fwdZ = cos(stats.yaw)

        for (enemy in enemies) {
            if (!enemy.isAlive) continue
            val dist = enemy.position.horizontalDistanceTo(stats.position)
            if (dist <= reach + if (enemy.type.isBoss) 1.8f else 0.5f) {
                val dx = (enemy.position.x - stats.position.x) / max(0.01f, dist)
                val dz = (enemy.position.z - stats.position.z) / max(0.01f, dist)
                val dot = fwdX * dx + fwdZ * dz
                if (dot > -0.15f || dist < 1.8f) {
                    applyDamageToEnemy(enemy, isHeavy = isHeavySwing, knockback = if (isHeavySwing) 8f else 4.5f)
                }
            }
        }
    }

    private fun firePlayerArrow(heavy: Boolean) {
        val speed = if (heavy) 38f else 30f
        val dirX = sin(stats.yaw)
        val dirZ = cos(stats.yaw)
        val dmg = getAttackPower() * if (heavy) 1.55f else 1.0f
        arrows.add(
            ArrowProjectile(
                id = nextId++,
                fromPlayer = true,
                position = Vec3(stats.position.x + dirX * 0.8f, stats.position.y + 1.45f, stats.position.z + dirZ * 0.8f),
                velocity = Vec3(dirX * speed, 1.5f, dirZ * speed),
                damage = dmg
            )
        )
    }

    private fun applyDamageToEnemy(enemy: EnemyInstance, isHeavy: Boolean, knockback: Float, rawDamageOverride: Float? = null) {
        if (enemy.isBlocking && !isHeavy) {
            audioEngine.playSfx(SfxType.PARRY_CLANG, 0.7f)
            spawnBurst(enemy.position, 0.7f, 0.8f, 1f, 8)
            spawnDamageIndicator(enemy.position, 0, isCrit = false)
            return
        }

        val isCrit = rng.nextFloat() < getCritChance()
        var dmg = rawDamageOverride ?: getAttackPower()
        if (isHeavy) dmg *= (1.55f + (skillRanks["sk_heavy_impact"] ?: 0) * 0.20f)
        if (comboStep == 3) dmg *= 1.30f
        if (isCrit) dmg *= (1.65f + (skillRanks["sk_lethal_edge"] ?: 0) * 0.12f)

        val finalDmg = dmg.toInt().coerceAtLeast(1)
        enemy.hp = max(0f, enemy.hp - finalDmg)

        // Knockback & reaction
        if (!enemy.type.isBoss) {
            val dx = enemy.position.x - stats.position.x
            val dz = enemy.position.z - stats.position.z
            val len = max(0.01f, sqrt(dx * dx + dz * dz))
            enemy.velocity.x += (dx / len) * knockback
            enemy.velocity.z += (dz / len) * knockback
            if (isHeavy || isCrit) {
                enemy.aiState = EnemyAiState.STAGGER
                enemy.stateTimer = 0.55f
                enemy.animState = AnimationState.STAGGER
            }
        } else {
            // Boss phase 2 transition
            if (enemy.phase == 1 && enemy.hp > 0f && enemy.hp <= enemy.maxHp * 0.52f) {
                enemy.phase = 2
                enemy.aiState = EnemyAiState.STAGGER
                enemy.stateTimer = 1.1f
                audioEngine.playSfx(SfxType.BOSS_INTRO)
                spawnBurst(enemy.position, 1f, 0.35f, 0.15f, 32)
                pushLootNotification("${enemy.type.displayName} Enraged!", "Phase 2 unleashed!", true)
            }
        }

        audioEngine.playSfx(SfxType.HIT_IMPACT)
        if (settings.vibrationEnabled) onVibrate(if (isHeavy || isCrit) 35L else 18L)
        cameraShake = if (isHeavy || isCrit) 0.28f else 0.14f
        if (isCrit) hitStopTimer = 0.045f

        spawnBurst(Vec3(enemy.position.x, enemy.position.y + 1.2f, enemy.position.z), 1f, if (isCrit) 0.85f else 0.35f, 0.2f, 12)
        spawnDamageIndicator(enemy.position, finalDmg, isCrit = isCrit)

        if (enemy.hp <= 0f) {
            handleEnemyDeath(enemy)
        }
    }

    private fun handleEnemyDeath(enemy: EnemyInstance) {
        enemy.aiState = EnemyAiState.DEATH
        enemy.animState = AnimationState.DEATH
        enemy.respawnTimer = if (enemy.type.isBoss) 999999f else 48f
        audioEngine.playSfx(SfxType.ENEMY_DEATH)
        grantExp(enemy.type.expReward)
        stats.gold += enemy.type.goldReward
        if (enemy.dropTag.isNotEmpty()) advanceQuestTag(enemy.dropTag)

        if (enemy.type.isBoss) {
            defeatedBosses.add(enemy.id)
            activeBoss = null
            if (enemy.type == EnemyType.FOREST_TYRANT) addItemToInventory("qst_tyrant_heart", 1)
            if (enemy.type == EnemyType.ANCIENT_COLOSSUS) addItemToInventory("qst_colossus_core", 1)
            pushLootNotification("BOSS VANQUISHED!", "${enemy.type.displayName} • +${enemy.type.expReward} EXP", true)
        } else if (rng.nextFloat() < 0.45f) {
            addItemToInventory("con_health_potion", 1)
            pushLootNotification("Loot Dropped", "Crimson Vitality Draught + ${enemy.type.goldReward}G", false)
        }
    }

    private fun updateArrows(dt: Float) {
        var idx = arrows.size - 1
        while (idx >= 0) {
            val a = arrows[idx]
            a.lifeTime -= dt
            a.velocity.y -= 5.2f * dt
            a.position.x += a.velocity.x * dt
            a.position.y += a.velocity.y * dt
            a.position.z += a.velocity.z * dt

            var remove = a.lifeTime <= 0f || a.position.y < TerrainMath.getHeight(a.position.x, a.position.z)
            if (!remove && a.fromPlayer) {
                for (en in enemies) {
                    if (en.isAlive && en.position.horizontalDistanceTo(a.position) < 1.5f && abs(en.position.y + 1.1f - a.position.y) < 1.8f) {
                        applyDamageToEnemy(en, isHeavy = false, knockback = 2.5f, rawDamageOverride = a.damage)
                        remove = true
                        break
                    }
                }
            } else if (!remove && !a.fromPlayer) {
                if (stats.position.horizontalDistanceTo(a.position) < 1.1f && abs(stats.position.y + 1.0f - a.position.y) < 1.5f) {
                    applyDamageToPlayer(a.damage, a.position)
                    remove = true
                }
            }

            if (remove) arrows.removeAt(idx)
            idx--
        }
    }

    private fun updateNpcs(dt: Float) {
        for (npc in npcs) {
            npc.animTime += dt
            val distToPlayer = npc.currentPos.horizontalDistanceTo(stats.position)
            if (distToPlayer < 4.5f) {
                npc.isTalking = true
                npc.yaw = Math3D.yawFromVector(stats.position.x - npc.currentPos.x, stats.position.z - npc.currentPos.z)
            } else {
                npc.isTalking = false
                if (npc.patrolRadius > 2.2f) {
                    val angle = playTimeSeconds * 0.35f + npc.id.hashCode()
                    val tx = npc.homePos.x + cos(angle) * (npc.patrolRadius * 0.65f)
                    val tz = npc.homePos.z + sin(angle) * (npc.patrolRadius * 0.65f)
                    val dx = tx - npc.currentPos.x
                    val dz = tz - npc.currentPos.z
                    if (dx * dx + dz * dz > 0.1f) {
                        npc.yaw = Math3D.yawFromVector(dx, dz)
                        npc.currentPos.x += sin(npc.yaw) * 1.4f * dt
                        npc.currentPos.z += cos(npc.yaw) * 1.4f * dt
                        npc.currentPos.y = TerrainMath.getHeight(npc.currentPos.x, npc.currentPos.z)
                    }
                }
            }
        }
    }

    private fun updateEnemiesAndBosses(dt: Float) {
        var currentBossBar: EnemyInstance? = null

        for (en in enemies) {
            val distToPlayer = en.position.horizontalDistanceTo(stats.position)

            // Distance-based AI throttling for mid-range Mali GPUs
            if (distToPlayer > 95f) continue

            if (!en.isAlive) {
                if (!en.type.isBoss) {
                    en.respawnTimer -= dt
                    if (en.respawnTimer <= 0f) {
                        en.hp = en.maxHp
                        en.position.copyFrom(en.spawnPos)
                        en.aiState = EnemyAiState.PATROL
                        en.animState = AnimationState.IDLE
                    }
                }
                continue
            }

            en.animTime += dt
            if (en.attackCooldown > 0f) en.attackCooldown = max(0f, en.attackCooldown - dt)
            if (en.stateTimer > 0f) en.stateTimer = max(0f, en.stateTimer - dt)

            // Decay knockback velocity
            en.position.x += en.velocity.x * dt
            en.position.z += en.velocity.z * dt
            en.velocity.x *= (1f - min(1f, dt * 7f))
            en.velocity.z *= (1f - min(1f, dt * 7f))
            en.position.y = TerrainMath.getHeight(en.position.x, en.position.z)

            if (en.type.isBoss && distToPlayer < en.type.aggroRange) {
                currentBossBar = en
                if (!en.bossIntroDone) {
                    en.bossIntroDone = true
                    audioEngine.playSfx(SfxType.BOSS_INTRO)
                    pushLootNotification("BOSS ENCOUNTER", en.type.displayName, true)
                }
            }

            if (en.aiState == EnemyAiState.STAGGER) {
                en.isTelegraphing = false
                if (en.stateTimer <= 0f) en.aiState = EnemyAiState.CHASE
                continue
            }

            val distFromSpawn = en.position.horizontalDistanceTo(en.spawnPos)
            val dx = stats.position.x - en.position.x
            val dz = stats.position.z - en.position.z

            when (en.aiState) {
                EnemyAiState.IDLE, EnemyAiState.PATROL -> {
                    en.isBlocking = false
                    en.patrolAngle += dt * 0.55f
                    val targetX = en.spawnPos.x + cos(en.patrolAngle) * 5.5f
                    val targetZ = en.spawnPos.z + sin(en.patrolAngle) * 5.5f
                    val pdx = targetX - en.position.x
                    val pdz = targetZ - en.position.z
                    if (pdx * pdx + pdz * pdz > 0.2f) {
                        en.yaw = Math3D.yawFromVector(pdx, pdz)
                        en.position.x += sin(en.yaw) * (en.type.moveSpeed * 0.42f) * dt
                        en.position.z += cos(en.yaw) * (en.type.moveSpeed * 0.42f) * dt
                        en.animState = AnimationState.WALKING
                    } else {
                        en.animState = AnimationState.IDLE
                    }
                    if (distToPlayer < en.type.aggroRange) {
                        en.aiState = EnemyAiState.DETECTION
                        en.stateTimer = 0.35f
                    }
                }
                EnemyAiState.DETECTION -> {
                    en.yaw = Math3D.yawFromVector(dx, dz)
                    en.animState = AnimationState.IDLE
                    if (en.stateTimer <= 0f) en.aiState = EnemyAiState.CHASE
                }
                EnemyAiState.CHASE -> {
                    if (distFromSpawn > 45f && !en.type.isBoss) {
                        en.aiState = EnemyAiState.RETURN
                        continue
                    }
                    en.yaw = Math3D.yawFromVector(dx, dz)

                    // Goblin flee when low HP
                    if (en.type == EnemyType.FOREST_GOBLIN && en.hp < en.maxHp * 0.22f && distToPlayer < 14f) {
                        en.yaw = Math3D.yawFromVector(-dx, -dz)
                        en.position.x += sin(en.yaw) * en.type.moveSpeed * dt
                        en.position.z += cos(en.yaw) * en.type.moveSpeed * dt
                        en.animState = AnimationState.RUNNING
                        continue
                    }

                    // Dark Knight occasional block
                    if (en.type == EnemyType.DARK_KNIGHT && distToPlayer < 5f && rng.nextFloat() < 0.015f) {
                        en.aiState = EnemyAiState.DEFENSE
                        en.stateTimer = 1.1f
                        en.isBlocking = true
                        en.animState = AnimationState.BLOCK
                        continue
                    }

                    // Shadow Archer maintains optimal standoff distance
                    if (en.type == EnemyType.SHADOW_ARCHER && distToPlayer < 7.5f) {
                        en.position.x -= sin(en.yaw) * en.type.moveSpeed * 0.8f * dt
                        en.position.z -= cos(en.yaw) * en.type.moveSpeed * 0.8f * dt
                        en.animState = AnimationState.WALKING
                    } else if (distToPlayer > en.type.attackRange * 0.85f) {
                        val mult = if (en.phase == 2) 1.25f else 1.0f
                        en.position.x += sin(en.yaw) * en.type.moveSpeed * mult * dt
                        en.position.z += cos(en.yaw) * en.type.moveSpeed * mult * dt
                        en.animState = AnimationState.RUNNING
                    }

                    if (distToPlayer <= en.type.attackRange && en.attackCooldown <= 0f) {
                        en.aiState = EnemyAiState.ATTACK
                        en.isTelegraphing = true
                        en.telegraphProgress = 0f
                        en.stateTimer = if (en.type.isBoss) 0.75f else 0.45f
                        en.animState = AnimationState.LIGHT_ATTACK
                    }
                }
                EnemyAiState.DEFENSE -> {
                    en.yaw = Math3D.yawFromVector(dx, dz)
                    en.isBlocking = true
                    en.animState = AnimationState.BLOCK
                    if (en.stateTimer <= 0f) {
                        en.isBlocking = false
                        en.aiState = EnemyAiState.CHASE
                    }
                }
                EnemyAiState.ATTACK -> {
                    en.yaw = Math3D.yawFromVector(dx, dz)
                    val totalWindup = if (en.type.isBoss) 0.75f else 0.45f
                    en.telegraphProgress = (1f - (en.stateTimer / totalWindup)).coerceIn(0f, 1f)
                    if (en.stateTimer <= 0f) {
                        en.isTelegraphing = false
                        executeEnemyAttack(en, distToPlayer)
                        en.attackCooldown = if (en.phase == 2) 1.05f else 1.55f
                        en.aiState = EnemyAiState.CHASE
                    }
                }
                EnemyAiState.RETURN -> {
                    val rdx = en.spawnPos.x - en.position.x
                    val rdz = en.spawnPos.z - en.position.z
                    if (rdx * rdx + rdz * rdz < 2f) {
                        en.hp = en.maxHp
                        en.aiState = EnemyAiState.PATROL
                    } else {
                        en.yaw = Math3D.yawFromVector(rdx, rdz)
                        en.position.x += sin(en.yaw) * en.type.moveSpeed * dt
                        en.position.z += cos(en.yaw) * en.type.moveSpeed * dt
                        en.animState = AnimationState.WALKING
                    }
                }
                EnemyAiState.STAGGER, EnemyAiState.DEATH -> Unit
            }
        }
        activeBoss = currentBossBar
    }

    private fun executeEnemyAttack(en: EnemyInstance, distToPlayer: Float) {
        audioEngine.playSfx(SfxType.ENEMY_ATTACK)

        if (en.type == EnemyType.SHADOW_ARCHER || (en.type == EnemyType.ANCIENT_COLOSSUS && distToPlayer > 7f)) {
            val dirX = sin(en.yaw)
            val dirZ = cos(en.yaw)
            val spd = if (en.type.isBoss) 26f else 22f
            arrows.add(
                ArrowProjectile(
                    id = nextId++,
                    fromPlayer = false,
                    position = Vec3(en.position.x + dirX * 1.2f, en.position.y + 1.6f, en.position.z + dirZ * 1.2f),
                    velocity = Vec3(dirX * spd, 0.8f, dirZ * spd),
                    damage = en.type.attackDamage
                )
            )
            return
        }

        // Boss Area / Charge Attack
        val hitRange = if (en.type.isBoss) en.type.attackRange + 2.2f else en.type.attackRange + 0.6f
        if (en.type.isBoss) {
            spawnBurst(en.position, 0.9f, 0.45f, 0.15f, 22)
            cameraShake = 0.35f
        }
        if (distToPlayer <= hitRange) {
            applyDamageToPlayer(en.type.attackDamage * if (en.phase == 2) 1.25f else 1.0f, en.position, attacker = en)
        }
    }

    private fun applyDamageToPlayer(rawDamage: Float, sourcePos: Vec3, attacker: EnemyInstance? = null) {
        if (invulnTimer > 0f || stats.hp <= 0f) return

        // Perfect Parry!
        if (isBlocking && parryWindowTimer > 0f) {
            audioEngine.playSfx(SfxType.PARRY_CLANG)
            if (settings.vibrationEnabled) onVibrate(40L)
            hitStopTimer = 0.06f
            spawnBurst(Vec3(stats.position.x, stats.position.y + 1.3f, stats.position.z), 0.3f, 0.9f, 1f, 16)
            attacker?.let {
                it.aiState = EnemyAiState.STAGGER
                it.stateTimer = 1.4f
                it.animState = AnimationState.STAGGER
            }
            pushLootNotification("PERFECT PARRY!", "Enemy staggered!", false)
            return
        }

        var mitigated = (rawDamage - getDefense() * 0.45f).coerceAtLeast(3f)
        if (isBlocking && stats.stamina >= 10f) {
            stats.stamina -= 10f
            mitigated *= 0.25f
            audioEngine.playSfx(SfxType.PARRY_CLANG, 0.55f)
        } else {
            playerAnimState = AnimationState.HIT_REACTION
            audioEngine.playSfx(SfxType.HIT_IMPACT, 0.9f)
            cameraShake = 0.25f
            if (settings.vibrationEnabled) onVibrate(30L)
        }

        val dmgInt = mitigated.toInt().coerceAtLeast(1)
        stats.hp = max(0f, stats.hp - dmgInt)
        spawnDamageIndicator(stats.position, dmgInt, isCrit = false, isPlayerHit = true)

        // Small knockback
        val dx = stats.position.x - sourcePos.x
        val dz = stats.position.z - sourcePos.z
        val len = max(0.01f, sqrt(dx * dx + dz * dz))
        stats.position.x += (dx / len) * 0.6f
        stats.position.z += (dz / len) * 0.6f
    }

    private fun updateParticlesAndIndicators(dt: Float) {
        var i = particles.size - 1
        while (i >= 0) {
            val p = particles[i]
            p.life -= dt
            p.position.x += p.velocity.x * dt
            p.position.y += p.velocity.y * dt
            p.position.z += p.velocity.z * dt
            if (p.life <= 0f) particles.removeAt(i)
            i--
        }

        var d = damageIndicators.size - 1
        while (d >= 0) {
            val di = damageIndicators[d]
            di.age += dt
            di.worldPos.y += dt * 1.4f
            if (di.age > 1.15f) damageIndicators.removeAt(d)
            d--
        }

        var l = lootNotifications.size - 1
        while (l >= 0) {
            val ln = lootNotifications[l]
            ln.age += dt
            if (ln.age > 3.6f) lootNotifications.removeAt(l)
            l--
        }
    }

    private fun updateNearestInteractablePrompt() {
        if (stats.inBoat) {
            nearestInteractPrompt = "Disembark Boat"
            nearestInteractTargetId = "boat_silverlake"
            return
        }
        val nearNpc = npcs.minByOrNull { it.currentPos.horizontalDistanceTo(stats.position) }
        if (nearNpc != null && nearNpc.currentPos.horizontalDistanceTo(stats.position) < 4.5f) {
            nearestInteractPrompt = "Talk: ${nearNpc.name}"
            nearestInteractTargetId = nearNpc.id
            return
        }
        val nearObj = interactables
            .filter { !(it.activated && (it.type == InteractableType.TREASURE_CHEST || it.type == InteractableType.COLLECTIBLE_ITEM)) }
            .minByOrNull { it.position.horizontalDistanceTo(stats.position) }
        if (nearObj != null && nearObj.position.horizontalDistanceTo(stats.position) < 5.0f) {
            nearestInteractPrompt = "${nearObj.subtitle}: ${nearObj.title}"
            nearestInteractTargetId = nearObj.id
        } else {
            nearestInteractPrompt = null
            nearestInteractTargetId = null
        }
    }

    private fun updateRegionAndMusic() {
        val region = TerrainMath.getRegionAt(stats.position.x, stats.position.z)
        lastRegion = region
        val theme = when {
            activeBoss != null -> MusicTheme.BOSS
            enemies.any { it.isAlive && (it.aiState == EnemyAiState.CHASE || it.aiState == EnemyAiState.ATTACK) } -> MusicTheme.COMBAT
            region == RegionId.DAWNREACH_VILLAGE -> MusicTheme.VILLAGE
            else -> MusicTheme.EXPLORATION
        }
        audioEngine.updateEnvironment(
            theme = theme,
            region = region,
            raining = currentWeather == WeatherType.RAIN && settings.weatherEffectsEnabled,
            dungeon = stats.inDungeon
        )
    }

    private fun addItemToInventory(itemId: String, qty: Int) {
        val existing = inventory.find { it.itemId == itemId }
        if (existing != null) existing.quantity += qty else inventory.add(InventoryEntry(itemId, qty))
    }

    private fun grantExp(amount: Int) {
        stats.exp += amount
        while (stats.exp >= stats.expToNext) {
            stats.exp -= stats.expToNext
            stats.level++
            stats.skillPoints++
            stats.expToNext = (stats.expToNext * 1.35f).toInt()
            stats.baseMaxHp += 22f
            stats.baseMaxStamina += 10f
            stats.hp = getMaxHp()
            stats.stamina = getMaxStamina()
            audioEngine.playSfx(SfxType.LEVEL_UP)
            spawnBurst(stats.position, 1f, 0.85f, 0.3f, 34)
            pushLootNotification("LEVEL UP! Level ${stats.level}", "+1 Skill Point & Full Restore", true)
        }
    }

    private fun advanceQuestTag(tag: String) {
        for (q in questList) {
            if (questStatuses[q.id] != QuestStatus.ACTIVE) continue
            var updated = false
            for (obj in q.objectives) {
                if (obj.targetTag == tag && !obj.isComplete) {
                    obj.currentCount++
                    updated = true
                }
            }
            if (updated && q.objectives.all { it.isComplete }) {
                questStatuses[q.id] = QuestStatus.COMPLETED
                grantExp(q.rewardExp)
                stats.gold += q.rewardGold
                q.rewardItemId?.let { addItemToInventory(it, 1) }
                audioEngine.playSfx(SfxType.QUEST_COMPLETE)
                pushLootNotification("Quest Complete: ${q.title}", "+${q.rewardExp} EXP • +${q.rewardGold}G", true)
                // Auto-activate next main quest if eligible
                questList.firstOrNull { it.prerequisiteQuestId == q.id && questStatuses[it.id] == QuestStatus.AVAILABLE }
                    ?.let { nextQ ->
                        questStatuses[nextQ.id] = QuestStatus.ACTIVE
                        pushLootNotification("New Main Quest", nextQ.title, true)
                    }
            }
        }
    }

    private fun spawnBurst(pos: Vec3, r: Float, g: Float, b: Float, count: Int) {
        val maxP = settings.graphicsQuality.maxParticles
        for (i in 0 until count) {
            if (particles.size >= maxP) break
            val angle = rng.nextFloat() * 6.283f
            val spd = 2f + rng.nextFloat() * 6f
            particles.add(
                ParticleVfx(
                    position = Vec3(pos.x, pos.y + 0.6f, pos.z),
                    velocity = Vec3(cos(angle) * spd, 2.5f + rng.nextFloat() * 5f, sin(angle) * spd),
                    r = r, g = g, b = b,
                    size = 0.22f + rng.nextFloat() * 0.16f,
                    life = 0.45f + rng.nextFloat() * 0.35f,
                    maxLife = 0.8f
                )
            )
        }
    }

    private fun spawnDust(pos: Vec3, count: Int) = spawnBurst(pos, 0.72f, 0.66f, 0.56f, count)

    private fun spawnDamageIndicator(pos: Vec3, amount: Int, isCrit: Boolean, isHeal: Boolean = false, isPlayerHit: Boolean = false) {
        damageIndicators.add(
            DamageIndicator(
                id = nextId++,
                worldPos = Vec3(pos.x + (rng.nextFloat() - 0.5f) * 0.8f, pos.y + 2.1f, pos.z),
                amount = amount,
                isCrit = isCrit,
                isHeal = isHeal,
                isPlayerHit = isPlayerHit
            )
        )
    }

    private fun pushLootNotification(title: String, detail: String, isQuest: Boolean) {
        lootNotifications.add(LootNotification(nextId++, title, detail, isQuest))
        if (lootNotifications.size > 4) lootNotifications.removeAt(0)
    }
}
