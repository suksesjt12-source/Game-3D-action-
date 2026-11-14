package com.example.game.render

import com.example.game.model.EnemyType

data class CharacterPartMeshes(
    val head: GpuMesh,
    val torso: GpuMesh,
    val cape: GpuMesh,
    val upperArm: GpuMesh,
    val forearmAndHand: GpuMesh,
    val thigh: GpuMesh,
    val shinAndBoot: GpuMesh,
    val helmet: GpuMesh,
    val pauldron: GpuMesh
)

data class CharacterVisualRig(
    val humanoidPlayer: CharacterPartMeshes,
    val goblin: CharacterPartMeshes,
    val darkKnight: CharacterPartMeshes,
    val shadowArcher: CharacterPartMeshes,
    val ancientGuardian: CharacterPartMeshes,
    val forestTyrant: CharacterPartMeshes,
    val ancientColossus: CharacterPartMeshes,
    val beastBody: GpuMesh,
    val beastHead: GpuMesh,
    val beastLeg: GpuMesh,
    val sword: GpuMesh,
    val greatSword: GpuMesh,
    val dualBlade: GpuMesh,
    val bow: GpuMesh,
    val arrow: GpuMesh,
    val shield: GpuMesh
) {
    fun partsForEnemy(type: EnemyType): CharacterPartMeshes = when (type) {
        EnemyType.FOREST_GOBLIN -> goblin
        EnemyType.DARK_KNIGHT -> darkKnight
        EnemyType.SHADOW_ARCHER -> shadowArcher
        EnemyType.ANCIENT_GUARDIAN -> ancientGuardian
        EnemyType.FOREST_TYRANT -> forestTyrant
        EnemyType.ANCIENT_COLOSSUS -> ancientColossus
        EnemyType.WILD_BEAST -> goblin
    }
}

object CharacterMeshBuilder {

    fun buildAll(): CharacterVisualRig {
        return CharacterVisualRig(
            humanoidPlayer = buildHumanoidRig(
                skinR = 0.88f, skinG = 0.72f, skinB = 0.60f,
                hairR = 0.22f, hairG = 0.16f, hairB = 0.12f,
                tunicR = 0.18f, tunicG = 0.36f, tunicB = 0.64f,
                armorR = 0.62f, armorG = 0.67f, armorB = 0.75f,
                capeR = 0.14f, capeG = 0.25f, capeB = 0.48f,
                hasPointedEars = false,
                hasHorns = false,
                hasAntlers = false,
                bulkScale = 1.0f
            ),
            goblin = buildHumanoidRig(
                skinR = 0.32f, skinG = 0.56f, skinB = 0.24f,
                hairR = 0.16f, hairG = 0.18f, hairB = 0.12f,
                tunicR = 0.42f, tunicG = 0.28f, tunicB = 0.16f,
                armorR = 0.48f, armorG = 0.38f, armorB = 0.26f,
                capeR = 0.32f, capeG = 0.22f, capeB = 0.12f,
                hasPointedEars = true,
                hasHorns = false,
                hasAntlers = false,
                bulkScale = 0.82f
            ),
            darkKnight = buildHumanoidRig(
                skinR = 0.25f, skinG = 0.24f, skinB = 0.28f,
                hairR = 0.10f, hairG = 0.10f, hairB = 0.12f,
                tunicR = 0.16f, tunicG = 0.15f, tunicB = 0.20f,
                armorR = 0.22f, armorG = 0.23f, armorB = 0.30f,
                capeR = 0.48f, capeG = 0.10f, capeB = 0.14f,
                hasPointedEars = false,
                hasHorns = true,
                hasAntlers = false,
                bulkScale = 1.15f
            ),
            shadowArcher = buildHumanoidRig(
                skinR = 0.46f, skinG = 0.44f, skinB = 0.52f,
                hairR = 0.15f, hairG = 0.14f, hairB = 0.22f,
                tunicR = 0.20f, tunicG = 0.16f, tunicB = 0.34f,
                armorR = 0.30f, armorG = 0.26f, armorB = 0.42f,
                capeR = 0.14f, capeG = 0.11f, capeB = 0.24f,
                hasPointedEars = true,
                hasHorns = false,
                hasAntlers = false,
                bulkScale = 0.95f
            ),
            ancientGuardian = buildHumanoidRig(
                skinR = 0.34f, skinG = 0.38f, skinB = 0.44f,
                hairR = 0.20f, hairG = 0.78f, hairB = 0.95f,
                tunicR = 0.28f, tunicG = 0.32f, tunicB = 0.38f,
                armorR = 0.44f, armorG = 0.48f, armorB = 0.55f,
                capeR = 0.18f, capeG = 0.62f, capeB = 0.85f,
                hasPointedEars = false,
                hasHorns = false,
                hasAntlers = false,
                bulkScale = 1.38f
            ),
            forestTyrant = buildHumanoidRig(
                skinR = 0.28f, skinG = 0.22f, skinB = 0.14f,
                hairR = 0.14f, hairG = 0.58f, hairB = 0.22f,
                tunicR = 0.22f, tunicG = 0.32f, tunicB = 0.16f,
                armorR = 0.32f, armorG = 0.25f, armorB = 0.15f,
                capeR = 0.16f, capeG = 0.46f, capeB = 0.18f,
                hasPointedEars = false,
                hasHorns = false,
                hasAntlers = true,
                bulkScale = 1.85f
            ),
            ancientColossus = buildHumanoidRig(
                skinR = 0.18f, skinG = 0.20f, skinB = 0.28f,
                hairR = 0.68f, hairG = 0.32f, hairB = 0.98f,
                tunicR = 0.22f, tunicG = 0.24f, tunicB = 0.34f,
                armorR = 0.76f, armorG = 0.58f, armorB = 0.20f,
                capeR = 0.48f, capeG = 0.22f, capeB = 0.82f,
                hasPointedEars = false,
                hasHorns = true,
                hasAntlers = false,
                bulkScale = 2.15f
            ),
            beastBody = buildBeastBody(),
            beastHead = buildBeastHead(),
            beastLeg = buildBeastLeg(),
            sword = MeshBuilder.createSwordMesh(1.0f, 0.085f),
            greatSword = MeshBuilder.createSwordMesh(1.55f, 0.15f),
            dualBlade = MeshBuilder.createSwordMesh(0.72f, 0.075f),
            bow = MeshBuilder.createBowMesh(),
            arrow = MeshBuilder.createArrowMesh(),
            shield = buildShieldMesh()
        )
    }

    private fun buildHumanoidRig(
        skinR: Float, skinG: Float, skinB: Float,
        hairR: Float, hairG: Float, hairB: Float,
        tunicR: Float, tunicG: Float, tunicB: Float,
        armorR: Float, armorG: Float, armorB: Float,
        capeR: Float, capeG: Float, capeB: Float,
        hasPointedEars: Boolean,
        hasHorns: Boolean,
        hasAntlers: Boolean,
        bulkScale: Float
    ): CharacterPartMeshes {
        // 1. Head with sculpted cranium, jaw, eyes, nose, hair cap, optional ears/antlers
        val headB = MeshDataBuilder()
        headB.addSphere(0f, 0.15f, 0f, 0.16f * bulkScale, 0.18f * bulkScale, 0.17f * bulkScale, 6, 9, skinR, skinG, skinB)
        // Jaw / chin
        headB.addBox(0f, 0.03f, 0.04f * bulkScale, 0.15f * bulkScale, 0.09f * bulkScale, 0.14f * bulkScale, skinR * 0.95f, skinG * 0.95f, skinB * 0.95f)
        // Eyes & brow ridge
        headB.addBox(-0.055f * bulkScale, 0.16f, 0.145f * bulkScale, 0.042f * bulkScale, 0.026f * bulkScale, 0.025f, 0.12f, 0.14f, 0.22f)
        headB.addBox(0.055f * bulkScale, 0.16f, 0.145f * bulkScale, 0.042f * bulkScale, 0.026f * bulkScale, 0.025f, 0.12f, 0.14f, 0.22f)
        headB.addBox(0f, 0.12f, 0.16f * bulkScale, 0.035f * bulkScale, 0.05f * bulkScale, 0.04f, skinR * 0.9f, skinG * 0.85f, skinB * 0.85f)
        // Hair crown & back flow
        headB.addSphere(0f, 0.22f * bulkScale, -0.02f, 0.175f * bulkScale, 0.14f * bulkScale, 0.185f * bulkScale, 5, 8, hairR, hairG, hairB)
        headB.addBox(0f, 0.13f, -0.14f * bulkScale, 0.22f * bulkScale, 0.16f * bulkScale, 0.08f * bulkScale, hairR, hairG, hairB)
        if (hasPointedEars) {
            headB.addCone(-0.20f * bulkScale, 0.15f, 0f, 0.04f, 0.15f, 5, skinR, skinG, skinB)
            headB.addCone(0.20f * bulkScale, 0.15f, 0f, 0.04f, 0.15f, 5, skinR, skinG, skinB)
        }
        if (hasAntlers) {
            headB.addCone(-0.18f * bulkScale, 0.30f * bulkScale, 0f, 0.06f, 0.48f, 6, 0.22f, 0.88f, 0.36f)
            headB.addCone(0.18f * bulkScale, 0.30f * bulkScale, 0f, 0.06f, 0.48f, 6, 0.22f, 0.88f, 0.36f)
        }

        // 2. Torso with chest armor plate, belt & tunic skirt
        val torsoB = MeshDataBuilder()
        torsoB.addTaperedCylinder(0f, 0f, 0f, 0.19f * bulkScale, 0.25f * bulkScale, 0.54f * bulkScale, 8, tunicR, tunicG, tunicB)
        // Sculpted breastplate & glowing core accent
        torsoB.addBox(0f, 0.34f * bulkScale, 0.06f * bulkScale, 0.42f * bulkScale, 0.30f * bulkScale, 0.26f * bulkScale, armorR, armorG, armorB)
        torsoB.addBox(0f, 0.04f * bulkScale, 0f, 0.40f * bulkScale, 0.09f * bulkScale, 0.28f * bulkScale, 0.36f, 0.22f, 0.10f)
        torsoB.addBox(0f, 0.04f * bulkScale, 0.145f * bulkScale, 0.10f * bulkScale, 0.08f * bulkScale, 0.04f, 0.95f, 0.75f, 0.22f)

        // 3. Flowing Cape / Cloak
        val capeBuilder = MeshDataBuilder()
        capeBuilder.addBox(0f, -0.38f * bulkScale, -0.05f, 0.44f * bulkScale, 0.78f * bulkScale, 0.04f, capeR, capeG, capeB)

        // 4. Upper Arm
        val upperArmB = MeshDataBuilder()
        upperArmB.addTaperedCylinder(0f, -0.28f * bulkScale, 0f, 0.065f * bulkScale, 0.085f * bulkScale, 0.28f * bulkScale, 7, tunicR, tunicG, tunicB)

        // 5. Forearm + Gauntlet + Proportional Hand & Fingers (thumb + 4 fingers)
        val forearmB = MeshDataBuilder()
        forearmB.addTaperedCylinder(0f, -0.24f * bulkScale, 0f, 0.055f * bulkScale, 0.072f * bulkScale, 0.24f * bulkScale, 7, armorR, armorG, armorB)
        // Palm
        forearmB.addBox(0f, -0.29f * bulkScale, 0.01f, 0.07f * bulkScale, 0.08f * bulkScale, 0.05f * bulkScale, skinR, skinG, skinB)
        // Thumb & 4 articulated finger blocks
        forearmB.addBox(0.045f * bulkScale, -0.30f * bulkScale, 0.03f, 0.022f * bulkScale, 0.05f * bulkScale, 0.022f * bulkScale, skinR, skinG, skinB)
        for (f in 0 until 4) {
            val fx = (-0.024f + f * 0.016f) * bulkScale
            forearmB.addBox(fx, -0.36f * bulkScale, 0.01f, 0.014f * bulkScale, 0.065f * bulkScale, 0.02f * bulkScale, skinR, skinG, skinB)
        }

        // 6. Thigh
        val thighB = MeshDataBuilder()
        thighB.addTaperedCylinder(0f, -0.36f * bulkScale, 0f, 0.078f * bulkScale, 0.105f * bulkScale, 0.36f * bulkScale, 7, tunicR * 0.72f, tunicG * 0.72f, tunicB * 0.72f)

        // 7. Shin + Armored Boot
        val shinB = MeshDataBuilder()
        shinB.addTaperedCylinder(0f, -0.36f * bulkScale, 0f, 0.062f * bulkScale, 0.078f * bulkScale, 0.36f * bulkScale, 7, 0.28f, 0.19f, 0.12f)
        shinB.addBox(0f, -0.39f * bulkScale, 0.055f * bulkScale, 0.11f * bulkScale, 0.085f * bulkScale, 0.22f * bulkScale, 0.24f, 0.16f, 0.10f)

        // 8. Optional Helmet & Pauldron
        val helmB = MeshDataBuilder()
        helmB.addSphere(0f, 0.20f * bulkScale, 0f, 0.19f * bulkScale, 0.19f * bulkScale, 0.20f * bulkScale, 6, 8, armorR, armorG, armorB)
        helmB.addBox(0f, 0.14f * bulkScale, 0.17f * bulkScale, 0.18f * bulkScale, 0.06f * bulkScale, 0.06f, armorR * 0.8f, armorG * 0.8f, armorB * 0.8f)
        if (hasHorns) {
            helmB.addCone(-0.18f * bulkScale, 0.26f * bulkScale, 0f, 0.045f, 0.24f * bulkScale, 5, 0.92f, 0.82f, 0.62f)
            helmB.addCone(0.18f * bulkScale, 0.26f * bulkScale, 0f, 0.045f, 0.24f * bulkScale, 5, 0.92f, 0.82f, 0.62f)
        }

        val pauldronB = MeshDataBuilder()
        pauldronB.addSphere(0f, 0.02f, 0f, 0.13f * bulkScale, 0.10f * bulkScale, 0.13f * bulkScale, 5, 7, armorR, armorG, armorB)

        return CharacterPartMeshes(
            head = headB.build(),
            torso = torsoB.build(),
            cape = capeBuilder.build(),
            upperArm = upperArmB.build(),
            forearmAndHand = forearmB.build(),
            thigh = thighB.build(),
            shinAndBoot = shinB.build(),
            helmet = helmB.build(),
            pauldron = pauldronB.build()
        )
    }

    private fun buildBeastBody(): GpuMesh {
        val b = MeshDataBuilder()
        b.addSphere(0f, 0.72f, 0f, 0.34f, 0.32f, 0.68f, 6, 8, 0.26f, 0.22f, 0.20f, 0.08f)
        // Spiked ruff & tail
        b.addCone(0f, 0.76f, -0.78f, 0.10f, 0.45f, 5, 0.20f, 0.17f, 0.16f)
        return b.build()
    }

    private fun buildBeastHead(): GpuMesh {
        val b = MeshDataBuilder()
        b.addSphere(0f, 0f, 0.1f, 0.22f, 0.20f, 0.25f, 5, 7, 0.28f, 0.24f, 0.22f)
        b.addBox(0f, -0.05f, 0.32f, 0.16f, 0.12f, 0.24f, 0.22f, 0.18f, 0.16f)
        b.addCone(-0.14f, 0.16f, 0f, 0.06f, 0.18f, 4, 0.22f, 0.18f, 0.16f)
        b.addCone(0.14f, 0.16f, 0f, 0.06f, 0.18f, 4, 0.22f, 0.18f, 0.16f)
        b.addBox(-0.08f, 0.06f, 0.26f, 0.04f, 0.03f, 0.03f, 0.98f, 0.25f, 0.18f)
        b.addBox(0.08f, 0.06f, 0.26f, 0.04f, 0.03f, 0.03f, 0.98f, 0.25f, 0.18f)
        return b.build()
    }

    private fun buildBeastLeg(): GpuMesh {
        val b = MeshDataBuilder()
        b.addTaperedCylinder(0f, -0.62f, 0f, 0.065f, 0.095f, 0.62f, 6, 0.24f, 0.20f, 0.18f)
        return b.build()
    }

    private fun buildShieldMesh(): GpuMesh {
        val b = MeshDataBuilder()
        b.addBox(0f, 0f, 0f, 0.56f, 0.72f, 0.07f, 0.32f, 0.36f, 0.46f)
        b.addBox(0f, 0f, 0.045f, 0.18f, 0.18f, 0.04f, 0.94f, 0.74f, 0.22f)
        return b.build()
    }
}
