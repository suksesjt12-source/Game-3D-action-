package com.example.game.model

object GameCatalog {

    val items: Map<String, ItemDef> = listOf(
        // Weapons (all 4 types represented with multiple tiers)
        ItemDef(
            id = "wpn_iron_sword",
            name = "Dawnreach Iron Sword",
            description = "Balanced frontier blade forged for swift strikes and clean parries.",
            category = ItemCategory.WEAPONS,
            equipSlot = EquipSlot.WEAPON,
            weaponType = WeaponType.SWORD,
            attackBonus = 24f,
            critBonus = 0.12f,
            priceGold = 60,
            colorHex = 0xFFCBD5E1
        ),
        ItemDef(
            id = "wpn_aether_sword",
            name = "Aether-Tempered Longsword",
            description = "Runic steel infused with Fracture crystal dust. Glows against corrupted foes.",
            category = ItemCategory.WEAPONS,
            equipSlot = EquipSlot.WEAPON,
            weaponType = WeaponType.SWORD,
            attackBonus = 40f,
            critBonus = 0.18f,
            priceGold = 220,
            colorHex = 0xFF38BDF8
        ),
        ItemDef(
            id = "wpn_titan_greatsword",
            name = "Titan Greatsword",
            description = "Massive two-handed claymore that shatters enemy guards and staggers elites.",
            category = ItemCategory.WEAPONS,
            equipSlot = EquipSlot.WEAPON,
            weaponType = WeaponType.GREAT_SWORD,
            attackBonus = 46f,
            critBonus = 0.16f,
            priceGold = 150,
            colorHex = 0xFFF59E0B
        ),
        ItemDef(
            id = "wpn_colossus_cleaver",
            name = "Colossus Fracture Greatsword",
            description = "Ancient royal greatsword recovered from the shattered citadel.",
            category = ItemCategory.WEAPONS,
            equipSlot = EquipSlot.WEAPON,
            weaponType = WeaponType.GREAT_SWORD,
            attackBonus = 66f,
            critBonus = 0.22f,
            priceGold = 380,
            colorHex = 0xFFA855F7
        ),
        ItemDef(
            id = "wpn_twin_fangs",
            name = "Twin Fang Dual Blades",
            description = "Paired hunter daggers built for relentless four-hit flurries and critical strikes.",
            category = ItemCategory.WEAPONS,
            equipSlot = EquipSlot.WEAPON,
            weaponType = WeaponType.DUAL_BLADES,
            attackBonus = 22f,
            critBonus = 0.28f,
            priceGold = 135,
            colorHex = 0xFF10B981
        ),
        ItemDef(
            id = "wpn_windstrider_bow",
            name = "Windstrider Recurve Bow",
            description = "Flexible ashwood bow capable of felling Shadow Archers from afar.",
            category = ItemCategory.WEAPONS,
            equipSlot = EquipSlot.WEAPON,
            weaponType = WeaponType.BOW,
            attackBonus = 30f,
            critBonus = 0.22f,
            priceGold = 130,
            colorHex = 0xFFD97706
        ),

        // Armor - Helmet, Chest, Gloves, Boots, Accessory
        ItemDef(
            id = "arm_scout_hood",
            name = "Greenveil Scout Helm",
            description = "Reinforced leather hood and visor worn by frontier rangers.",
            category = ItemCategory.ARMOR,
            equipSlot = EquipSlot.HELMET,
            defenseBonus = 5f,
            healthBonus = 20f,
            priceGold = 70,
            colorHex = 0xFF166534
        ),
        ItemDef(
            id = "arm_knight_helm",
            name = "Citadel Steel Helm",
            description = "Visored greathelm salvaged from Dawnreach's royal armory.",
            category = ItemCategory.ARMOR,
            equipSlot = EquipSlot.HELMET,
            defenseBonus = 11f,
            healthBonus = 45f,
            priceGold = 180,
            colorHex = 0xFF94A3B8
        ),
        ItemDef(
            id = "arm_traveler_tunic",
            name = "Wanderer's Brigandine",
            description = "Stitched leather chestguard layered over rugged frontier cloth.",
            category = ItemCategory.ARMOR,
            equipSlot = EquipSlot.CHEST,
            defenseBonus = 7f,
            healthBonus = 25f,
            priceGold = 50,
            colorHex = 0xFF1D4ED8
        ),
        ItemDef(
            id = "arm_aether_plate",
            name = "Aether-Forged Breastplate",
            description = "Gilded obsidian cuirass humming with protective Fracture runes.",
            category = ItemCategory.ARMOR,
            equipSlot = EquipSlot.CHEST,
            defenseBonus = 18f,
            healthBonus = 75f,
            priceGold = 280,
            colorHex = 0xFF0284C7
        ),
        ItemDef(
            id = "arm_iron_gauntlets",
            name = "Forge-Tempered Gauntlets",
            description = "Articulated steel gloves that steady weapon grips and boost attack power.",
            category = ItemCategory.ARMOR,
            equipSlot = EquipSlot.GLOVES,
            defenseBonus = 6f,
            attackBonus = 5f,
            priceGold = 95,
            colorHex = 0xFF64748B
        ),
        ItemDef(
            id = "arm_leather_boots",
            name = "Frontier Greaves",
            description = "Mud-resistant riding boots suited for cliffs and forest trails.",
            category = ItemCategory.ARMOR,
            equipSlot = EquipSlot.BOOTS,
            defenseBonus = 4f,
            staminaBonus = 15f,
            priceGold = 55,
            colorHex = 0xFF78350F
        ),
        ItemDef(
            id = "arm_wind_boots",
            name = "Silverlake Strider Boots",
            description = "Lightweight enchanted boots granting extra stamina reserve.",
            category = ItemCategory.ARMOR,
            equipSlot = EquipSlot.BOOTS,
            defenseBonus = 8f,
            staminaBonus = 35f,
            priceGold = 165,
            colorHex = 0xFF0EA5E9
        ),
        ItemDef(
            id = "acc_fracture_amulet",
            name = "Amulet of the First Dawn",
            description = "Ancient talisman sharpening instincts and critical strike precision.",
            category = ItemCategory.ARMOR,
            equipSlot = EquipSlot.ACCESSORY,
            critBonus = 0.08f,
            healthBonus = 35f,
            staminaBonus = 20f,
            priceGold = 210,
            colorHex = 0xFFF59E0B
        ),

        // Consumables
        ItemDef(
            id = "con_health_potion",
            name = "Crimson Vitality Draught",
            description = "Restores 85 HP instantly during battle.",
            category = ItemCategory.CONSUMABLES,
            healAmount = 85f,
            priceGold = 25,
            colorHex = 0xFFEF4444
        ),
        ItemDef(
            id = "con_greater_potion",
            name = "Elixir of Aether",
            description = "Potent alchemical elixir restoring 180 HP and 50 Stamina.",
            category = ItemCategory.CONSUMABLES,
            healAmount = 180f,
            staminaRestore = 50f,
            priceGold = 65,
            colorHex = 0xFFEC4899
        ),
        ItemDef(
            id = "con_stamina_tonic",
            name = "Windleaf Tonic",
            description = "Restores 85 Stamina for relentless combos and dodges.",
            category = ItemCategory.CONSUMABLES,
            staminaRestore = 85f,
            priceGold = 20,
            colorHex = 0xFF10B981
        ),

        // Materials
        ItemDef(
            id = "mat_ancient_bark",
            name = "Ancient Heartwood Bark",
            description = "Living timber harvested from Greenveil's oldest groves.",
            category = ItemCategory.MATERIALS,
            priceGold = 18,
            colorHex = 0xFF15803D
        ),
        ItemDef(
            id = "mat_ashen_ore",
            name = "Ashen Igneous Ore",
            description = "Dense volcanic iron prized by Dawnreach blacksmiths.",
            category = ItemCategory.MATERIALS,
            priceGold = 28,
            colorHex = 0xFFEA580C
        ),
        ItemDef(
            id = "mat_beast_pelt",
            name = "Shadowmane Pelt",
            description = "Thick dark fur taken from roaming wild beasts.",
            category = ItemCategory.MATERIALS,
            priceGold = 22,
            colorHex = 0xFF57534E
        ),
        ItemDef(
            id = "mat_aether_shard",
            name = "Fracture Aether Shard",
            description = "Crystallized sky-energy humming with ancient power.",
            category = ItemCategory.MATERIALS,
            priceGold = 55,
            colorHex = 0xFF38BDF8
        ),

        // Quest Items & Collectibles
        ItemDef(
            id = "qst_tyrant_heart",
            name = "Heart of the Forest Tyrant",
            description = "Pulsing emerald core proof of the Tyrant's defeat.",
            category = ItemCategory.QUEST_ITEMS,
            priceGold = 0,
            colorHex = 0xFF22C55E
        ),
        ItemDef(
            id = "qst_colossus_core",
            name = "Colossus Fracture Core",
            description = "Keystone relic needed to seal the Fracture Altar in Forgotten Kingdom.",
            category = ItemCategory.QUEST_ITEMS,
            priceGold = 0,
            colorHex = 0xFFA855F7
        ),
        ItemDef(
            id = "col_ancient_relic",
            name = "Relic of the Lost Crown",
            description = "An intact silver signet from before The Fracture.",
            category = ItemCategory.COLLECTIBLES,
            priceGold = 90,
            colorHex = 0xFFFCD34D
        )
    ).associateBy { it.id }

    val skills: List<SkillDef> = listOf(
        SkillDef("sk_sword_mastery", "Sword Mastery", "Sword Mastery", "Increases all weapon attack damage by +12% per rank.", 3, "+12% Attack Damage"),
        SkillDef("sk_heavy_impact", "Crushing Blow", "Heavy Attack", "Heavy & Charged Attacks deal +20% bonus damage and extra stagger.", 3, "+20% Heavy Damage"),
        SkillDef("sk_endurance", "Frontier Endurance", "Stamina", "Raises Maximum Stamina by +25 and speeds up stamina recovery.", 3, "+25 Max Stamina"),
        SkillDef("sk_iron_ward", "Iron Bastion", "Defensive Ability", "Increases Defense by +5 and widens Perfect Parry timing window.", 3, "+5 DEF & Parry Window"),
        SkillDef("sk_phantom_step", "Phantom Step", "Dodge Mastery", "Reduces Dodge/Roll stamina cost by 20% and extends invulnerability.", 3, "-20% Dodge Cost"),
        SkillDef("sk_lethal_edge", "Lethal Precision", "Critical Strike", "Raises Critical Strike chance by +6% and crit damage multiplier.", 3, "+6% Crit Rate")
    )

    fun buildInitialQuests(): List<QuestDef> = listOf(
        // 8 MAIN QUESTS
        QuestDef(
            id = "mq_01",
            title = "1. Awakening",
            description = "You awaken near Dawnreach Village after The Fracture tore the frontier sky. Speak with Elder Vance in the village square.",
            type = QuestType.MAIN,
            giverNpcId = "npc_elder_vance",
            objectives = listOf(QuestObjective("obj_mq01", "Speak with Elder Vance in Dawnreach Village", "talk_npc_elder_vance", 1)),
            rewardExp = 80,
            rewardGold = 50,
            rewardItemId = "con_health_potion"
        ),
        QuestDef(
            id = "mq_02",
            title = "2. The Lost Village",
            description = "Forest Goblins have encroached on Dawnreach's western road. Defeat 3 Forest Goblins to secure the village perimeter.",
            type = QuestType.MAIN,
            giverNpcId = "npc_elder_vance",
            prerequisiteQuestId = "mq_01",
            objectives = listOf(QuestObjective("obj_mq02", "Defeat Forest Goblins near Dawnreach", "kill_goblin", 3)),
            rewardExp = 150,
            rewardGold = 100,
            rewardItemId = "wpn_twin_fangs"
        ),
        QuestDef(
            id = "mq_03",
            title = "3. Into Greenveil",
            description = "Venture deep into Greenveil Forest, discover the Ancient Ruins, and gather 2 Ancient Heartwood Bark.",
            type = QuestType.MAIN,
            giverNpcId = "npc_aria",
            prerequisiteQuestId = "mq_02",
            objectives = listOf(
                QuestObjective("obj_mq03_a", "Reach Greenveil Forest", "visit_greenveil", 1),
                QuestObjective("obj_mq03_b", "Collect Ancient Heartwood Bark", "collect_bark", 2)
            ),
            rewardExp = 200,
            rewardGold = 120,
            rewardItemId = "wpn_titan_greatsword"
        ),
        QuestDef(
            id = "mq_04",
            title = "4. The Ancient Signal",
            description = "A corrupted guardian stirs in the Emerald Heart Glade of Greenveil Forest. Slay The Forest Tyrant!",
            type = QuestType.MAIN,
            giverNpcId = "npc_aria",
            prerequisiteQuestId = "mq_03",
            objectives = listOf(QuestObjective("obj_mq04", "Defeat The Forest Tyrant", "kill_forest_tyrant", 1)),
            rewardExp = 450,
            rewardGold = 280,
            rewardItemId = "wpn_aether_sword"
        ),
        QuestDef(
            id = "mq_05",
            title = "5. The Ashen Path",
            description = "Scale the Ashen Highlands past the volcanic ravine bridge and ignite the Ancient Beacon Brazier.",
            type = QuestType.MAIN,
            giverNpcId = "npc_elder_vance",
            prerequisiteQuestId = "mq_04",
            objectives = listOf(
                QuestObjective("obj_mq05_a", "Reach Ashen Highlands", "visit_ashen", 1),
                QuestObjective("obj_mq05_b", "Ignite the Ashen Beacon Brazier", "activate_beacon", 1)
            ),
            rewardExp = 360,
            rewardGold = 220,
            rewardItemId = "arm_aether_plate"
        ),
        QuestDef(
            id = "mq_06",
            title = "6. Forgotten Kingdom",
            description = "March north into the ruined Forgotten Kingdom and unlock the Royal Citadel Waypoint.",
            type = QuestType.MAIN,
            giverNpcId = "npc_elder_vance",
            prerequisiteQuestId = "mq_05",
            objectives = listOf(
                QuestObjective("obj_mq06_a", "Enter Forgotten Kingdom", "visit_forgotten", 1),
                QuestObjective("obj_mq06_b", "Unlock Citadel Waypoint Shrine", "waypoint_forgotten", 1)
            ),
            rewardExp = 400,
            rewardGold = 250,
            rewardItemId = "con_greater_potion"
        ),
        QuestDef(
            id = "mq_07",
            title = "7. The First Guardian",
            description = "Confront The Ancient Colossus guarding the inner Citadel Courtyard.",
            type = QuestType.MAIN,
            giverNpcId = "npc_aria",
            prerequisiteQuestId = "mq_06",
            objectives = listOf(QuestObjective("obj_mq07", "Defeat The Ancient Colossus", "kill_ancient_colossus", 1)),
            rewardExp = 750,
            rewardGold = 450,
            rewardItemId = "wpn_colossus_cleaver"
        ),
        QuestDef(
            id = "mq_08",
            title = "8. The Fracture",
            description = "Ascend to the Northern Altar beyond the Citadel and seal the Fracture Rift once and for all.",
            type = QuestType.MAIN,
            giverNpcId = "npc_elder_vance",
            prerequisiteQuestId = "mq_07",
            objectives = listOf(QuestObjective("obj_mq08", "Seal the Fracture Core Altar", "activate_fracture_altar", 1)),
            rewardExp = 1200,
            rewardGold = 800,
            rewardItemId = "acc_fracture_amulet"
        ),

        // 10 SIDE QUESTS
        QuestDef(
            id = "sq_01",
            title = "Wolf Pack at the Gate",
            description = "Hunt 3 Wild Beasts prowling the border between Dawnreach and Greenveil.",
            type = QuestType.SIDE,
            giverNpcId = "npc_guard",
            objectives = listOf(QuestObjective("obj_sq01", "Defeat Wild Beasts", "kill_beast", 3)),
            rewardExp = 140,
            rewardGold = 85,
            rewardItemId = "arm_scout_hood"
        ),
        QuestDef(
            id = "sq_02",
            title = "Brom's Volcanic Ore",
            description = "Blacksmith Brom needs 2 Ashen Igneous Ore from the Ashen Highlands to forge superior blades.",
            type = QuestType.SIDE,
            giverNpcId = "npc_blacksmith",
            objectives = listOf(QuestObjective("obj_sq02", "Gather Ashen Igneous Ore", "collect_ore", 2)),
            rewardExp = 180,
            rewardGold = 140,
            rewardItemId = "arm_iron_gauntlets"
        ),
        QuestDef(
            id = "sq_03",
            title = "Echoes of Silverlake",
            description = "Visit the Silverlake fisher docks and row across to the center island shrine.",
            type = QuestType.SIDE,
            giverNpcId = "npc_traveler",
            objectives = listOf(
                QuestObjective("obj_sq03_a", "Reach Silverlake Valley", "visit_silverlake", 1),
                QuestObjective("obj_sq03_b", "Open the Lake Island Chest", "chest_silverlake_island", 1)
            ),
            rewardExp = 210,
            rewardGold = 150,
            rewardItemId = "arm_wind_boots"
        ),
        QuestDef(
            id = "sq_04",
            title = "Arrows in the Canopy",
            description = "Eliminate 2 Shadow Archers ambushing travelers along the northern forest trail.",
            type = QuestType.SIDE,
            giverNpcId = "npc_guard",
            objectives = listOf(QuestObjective("obj_sq04", "Defeat Shadow Archers", "kill_archer", 2)),
            rewardExp = 190,
            rewardGold = 120,
            rewardItemId = "wpn_windstrider_bow"
        ),
        QuestDef(
            id = "sq_05",
            title = "Fallen Knights of Old",
            description = "Defeat 2 elite Dark Knights patrolling the Forgotten Kingdom boulevard.",
            type = QuestType.SIDE,
            giverNpcId = "npc_aria",
            objectives = listOf(QuestObjective("obj_sq05", "Defeat Dark Knights", "kill_dark_knight", 2)),
            rewardExp = 320,
            rewardGold = 210,
            rewardItemId = "arm_knight_helm"
        ),
        QuestDef(
            id = "sq_06",
            title = "Stone Sentinels",
            description = "Destroy 2 Ancient Guardians defending the ruined aqueducts.",
            type = QuestType.SIDE,
            giverNpcId = "npc_traveler",
            objectives = listOf(QuestObjective("obj_sq06", "Defeat Ancient Guardians", "kill_guardian", 2)),
            rewardExp = 380,
            rewardGold = 260,
            rewardItemId = "con_greater_potion"
        ),
        QuestDef(
            id = "sq_07",
            title = "Relics of the Crown",
            description = "Recover 2 Ancient Relics hidden in treasure chests across the frontier.",
            type = QuestType.SIDE,
            giverNpcId = "npc_merchant",
            objectives = listOf(QuestObjective("obj_sq07", "Open Frontier Treasure Chests", "open_any_chest", 2)),
            rewardExp = 175,
            rewardGold = 160,
            rewardItemId = "con_stamina_tonic"
        ),
        QuestDef(
            id = "sq_08",
            title = "Stones That Speak",
            description = "Read 2 Ancient Lore Tablets carved before The Fracture.",
            type = QuestType.SIDE,
            giverNpcId = "npc_villager",
            objectives = listOf(QuestObjective("obj_sq08", "Inspect Ancient Lore Tablets", "read_lore", 2)),
            rewardExp = 160,
            rewardGold = 110,
            rewardItemId = "con_health_potion"
        ),
        QuestDef(
            id = "sq_09",
            title = "Beneath the Citadel",
            description = "Enter the Forgotten Kingdom Dungeon Vault and claim its hidden treasure.",
            type = QuestType.SIDE,
            giverNpcId = "npc_blacksmith",
            objectives = listOf(QuestObjective("obj_sq09", "Enter the Forgotten Dungeon", "enter_dungeon", 1)),
            rewardExp = 280,
            rewardGold = 190,
            rewardItemId = "mat_aether_shard"
        ),
        QuestDef(
            id = "sq_10",
            title = "Merchant's Supply Run",
            description = "Speak with Merchant Lyra in Dawnreach Village to inspect frontier provisions.",
            type = QuestType.SIDE,
            giverNpcId = "npc_villager",
            objectives = listOf(QuestObjective("obj_sq10", "Speak with Merchant Lyra", "talk_npc_merchant", 1)),
            rewardExp = 90,
            rewardGold = 75,
            rewardItemId = "con_health_potion"
        ),

        // 5 EXPLORATION OBJECTIVES
        QuestDef(
            id = "ex_01",
            title = "Frontier Cartographer: Greenveil",
            description = "Discover Greenveil Forest and attune to its Waypoint Shrine.",
            type = QuestType.EXPLORATION,
            giverNpcId = "npc_traveler",
            objectives = listOf(QuestObjective("obj_ex01", "Attune Greenveil Waypoint", "waypoint_greenveil", 1)),
            rewardExp = 140,
            rewardGold = 80
        ),
        QuestDef(
            id = "ex_02",
            title = "Frontier Cartographer: Silverlake",
            description = "Discover Silverlake Valley and attune to the Dockside Waypoint.",
            type = QuestType.EXPLORATION,
            giverNpcId = "npc_traveler",
            objectives = listOf(QuestObjective("obj_ex02", "Attune Silverlake Waypoint", "waypoint_silverlake", 1)),
            rewardExp = 140,
            rewardGold = 80
        ),
        QuestDef(
            id = "ex_03",
            title = "Frontier Cartographer: Ashen Peaks",
            description = "Discover the Ashen Highlands and attune to the Ridge Waypoint.",
            type = QuestType.EXPLORATION,
            giverNpcId = "npc_traveler",
            objectives = listOf(QuestObjective("obj_ex03", "Attune Ashen Highlands Waypoint", "waypoint_ashen", 1)),
            rewardExp = 160,
            rewardGold = 95
        ),
        QuestDef(
            id = "ex_04",
            title = "Waterfall of Whispering Pines",
            description = "Locate the hidden waterfall cascade deep in Greenveil Forest.",
            type = QuestType.EXPLORATION,
            giverNpcId = "npc_villager",
            objectives = listOf(QuestObjective("obj_ex04", "Visit Greenveil Waterfall", "visit_waterfall", 1)),
            rewardExp = 150,
            rewardGold = 90
        ),
        QuestDef(
            id = "ex_05",
            title = "Rim of the Volcano",
            description = "Climb to the volcanic ridge overlooking Ashen Highlands.",
            type = QuestType.EXPLORATION,
            giverNpcId = "npc_traveler",
            objectives = listOf(QuestObjective("obj_ex05", "Reach the Volcanic Overlook", "visit_volcano", 1)),
            rewardExp = 180,
            rewardGold = 120
        )
    )

    fun buildNpcs(): List<NpcDef> = listOf(
        NpcDef(
            id = "npc_elder_vance",
            name = "Elder Vance",
            role = NpcRole.VILLAGE_LEADER,
            region = RegionId.DAWNREACH_VILLAGE,
            homePos = Vec3(0f, 0f, -12f),
            patrolRadius = 2.5f
        ),
        NpcDef(
            id = "npc_blacksmith",
            name = "Brom Ironanvil",
            role = NpcRole.BLACKSMITH,
            region = RegionId.DAWNREACH_VILLAGE,
            homePos = Vec3(-18f, 0f, -6f),
            patrolRadius = 2f,
            shopItemIds = listOf(
                "wpn_iron_sword", "wpn_titan_greatsword", "wpn_twin_fangs",
                "wpn_windstrider_bow", "arm_knight_helm", "arm_iron_gauntlets"
            )
        ),
        NpcDef(
            id = "npc_merchant",
            name = "Lyra Brightweave",
            role = NpcRole.MERCHANT,
            region = RegionId.DAWNREACH_VILLAGE,
            homePos = Vec3(18f, 0f, -4f),
            patrolRadius = 2.5f,
            shopItemIds = listOf(
                "con_health_potion", "con_greater_potion", "con_stamina_tonic",
                "arm_scout_hood", "arm_leather_boots", "acc_fracture_amulet"
            )
        ),
        NpcDef(
            id = "npc_aria",
            name = "Captain Aria",
            role = NpcRole.QUEST_GIVER,
            region = RegionId.DAWNREACH_VILLAGE,
            homePos = Vec3(-10f, 0f, 14f),
            patrolRadius = 4f
        ),
        NpcDef(
            id = "npc_guard",
            name = "Warden Kael",
            role = NpcRole.GUARD,
            region = RegionId.DAWNREACH_VILLAGE,
            homePos = Vec3(-34f, 0f, 2f),
            patrolRadius = 5f
        ),
        NpcDef(
            id = "npc_villager",
            name = "Maren the Chronicler",
            role = NpcRole.VILLAGER,
            region = RegionId.DAWNREACH_VILLAGE,
            homePos = Vec3(10f, 0f, 16f),
            patrolRadius = 8f
        ),
        NpcDef(
            id = "npc_traveler",
            name = "Rowan the Wayfarer",
            role = NpcRole.TRAVELER,
            region = RegionId.SILVERLAKE_VALLEY,
            homePos = Vec3(220f, 0f, 305f),
            patrolRadius = 4f
        )
    )

    fun getDialogueForNpc(npc: NpcDef, activeQuestIds: Set<String>, completedQuestIds: Set<String>): DialogueNode {
        return when (npc.id) {
            "npc_elder_vance" -> DialogueNode(
                id = "root",
                speakerName = "${npc.name} • ${npc.role.title}",
                text = when {
                    "mq_08" in completedQuestIds ->
                        "The Fracture Rift is sealed, hero! Dawnreach and all the frontier owe their dawn to your courage."
                    "mq_07" in completedQuestIds ->
                        "The Ancient Colossus has fallen. Take the Core to the Fracture Altar north of the Citadel and end this crisis!"
                    "mq_02" in completedQuestIds ->
                        "You pushed the goblins back from our walls. Seek Captain Aria for the expedition into Greenveil, or journey north when ready."
                    else ->
                        "Welcome back to consciousness, traveler. Since The Fracture shattered the northern sky, monsters roam Greenveil and the Forgotten Kingdom."
                },
                choices = buildList {
                    if ("mq_02" !in activeQuestIds && "mq_02" !in completedQuestIds) {
                        add(DialogueChoice("I'll clear the goblins near Dawnreach.", acceptQuestId = "mq_02"))
                    }
                    if ("mq_05" !in activeQuestIds && "mq_05" !in completedQuestIds && "mq_04" in completedQuestIds) {
                        add(DialogueChoice("Tell me about the Ashen Path.", acceptQuestId = "mq_05"))
                    }
                    if ("mq_06" !in activeQuestIds && "mq_06" !in completedQuestIds && "mq_05" in completedQuestIds) {
                        add(DialogueChoice("I'm ready for the Forgotten Kingdom.", acceptQuestId = "mq_06"))
                    }
                    if ("mq_08" !in activeQuestIds && "mq_08" !in completedQuestIds && "mq_07" in completedQuestIds) {
                        add(DialogueChoice("I will seal the Fracture Altar.", acceptQuestId = "mq_08"))
                    }
                    add(DialogueChoice("Farewell, Elder."))
                }
            )
            "npc_blacksmith" -> DialogueNode(
                id = "root",
                speakerName = "${npc.name} • ${npc.role.title}",
                text = "Steel and runic fire keep adventurers breathing! I can forge upgrades onto your equipped weapon, or outfit you with heavier arms.",
                choices = listOf(
                    DialogueChoice("Upgrade Equipped Weapon (45 Gold)", openUpgrade = true),
                    DialogueChoice("Browse Armory & Weapons", openShop = true),
                    DialogueChoice("Maybe later.")
                )
            )
            "npc_merchant" -> DialogueNode(
                id = "root",
                speakerName = "${npc.name} • ${npc.role.title}",
                text = "Potions, windleaf tonics, and frontier gear—freshly packed for the wilds! What catches your eye?",
                choices = listOf(
                    DialogueChoice("Open Merchant Shop", openShop = true),
                    DialogueChoice("Just passing through.")
                )
            )
            "npc_aria" -> DialogueNode(
                id = "root",
                speakerName = "${npc.name} • ${npc.role.title}",
                text = "Our scouts report glowing runes deep inside Greenveil Forest and dark knights marching near the Citadel. Stay sharp and parry heavy blows!",
                choices = buildList {
                    if ("mq_03" !in activeQuestIds && "mq_03" !in completedQuestIds) {
                        add(DialogueChoice("Accept: Into Greenveil", acceptQuestId = "mq_03"))
                    }
                    if ("mq_04" !in activeQuestIds && "mq_04" !in completedQuestIds && "mq_03" in completedQuestIds) {
                        add(DialogueChoice("Accept: Hunt The Forest Tyrant", acceptQuestId = "mq_04"))
                    }
                    if ("mq_07" !in activeQuestIds && "mq_07" !in completedQuestIds && "mq_06" in completedQuestIds) {
                        add(DialogueChoice("Accept: Defeat The Ancient Colossus", acceptQuestId = "mq_07"))
                    }
                    add(DialogueChoice("Understood, Captain."))
                }
            )
            "npc_guard" -> DialogueNode(
                id = "root",
                speakerName = "${npc.name} • ${npc.role.title}",
                text = "Keep your shield raised when crossing the western bridge. Wild beasts and shadow archers stalk the treeline.",
                choices = listOf(DialogueChoice("I'll watch the perimeter."))
            )
            "npc_villager" -> DialogueNode(
                id = "root",
                speakerName = "${npc.name} • ${npc.role.title}",
                text = "Before The Fracture, Dawnreach traded freely across Silverlake Valley. Now ancient stone tablets hum with forgotten history.",
                choices = listOf(DialogueChoice("Interesting lore. Thank you."))
            )
            else -> DialogueNode(
                id = "root",
                speakerName = "${npc.name} • ${npc.role.title}",
                text = "The waters of Silverlake are calm today! There's a sturdy rowboat moored by the dock if you want to reach the island shrine.",
                choices = listOf(DialogueChoice("I'll take a look at the boat."))
            )
        }
    }
}
