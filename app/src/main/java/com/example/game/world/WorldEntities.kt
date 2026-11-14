package com.example.game.world

import com.example.game.math.TerrainMath
import com.example.game.model.EnemyInstance
import com.example.game.model.EnemyType
import com.example.game.model.InteractableType
import com.example.game.model.RegionId
import com.example.game.model.Vec3
import com.example.game.model.WorldInteractable

object WorldEntities {

    fun createInitialInteractables(): MutableList<WorldInteractable> {
        fun ground(x: Float, z: Float, offset: Float = 0f): Vec3 =
            Vec3(x, TerrainMath.getHeight(x, z) + offset, z)

        return mutableListOf(
            // 5 Fast-Travel Waypoint Shrines (1 per region)
            WorldInteractable(
                id = "wp_dawnreach",
                type = InteractableType.WAYPOINT_SHRINE,
                title = "Dawnreach Plaza Shrine",
                subtitle = "Fast Travel Waypoint",
                region = RegionId.DAWNREACH_VILLAGE,
                position = ground(0f, 0f),
                activated = true
            ),
            WorldInteractable(
                id = "wp_greenveil",
                type = InteractableType.WAYPOINT_SHRINE,
                title = "Greenveil Canopy Shrine",
                subtitle = "Attune Waypoint",
                region = RegionId.GREENVEIL_FOREST,
                position = ground(-260f, -60f),
                questTag = "waypoint_greenveil"
            ),
            WorldInteractable(
                id = "wp_silverlake",
                type = InteractableType.WAYPOINT_SHRINE,
                title = "Silverlake Dock Shrine",
                subtitle = "Attune Waypoint",
                region = RegionId.SILVERLAKE_VALLEY,
                position = ground(215f, 295f),
                questTag = "waypoint_silverlake"
            ),
            WorldInteractable(
                id = "wp_ashen",
                type = InteractableType.WAYPOINT_SHRINE,
                title = "Ashen Ridge Shrine",
                subtitle = "Attune Waypoint",
                region = RegionId.ASHEN_HIGHLANDS,
                position = ground(310f, -310f),
                questTag = "waypoint_ashen"
            ),
            WorldInteractable(
                id = "wp_forgotten",
                type = InteractableType.WAYPOINT_SHRINE,
                title = "Citadel Gate Shrine",
                subtitle = "Attune Waypoint",
                region = RegionId.FORGOTTEN_KINGDOM,
                position = ground(0f, -520f),
                questTag = "waypoint_forgotten"
            ),

            // Treasure Chests
            WorldInteractable(
                id = "chest_dawnreach_supply",
                type = InteractableType.TREASURE_CHEST,
                title = "Militia Supply Chest",
                subtitle = "Open Chest",
                region = RegionId.DAWNREACH_VILLAGE,
                position = ground(24f, -18f),
                rewardItemId = "con_health_potion",
                rewardGold = 45,
                questTag = "open_any_chest"
            ),
            WorldInteractable(
                id = "chest_greenveil_ruins",
                type = InteractableType.TREASURE_CHEST,
                title = "Mossy Ruin Chest",
                subtitle = "Open Chest",
                region = RegionId.GREENVEIL_FOREST,
                position = ground(-360f, -80f),
                rewardItemId = "col_ancient_relic",
                rewardGold = 95,
                questTag = "open_any_chest"
            ),
            WorldInteractable(
                id = "chest_silverlake_island",
                type = InteractableType.TREASURE_CHEST,
                title = "Sunken Crown Chest",
                subtitle = "Open Chest",
                region = RegionId.SILVERLAKE_VALLEY,
                position = ground(460f, 460f),
                rewardItemId = "arm_wind_boots",
                rewardGold = 150,
                questTag = "chest_silverlake_island"
            ),
            WorldInteractable(
                id = "chest_ashen_cave",
                type = InteractableType.TREASURE_CHEST,
                title = "Obsidian Cavern Chest",
                subtitle = "Open Chest",
                region = RegionId.ASHEN_HIGHLANDS,
                position = ground(415f, -430f),
                rewardItemId = "mat_aether_shard",
                rewardGold = 120,
                questTag = "open_any_chest"
            ),
            WorldInteractable(
                id = "chest_dungeon_vault",
                type = InteractableType.TREASURE_CHEST,
                title = "Royal Vault Reliquary",
                subtitle = "Open Chest",
                region = RegionId.FORGOTTEN_KINGDOM,
                position = ground(-95f, -610f),
                rewardItemId = "col_ancient_relic",
                rewardGold = 240,
                questTag = "open_any_chest"
            ),

            // Collectible Materials (Bark & Volcanic Ore)
            WorldInteractable(
                id = "col_bark_1",
                type = InteractableType.COLLECTIBLE_ITEM,
                title = "Ancient Heartwood Bark",
                subtitle = "Gather Material",
                region = RegionId.GREENVEIL_FOREST,
                position = ground(-210f, 40f),
                rewardItemId = "mat_ancient_bark",
                questTag = "collect_bark"
            ),
            WorldInteractable(
                id = "col_bark_2",
                type = InteractableType.COLLECTIBLE_ITEM,
                title = "Ancient Heartwood Bark",
                subtitle = "Gather Material",
                region = RegionId.GREENVEIL_FOREST,
                position = ground(-330f, -150f),
                rewardItemId = "mat_ancient_bark",
                questTag = "collect_bark"
            ),
            WorldInteractable(
                id = "col_ore_1",
                type = InteractableType.COLLECTIBLE_ITEM,
                title = "Ashen Igneous Ore",
                subtitle = "Mine Vein",
                region = RegionId.ASHEN_HIGHLANDS,
                position = ground(340f, -260f),
                rewardItemId = "mat_ashen_ore",
                questTag = "collect_ore"
            ),
            WorldInteractable(
                id = "col_ore_2",
                type = InteractableType.COLLECTIBLE_ITEM,
                title = "Ashen Igneous Ore",
                subtitle = "Mine Vein",
                region = RegionId.ASHEN_HIGHLANDS,
                position = ground(445f, -380f),
                rewardItemId = "mat_ashen_ore",
                questTag = "collect_ore"
            ),

            // Silverlake Rowboat at Dock
            WorldInteractable(
                id = "boat_silverlake",
                type = InteractableType.BOAT_DOCK,
                title = "Silverlake Wooden Boat",
                subtitle = "Board / Exit Boat",
                region = RegionId.SILVERLAKE_VALLEY,
                position = Vec3(246f, TerrainMath.WATER_LEVEL + 0.3f, 336f)
            ),

            // Forgotten Kingdom Dungeon Portal
            WorldInteractable(
                id = "portal_dungeon",
                type = InteractableType.DUNGEON_PORTAL,
                title = "Catacomb Dungeon Gate",
                subtitle = "Enter / Exit Vault",
                region = RegionId.FORGOTTEN_KINGDOM,
                position = ground(-92f, -578f),
                questTag = "enter_dungeon"
            ),

            // Lore Tablets
            WorldInteractable(
                id = "lore_greenveil",
                type = InteractableType.LORE_TABLET,
                title = "Tablet of the First Canopy",
                subtitle = "Read Inscription",
                region = RegionId.GREENVEIL_FOREST,
                position = ground(-345f, -72f),
                questTag = "read_lore"
            ),
            WorldInteractable(
                id = "lore_citadel",
                type = InteractableType.LORE_TABLET,
                title = "Chronicle of The Fracture",
                subtitle = "Read Inscription",
                region = RegionId.FORGOTTEN_KINGDOM,
                position = ground(24f, -560f),
                questTag = "read_lore"
            ),

            // Main Quest Story Mechanisms
            WorldInteractable(
                id = "ashen_beacon",
                type = InteractableType.BEACON_BRAZIER,
                title = "Ashen Ridge Signal Brazier",
                subtitle = "Ignite Beacon",
                region = RegionId.ASHEN_HIGHLANDS,
                position = ground(480f, -475f),
                questTag = "activate_beacon"
            ),
            WorldInteractable(
                id = "fracture_altar",
                type = InteractableType.FRACTURE_ALTAR,
                title = "The Fracture Core Altar",
                subtitle = "Seal The Fracture",
                region = RegionId.FORGOTTEN_KINGDOM,
                position = ground(0f, -805f),
                questTag = "activate_fracture_altar"
            )
        )
    }

    fun createInitialEnemies(): MutableList<EnemyInstance> {
        fun spawn(
            id: String,
            type: EnemyType,
            region: RegionId,
            x: Float,
            z: Float,
            dropTag: String
        ): EnemyInstance {
            val y = TerrainMath.getHeight(x, z)
            return EnemyInstance(
                id = id,
                type = type,
                region = region,
                spawnPos = Vec3(x, y, z),
                dropTag = dropTag
            )
        }

        return mutableListOf(
            // Goblins near Dawnreach western gate & Greenveil approach
            spawn("en_gob_1", EnemyType.FOREST_GOBLIN, RegionId.GREENVEIL_FOREST, -125f, -15f, "kill_goblin"),
            spawn("en_gob_2", EnemyType.FOREST_GOBLIN, RegionId.GREENVEIL_FOREST, -152f, 22f, "kill_goblin"),
            spawn("en_gob_3", EnemyType.FOREST_GOBLIN, RegionId.GREENVEIL_FOREST, -178f, -42f, "kill_goblin"),
            spawn("en_gob_4", EnemyType.FOREST_GOBLIN, RegionId.GREENVEIL_FOREST, -280f, -110f, "kill_goblin"),

            // Wild Beasts
            spawn("en_bst_1", EnemyType.WILD_BEAST, RegionId.GREENVEIL_FOREST, -195f, 75f, "kill_beast"),
            spawn("en_bst_2", EnemyType.WILD_BEAST, RegionId.GREENVEIL_FOREST, -255f, 110f, "kill_beast"),
            spawn("en_bst_3", EnemyType.WILD_BEAST, RegionId.SILVERLAKE_VALLEY, 175f, 210f, "kill_beast"),
            spawn("en_bst_4", EnemyType.WILD_BEAST, RegionId.ASHEN_HIGHLANDS, 240f, -210f, "kill_beast"),

            // Shadow Archers
            spawn("en_arc_1", EnemyType.SHADOW_ARCHER, RegionId.GREENVEIL_FOREST, -340f, -120f, "kill_archer"),
            spawn("en_arc_2", EnemyType.SHADOW_ARCHER, RegionId.GREENVEIL_FOREST, -410f, -220f, "kill_archer"),
            spawn("en_arc_3", EnemyType.SHADOW_ARCHER, RegionId.FORGOTTEN_KINGDOM, -45f, -430f, "kill_archer"),

            // Dark Knights
            spawn("en_dkn_1", EnemyType.DARK_KNIGHT, RegionId.ASHEN_HIGHLANDS, 385f, -360f, "kill_dark_knight"),
            spawn("en_dkn_2", EnemyType.DARK_KNIGHT, RegionId.FORGOTTEN_KINGDOM, -22f, -555f, "kill_dark_knight"),
            spawn("en_dkn_3", EnemyType.DARK_KNIGHT, RegionId.FORGOTTEN_KINGDOM, 26f, -590f, "kill_dark_knight"),

            // Ancient Guardians
            spawn("en_grd_1", EnemyType.ANCIENT_GUARDIAN, RegionId.ASHEN_HIGHLANDS, 455f, -460f, "kill_guardian"),
            spawn("en_grd_2", EnemyType.ANCIENT_GUARDIAN, RegionId.FORGOTTEN_KINGDOM, 0f, -465f, "kill_guardian"),
            spawn("en_grd_3", EnemyType.ANCIENT_GUARDIAN, RegionId.FORGOTTEN_KINGDOM, -88f, -600f, "kill_guardian"),

            // BOSS 1: The Forest Tyrant (Greenveil Emerald Glade)
            spawn("boss_forest_tyrant", EnemyType.FOREST_TYRANT, RegionId.GREENVEIL_FOREST, -520f, -290f, "kill_forest_tyrant"),

            // BOSS 2: The Ancient Colossus (Forgotten Kingdom Citadel Arena)
            spawn("boss_ancient_colossus", EnemyType.ANCIENT_COLOSSUS, RegionId.FORGOTTEN_KINGDOM, 0f, -715f, "kill_ancient_colossus")
        )
    }
}
