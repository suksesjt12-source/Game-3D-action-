package com.example.game.render

import com.example.game.math.TerrainMath
import com.example.game.model.GraphicsQuality
import com.example.game.model.RegionId
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class PropKind {
    PINE_TREE,
    BROADLEAF_TREE,
    ROCK,
    BUSH,
    VILLAGE_HOUSE,
    CASTLE_TOWER,
    RUIN_ARCH,
    WOODEN_DOCK
}

data class PropPlacement(
    val kind: PropKind,
    val x: Float,
    val y: Float,
    val z: Float,
    val yaw: Float,
    val scale: Float
)

data class TerrainChunk(
    val cx: Int,
    val cz: Int,
    val terrainMesh: GpuMesh,
    val waterMesh: GpuMesh?,
    val props: List<PropPlacement>
) {
    fun release() {
        terrainMesh.release()
        waterMesh?.release()
    }
}

/**
 * Dynamic 100m x 100m chunk-based world streamer across the 2km x 2km Aetherfall frontier.
 * Loads/unloads chunks around the player and scales vegetation density with GraphicsQuality.
 */
class WorldChunkManager {
    companion object {
        const val CHUNK_SIZE = 100f
        const val GRID_STEP = 10 // 10m terrain grid per chunk = crisp yet Mali-friendly poly count
    }

    private val loadedChunks = HashMap<Long, TerrainChunk>()
    private val landmarkProps = buildLandmarks()

    private fun chunkKey(cx: Int, cz: Int): Long =
        (cx.toLong() shl 32) xor (cz.toLong() and 0xffffffffL)

    fun updateAround(px: Float, pz: Float, quality: GraphicsQuality): Collection<TerrainChunk> {
        val pcx = floor(px / CHUNK_SIZE).toInt()
        val pcz = floor(pz / CHUNK_SIZE).toInt()
        val radius = when (quality) {
            GraphicsQuality.LOW -> 1
            GraphicsQuality.MEDIUM -> 2
            GraphicsQuality.HIGH -> 2
        }

        val needed = HashSet<Long>()
        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                val cx = pcx + dx
                val cz = pcz + dz
                if (cx in -10..9 && cz in -10..9) {
                    val key = chunkKey(cx, cz)
                    needed.add(key)
                    if (!loadedChunks.containsKey(key)) {
                        loadedChunks[key] = buildChunk(cx, cz)
                    }
                }
            }
        }

        // Unload far chunks to preserve RAM & VRAM on mid-range Android devices
        val iter = loadedChunks.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (!needed.contains(entry.key)) {
                entry.value.release()
                iter.remove()
            }
        }
        return loadedChunks.values
    }

    fun getLandmarks(): List<PropPlacement> = landmarkProps

    fun releaseAll() {
        loadedChunks.values.forEach { it.release() }
        loadedChunks.clear()
    }

    private fun buildChunk(cx: Int, cz: Int): TerrainChunk {
        val startX = cx * CHUNK_SIZE
        val startZ = cz * CHUNK_SIZE
        val cells = (CHUNK_SIZE / GRID_STEP).toInt()
        val vertsPerSide = cells + 1

        val tBuilder = MeshDataBuilder()
        var hasWater = false

        for (iz in 0 until vertsPerSide) {
            for (ix in 0 until vertsPerSide) {
                val wx = startX + ix * GRID_STEP
                val wz = startZ + iz * GRID_STEP
                val wy = TerrainMath.getHeight(wx, wz)
                if (wy < TerrainMath.WATER_LEVEL + 0.5f) hasWater = true
                val (nx, ny, nz) = TerrainMath.getNormal(wx, wz)
                val col = TerrainMath.getTerrainColor(wx, wz, wy, ny)
                tBuilder.addVertex(
                    wx, wy, wz,
                    nx, ny, nz,
                    col[0], col[1], col[2],
                    ix.toFloat() / cells, iz.toFloat() / cells
                )
            }
        }

        for (iz in 0 until cells) {
            for (ix in 0 until cells) {
                val p0 = iz * vertsPerSide + ix
                val p1 = p0 + 1
                val p2 = p0 + vertsPerSide
                val p3 = p2 + 1
                tBuilder.addTriangle(p0, p2, p1)
                tBuilder.addTriangle(p1, p2, p3)
            }
        }

        val waterMesh = if (hasWater) {
            val wb = MeshDataBuilder()
            val wCells = 5
            val wStep = CHUNK_SIZE / wCells
            val wVerts = wCells + 1
            for (iz in 0 until wVerts) {
                for (ix in 0 until wVerts) {
                    val wx = startX + ix * wStep
                    val wz = startZ + iz * wStep
                    wb.addVertex(wx, TerrainMath.WATER_LEVEL, wz, 0f, 1f, 0f, 0.1f, 0.45f, 0.62f, ix.toFloat(), iz.toFloat())
                }
            }
            for (iz in 0 until wCells) {
                for (ix in 0 until wCells) {
                    val p0 = iz * wVerts + ix
                    val p1 = p0 + 1
                    val p2 = p0 + wVerts
                    val p3 = p2 + 1
                    wb.addTriangle(p0, p2, p1)
                    wb.addTriangle(p1, p2, p3)
                }
            }
            wb.build()
        } else null

        val props = generateChunkProps(cx, cz, startX, startZ)
        return TerrainChunk(cx, cz, tBuilder.build(), waterMesh, props)
    }

    private fun generateChunkProps(cx: Int, cz: Int, startX: Float, startZ: Float): List<PropPlacement> {
        val list = ArrayList<PropPlacement>(24)
        var seed = (cx * 73856093) xor (cz * 19349663) xor 0x5f3759df
        fun nextRand(): Float {
            seed = seed * 1664525 + 1013904223
            return ((seed ushr 8) and 0xffffff).toFloat() / 16777215f
        }

        for (i in 0 until 22) {
            val px = startX + nextRand() * CHUNK_SIZE
            val pz = startZ + nextRand() * CHUNK_SIZE
            val py = TerrainMath.getHeight(px, pz)
            val (_, slopeY, _) = TerrainMath.getNormal(px, pz)

            // Keep roads, water, and Dawnreach central plaza clear
            if (py < TerrainMath.WATER_LEVEL + 0.6f) continue
            if (TerrainMath.getRoadFactor(px, pz) > 0.25f) continue
            if (sqrt(px * px + pz * pz) < 34f) continue
            if (slopeY < 0.72f) {
                if (i % 3 == 0) {
                    list.add(PropPlacement(PropKind.ROCK, px, py - 0.3f, pz, nextRand() * 6.28f, 1.1f + nextRand() * 1.3f))
                }
                continue
            }

            val region = TerrainMath.getRegionAt(px, pz)
            when (region) {
                RegionId.GREENVEIL_FOREST -> {
                    val roll = nextRand()
                    val kind = when {
                        roll < 0.44f -> PropKind.PINE_TREE
                        roll < 0.72f -> PropKind.BROADLEAF_TREE
                        roll < 0.88f -> PropKind.BUSH
                        else -> PropKind.ROCK
                    }
                    list.add(PropPlacement(kind, px, py, pz, nextRand() * 6.28f, 0.85f + nextRand() * 0.65f))
                }
                RegionId.ASHEN_HIGHLANDS -> {
                    val kind = if (nextRand() < 0.72f) PropKind.ROCK else PropKind.BUSH
                    list.add(PropPlacement(kind, px, py, pz, nextRand() * 6.28f, 1.0f + nextRand() * 1.4f))
                }
                RegionId.SILVERLAKE_VALLEY -> {
                    val roll = nextRand()
                    val kind = when {
                        roll < 0.35f -> PropKind.BROADLEAF_TREE
                        roll < 0.72f -> PropKind.BUSH
                        else -> PropKind.ROCK
                    }
                    list.add(PropPlacement(kind, px, py, pz, nextRand() * 6.28f, 0.8f + nextRand() * 0.55f))
                }
                RegionId.FORGOTTEN_KINGDOM -> {
                    if (nextRand() < 0.55f) {
                        list.add(PropPlacement(PropKind.ROCK, px, py, pz, nextRand() * 6.28f, 0.9f + nextRand() * 0.9f))
                    }
                }
                RegionId.DAWNREACH_VILLAGE -> {
                    val roll = nextRand()
                    if (roll < 0.45f) {
                        list.add(PropPlacement(PropKind.BROADLEAF_TREE, px, py, pz, nextRand() * 6.28f, 0.9f + nextRand() * 0.4f))
                    } else if (roll < 0.80f) {
                        list.add(PropPlacement(PropKind.BUSH, px, py, pz, nextRand() * 6.28f, 0.75f + nextRand() * 0.3f))
                    }
                }
            }
        }
        return list
    }

    private fun buildLandmarks(): List<PropPlacement> {
        fun place(kind: PropKind, x: Float, z: Float, yaw: Float, scale: Float = 1f, yOffset: Float = 0f): PropPlacement =
            PropPlacement(kind, x, TerrainMath.getHeight(x, z) + yOffset, z, yaw, scale)

        return listOf(
            // Dawnreach Village Timber Houses
            place(PropKind.VILLAGE_HOUSE, -24f, -14f, 0.35f),
            place(PropKind.VILLAGE_HOUSE, 24f, -12f, -0.45f),
            place(PropKind.VILLAGE_HOUSE, -22f, 22f, 2.3f),
            place(PropKind.VILLAGE_HOUSE, 24f, 22f, -2.5f),
            place(PropKind.VILLAGE_HOUSE, 0f, -34f, 0f),

            // Silverlake Fisher Village Houses & Wooden Dock
            place(PropKind.VILLAGE_HOUSE, 198f, 292f, 0.8f),
            place(PropKind.VILLAGE_HOUSE, 212f, 322f, 0.95f),
            PropPlacement(PropKind.WOODEN_DOCK, 238f, TerrainMath.WATER_LEVEL + 0.35f, 330f, 0.72f, 1f),

            // Greenveil Ancient Ruin Arches
            place(PropKind.RUIN_ARCH, -352f, -78f, 0.5f),
            place(PropKind.RUIN_ARCH, -372f, -96f, 0.5f),

            // Forgotten Kingdom Castle Towers & Royal Ruined Avenue
            place(PropKind.RUIN_ARCH, 0f, -400f, 0f, 1.3f),
            place(PropKind.RUIN_ARCH, 0f, -490f, 0f, 1.3f),
            place(PropKind.CASTLE_TOWER, -38f, -640f, 0f, 1.15f),
            place(PropKind.CASTLE_TOWER, 38f, -640f, 0f, 1.15f),
            place(PropKind.CASTLE_TOWER, -48f, -730f, 0f, 1.25f),
            place(PropKind.CASTLE_TOWER, 48f, -730f, 0f, 1.25f),
            place(PropKind.RUIN_ARCH, 0f, -785f, 0f, 1.4f)
        )
    }
}
