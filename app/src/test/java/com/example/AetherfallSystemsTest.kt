package com.example

import com.example.game.audio.AudioEngine
import com.example.game.math.TerrainMath
import com.example.game.model.*
import com.example.game.save.SaveManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import com.example.game.world.GameSimulation

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AetherfallSystemsTest {

    @Test
    fun worldTerrainCoversAllFiveRegions() {
        assertEquals(RegionId.DAWNREACH_VILLAGE, TerrainMath.getRegionAt(0f, 0f))
        assertEquals(RegionId.GREENVEIL_FOREST, TerrainMath.getRegionAt(-400f, -100f))
        assertEquals(RegionId.ASHEN_HIGHLANDS, TerrainMath.getRegionAt(480f, -480f))
        assertEquals(RegionId.SILVERLAKE_VALLEY, TerrainMath.getRegionAt(440f, 440f))
        assertEquals(RegionId.FORGOTTEN_KINGDOM, TerrainMath.getRegionAt(0f, -650f))

        // Silverlake basin must be submerged below water level
        assertTrue(TerrainMath.getHeight(400f, 400f) < TerrainMath.WATER_LEVEL)
        // Ashen Highlands volcano peak rises sharply
        assertTrue(TerrainMath.getHeight(560f, -540f) > 25f)
    }

    @Test
    fun questsCatalogMatchesRequirements() {
        val quests = GameCatalog.buildInitialQuests()
        assertEquals(8, quests.count { it.type == QuestType.MAIN })
        assertEquals(10, quests.count { it.type == QuestType.SIDE })
        assertEquals(5, quests.count { it.type == QuestType.EXPLORATION })
    }

    @Test
    fun combatMovementProgressionAndSaveLoadWorkEndToEnd() {
        val context = RuntimeEnvironment.getApplication()
        val audio = AudioEngine()
        val sim = GameSimulation(audio) {}

        // Move forward & jump
        sim.moveInputY = -0.9f
        sim.step(0.1f)
        sim.triggerJump()
        assertFalse(sim.isGrounded)

        // Attack & weapon switch
        val startWpn = sim.equipment.weaponId
        sim.switchNextWeapon()
        assertNotEquals(startWpn, sim.equipment.weaponId)
        sim.triggerAttack(heavy = false)
        assertTrue(sim.attackTimer > 0f)

        // Skill upgrade
        val spBefore = sim.stats.skillPoints
        assertTrue(sim.upgradeSkill("sk_sword_mastery"))
        assertEquals(spBefore - 1, sim.stats.skillPoints)

        // Atomic Save & Load verification
        val saveMgr = SaveManager(context)
        sim.stats.gold = 777
        assertTrue(saveMgr.saveGame(1, sim.exportSavePayload()))
        val loaded = saveMgr.loadGame(1)
        assertNotNull(loaded)
        assertEquals(777, loaded!!.stats.gold)
    }
}
