package com.example.game.render

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.math.Math3D
import com.example.game.math.TerrainMath
import com.example.game.model.*
import com.example.game.world.GameSimulation
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

data class ProjectedDamageHud(
    val id: Long,
    val normX: Float,
    val normY: Float,
    val amount: Int,
    val isCrit: Boolean,
    val isHeal: Boolean,
    val isPlayerHit: Boolean,
    val alpha: Float
)

data class HudRenderBridge(
    val projectedDamage: List<ProjectedDamageHud> = emptyList(),
    val measuredFps: Int = 30
)

class AetherfallRenderer(
    private val simulation: GameSimulation,
    private val onFrameUpdate: (HudRenderBridge) -> Unit
) : GLSurfaceView.Renderer {

    @Volatile var isMenuPanoramaMode: Boolean = true
    @Volatile var isSimulationPaused: Boolean = false

    private lateinit var worldShader: ShaderProgram
    private lateinit var waterShader: ShaderProgram
    private lateinit var skyShader: ShaderProgram
    private lateinit var vfxShader: ShaderProgram

    private lateinit var skyDome: GpuMesh
    private lateinit var pineMesh: GpuMesh
    private lateinit var broadleafMesh: GpuMesh
    private lateinit var rockMesh: GpuMesh
    private lateinit var bushMesh: GpuMesh
    private lateinit var houseMesh: GpuMesh
    private lateinit var towerMesh: GpuMesh
    private lateinit var archMesh: GpuMesh
    private lateinit var dockMesh: GpuMesh
    private lateinit var boatMesh: GpuMesh
    private lateinit var chestMesh: GpuMesh
    private lateinit var obeliskMesh: GpuMesh
    private lateinit var particleSphere: GpuMesh
    private lateinit var warningRingMesh: GpuMesh
    private lateinit var rig: CharacterVisualRig

    private val chunkManager = WorldChunkManager()
    val camera = ThirdPersonCamera()

    private val modelMat = FloatArray(16)
    private val mvpMat = FloatArray(16)
    private val normalMat = FloatArray(9)

    private var lastFrameNs = System.nanoTime()
    private var totalTime = 0f
    private var fpsCounter = 0
    private var fpsTimer = 0f
    private var currentFps = 30

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)
        GLES30.glClearColor(0.05f, 0.08f, 0.14f, 1f)

        worldShader = ShaderProgram(ShaderLibrary.WORLD_VERTEX_SHADER, ShaderLibrary.WORLD_FRAGMENT_SHADER)
        waterShader = ShaderProgram(ShaderLibrary.WATER_VERTEX_SHADER, ShaderLibrary.WATER_FRAGMENT_SHADER)
        skyShader = ShaderProgram(ShaderLibrary.SKY_VERTEX_SHADER, ShaderLibrary.SKY_FRAGMENT_SHADER)
        vfxShader = ShaderProgram(ShaderLibrary.VFX_VERTEX_SHADER, ShaderLibrary.VFX_FRAGMENT_SHADER)

        skyDome = MeshBuilder.createSkyDome()
        pineMesh = MeshBuilder.createPineTree()
        broadleafMesh = MeshBuilder.createBroadleafTree()
        rockMesh = MeshBuilder.createNaturalRock()
        bushMesh = MeshBuilder.createBush()
        houseMesh = MeshBuilder.createVillageHouse()
        towerMesh = MeshBuilder.createCastleTower()
        archMesh = MeshBuilder.createRuinArch()
        dockMesh = MeshBuilder.createWoodenDock()
        boatMesh = MeshBuilder.createRowboat()
        chestMesh = MeshBuilder.createTreasureChest()
        obeliskMesh = MeshBuilder.createWaypointObelisk()
        particleSphere = MeshBuilder.createSphereVfx()
        warningRingMesh = MeshBuilder.createRingVfx()
        rig = CharacterMeshBuilder.buildAll()

        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        camera.setViewport(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val nowNs = System.nanoTime()
        val rawDt = ((nowNs - lastFrameNs) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.06f)
        lastFrameNs = nowNs
        totalTime += rawDt

        fpsCounter++
        fpsTimer += rawDt
        if (fpsTimer >= 0.6f) {
            currentFps = (fpsCounter / fpsTimer).toInt().coerceAtLeast(1)
            fpsCounter = 0
            fpsTimer = 0f
        }

        if (!isMenuPanoramaMode && !isSimulationPaused) {
            simulation.step(rawDt)
        }

        // Snapshot minimal camera & environment settings
        val settings = simulation.settings
        val quality = settings.graphicsQuality
        val playerPos: Vec3
        val playerYaw: Float
        val playerAnim: AnimationState
        val playerAnimTime: Float
        val comboStep: Int
        val inCombat: Boolean
        val shake: Float
        val timeMin: Float
        val weather: WeatherType
        val inDungeon: Boolean
        val inBoat: Boolean
        val equipment: EquipmentLoadout
        val attackTimer: Float

        synchronized(simulation.stateLock) {
            playerPos = simulation.stats.position.copy()
            playerYaw = simulation.stats.yaw
            playerAnim = simulation.playerAnimState
            playerAnimTime = simulation.playerAnimTime
            comboStep = simulation.comboStep
            inCombat = simulation.activeBoss != null || simulation.enemies.any {
                it.isAlive && (it.aiState == EnemyAiState.CHASE || it.aiState == EnemyAiState.ATTACK)
            }
            shake = simulation.cameraShake
            timeMin = simulation.worldTimeMinutes
            weather = simulation.currentWeather
            inDungeon = simulation.stats.inDungeon
            inBoat = simulation.stats.inBoat
            equipment = simulation.equipment.copy()
            attackTimer = simulation.attackTimer
        }

        if (isMenuPanoramaMode) {
            camera.updateMenuPanorama(totalTime)
        } else {
            camera.updateFollow(
                playerPos = playerPos,
                yaw = simulation.cameraYaw,
                pitch = simulation.cameraPitch,
                desiredDistance = simulation.cameraDistance,
                inCombat = inCombat,
                shakeAmount = shake,
                dt = rawDt
            )
        }

        // Compute dynamic Day/Night lighting & weather atmosphere
        val dayFrac = (timeMin / 1440f) * 2f * PI.toFloat() - PI.toFloat() * 0.5f
        val sunY = sin(dayFrac)
        val sunX = cos(dayFrac) * 0.8f
        val sunZ = 0.45f
        val dayStrength = Math3D.smoothstep(-0.15f, 0.35f, sunY)
        val sunsetFactor = (1f - abs(sunY) * 3.5f).coerceIn(0f, 1f)

        val zenithR = Math3D.lerp(0.03f, 0.16f, dayStrength) + sunsetFactor * 0.14f
        val zenithG = Math3D.lerp(0.05f, 0.44f, dayStrength) + sunsetFactor * 0.05f
        val zenithB = Math3D.lerp(0.14f, 0.82f, dayStrength)

        var horizR = Math3D.lerp(0.07f, 0.56f, dayStrength) + sunsetFactor * 0.36f
        var horizG = Math3D.lerp(0.10f, 0.72f, dayStrength) + sunsetFactor * 0.12f
        var horizB = Math3D.lerp(0.20f, 0.90f, dayStrength)

        if (weather == WeatherType.CLOUDY || weather == WeatherType.RAIN) {
            horizR *= 0.70f; horizG *= 0.74f; horizB *= 0.80f
        } else if (weather == WeatherType.FOG) {
            horizR = Math3D.lerp(horizR, 0.58f, 0.5f)
            horizG = Math3D.lerp(horizG, 0.64f, 0.5f)
            horizB = Math3D.lerp(horizB, 0.70f, 0.5f)
        }
        if (inDungeon) {
            horizR = 0.04f; horizG = 0.03f; horizB = 0.06f
        }

        val sunColorR = if (inDungeon) 0.18f else Math3D.lerp(0.25f, 1.0f, dayStrength) + sunsetFactor * 0.25f
        val sunColorG = if (inDungeon) 0.14f else Math3D.lerp(0.30f, 0.94f, dayStrength)
        val sunColorB = if (inDungeon) 0.28f else Math3D.lerp(0.55f, 0.82f, dayStrength)

        val fogDensity = when {
            inDungeon -> 0.024f
            !settings.weatherEffectsEnabled -> 0.0055f
            weather == WeatherType.FOG -> 0.0165f
            weather == WeatherType.RAIN -> 0.0105f
            else -> 1f / quality.drawDistance.coerceAtLeast(80f)
        }
        val wetness = if (weather == WeatherType.RAIN && settings.weatherEffectsEnabled) 0.72f else 0.0f

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // 1. Render Sky Dome
        if (!inDungeon) {
            drawSkyDome(
                zenith = floatArrayOf(zenithR, zenithG, zenithB),
                horizon = floatArrayOf(horizR, horizG, horizB),
                sunDir = floatArrayOf(sunX, sunY.coerceAtLeast(-0.1f), sunZ),
                sunCol = floatArrayOf(sunColorR, sunColorG, sunColorB),
                starAlpha = (1f - dayStrength).coerceIn(0f, 1f)
            )
        }

        // 2. Setup World Shader Uniforms
        worldShader.use()
        GLES30.glUniform3f(worldShader.uniform("uCameraPos"), camera.eyePos.x, camera.eyePos.y, camera.eyePos.z)
        GLES30.glUniform3f(worldShader.uniform("uSunDir"), sunX, max(0.18f, sunY), sunZ)
        GLES30.glUniform3f(worldShader.uniform("uSunColor"), sunColorR, sunColorG, sunColorB)
        GLES30.glUniform3f(worldShader.uniform("uSkyAmbient"), horizR * 0.48f, horizG * 0.50f, horizB * 0.56f)
        GLES30.glUniform3f(worldShader.uniform("uGroundAmbient"), 0.14f, 0.13f, 0.12f)
        GLES30.glUniform3f(worldShader.uniform("uFogColor"), horizR, horizG, horizB)
        GLES30.glUniform1f(worldShader.uniform("uFogDensity"), fogDensity)
        GLES30.glUniform1f(worldShader.uniform("uWetness"), wetness)

        // Dynamic regional torch / lava / dungeon point light
        val region = TerrainMath.getRegionAt(playerPos.x, playerPos.z)
        when {
            inDungeon -> {
                GLES30.glUniform3f(worldShader.uniform("uPointLightPos"), playerPos.x, playerPos.y + 2.8f, playerPos.z)
                GLES30.glUniform3f(worldShader.uniform("uPointLightColor"), 1.6f, 0.85f, 0.35f)
            }
            region == RegionId.ASHEN_HIGHLANDS -> {
                GLES30.glUniform3f(worldShader.uniform("uPointLightPos"), 560f, TerrainMath.getHeight(560f, -540f) + 6f, -540f)
                GLES30.glUniform3f(worldShader.uniform("uPointLightColor"), 2.4f, 0.65f, 0.12f)
            }
            else -> {
                GLES30.glUniform3f(worldShader.uniform("uPointLightPos"), -18f, TerrainMath.getHeight(-18f, -6f) + 2.5f, -6f)
                GLES30.glUniform3f(worldShader.uniform("uPointLightColor"), 1.3f, 0.70f, 0.25f)
            }
        }

        // 3. Stream & Draw Terrain Chunks + LOD Props
        val streamCenter = if (isMenuPanoramaMode) Vec3(0f, 2f, 0f) else playerPos
        val chunks = chunkManager.updateAround(streamCenter.x, streamCenter.z, quality)

        setWorldMaterial(roughness = 0.85f, emissiveR = 0f, emissiveG = 0f, emissiveB = 0f)
        Math3D.setIdentity(modelMat)
        uploadModelMatrices(worldShader)
        for (chunk in chunks) {
            chunk.terrainMesh.draw()
        }

        val maxPropDist = quality.drawDistance
        val vegDensity = quality.vegetationDensity
        for (chunk in chunks) {
            val count = (chunk.props.size * vegDensity).toInt().coerceAtMost(chunk.props.size)
            for (i in 0 until count) {
                val p = chunk.props[i]
                val dx = p.x - camera.eyePos.x
                val dz = p.z - camera.eyePos.z
                if (dx * dx + dz * dz > maxPropDist * maxPropDist) continue
                drawProp(p)
            }
        }
        for (lm in chunkManager.getLandmarks()) {
            val dx = lm.x - camera.eyePos.x
            val dz = lm.z - camera.eyePos.z
            if (dx * dx + dz * dz < (maxPropDist + 40f) * (maxPropDist + 40f)) {
                drawProp(lm)
            }
        }

        // 4. Draw World Interactables (Chests, Waypoint Shrines, Boat, Portal, Beacon, Altar)
        val projectedDamage = ArrayList<ProjectedDamageHud>()
        synchronized(simulation.stateLock) {
            for (obj in simulation.interactables) {
                if (obj.position.horizontalDistanceTo(streamCenter) > maxPropDist) continue
                when (obj.type) {
                    InteractableType.TREASURE_CHEST -> {
                        setWorldMaterial(0.45f, if (!obj.activated) 0.14f else 0f, if (!obj.activated) 0.10f else 0f, 0f)
                        drawMeshAt(chestMesh, obj.position.x, obj.position.y, obj.position.z, 0f, if (obj.activated) 0.9f else 1.05f)
                    }
                    InteractableType.WAYPOINT_SHRINE -> {
                        val glow = if (obj.activated) 0.45f + 0.18f * sin(totalTime * 3.2f) else 0.12f
                        setWorldMaterial(0.32f, 0.05f, glow * 0.7f, glow)
                        drawMeshAt(obeliskMesh, obj.position.x, obj.position.y, obj.position.z, totalTime * 0.35f, 1.0f)
                    }
                    InteractableType.BOAT_DOCK -> {
                        if (!inBoat) {
                            setWorldMaterial(0.65f, 0f, 0f, 0f)
                            drawMeshAt(boatMesh, obj.position.x, obj.position.y + sin(totalTime * 2.5f) * 0.06f, obj.position.z, 0.6f, 1.0f)
                        }
                    }
                    InteractableType.DUNGEON_PORTAL -> {
                        setWorldMaterial(0.35f, 0.35f, 0.10f, 0.55f)
                        drawMeshAt(archMesh, obj.position.x, obj.position.y, obj.position.z, 0f, 0.85f)
                    }
                    InteractableType.COLLECTIBLE_ITEM -> if (!obj.activated) {
                        setWorldMaterial(0.35f, 0.12f, 0.32f, 0.14f)
                        drawMeshAt(particleSphere, obj.position.x, obj.position.y + 0.6f + sin(totalTime * 3f) * 0.15f, obj.position.z, totalTime, 0.65f)
                    }
                    InteractableType.LORE_TABLET -> {
                        setWorldMaterial(0.6f, 0.05f, 0.18f, 0.25f)
                        drawMeshAt(obeliskMesh, obj.position.x, obj.position.y, obj.position.z, 0f, 0.55f)
                    }
                    InteractableType.BEACON_BRAZIER -> {
                        val lit = if (obj.activated) 0.8f else 0.15f
                        setWorldMaterial(0.45f, lit, lit * 0.35f, 0.05f)
                        drawMeshAt(obeliskMesh, obj.position.x, obj.position.y, obj.position.z, 0f, 0.8f)
                    }
                    InteractableType.FRACTURE_ALTAR -> {
                        setWorldMaterial(0.25f, 0.25f, 0.55f, 0.85f)
                        drawMeshAt(obeliskMesh, obj.position.x, obj.position.y, obj.position.z, totalTime * 0.8f, 1.35f)
                    }
                }
            }

            // 5. Draw NPCs
            for (npc in simulation.npcs) {
                if (npc.currentPos.horizontalDistanceTo(streamCenter) > maxPropDist) continue
                val npcAnim = when {
                    npc.isTalking -> AnimationState.INTERACTING
                    npc.patrolRadius > 2.2f -> AnimationState.WALKING
                    else -> AnimationState.IDLE
                }
                val tint = when (npc.role) {
                    NpcRole.BLACKSMITH -> floatArrayOf(1.0f, 0.72f, 0.52f)
                    NpcRole.MERCHANT -> floatArrayOf(0.82f, 1.0f, 0.82f)
                    NpcRole.GUARD -> floatArrayOf(0.75f, 0.85f, 1.0f)
                    else -> floatArrayOf(1f, 0.95f, 0.88f)
                }
                drawArticulatedHumanoid(
                    parts = rig.humanoidPlayer,
                    pos = npc.currentPos,
                    yaw = npc.yaw,
                    pose = CharacterAnimator.evaluatePose(npcAnim, npc.animTime),
                    scale = 1.0f,
                    weaponType = null,
                    showHelmet = npc.role == NpcRole.GUARD,
                    showShield = false,
                    tint = tint
                )
            }

            // 6. Draw Enemies & 2 Bosses
            for (en in simulation.enemies) {
                val dist = en.position.horizontalDistanceTo(streamCenter)
                if (dist > maxPropDist) continue
                if (!en.isAlive && !en.type.isBoss) continue

                if (en.type == EnemyType.WILD_BEAST) {
                    drawWildBeast(en)
                } else {
                    val parts = rig.partsForEnemy(en.type)
                    val scale = when (en.type) {
                        EnemyType.FOREST_TYRANT -> 1.85f
                        EnemyType.ANCIENT_COLOSSUS -> 2.15f
                        EnemyType.ANCIENT_GUARDIAN -> 1.38f
                        EnemyType.DARK_KNIGHT -> 1.15f
                        EnemyType.FOREST_GOBLIN -> 0.84f
                        else -> 0.98f
                    }
                    val enWpn = when (en.type) {
                        EnemyType.DARK_KNIGHT -> WeaponType.GREAT_SWORD
                        EnemyType.SHADOW_ARCHER -> WeaponType.BOW
                        EnemyType.FOREST_GOBLIN -> WeaponType.SWORD
                        EnemyType.ANCIENT_COLOSSUS -> WeaponType.GREAT_SWORD
                        else -> null
                    }
                    val tint = if (en.phase == 2) floatArrayOf(1.25f, 0.65f, 0.65f) else floatArrayOf(1f, 1f, 1f)
                    drawArticulatedHumanoid(
                        parts = parts,
                        pos = en.position,
                        yaw = en.yaw,
                        pose = CharacterAnimator.evaluatePose(en.animState, en.animTime),
                        scale = scale,
                        weaponType = enWpn,
                        showHelmet = en.type == EnemyType.DARK_KNIGHT || en.type == EnemyType.ANCIENT_COLOSSUS,
                        showShield = en.isBlocking,
                        tint = tint
                    )
                }
            }

            // 7. Draw Player Character (with visible Armor, Helmet, Weapon, or Boat)
            if (!isMenuPanoramaMode) {
                if (inBoat) {
                    setWorldMaterial(0.62f, 0f, 0f, 0f)
                    drawMeshAt(boatMesh, playerPos.x, playerPos.y - 0.1f, playerPos.z, playerYaw, 1.0f)
                }
                val armorTint = when (equipment.chestId) {
                    "arm_aether_plate" -> floatArrayOf(0.68f, 0.92f, 1.15f)
                    else -> floatArrayOf(1f, 1f, 1f)
                }
                drawArticulatedHumanoid(
                    parts = rig.humanoidPlayer,
                    pos = playerPos,
                    yaw = playerYaw,
                    pose = CharacterAnimator.evaluatePose(playerAnim, playerAnimTime, comboStep),
                    scale = 1.0f,
                    weaponType = GameCatalog.items[equipment.weaponId]?.weaponType ?: WeaponType.SWORD,
                    showHelmet = equipment.helmetId != null,
                    showShield = simulation.isBlocking,
                    tint = armorTint
                )
            }

            // 8. Draw Arrows
            setWorldMaterial(0.3f, 0.15f, 0.45f, 0.65f)
            for (arr in simulation.arrows) {
                val ayaw = Math3D.yawFromVector(arr.velocity.x, arr.velocity.z)
                drawMeshAt(rig.arrow, arr.position.x, arr.position.y, arr.position.z, ayaw, 1.1f)
            }

            // Project Damage Numbers for Compose Overlay
            for (di in simulation.damageIndicators) {
                camera.projectToScreenNorm(di.worldPos)?.let { (nx, ny) ->
                    projectedDamage.add(
                        ProjectedDamageHud(
                            id = di.id,
                            normX = nx,
                            normY = ny,
                            amount = di.amount,
                            isCrit = di.isCrit,
                            isHeal = di.isHeal,
                            isPlayerHit = di.isPlayerHit,
                            alpha = (1f - di.age / 1.15f).coerceIn(0f, 1f)
                        )
                    )
                }
            }
        }

        // 9. Draw Animated Water Planes (Alpha Blended)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        waterShader.use()
        GLES30.glUniform3f(waterShader.uniform("uCameraPos"), camera.eyePos.x, camera.eyePos.y, camera.eyePos.z)
        GLES30.glUniform3f(waterShader.uniform("uSunDir"), sunX, max(0.2f, sunY), sunZ)
        GLES30.glUniform3f(waterShader.uniform("uSunColor"), sunColorR, sunColorG, sunColorB)
        GLES30.glUniform3f(waterShader.uniform("uFogColor"), horizR, horizG, horizB)
        GLES30.glUniform1f(waterShader.uniform("uFogDensity"), fogDensity)
        GLES30.glUniform1f(waterShader.uniform("uTime"), totalTime)
        Math3D.setIdentity(modelMat)
        Matrix.multiplyMM(mvpMat, 0, camera.viewProjMatrix, 0, modelMat, 0)
        GLES30.glUniformMatrix4fv(waterShader.uniform("uModel"), 1, false, modelMat, 0)
        GLES30.glUniformMatrix4fv(waterShader.uniform("uMVP"), 1, false, mvpMat, 0)
        for (chunk in chunks) {
            chunk.waterMesh?.draw()
        }

        // 10. Draw VFX Particles, Weapon Slash Arc & Boss Attack Telegraph Rings
        vfxShader.use()
        synchronized(simulation.stateLock) {
            // Boss Attack Telegraph Warning Rings
            for (en in simulation.enemies) {
                if (en.isAlive && en.isTelegraphing && en.type.isBoss) {
                    val ringScale = (en.type.attackRange + 1.5f) * (0.4f + 0.6f * en.telegraphProgress)
                    Math3D.setIdentity(modelMat)
                    Math3D.translate(modelMat, en.position.x, en.position.y + 0.15f, en.position.z)
                    Math3D.scale(modelMat, ringScale, 1f, ringScale)
                    Matrix.multiplyMM(mvpMat, 0, camera.viewProjMatrix, 0, modelMat, 0)
                    GLES30.glUniformMatrix4fv(vfxShader.uniform("uModel"), 1, false, modelMat, 0)
                    GLES30.glUniformMatrix4fv(vfxShader.uniform("uMVP"), 1, false, mvpMat, 0)
                    GLES30.glUniform4f(vfxShader.uniform("uColorAlpha"), 1.0f, 0.15f, 0.12f, 0.72f)
                    warningRingMesh.draw()
                }
            }

            // Player Weapon Slash Arc VFX
            if (attackTimer > 0f && simulation.getEquippedWeaponType() != WeaponType.BOW) {
                Math3D.setIdentity(modelMat)
                Math3D.translate(modelMat, playerPos.x, playerPos.y + 1.15f, playerPos.z)
                Math3D.rotateY(modelMat, playerYaw)
                val arcScale = if (simulation.isHeavySwing) 2.9f else 2.2f
                Math3D.scale(modelMat, arcScale, 1.0f, arcScale)
                Matrix.multiplyMM(mvpMat, 0, camera.viewProjMatrix, 0, modelMat, 0)
                GLES30.glUniformMatrix4fv(vfxShader.uniform("uModel"), 1, false, modelMat, 0)
                GLES30.glUniformMatrix4fv(vfxShader.uniform("uMVP"), 1, false, mvpMat, 0)
                GLES30.glUniform4f(
                    vfxShader.uniform("uColorAlpha"),
                    if (simulation.isHeavySwing) 1.0f else 0.35f,
                    if (simulation.isHeavySwing) 0.65f else 0.85f,
                    1.0f,
                    0.55f
                )
                warningRingMesh.draw()
            }

            for (p in simulation.particles) {
                Math3D.setIdentity(modelMat)
                Math3D.translate(modelMat, p.position.x, p.position.y, p.position.z)
                Math3D.scale(modelMat, p.size, p.size, p.size)
                Matrix.multiplyMM(mvpMat, 0, camera.viewProjMatrix, 0, modelMat, 0)
                GLES30.glUniformMatrix4fv(vfxShader.uniform("uModel"), 1, false, modelMat, 0)
                GLES30.glUniformMatrix4fv(vfxShader.uniform("uMVP"), 1, false, mvpMat, 0)
                val a = (p.life / p.maxLife).coerceIn(0f, 1f)
                GLES30.glUniform4f(vfxShader.uniform("uColorAlpha"), p.r, p.g, p.b, a)
                particleSphere.draw()
            }
        }
        GLES30.glDisable(GLES30.GL_BLEND)

        onFrameUpdate(HudRenderBridge(projectedDamage, currentFps))

        // Enforce 30 FPS / 60 FPS target pacing accurately
        val targetFrameMs = if (settings.targetFps == 60) 16L else 33L
        val elapsedMs = (System.nanoTime() - nowNs) / 1_000_000L
        val sleepMs = targetFrameMs - elapsedMs
        if (sleepMs > 1L) {
            try {
                Thread.sleep(sleepMs)
            } catch (_: InterruptedException) {
            }
        }
    }

    private fun drawSkyDome(
        zenith: FloatArray,
        horizon: FloatArray,
        sunDir: FloatArray,
        sunCol: FloatArray,
        starAlpha: Float
    ) {
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glCullFace(GLES30.GL_FRONT)
        skyShader.use()
        val skyView = camera.viewMatrix.copyOf()
        skyView[12] = 0f; skyView[13] = 0f; skyView[14] = 0f
        Matrix.multiplyMM(mvpMat, 0, camera.projMatrix, 0, skyView, 0)
        GLES30.glUniformMatrix4fv(skyShader.uniform("uMVP"), 1, false, mvpMat, 0)
        GLES30.glUniform3fv(skyShader.uniform("uZenithColor"), 1, zenith, 0)
        GLES30.glUniform3fv(skyShader.uniform("uHorizonColor"), 1, horizon, 0)
        GLES30.glUniform3fv(skyShader.uniform("uSunDir"), 1, sunDir, 0)
        GLES30.glUniform3fv(skyShader.uniform("uSunColor"), 1, sunCol, 0)
        GLES30.glUniform1f(skyShader.uniform("uStarAlpha"), starAlpha)
        skyDome.draw()
        GLES30.glCullFace(GLES30.GL_BACK)
        GLES30.glDepthFunc(GLES30.GL_LESS)
    }

    private fun drawProp(p: PropPlacement) {
        setWorldMaterial(
            roughness = when (p.kind) {
                PropKind.CASTLE_TOWER, PropKind.RUIN_ARCH, PropKind.ROCK -> 0.88f
                PropKind.VILLAGE_HOUSE, PropKind.WOODEN_DOCK -> 0.72f
                else -> 0.92f
            },
            emissiveR = 0f, emissiveG = 0f, emissiveB = 0f
        )
        val mesh = when (p.kind) {
            PropKind.PINE_TREE -> pineMesh
            PropKind.BROADLEAF_TREE -> broadleafMesh
            PropKind.ROCK -> rockMesh
            PropKind.BUSH -> bushMesh
            PropKind.VILLAGE_HOUSE -> houseMesh
            PropKind.CASTLE_TOWER -> towerMesh
            PropKind.RUIN_ARCH -> archMesh
            PropKind.WOODEN_DOCK -> dockMesh
        }
        drawMeshAt(mesh, p.x, p.y, p.z, p.yaw, p.scale)
    }

    private fun drawArticulatedHumanoid(
        parts: CharacterPartMeshes,
        pos: Vec3,
        yaw: Float,
        pose: CharacterPose,
        scale: Float,
        weaponType: WeaponType?,
        showHelmet: Boolean,
        showShield: Boolean,
        tint: FloatArray
    ) {
        setWorldMaterial(0.45f, 0f, 0f, 0f, tint)

        val rootMat = FloatArray(16)
        Math3D.setIdentity(rootMat)
        Math3D.translate(rootMat, pos.x, pos.y + (0.76f + pose.rootOffsetY) * scale, pos.z)
        Math3D.rotateY(rootMat, yaw)
        Math3D.rotateX(rootMat, pose.rootPitch)

        // Torso
        val torsoMat = rootMat.copyOf()
        Math3D.rotateY(torsoMat, pose.torsoYaw)
        Math3D.rotateX(torsoMat, pose.torsoPitch)
        drawWithMatrix(parts.torso, torsoMat)

        // Cape
        val capeMat = torsoMat.copyOf()
        Math3D.translate(capeMat, 0f, 0.50f * scale, -0.14f * scale)
        Math3D.rotateX(capeMat, pose.capePitch)
        drawWithMatrix(parts.cape, capeMat)

        // Head & Optional Helmet
        val headMat = torsoMat.copyOf()
        Math3D.translate(headMat, 0f, 0.56f * scale, 0f)
        drawWithMatrix(parts.head, headMat)
        if (showHelmet) drawWithMatrix(parts.helmet, headMat)

        // Left Arm + Pauldron + Optional Shield / Offhand Blade
        val lShoulder = torsoMat.copyOf()
        Math3D.translate(lShoulder, -0.28f * scale, 0.48f * scale, 0f)
        drawWithMatrix(parts.pauldron, lShoulder)
        Math3D.rotateX(lShoulder, pose.leftShoulderPitch)
        Math3D.rotateZ(lShoulder, pose.leftShoulderRoll)
        drawWithMatrix(parts.upperArm, lShoulder)

        val lElbow = lShoulder.copyOf()
        Math3D.translate(lElbow, 0f, -0.28f * scale, 0f)
        Math3D.rotateX(lElbow, pose.leftElbowPitch)
        drawWithMatrix(parts.forearmAndHand, lElbow)
        if (showShield) {
            val shMat = lElbow.copyOf()
            Math3D.translate(shMat, -0.10f * scale, -0.18f * scale, 0.14f * scale)
            drawWithMatrix(rig.shield, shMat)
        } else if (weaponType == WeaponType.DUAL_BLADES) {
            val offMat = lElbow.copyOf()
            Math3D.translate(offMat, 0f, -0.32f * scale, 0.08f * scale)
            Math3D.rotateX(offMat, 1.35f)
            drawWithMatrix(rig.dualBlade, offMat)
        } else if (weaponType == WeaponType.BOW) {
            val bowMat = lElbow.copyOf()
            Math3D.translate(bowMat, 0f, -0.30f * scale, 0.12f * scale)
            drawWithMatrix(rig.bow, bowMat)
        }

        // Right Arm + Pauldron + Main Weapon
        val rShoulder = torsoMat.copyOf()
        Math3D.translate(rShoulder, 0.28f * scale, 0.48f * scale, 0f)
        drawWithMatrix(parts.pauldron, rShoulder)
        Math3D.rotateX(rShoulder, pose.rightShoulderPitch)
        Math3D.rotateZ(rShoulder, pose.rightShoulderRoll)
        drawWithMatrix(parts.upperArm, rShoulder)

        val rElbow = rShoulder.copyOf()
        Math3D.translate(rElbow, 0f, -0.28f * scale, 0f)
        Math3D.rotateX(rElbow, pose.rightElbowPitch)
        drawWithMatrix(parts.forearmAndHand, rElbow)

        if (weaponType != null && weaponType != WeaponType.BOW) {
            val wpnMat = rElbow.copyOf()
            Math3D.translate(wpnMat, 0f, -0.32f * scale, 0.06f * scale)
            Math3D.rotateX(wpnMat, 1.45f)
            val wpnMesh = when (weaponType) {
                WeaponType.SWORD -> rig.sword
                WeaponType.GREAT_SWORD -> rig.greatSword
                WeaponType.DUAL_BLADES -> rig.dualBlade
                WeaponType.BOW -> rig.bow
            }
            drawWithMatrix(wpnMesh, wpnMat)
        }

        // Left Leg (Thigh + Shin/Boot)
        val lHip = rootMat.copyOf()
        Math3D.translate(lHip, -0.12f * scale, 0f, 0f)
        Math3D.rotateX(lHip, pose.leftHipPitch)
        drawWithMatrix(parts.thigh, lHip)
        val lKnee = lHip.copyOf()
        Math3D.translate(lKnee, 0f, -0.36f * scale, 0f)
        Math3D.rotateX(lKnee, pose.leftKneePitch)
        drawWithMatrix(parts.shinAndBoot, lKnee)

        // Right Leg (Thigh + Shin/Boot)
        val rHip = rootMat.copyOf()
        Math3D.translate(rHip, 0.12f * scale, 0f, 0f)
        Math3D.rotateX(rHip, pose.rightHipPitch)
        drawWithMatrix(parts.thigh, rHip)
        val rKnee = rHip.copyOf()
        Math3D.translate(rKnee, 0f, -0.36f * scale, 0f)
        Math3D.rotateX(rKnee, pose.rightKneePitch)
        drawWithMatrix(parts.shinAndBoot, rKnee)
    }

    private fun drawWildBeast(en: EnemyInstance) {
        setWorldMaterial(0.75f, 0f, 0f, 0f)
        val root = FloatArray(16)
        Math3D.setIdentity(root)
        Math3D.translate(root, en.position.x, en.position.y, en.position.z)
        Math3D.rotateY(root, en.yaw)
        drawWithMatrix(rig.beastBody, root)

        val head = root.copyOf()
        Math3D.translate(head, 0f, 0.88f, 0.65f)
        drawWithMatrix(rig.beastHead, head)

        val stride = sin(en.animTime * 9f) * if (en.animState == AnimationState.IDLE) 0.05f else 0.55f
        val legOffsets = arrayOf(
            floatArrayOf(-0.22f, 0.62f, 0.42f, stride),
            floatArrayOf(0.22f, 0.62f, 0.42f, -stride),
            floatArrayOf(-0.22f, 0.62f, -0.45f, -stride),
            floatArrayOf(0.22f, 0.62f, -0.45f, stride)
        )
        for (lo in legOffsets) {
            val legM = root.copyOf()
            Math3D.translate(legM, lo[0], lo[1], lo[2])
            Math3D.rotateX(legM, lo[3])
            drawWithMatrix(rig.beastLeg, legM)
        }
    }

    private fun setWorldMaterial(
        roughness: Float,
        emissiveR: Float,
        emissiveG: Float,
        emissiveB: Float,
        tint: FloatArray = floatArrayOf(1f, 1f, 1f)
    ) {
        GLES30.glUniform1f(worldShader.uniform("uRoughness"), roughness)
        GLES30.glUniform3f(worldShader.uniform("uEmissive"), emissiveR, emissiveG, emissiveB)
        GLES30.glUniform3fv(worldShader.uniform("uTint"), 1, tint, 0)
    }

    private fun drawMeshAt(mesh: GpuMesh, x: Float, y: Float, z: Float, yaw: Float, scale: Float) {
        Math3D.setIdentity(modelMat)
        Math3D.translate(modelMat, x, y, z)
        if (yaw != 0f) Math3D.rotateY(modelMat, yaw)
        if (scale != 1f) Math3D.scale(modelMat, scale, scale, scale)
        uploadModelMatrices(worldShader)
        mesh.draw()
    }

    private fun drawWithMatrix(mesh: GpuMesh, m: FloatArray) {
        System.arraycopy(m, 0, modelMat, 0, 16)
        uploadModelMatrices(worldShader)
        mesh.draw()
    }

    private fun uploadModelMatrices(shader: ShaderProgram) {
        Matrix.multiplyMM(mvpMat, 0, camera.viewProjMatrix, 0, modelMat, 0)
        normalMat[0] = modelMat[0]; normalMat[1] = modelMat[1]; normalMat[2] = modelMat[2]
        normalMat[3] = modelMat[4]; normalMat[4] = modelMat[5]; normalMat[5] = modelMat[6]
        normalMat[6] = modelMat[8]; normalMat[7] = modelMat[9]; normalMat[8] = modelMat[10]
        GLES30.glUniformMatrix4fv(shader.uniform("uModel"), 1, false, modelMat, 0)
        GLES30.glUniformMatrix4fv(shader.uniform("uMVP"), 1, false, mvpMat, 0)
        GLES30.glUniformMatrix3fv(shader.uniform("uNormalMat"), 1, false, normalMat, 0)
    }

    fun release() {
        chunkManager.releaseAll()
    }
}
