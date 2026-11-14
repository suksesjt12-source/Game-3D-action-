package com.example.game.render

import android.opengl.Matrix
import com.example.game.math.Math3D
import com.example.game.math.TerrainMath
import com.example.game.model.Vec3
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Smooth third-person camera with touch rotation, zoom, terrain collision avoidance,
 * combat framing adjustment, and screen-space projection for floating damage numbers.
 */
class ThirdPersonCamera {
    val viewMatrix = FloatArray(16)
    val projMatrix = FloatArray(16)
    val viewProjMatrix = FloatArray(16)

    val eyePos = Vec3(0f, 6f, 24f)
    val targetPos = Vec3(0f, 3f, 16f)

    private var smoothDist = 6.8f
    private var viewportW = 1280
    private var viewportH = 720
    private val rng = Random(7)

    fun setViewport(width: Int, height: Int) {
        viewportW = width.coerceAtLeast(1)
        viewportH = height.coerceAtLeast(1)
        val aspect = viewportW.toFloat() / viewportH.toFloat()
        Matrix.perspectiveM(projMatrix, 0, 56f, aspect, 0.25f, 280f)
    }

    fun updateFollow(
        playerPos: Vec3,
        yaw: Float,
        pitch: Float,
        desiredDistance: Float,
        inCombat: Boolean,
        shakeAmount: Float,
        dt: Float
    ) {
        val targetDist = (desiredDistance + if (inCombat) 1.3f else 0f).coerceIn(3.6f, 12.5f)
        smoothDist = Math3D.lerp(smoothDist, targetDist, (dt * 7f).coerceAtMost(1f))

        val focusY = playerPos.y + 1.65f
        targetPos.x = Math3D.lerp(targetPos.x, playerPos.x, (dt * 12f).coerceAtMost(1f))
        targetPos.y = Math3D.lerp(targetPos.y, focusY, (dt * 10f).coerceAtMost(1f))
        targetPos.z = Math3D.lerp(targetPos.z, playerPos.z, (dt * 12f).coerceAtMost(1f))

        val cp = cos(pitch)
        val sp = sin(pitch)
        val sy = sin(yaw)
        val cy = cos(yaw)

        var camX = targetPos.x - sy * cp * smoothDist
        var camY = targetPos.y + sp * smoothDist
        var camZ = targetPos.z - cy * cp * smoothDist

        // Terrain collision clamping so camera never clips underground
        val groundH = TerrainMath.getHeight(camX, camZ)
        camY = max(camY, groundH + 0.85f)

        // Screen shake on heavy strikes or boss slams
        if (shakeAmount > 0.005f) {
            camX += (rng.nextFloat() - 0.5f) * shakeAmount * 0.7f
            camY += (rng.nextFloat() - 0.5f) * shakeAmount * 0.7f
            camZ += (rng.nextFloat() - 0.5f) * shakeAmount * 0.7f
        }

        eyePos.x = camX
        eyePos.y = camY
        eyePos.z = camZ

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyePos.x, eyePos.y, eyePos.z,
            targetPos.x, targetPos.y, targetPos.z,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)
    }

    fun updateMenuPanorama(time: Float) {
        val angle = time * 0.14f
        val radius = 36f
        val cx = sin(angle) * radius
        val cz = cos(angle) * radius
        val cy = TerrainMath.getHeight(cx, cz) + 13f
        eyePos.x = cx; eyePos.y = cy; eyePos.z = cz
        targetPos.x = 0f; targetPos.y = 4.5f; targetPos.z = -12f

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyePos.x, eyePos.y, eyePos.z,
            targetPos.x, targetPos.y, targetPos.z,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)
    }

    /**
     * Projects a 3D world point to normalized [0..1, 0..1] screen coordinates for Compose HUD indicators.
     */
    fun projectToScreenNorm(worldPos: Vec3): Pair<Float, Float>? {
        val inp = floatArrayOf(worldPos.x, worldPos.y, worldPos.z, 1f)
        val out = FloatArray(4)
        Matrix.multiplyMV(out, 0, viewProjMatrix, 0, inp, 0)
        if (out[3] <= 0.1f) return null
        val ndcX = out[0] / out[3]
        val ndcY = out[1] / out[3]
        if (ndcX < -1.1f || ndcX > 1.1f || ndcY < -1.1f || ndcY > 1.1f) return null
        return Pair((ndcX + 1f) * 0.5f, (1f - ndcY) * 0.5f)
    }
}
