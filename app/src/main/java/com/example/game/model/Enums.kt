package com.example.game.model

enum class RegionId(val displayName: String, val subtitle: String) {
    DAWNREACH_VILLAGE("Dawnreach Village", "Bastion of the Frontier"),
    GREENVEIL_FOREST("Greenveil Forest", "Whispering Canopy & Ancient Ruins"),
    ASHEN_HIGHLANDS("Ashen Highlands", "Volcanic Peaks & Ember Crags"),
    SILVERLAKE_VALLEY("Silverlake Valley", "Shimmering Waters & Fisher Docks"),
    FORGOTTEN_KINGDOM("Forgotten Kingdom", "Shattered Citadel of the Ancients")
}

enum class WeaponType(
    val displayName: String,
    val baseDamage: Float,
    val attackSpeed: Float,
    val range: Float,
    val critChance: Float,
    val staminaCost: Float
) {
    SWORD("Sword", 24f, 1.15f, 3.0f, 0.12f, 12f),
    GREAT_SWORD("Great Sword", 42f, 0.72f, 3.8f, 0.18f, 22f),
    DUAL_BLADES("Dual Blades", 17f, 1.65f, 2.5f, 0.25f, 9f),
    BOW("Bow", 26f, 1.0f, 28.0f, 0.20f, 14f)
}

enum class EquipSlot(val displayName: String) {
    WEAPON("Weapon"),
    HELMET("Helmet"),
    CHEST("Chest Armor"),
    GLOVES("Gloves"),
    BOOTS("Boots"),
    ACCESSORY("Accessory")
}

enum class ItemCategory(val displayName: String) {
    WEAPONS("Weapons"),
    ARMOR("Armor"),
    CONSUMABLES("Consumables"),
    MATERIALS("Materials"),
    QUEST_ITEMS("Quest Items"),
    COLLECTIBLES("Collectibles")
}

enum class QuestType(val label: String) {
    MAIN("Main Quest"),
    SIDE("Side Quest"),
    EXPLORATION("Exploration")
}

enum class QuestStatus {
    AVAILABLE,
    ACTIVE,
    COMPLETED
}

enum class TimeOfDay(val label: String) {
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    NIGHT("Night")
}

enum class WeatherType(val label: String) {
    CLEAR("Clear"),
    CLOUDY("Cloudy"),
    RAIN("Rain"),
    FOG("Fog")
}

enum class GraphicsQuality(
    val label: String,
    val resolutionScale: Float,
    val vegetationDensity: Float,
    val drawDistance: Float,
    val maxParticles: Int,
    val enableWeatherParticles: Boolean,
    val enableShadows: Boolean
) {
    LOW("Low", 0.65f, 0.40f, 95f, 90, false, false),
    MEDIUM("Medium", 0.85f, 0.72f, 145f, 200, true, true),
    HIGH("High", 1.0f, 1.0f, 210f, 380, true, true)
}

enum class EnemyType(
    val displayName: String,
    val maxHp: Float,
    val attackDamage: Float,
    val moveSpeed: Float,
    val attackRange: Float,
    val aggroRange: Float,
    val expReward: Int,
    val goldReward: Int,
    val isBoss: Boolean = false
) {
    FOREST_GOBLIN("Forest Goblin", 85f, 12f, 5.0f, 2.4f, 18f, 35, 18),
    WILD_BEAST("Wild Beast", 110f, 16f, 6.4f, 2.5f, 20f, 45, 14),
    DARK_KNIGHT("Dark Knight", 210f, 24f, 4.4f, 3.0f, 22f, 95, 45),
    SHADOW_ARCHER("Shadow Archer", 95f, 18f, 4.6f, 18.0f, 25f, 55, 24),
    ANCIENT_GUARDIAN("Ancient Guardian", 280f, 32f, 3.6f, 3.5f, 22f, 135, 65),
    FOREST_TYRANT("The Forest Tyrant", 920f, 38f, 5.2f, 4.6f, 32f, 550, 320, isBoss = true),
    ANCIENT_COLOSSUS("The Ancient Colossus", 1350f, 46f, 4.3f, 5.4f, 36f, 850, 500, isBoss = true)
}

enum class EnemyAiState {
    IDLE,
    PATROL,
    DETECTION,
    CHASE,
    ATTACK,
    DEFENSE,
    STAGGER,
    RETURN,
    DEATH
}

enum class NpcRole(val title: String) {
    VILLAGE_LEADER("Village Leader"),
    BLACKSMITH("Blacksmith"),
    MERCHANT("Merchant"),
    QUEST_GIVER("Quest Giver"),
    VILLAGER("Villager"),
    GUARD("Guard"),
    TRAVELER("Traveler")
}

enum class AnimationState {
    IDLE,
    WALKING,
    RUNNING,
    SPRINTING,
    JUMPING,
    FALLING,
    LANDING,
    DODGING,
    ROLLING,
    LIGHT_ATTACK,
    HEAVY_ATTACK,
    COMBO_ATTACK,
    BLOCK,
    PARRY,
    HIT_REACTION,
    STAGGER,
    DEATH,
    CLIMBING,
    SWIMMING,
    INTERACTING,
    COLLECTING_ITEM
}

enum class ScreenMode {
    MAIN_MENU,
    LOADING,
    PLAYING,
    PAUSED,
    INVENTORY,
    EQUIPMENT,
    SKILLS,
    QUESTS,
    WORLD_MAP,
    SETTINGS,
    DIALOGUE,
    SHOP,
    CREDITS,
    GAME_OVER,
    VICTORY
}
