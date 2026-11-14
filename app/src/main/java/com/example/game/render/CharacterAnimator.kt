package com.example.game.render

import com.example.game.model.AnimationState
import kotlin.math.cos
import kotlin.math.sin

data class CharacterPose(
    val rootOffsetY: Float = 0f,
    val rootPitch: Float = 0f,
    val torsoPitch: Float = 0f,
    val torsoYaw: Float = 0f,
    val leftShoulderPitch: Float = 0f,
    val leftShoulderRoll: Float = 0f,
    val leftElbowPitch: Float = 0f,
    val rightShoulderPitch: Float = 0f,
    val rightShoulderRoll: Float = 0f,
    val rightElbowPitch: Float = 0f,
    val leftHipPitch: Float = 0f,
    val leftKneePitch: Float = 0f,
    val rightHipPitch: Float = 0f,
    val rightKneePitch: Float = 0f,
    val capePitch: Float = 0.15f
)

/**
 * Evaluates smooth procedural skeletal poses for all 21 required character animations.
 */
object CharacterAnimator {

    fun evaluatePose(state: AnimationState, t: Float, comboStep: Int = 1): CharacterPose {
        return when (state) {
            AnimationState.IDLE -> {
                val breath = sin(t * 2.2f)
                CharacterPose(
                    rootOffsetY = breath * 0.02f,
                    leftShoulderPitch = breath * 0.05f,
                    rightShoulderPitch = -breath * 0.05f,
                    leftElbowPitch = -0.18f,
                    rightElbowPitch = -0.22f,
                    capePitch = 0.12f + breath * 0.04f
                )
            }
            AnimationState.WALKING -> {
                val c = sin(t * 6.5f)
                val s = cos(t * 6.5f)
                CharacterPose(
                    rootOffsetY = kotlin.math.abs(c) * 0.04f,
                    torsoPitch = 0.06f,
                    leftHipPitch = c * 0.55f,
                    rightHipPitch = -c * 0.55f,
                    leftKneePitch = kotlin.math.max(0f, -s) * 0.45f,
                    rightKneePitch = kotlin.math.max(0f, s) * 0.45f,
                    leftShoulderPitch = -c * 0.45f,
                    rightShoulderPitch = c * 0.45f,
                    leftElbowPitch = -0.25f,
                    rightElbowPitch = -0.25f,
                    capePitch = 0.24f + kotlin.math.abs(c) * 0.08f
                )
            }
            AnimationState.RUNNING -> {
                val c = sin(t * 10.5f)
                val s = cos(t * 10.5f)
                CharacterPose(
                    rootOffsetY = kotlin.math.abs(c) * 0.09f,
                    torsoPitch = 0.18f,
                    leftHipPitch = c * 0.90f,
                    rightHipPitch = -c * 0.90f,
                    leftKneePitch = kotlin.math.max(0f, -s) * 0.85f,
                    rightKneePitch = kotlin.math.max(0f, s) * 0.85f,
                    leftShoulderPitch = -c * 0.82f,
                    rightShoulderPitch = c * 0.82f,
                    leftElbowPitch = -0.75f,
                    rightElbowPitch = -0.75f,
                    capePitch = 0.45f + sin(t * 9f) * 0.12f
                )
            }
            AnimationState.SPRINTING -> {
                val c = sin(t * 14.5f)
                val s = cos(t * 14.5f)
                CharacterPose(
                    rootOffsetY = kotlin.math.abs(c) * 0.12f,
                    torsoPitch = 0.34f,
                    leftHipPitch = c * 1.15f,
                    rightHipPitch = -c * 1.15f,
                    leftKneePitch = kotlin.math.max(0f, -s) * 1.05f,
                    rightKneePitch = kotlin.math.max(0f, s) * 1.05f,
                    leftShoulderPitch = -c * 1.05f,
                    rightShoulderPitch = c * 1.05f,
                    leftElbowPitch = -0.95f,
                    rightElbowPitch = -0.95f,
                    capePitch = 0.72f + sin(t * 14f) * 0.16f
                )
            }
            AnimationState.JUMPING -> CharacterPose(
                rootOffsetY = 0.08f,
                torsoPitch = 0.10f,
                leftHipPitch = -0.55f,
                leftKneePitch = 0.85f,
                rightHipPitch = 0.25f,
                rightKneePitch = 0.40f,
                leftShoulderPitch = -0.85f,
                rightShoulderPitch = -0.85f,
                capePitch = 0.55f
            )
            AnimationState.FALLING -> CharacterPose(
                torsoPitch = -0.05f,
                leftShoulderRoll = -0.8f,
                rightShoulderRoll = 0.8f,
                leftHipPitch = -0.25f,
                rightHipPitch = 0.20f,
                leftKneePitch = 0.35f,
                rightKneePitch = 0.45f,
                capePitch = 0.92f
            )
            AnimationState.LANDING -> CharacterPose(
                rootOffsetY = -0.14f,
                torsoPitch = 0.28f,
                leftHipPitch = -0.65f,
                rightHipPitch = -0.65f,
                leftKneePitch = 0.95f,
                rightKneePitch = 0.95f,
                leftShoulderPitch = 0.35f,
                rightShoulderPitch = 0.35f,
                capePitch = 0.38f
            )
            AnimationState.DODGING -> CharacterPose(
                rootOffsetY = -0.10f,
                torsoPitch = 0.32f,
                torsoYaw = 0.35f,
                leftShoulderPitch = 0.4f,
                rightShoulderPitch = -0.5f,
                leftHipPitch = -0.4f,
                rightHipPitch = 0.5f,
                capePitch = 0.75f
            )
            AnimationState.ROLLING -> CharacterPose(
                rootOffsetY = -0.35f,
                rootPitch = t * 14.5f,
                torsoPitch = 0.55f,
                leftHipPitch = -1.1f,
                rightHipPitch = -1.1f,
                leftKneePitch = 1.4f,
                rightKneePitch = 1.4f,
                leftShoulderPitch = -0.7f,
                rightShoulderPitch = -0.7f,
                capePitch = 0.85f
            )
            AnimationState.LIGHT_ATTACK -> {
                val swing = sin((t * 11f).coerceAtMost(3.14f))
                val dir = if (comboStep % 2 == 0) -1f else 1f
                CharacterPose(
                    torsoPitch = 0.18f,
                    torsoYaw = dir * (0.65f - swing * 1.25f),
                    rightShoulderPitch = -1.35f + swing * 1.15f,
                    rightShoulderRoll = 0.35f * dir,
                    rightElbowPitch = -0.35f,
                    leftShoulderPitch = 0.30f,
                    leftHipPitch = -0.35f,
                    rightHipPitch = 0.35f,
                    capePitch = 0.42f
                )
            }
            AnimationState.HEAVY_ATTACK -> {
                val p = (t * 2.2f).coerceIn(0f, 1f)
                val slam = if (p < 0.45f) -2.1f * (p / 0.45f) else -2.1f + 2.6f * ((p - 0.45f) / 0.55f)
                CharacterPose(
                    torsoPitch = if (p < 0.45f) -0.18f else 0.42f,
                    rightShoulderPitch = slam,
                    leftShoulderPitch = slam * 0.8f,
                    rightElbowPitch = -0.25f,
                    leftHipPitch = -0.35f,
                    rightHipPitch = 0.40f,
                    capePitch = 0.52f
                )
            }
            AnimationState.COMBO_ATTACK -> CharacterPose(
                torsoPitch = 0.25f,
                torsoYaw = t * 12f,
                rightShoulderPitch = -1.05f,
                rightShoulderRoll = 0.95f,
                leftShoulderPitch = -0.65f,
                leftShoulderRoll = -0.85f,
                capePitch = 0.88f
            )
            AnimationState.BLOCK -> CharacterPose(
                rootOffsetY = -0.06f,
                torsoPitch = 0.12f,
                leftShoulderPitch = -1.05f,
                leftShoulderRoll = -0.45f,
                leftElbowPitch = -0.95f,
                rightShoulderPitch = -0.65f,
                rightElbowPitch = -0.65f,
                leftHipPitch = -0.35f,
                rightHipPitch = 0.35f,
                leftKneePitch = 0.45f,
                rightKneePitch = 0.35f
            )
            AnimationState.PARRY -> CharacterPose(
                torsoPitch = -0.10f,
                torsoYaw = -0.45f,
                leftShoulderPitch = -1.45f,
                leftShoulderRoll = -0.85f,
                rightShoulderPitch = -0.95f,
                capePitch = 0.45f
            )
            AnimationState.HIT_REACTION -> CharacterPose(
                torsoPitch = -0.35f,
                torsoYaw = 0.25f,
                leftShoulderPitch = -0.4f,
                rightShoulderPitch = -0.4f,
                capePitch = 0.35f
            )
            AnimationState.STAGGER -> {
                val sway = sin(t * 7f) * 0.22f
                CharacterPose(
                    rootOffsetY = -0.15f,
                    torsoPitch = 0.38f + sway,
                    leftShoulderPitch = 0.55f,
                    rightShoulderPitch = 0.55f,
                    leftKneePitch = 0.55f,
                    rightKneePitch = 0.55f
                )
            }
            AnimationState.DEATH -> CharacterPose(
                rootOffsetY = -0.78f,
                rootPitch = -1.48f,
                leftShoulderPitch = -0.5f,
                rightShoulderPitch = -0.3f,
                capePitch = 0.05f
            )
            AnimationState.CLIMBING -> {
                val c = sin(t * 8f)
                CharacterPose(
                    torsoPitch = 0.25f,
                    leftShoulderPitch = -1.75f + c * 0.55f,
                    rightShoulderPitch = -1.75f - c * 0.55f,
                    leftHipPitch = -0.75f - c * 0.45f,
                    rightHipPitch = -0.75f + c * 0.45f,
                    leftKneePitch = 0.85f,
                    rightKneePitch = 0.85f
                )
            }
            AnimationState.SWIMMING -> {
                val c = sin(t * 6f)
                CharacterPose(
                    rootOffsetY = -0.12f,
                    torsoPitch = 1.15f,
                    leftShoulderPitch = -1.5f + c * 0.8f,
                    rightShoulderPitch = -1.5f - c * 0.8f,
                    leftHipPitch = c * 0.45f,
                    rightHipPitch = -c * 0.45f,
                    capePitch = 0.6f
                )
            }
            AnimationState.INTERACTING -> {
                val reach = sin(t * 5f)
                CharacterPose(
                    torsoPitch = 0.15f,
                    rightShoulderPitch = -0.95f - reach * 0.25f,
                    rightElbowPitch = -0.35f
                )
            }
            AnimationState.COLLECTING_ITEM -> CharacterPose(
                rootOffsetY = -0.25f,
                torsoPitch = 0.55f,
                rightShoulderPitch = -1.15f,
                leftHipPitch = -0.65f,
                leftKneePitch = 0.95f,
                rightHipPitch = -0.45f,
                rightKneePitch = 0.85f
            )
        }
    }
}
