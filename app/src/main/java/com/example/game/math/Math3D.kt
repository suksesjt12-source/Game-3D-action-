package com.example.game.math

import android.opengl.Matrix
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Math3D {
    const val DEG_TO_RAD = (PI / 180.0).toFloat()
    const val RAD_TO_DEG = (180.0 / PI).toFloat()

    fun clamp(v: Float, min: Float, max: Float): Float =
        if (v < min) min else if (v > max) max else v

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun normalizeAngleRad(angle: Float): Float {
        var a = angle
        while (a > PI.toFloat()) a -= (2f * PI.toFloat())
        while (a < -PI.toFloat()) a += (2f * PI.toFloat())
        return a
    }

    fun lerpAngleRad(from: Float, to: Float, t: Float): Float {
        val diff = normalizeAngleRad(to - from)
        return from + diff * t
    }

    fun yawFromVector(dx: Float, dz: Float): Float = atan2(dx, dz)

    fun setIdentity(m: FloatArray) = Matrix.setIdentityM(m, 0)

    fun translate(m: FloatArray, x: Float, y: Float, z: Float) =
        Matrix.translateM(m, 0, x, y, z)

    fun rotateY(m: FloatArray, rad: Float) =
        Matrix.rotateM(m, 0, rad * RAD_TO_DEG, 0f, 1f, 0f)

    fun rotateX(m: FloatArray, rad: Float) =
        Matrix.rotateM(m, 0, rad * RAD_TO_DEG, 1f, 0f, 0f)

    fun rotateZ(m: FloatArray, rad: Float) =
        Matrix.rotateM(m, 0, rad * RAD_TO_DEG, 0f, 0f, 1f)

    fun scale(m: FloatArray, x: Float, y: Float, z: Float) =
        Matrix.scaleM(m, 0, x, y, z)

    fun length2D(x: Float, z: Float): Float = sqrt(x * x + z * z)
}
