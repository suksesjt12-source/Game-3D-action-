package com.example.game.math

import com.example.game.model.RegionId
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Deterministic 2 km x 2 km (-1000..+1000m) open-world heightmap, biome classification,
 * rivers, Silverlake basin, Ashen volcano caldera, and Forgotten Kingdom plateau.
 */
object TerrainMath {
    const val WORLD_HALF_SIZE = 1000f
    const val WATER_LEVEL = -1.2f

    private fun hash2(ix: Int, iz: Int): Float {
        var n = ix * 374761393 + iz * 668265263
        n = (n xor (n shr 13)) * 1274126177
        return ((n xor (n shr 16)) and 0x7fffffff).toFloat() / 2147483647f
    }

    fun valueNoise(x: Float, z: Float): Float {
        val ix = floor(x).toInt()
        val iz = floor(z).toInt()
        val fx = x - ix
        val fz = z - iz
        val ux = fx * fx * (3f - 2f * fx)
        val uz = fz * fz * (3f - 2f * fz)
        val a = hash2(ix, iz)
        val b = hash2(ix + 1, iz)
        val c = hash2(ix, iz + 1)
        val d = hash2(ix + 1, iz + 1)
        return Math3D.lerp(Math3D.lerp(a, b, ux), Math3D.lerp(c, d, ux), uz) * 2f - 1f
    }

    fun fbm(x: Float, z: Float, octaves: Int = 4): Float {
        var sum = 0f
        var amp = 0.55f
        var freq = 1f
        for (i in 0 until octaves) {
            sum += amp * valueNoise(x * freq, z * freq)
            freq *= 2.05f
            amp *= 0.48f
        }
        return sum
    }

    fun getRegionAt(x: Float, z: Float): RegionId {
        val distCenter = sqrt(x * x + z * z)
        if (distCenter < 115f) return RegionId.DAWNREACH_VILLAGE
        if (z < -260f && x > -340f && x < 310f) return RegionId.FORGOTTEN_KINGDOM
        if (x >= 130f && z < -110f) return RegionId.ASHEN_HIGHLANDS
        if (x >= 110f && z >= 80f) return RegionId.SILVERLAKE_VALLEY
        if (x < -100f || z > 120f) return RegionId.GREENVEIL_FOREST
        return RegionId.DAWNREACH_VILLAGE
    }

    fun getHeight(x: Float, z: Float): Float {
        // Base rolling hills
        var h = fbm(x * 0.0045f, z * 0.0045f, 4) * 14f + 2.5f

        // Dawnreach Village flat meadow around (0, 0)
        val dVillage = sqrt(x * x + z * z)
        val villageBlend = 1f - Math3D.smoothstep(45f, 120f, dVillage)
        h = Math3D.lerp(h, 2.0f, villageBlend)

        // Greenveil Forest gently undulating valley + waterfall cliff at (-460, -180)
        val dWaterfall = sqrt((x + 475f) * (x + 475f) + (z + 205f) * (z + 205f))
        if (dWaterfall < 85f) {
            val cliff = (1f - Math3D.smoothstep(22f, 85f, dWaterfall)) * 18f
            h += cliff
        }

        // River channel winding from (-460, -180) -> (-220, 80) -> (260, 320) into Silverlake
        val riverZ = -180f + (x + 460f) * 0.7f + sin(x * 0.015f) * 45f
        if (x in -470f..310f) {
            val distRiver = abs(z - riverZ)
            val riverMask = 1f - Math3D.smoothstep(6f, 24f, distRiver)
            h = Math3D.lerp(h, WATER_LEVEL - 1.6f, riverMask * 0.9f)
        }

        // Silverlake Valley expansive lake centered at (440, 440) with central treasure island at (460, 460)
        val dLake = sqrt((x - 440f) * (x - 440f) + (z - 440f) * (z - 440f))
        val lakeMask = 1f - Math3D.smoothstep(160f, 245f, dLake)
        h = Math3D.lerp(h, WATER_LEVEL - 4.2f, lakeMask)
        val dIsland = sqrt((x - 460f) * (x - 460f) + (z - 460f) * (z - 460f))
        val islandMask = 1f - Math3D.smoothstep(18f, 42f, dIsland)
        h = Math3D.lerp(h, 3.2f, islandMask)

        // Ashen Highlands volcanic peaks & caldera around (560, -540)
        val dVolcano = sqrt((x - 560f) * (x - 560f) + (z + 540f) * (z + 540f))
        val volcanoCone = exp(-(dVolcano * dVolcano) / (240f * 240f)) * 54f
        val calderaDip = exp(-(dVolcano * dVolcano) / (55f * 55f)) * 22f
        h += volcanoCone - calderaDip

        // Ashen Highlands ridge & ravine
        val ashenMask = Math3D.smoothstep(140f, 280f, x) * Math3D.smoothstep(-100f, -260f, z)
        h += ashenMask * (abs(fbm(x * 0.012f, z * 0.012f, 3)) * 12f)

        // Forgotten Kingdom elevated stone plateau around (0, -640)
        val dCitadel = sqrt(x * x + (z + 650f) * (z + 650f))
        val citadelBlend = 1f - Math3D.smoothstep(110f, 210f, dCitadel)
        h = Math3D.lerp(h, 9.5f, citadelBlend)

        // Royal Road from Dawnreach (0, -40) -> Forgotten Kingdom (0, -580)
        if (z in -620f..-35f && abs(x) < 28f) {
            val roadMask = 1f - Math3D.smoothstep(5f, 26f, abs(x))
            val targetRoadY = Math3D.lerp(2.0f, 9.5f, Math3D.smoothstep(-40f, -560f, z))
            h = Math3D.lerp(h, targetRoadY, roadMask * 0.85f)
        }

        // Outer world mountain rim beyond 880m to keep player inside 2km x 2km frontier naturally
        val maxCoord = max(abs(x), abs(z))
        if (maxCoord > 860f) {
            val rim = Math3D.smoothstep(860f, 990f, maxCoord)
            h += rim * 42f
        }

        return h
    }

    fun getNormal(x: Float, z: Float): Triple<Float, Float, Float> {
        val eps = 1.2f
        val hL = getHeight(x - eps, z)
        val hR = getHeight(x + eps, z)
        val hD = getHeight(x, z - eps)
        val hU = getHeight(x, z + eps)
        val nx = hL - hR
        val ny = 2f * eps
        val nz = hD - hU
        val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
        return Triple(nx / len, ny / len, nz / len)
    }

    fun getRoadFactor(x: Float, z: Float): Float {
        // Road North (Dawnreach -> Forgotten Kingdom)
        if (z in -640f..0f && abs(x) < 10f) {
            return 1f - Math3D.smoothstep(3f, 9f, abs(x))
        }
        // Road West (Dawnreach -> Greenveil)
        if (x in -550f..0f && abs(z - x * 0.35f) < 10f) {
            return 1f - Math3D.smoothstep(3f, 9f, abs(z - x * 0.35f))
        }
        // Road Southeast (Dawnreach -> Silverlake)
        if (x in 0f..250f && abs(z - x * 1.25f) < 10f) {
            return 1f - Math3D.smoothstep(3f, 9f, abs(z - x * 1.25f))
        }
        // Road Northeast (Dawnreach -> Ashen Highlands)
        if (x in 0f..500f && abs(z + x * 0.95f) < 10f) {
            return 1f - Math3D.smoothstep(3f, 9f, abs(z + x * 0.95f))
        }
        return 0f
    }

    fun getTerrainColor(x: Float, z: Float, height: Float, slopeY: Float): FloatArray {
        val road = getRoadFactor(x, z)
        val region = getRegionAt(x, z)

        // Base biome color
        var r = 0.20f
        var g = 0.44f
        var b = 0.18f

        when (region) {
            RegionId.DAWNREACH_VILLAGE -> {
                r = 0.28f; g = 0.46f; b = 0.22f
            }
            RegionId.GREENVEIL_FOREST -> {
                val moss = 0.5f + 0.5f * valueNoise(x * 0.05f, z * 0.05f)
                r = Math3D.lerp(0.11f, 0.22f, moss)
                g = Math3D.lerp(0.32f, 0.48f, moss)
                b = Math3D.lerp(0.10f, 0.16f, moss)
            }
            RegionId.ASHEN_HIGHLANDS -> {
                val ash = 0.5f + 0.5f * valueNoise(x * 0.04f, z * 0.04f)
                r = Math3D.lerp(0.24f, 0.36f, ash)
                g = Math3D.lerp(0.19f, 0.23f, ash)
                b = Math3D.lerp(0.18f, 0.21f, ash)
                // Lava glow near volcano caldera
                val dVolcano = sqrt((x - 560f) * (x - 560f) + (z + 540f) * (z + 540f))
                if (dVolcano < 65f) {
                    val glow = 1f - Math3D.smoothstep(20f, 65f, dVolcano)
                    r = Math3D.lerp(r, 0.95f, glow * 0.7f)
                    g = Math3D.lerp(g, 0.32f, glow * 0.7f)
                    b = Math3D.lerp(b, 0.08f, glow * 0.7f)
                }
            }
            RegionId.SILVERLAKE_VALLEY -> {
                r = 0.22f; g = 0.45f; b = 0.26f
            }
            RegionId.FORGOTTEN_KINGDOM -> {
                val stone = 0.5f + 0.5f * valueNoise(x * 0.08f, z * 0.08f)
                r = Math3D.lerp(0.26f, 0.36f, stone)
                g = Math3D.lerp(0.28f, 0.37f, stone)
                b = Math3D.lerp(0.33f, 0.43f, stone)
            }
        }

        // Sandy/pebble shore near water
        if (height < WATER_LEVEL + 1.1f) {
            val shore = 1f - Math3D.smoothstep(WATER_LEVEL - 0.5f, WATER_LEVEL + 1.1f, height)
            r = Math3D.lerp(r, 0.56f, shore * 0.85f)
            g = Math3D.lerp(g, 0.51f, shore * 0.85f)
            b = Math3D.lerp(b, 0.38f, shore * 0.85f)
        }

        // Steep rocky cliffs
        if (slopeY < 0.82f) {
            val rock = 1f - Math3D.smoothstep(0.55f, 0.82f, slopeY)
            r = Math3D.lerp(r, 0.35f, rock)
            g = Math3D.lerp(g, 0.33f, rock)
            b = Math3D.lerp(b, 0.32f, rock)
        }

        // Dirt / Cobble road blending
        if (road > 0.01f) {
            r = Math3D.lerp(r, 0.45f, road * 0.85f)
            g = Math3D.lerp(g, 0.37f, road * 0.85f)
            b = Math3D.lerp(b, 0.26f, road * 0.85f)
        }

        return floatArrayOf(min(1f, r), min(1f, g), min(1f, b))
    }
}
