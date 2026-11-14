package com.example.game.model

import kotlin.math.sqrt

data class Vec3(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
) {
    fun distanceTo(other: Vec3): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun horizontalDistanceTo(other: Vec3): Float {
        val dx = x - other.x
        val dz = z - other.z
        return sqrt(dx * dx + dz * dz)
    }

    fun copyFrom(other: Vec3) {
        x = other.x
        y = other.y
        z = other.z
    }
}

data class ItemDef(
    val id: String,
    val name: String,
    val description: String,
    val category: ItemCategory,
    val equipSlot: EquipSlot? = null,
    val weaponType: WeaponType? = null,
    val attackBonus: Float = 0f,
    val defenseBonus: Float = 0f,
    val healthBonus: Float = 0f,
    val staminaBonus: Float = 0f,
    val critBonus: Float = 0f,
    val healAmount: Float = 0f,
    val staminaRestore: Float = 0f,
    val priceGold: Int = 25,
    val colorHex: Long = 0xFF38BDF8
)

data class InventoryEntry(
    val itemId: String,
    var quantity: Int,
    var upgradeLevel: Int = 0
)

data class EquipmentLoadout(
    var weaponId: String = "wpn_iron_sword",
    var helmetId: String? = null,
    var chestId: String? = "arm_traveler_tunic",
    var glovesId: String? = null,
    var bootsId: String? = "arm_leather_boots",
    var accessoryId: String? = null
)

data class SkillDef(
    val id: String,
    val name: String,
    val branch: String,
    val description: String,
    val maxRank: Int = 3,
    val bonusPerRankLabel: String
)

data class QuestObjective(
    val id: String,
    val description: String,
    val targetTag: String,
    val requiredCount: Int,
    var currentCount: Int = 0
) {
    val isComplete: Boolean get() = currentCount >= requiredCount
}

data class QuestDef(
    val id: String,
    val title: String,
    val description: String,
    val type: QuestType,
    val giverNpcId: String,
    val prerequisiteQuestId: String? = null,
    val objectives: List<QuestObjective>,
    val rewardExp: Int,
    val rewardGold: Int,
    val rewardItemId: String? = null
)

data class DialogueChoice(
    val label: String,
    val nextNodeId: String? = null,
    val acceptQuestId: String? = null,
    val completeQuestId: String? = null,
    val openShop: Boolean = false,
    val openUpgrade: Boolean = false
)

data class DialogueNode(
    val id: String,
    val speakerName: String,
    val text: String,
    val choices: List<DialogueChoice>
)

data class NpcDef(
    val id: String,
    val name: String,
    val role: NpcRole,
    val region: RegionId,
    val homePos: Vec3,
    var currentPos: Vec3 = homePos.copy(),
    var yaw: Float = 0f,
    val patrolRadius: Float = 6f,
    val shopItemIds: List<String> = emptyList(),
    var animTime: Float = 0f,
    var isTalking: Boolean = false
)

data class EnemyInstance(
    val id: String,
    val type: EnemyType,
    val region: RegionId,
    val spawnPos: Vec3,
    val position: Vec3 = spawnPos.copy(),
    var velocity: Vec3 = Vec3(),
    var yaw: Float = 0f,
    var hp: Float = type.maxHp,
    var maxHp: Float = type.maxHp,
    var aiState: EnemyAiState = EnemyAiState.PATROL,
    var stateTimer: Float = 0f,
    var attackCooldown: Float = 0f,
    var patrolAngle: Float = 0f,
    var animState: AnimationState = AnimationState.IDLE,
    var animTime: Float = 0f,
    var phase: Int = 1,
    var isBlocking: Boolean = false,
    var isTelegraphing: Boolean = false,
    var telegraphProgress: Float = 0f,
    var bossIntroDone: Boolean = false,
    var dropTag: String = "",
    var respawnTimer: Float = 0f
) {
    val isAlive: Boolean get() = hp > 0f && aiState != EnemyAiState.DEATH
}

enum class InteractableType {
    TREASURE_CHEST,
    COLLECTIBLE_ITEM,
    WAYPOINT_SHRINE,
    DUNGEON_PORTAL,
    BOAT_DOCK,
    LORE_TABLET,
    FRACTURE_ALTAR,
    BEACON_BRAZIER
}

data class WorldInteractable(
    val id: String,
    val type: InteractableType,
    val title: String,
    val subtitle: String,
    val region: RegionId,
    val position: Vec3,
    var activated: Boolean = false,
    val rewardItemId: String? = null,
    val rewardGold: Int = 0,
    val questTag: String = ""
)

data class ArrowProjectile(
    val id: Long,
    val fromPlayer: Boolean,
    val position: Vec3,
    val velocity: Vec3,
    val damage: Float,
    var lifeTime: Float = 3.0f
)

data class ParticleVfx(
    val position: Vec3,
    val velocity: Vec3,
    val r: Float,
    val g: Float,
    val b: Float,
    val size: Float,
    var life: Float,
    val maxLife: Float
)

data class DamageIndicator(
    val id: Long,
    val worldPos: Vec3,
    val amount: Int,
    val isCrit: Boolean,
    val isHeal: Boolean = false,
    val isPlayerHit: Boolean = false,
    var age: Float = 0f
)

data class LootNotification(
    val id: Long,
    val title: String,
    val detail: String,
    val isQuest: Boolean = false,
    var age: Float = 0f
)

data class GameSettings(
    val graphicsQuality: GraphicsQuality = GraphicsQuality.MEDIUM,
    val targetFps: Int = 30,
    val masterVolume: Float = 0.85f,
    val musicVolume: Float = 0.70f,
    val sfxVolume: Float = 0.90f,
    val ambientVolume: Float = 0.75f,
    val uiScale: Float = 1.0f,
    val touchSensitivity: Float = 1.0f,
    val cameraSensitivity: Float = 1.0f,
    val vibrationEnabled: Boolean = true,
    val compactButtonLayout: Boolean = false,
    val weatherEffectsEnabled: Boolean = true
)

data class SaveSlotSummary(
    val slotIndex: Int,
    val exists: Boolean,
    val playerLevel: Int = 1,
    val regionName: String = "",
    val playTimeSeconds: Int = 0,
    val completedQuests: Int = 0,
    val savedTimestamp: Long = 0L
)

data class PlayerStatsState(
    var level: Int = 1,
    var exp: Int = 0,
    var expToNext: Int = 120,
    var skillPoints: Int = 1,
    var gold: Int = 80,
    var baseMaxHp: Float = 160f,
    var baseMaxStamina: Float = 110f,
    var hp: Float = 160f,
    var stamina: Float = 110f,
    var position: Vec3 = Vec3(0f, 4f, 6f),
    var yaw: Float = 0f,
    var inBoat: Boolean = false,
    var inDungeon: Boolean = false
)
