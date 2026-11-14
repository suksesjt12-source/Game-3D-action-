package com.example.game.render

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GpuMesh(vertices: FloatArray, indices: IntArray) {
    private val vaoId: Int
    private val vboId: Int
    private val iboId: Int
    val indexCount: Int = indices.size

    init {
        val vaos = IntArray(1)
        val bufs = IntArray(2)
        GLES30.glGenVertexArrays(1, vaos, 0)
        GLES30.glGenBuffers(2, bufs, 0)
        vaoId = vaos[0]
        vboId = bufs[0]
        iboId = bufs[1]

        val vb = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vb.put(vertices).position(0)
        val ib = ByteBuffer.allocateDirect(indices.size * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
        ib.put(indices).position(0)

        GLES30.glBindVertexArray(vaoId)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, vb, GLES30.GL_STATIC_DRAW)

        val stride = 11 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, stride, 6 * 4)
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribPointer(3, 2, GLES30.GL_FLOAT, false, stride, 9 * 4)

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, iboId)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.size * 4, ib, GLES30.GL_STATIC_DRAW)
        GLES30.glBindVertexArray(0)
    }

    fun draw(mode: Int = GLES30.GL_TRIANGLES) {
        GLES30.glBindVertexArray(vaoId)
        GLES30.glDrawElements(mode, indexCount, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glBindVertexArray(0)
    }

    fun release() {
        GLES30.glDeleteVertexArrays(1, intArrayOf(vaoId), 0)
        GLES30.glDeleteBuffers(2, intArrayOf(vboId, iboId), 0)
    }
}

class MeshDataBuilder {
    private val verts = ArrayList<Float>(1024)
    private val inds = ArrayList<Int>(1024)

    fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, r: Float, g: Float, b: Float, u: Float, v: Float): Int {
        val idx = verts.size / 11
        verts.add(x); verts.add(y); verts.add(z)
        verts.add(nx); verts.add(ny); verts.add(nz)
        verts.add(r); verts.add(g); verts.add(b)
        verts.add(u); verts.add(v)
        return idx
    }

    fun addTriangle(a: Int, b: Int, c: Int) {
        inds.add(a); inds.add(b); inds.add(c)
    }

    fun addBox(cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float) {
        val hx = sx * 0.5f; val hy = sy * 0.5f; val hz = sz * 0.5f
        val faces = arrayOf(
            floatArrayOf(0f, 0f, 1f, -hx, -hy, hz, hx, -hy, hz, hx, hy, hz, -hx, hy, hz),
            floatArrayOf(0f, 0f, -1f, hx, -hy, -hz, -hx, -hy, -hz, -hx, hy, -hz, hx, hy, -hz),
            floatArrayOf(1f, 0f, 0f, hx, -hy, hz, hx, -hy, -hz, hx, hy, -hz, hx, hy, hz),
            floatArrayOf(-1f, 0f, 0f, -hx, -hy, -hz, -hx, -hy, hz, -hx, hy, hz, -hx, hy, -hz),
            floatArrayOf(0f, 1f, 0f, -hx, hy, hz, hx, hy, hz, hx, hy, -hz, -hx, hy, -hz),
            floatArrayOf(0f, -1f, 0f, -hx, -hy, -hz, hx, -hy, -hz, hx, -hy, hz, -hx, -hy, hz)
        )
        for (f in faces) {
            val base = verts.size / 11
            val nx = f[0]; val ny = f[1]; val nz = f[2]
            addVertex(cx + f[3], cy + f[4], cz + f[5], nx, ny, nz, r, g, b, 0f, 0f)
            addVertex(cx + f[6], cy + f[7], cz + f[8], nx, ny, nz, r, g, b, 1f, 0f)
            addVertex(cx + f[9], cy + f[10], cz + f[11], nx, ny, nz, r, g, b, 1f, 1f)
            addVertex(cx + f[12], cy + f[13], cz + f[14], nx, ny, nz, r, g, b, 0f, 1f)
            addTriangle(base, base + 1, base + 2)
            addTriangle(base, base + 2, base + 3)
        }
    }

    fun addTaperedCylinder(
        cx: Float, baseY: Float, cz: Float,
        bottomR: Float, topR: Float, height: Float,
        segments: Int, r: Float, g: Float, b: Float
    ) {
        val start = verts.size / 11
        for (i in 0..segments) {
            val a = (i.toFloat() / segments) * 2f * PI.toFloat()
            val ca = cos(a); val sa = sin(a)
            addVertex(cx + ca * bottomR, baseY, cz + sa * bottomR, ca, 0.1f, sa, r * 0.85f, g * 0.85f, b * 0.85f, i.toFloat() / segments, 0f)
            addVertex(cx + ca * topR, baseY + height, cz + sa * topR, ca, 0.15f, sa, r, g, b, i.toFloat() / segments, 1f)
        }
        for (i in 0 until segments) {
            val p0 = start + i * 2
            val p1 = p0 + 1
            val p2 = p0 + 2
            val p3 = p0 + 3
            addTriangle(p0, p1, p2)
            addTriangle(p2, p1, p3)
        }
        // Top cap if topR > 0.01f
        if (topR > 0.01f) {
            val capCenter = addVertex(cx, baseY + height, cz, 0f, 1f, 0f, r * 1.05f, g * 1.05f, b * 1.05f, 0.5f, 0.5f)
            for (i in 0 until segments) {
                val p1 = start + i * 2 + 1
                val p3 = start + (i + 1) * 2 + 1
                addTriangle(capCenter, p3, p1)
            }
        }
    }

    fun addCone(cx: Float, baseY: Float, cz: Float, radius: Float, height: Float, segments: Int, r: Float, g: Float, b: Float) {
        addTaperedCylinder(cx, baseY, cz, radius, 0.01f, height, segments, r, g, b)
    }

    fun addSphere(cx: Float, cy: Float, cz: Float, rx: Float, ry: Float, rz: Float, rings: Int, sectors: Int, r: Float, g: Float, b: Float, noiseAmt: Float = 0f) {
        val start = verts.size / 11
        for (i in 0..rings) {
            val phi = PI.toFloat() * i / rings
            val sp = sin(phi); val cp = cos(phi)
            for (j in 0..sectors) {
                val theta = 2f * PI.toFloat() * j / sectors
                val st = sin(theta); val ct = cos(theta)
                val nx = sp * ct; val ny = cp; val nz = sp * st
                val jitter = if (noiseAmt > 0f) 1f + sin(i * 3.1f + j * 2.7f) * noiseAmt else 1f
                val shade = 0.85f + 0.20f * ny
                addVertex(
                    cx + nx * rx * jitter,
                    cy + ny * ry * jitter,
                    cz + nz * rz * jitter,
                    nx, ny, nz,
                    (r * shade).coerceAtMost(1f),
                    (g * shade).coerceAtMost(1f),
                    (b * shade).coerceAtMost(1f),
                    j.toFloat() / sectors,
                    i.toFloat() / rings
                )
            }
        }
        val row = sectors + 1
        for (i in 0 until rings) {
            for (j in 0 until sectors) {
                val p0 = start + i * row + j
                val p1 = p0 + 1
                val p2 = p0 + row
                val p3 = p2 + 1
                addTriangle(p0, p2, p1)
                addTriangle(p1, p2, p3)
            }
        }
    }

    fun build(): GpuMesh = GpuMesh(verts.toFloatArray(), inds.toIntArray())
}

object MeshBuilder {

    fun createSkyDome(): GpuMesh {
        val b = MeshDataBuilder()
        b.addSphere(0f, 0f, 0f, 1f, 1f, 1f, 14, 20, 1f, 1f, 1f)
        return b.build()
    }

    fun createPineTree(): GpuMesh {
        val b = MeshDataBuilder()
        // Detailed bark trunk + root flare
        b.addTaperedCylinder(0f, 0f, 0f, 0.55f, 0.22f, 3.6f, 8, 0.36f, 0.24f, 0.14f)
        // 3 layered foliage tiers
        b.addCone(0f, 2.3f, 0f, 2.4f, 2.8f, 9, 0.11f, 0.36f, 0.13f)
        b.addCone(0f, 4.1f, 0f, 1.9f, 2.5f, 9, 0.14f, 0.44f, 0.16f)
        b.addCone(0f, 5.7f, 0f, 1.3f, 2.2f, 8, 0.18f, 0.52f, 0.19f)
        return b.build()
    }

    fun createBroadleafTree(): GpuMesh {
        val b = MeshDataBuilder()
        b.addTaperedCylinder(0f, 0f, 0f, 0.52f, 0.28f, 3.4f, 8, 0.38f, 0.25f, 0.15f)
        b.addSphere(0f, 4.8f, 0f, 2.3f, 1.8f, 2.2f, 7, 10, 0.18f, 0.48f, 0.16f, 0.12f)
        b.addSphere(-1.1f, 4.1f, 0.8f, 1.4f, 1.2f, 1.4f, 6, 8, 0.15f, 0.42f, 0.14f, 0.1f)
        b.addSphere(1.2f, 4.3f, -0.7f, 1.5f, 1.25f, 1.5f, 6, 8, 0.22f, 0.52f, 0.18f, 0.1f)
        return b.build()
    }

    fun createNaturalRock(): GpuMesh {
        val b = MeshDataBuilder()
        b.addSphere(0f, 0.55f, 0f, 1.25f, 0.85f, 1.15f, 6, 8, 0.42f, 0.41f, 0.40f, 0.22f)
        return b.build()
    }

    fun createBush(): GpuMesh {
        val b = MeshDataBuilder()
        b.addSphere(0f, 0.55f, 0f, 1.1f, 0.72f, 1.05f, 5, 7, 0.17f, 0.44f, 0.15f, 0.15f)
        b.addSphere(0.55f, 0.45f, 0.35f, 0.68f, 0.52f, 0.65f, 4, 6, 0.21f, 0.50f, 0.18f, 0.12f)
        return b.build()
    }

    fun createVillageHouse(): GpuMesh {
        val b = MeshDataBuilder()
        // Stone foundation
        b.addBox(0f, 0.45f, 0f, 7.4f, 0.9f, 6.4f, 0.42f, 0.41f, 0.44f)
        // Plaster walls
        b.addBox(0f, 2.65f, 0f, 6.8f, 3.5f, 5.8f, 0.76f, 0.70f, 0.58f)
        // Timber corner beams
        b.addBox(-3.35f, 2.65f, 2.85f, 0.32f, 3.5f, 0.32f, 0.34f, 0.22f, 0.13f)
        b.addBox(3.35f, 2.65f, 2.85f, 0.32f, 3.5f, 0.32f, 0.34f, 0.22f, 0.13f)
        b.addBox(-3.35f, 2.65f, -2.85f, 0.32f, 3.5f, 0.32f, 0.34f, 0.22f, 0.13f)
        b.addBox(3.35f, 2.65f, -2.85f, 0.32f, 3.5f, 0.32f, 0.34f, 0.22f, 0.13f)
        // Wooden door & warm glowing windows
        b.addBox(0f, 1.85f, 2.95f, 1.25f, 2.1f, 0.16f, 0.36f, 0.21f, 0.12f)
        b.addBox(-1.9f, 2.6f, 2.94f, 0.9f, 0.85f, 0.12f, 0.96f, 0.74f, 0.32f)
        b.addBox(1.9f, 2.6f, 2.94f, 0.9f, 0.85f, 0.12f, 0.96f, 0.74f, 0.32f)
        // Pitched terracotta-shingle roof (4-sided pyramid/gable)
        b.addTaperedCylinder(0f, 4.35f, 0f, 5.2f, 0.4f, 2.6f, 4, 0.52f, 0.22f, 0.16f)
        // Stone chimney
        b.addBox(2.2f, 5.4f, -1.2f, 0.95f, 2.4f, 0.95f, 0.44f, 0.42f, 0.45f)
        return b.build()
    }

    fun createCastleTower(): GpuMesh {
        val b = MeshDataBuilder()
        b.addTaperedCylinder(0f, 0f, 0f, 5.4f, 4.6f, 16f, 12, 0.36f, 0.37f, 0.42f)
        b.addTaperedCylinder(0f, 16f, 0f, 5.1f, 5.1f, 1.6f, 12, 0.30f, 0.31f, 0.36f)
        // Battlement merlons
        for (i in 0 until 8) {
            val a = i * (2f * PI.toFloat() / 8f)
            b.addBox(cos(a) * 4.5f, 18.2f, sin(a) * 4.5f, 1.3f, 1.3f, 1.3f, 0.40f, 0.41f, 0.46f)
        }
        return b.build()
    }

    fun createRuinArch(): GpuMesh {
        val b = MeshDataBuilder()
        b.addBox(-3.6f, 3.2f, 0f, 1.4f, 6.4f, 1.4f, 0.42f, 0.44f, 0.48f)
        b.addBox(3.6f, 3.2f, 0f, 1.4f, 6.4f, 1.4f, 0.42f, 0.44f, 0.48f)
        b.addBox(0f, 6.8f, 0f, 9.2f, 1.2f, 1.6f, 0.38f, 0.40f, 0.45f)
        return b.build()
    }

    fun createWoodenDock(): GpuMesh {
        val b = MeshDataBuilder()
        b.addBox(0f, 0.1f, 0f, 5.2f, 0.35f, 14f, 0.45f, 0.30f, 0.18f)
        for (z in listOf(-6f, -2f, 2f, 6f)) {
            b.addTaperedCylinder(-2.3f, -2.5f, z, 0.25f, 0.22f, 3.4f, 6, 0.32f, 0.20f, 0.12f)
            b.addTaperedCylinder(2.3f, -2.5f, z, 0.25f, 0.22f, 3.4f, 6, 0.32f, 0.20f, 0.12f)
        }
        return b.build()
    }

    fun createRowboat(): GpuMesh {
        val b = MeshDataBuilder()
        b.addBox(0f, 0.15f, 0f, 1.7f, 0.45f, 4.0f, 0.46f, 0.28f, 0.14f)
        b.addBox(-0.82f, 0.48f, 0f, 0.15f, 0.52f, 3.8f, 0.54f, 0.34f, 0.18f)
        b.addBox(0.82f, 0.48f, 0f, 0.15f, 0.52f, 3.8f, 0.54f, 0.34f, 0.18f)
        b.addBox(0f, 0.45f, 0f, 1.5f, 0.15f, 0.45f, 0.36f, 0.22f, 0.10f)
        b.addCone(0f, 0.2f, 2.0f, 0.85f, 1.2f, 4, 0.52f, 0.32f, 0.17f)
        return b.build()
    }

    fun createTreasureChest(): GpuMesh {
        val b = MeshDataBuilder()
        b.addBox(0f, 0.38f, 0f, 1.35f, 0.72f, 0.92f, 0.46f, 0.26f, 0.12f)
        b.addTaperedCylinder(0f, 0.74f, 0f, 0.72f, 0.55f, 0.35f, 6, 0.52f, 0.30f, 0.15f)
        // Golden trim & lock
        b.addBox(0f, 0.52f, 0.48f, 0.26f, 0.30f, 0.12f, 0.96f, 0.76f, 0.18f)
        b.addBox(-0.55f, 0.48f, 0f, 0.10f, 0.85f, 0.98f, 0.88f, 0.68f, 0.15f)
        b.addBox(0.55f, 0.48f, 0f, 0.10f, 0.85f, 0.98f, 0.88f, 0.68f, 0.15f)
        return b.build()
    }

    fun createWaypointObelisk(): GpuMesh {
        val b = MeshDataBuilder()
        b.addBox(0f, 0.3f, 0f, 2.2f, 0.6f, 2.2f, 0.32f, 0.36f, 0.44f)
        b.addTaperedCylinder(0f, 0.6f, 0f, 0.72f, 0.34f, 3.2f, 4, 0.24f, 0.30f, 0.40f)
        // Floating glowing Aether crystal
        b.addCone(0f, 4.3f, 0f, 0.45f, 0.95f, 4, 0.25f, 0.82f, 1.0f)
        b.addCone(0f, 4.3f, 0f, 0.45f, -0.75f, 4, 0.20f, 0.68f, 0.96f)
        return b.build()
    }

    fun createSwordMesh(scaleLen: Float = 1f, bladeWidth: Float = 0.09f): GpuMesh {
        val b = MeshDataBuilder()
        // Pommel & leather grip
        b.addSphere(0f, -0.14f, 0f, 0.06f, 0.06f, 0.06f, 4, 6, 0.92f, 0.72f, 0.22f)
        b.addTaperedCylinder(0f, -0.12f, 0f, 0.035f, 0.035f, 0.28f, 6, 0.26f, 0.16f, 0.10f)
        // Crossguard
        b.addBox(0f, 0.16f, 0f, 0.36f * scaleLen.coerceAtMost(1.3f), 0.05f, 0.08f, 0.92f, 0.72f, 0.22f)
        // Blade
        b.addBox(0f, 0.18f + 0.55f * scaleLen, 0f, bladeWidth, 1.10f * scaleLen, 0.03f, 0.82f, 0.88f, 0.95f)
        b.addCone(0f, 0.18f + 1.10f * scaleLen, 0f, bladeWidth * 0.55f, 0.22f * scaleLen, 4, 0.90f, 0.95f, 1.0f)
        return b.build()
    }

    fun createBowMesh(): GpuMesh {
        val b = MeshDataBuilder()
        b.addBox(0f, 0f, 0f, 0.06f, 0.42f, 0.07f, 0.45f, 0.28f, 0.14f)
        b.addBox(0f, 0.42f, -0.14f, 0.045f, 0.48f, 0.05f, 0.58f, 0.36f, 0.18f)
        b.addBox(0f, -0.42f, -0.14f, 0.045f, 0.48f, 0.05f, 0.58f, 0.36f, 0.18f)
        b.addBox(0f, 0f, -0.26f, 0.015f, 1.24f, 0.015f, 0.92f, 0.92f, 0.88f)
        return b.build()
    }

    fun createArrowMesh(): GpuMesh {
        val b = MeshDataBuilder()
        b.addBox(0f, 0f, 0f, 0.04f, 0.04f, 0.85f, 0.55f, 0.38f, 0.20f)
        b.addBox(0f, 0f, 0.45f, 0.08f, 0.03f, 0.14f, 0.86f, 0.92f, 0.98f)
        return b.build()
    }

    fun createSphereVfx(): GpuMesh {
        val b = MeshDataBuilder()
        b.addSphere(0f, 0f, 0f, 0.5f, 0.5f, 0.5f, 5, 7, 1f, 1f, 1f)
        return b.build()
    }

    fun createRingVfx(): GpuMesh {
        val b = MeshDataBuilder()
        val segs = 24
        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segs) * 2f * PI.toFloat()
            val v0 = b.addVertex(cos(a0) * 0.86f, 0.08f, sin(a0) * 0.86f, 0f, 1f, 0f, 1f, 1f, 1f, 0f, 0f)
            val v1 = b.addVertex(cos(a0) * 1.0f, 0.08f, sin(a0) * 1.0f, 0f, 1f, 0f, 1f, 1f, 1f, 1f, 0f)
            val v2 = b.addVertex(cos(a1) * 0.86f, 0.08f, sin(a1) * 0.86f, 0f, 1f, 0f, 1f, 1f, 1f, 0f, 1f)
            val v3 = b.addVertex(cos(a1) * 1.0f, 0.08f, sin(a1) * 1.0f, 0f, 1f, 0f, 1f, 1f, 1f, 1f, 1f)
            b.addTriangle(v0, v1, v2)
            b.addTriangle(v2, v1, v3)
        }
        return b.build()
    }
}
