package com.kamenrider.simulator.data.model

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Domain Models
// ---------------------------------------------------------------------------

/**
 * Represents a collectible item that can be inserted into a Driver belt.
 * e.g. Rider Gashat, Lock Seed, Eyecon, Full Bottle, etc.
 */
@Serializable
data class RiderItem(
    val id: String,
    val name: String,
    val description: String,
    val imageAsset: String,       // path under assets/image/
    val modelAsset: String,       // path under assets/object3d/
    val linkedFormId: String,     // which transformation form this unlocks
    val rarity: ItemRarity = ItemRarity.COMMON
)

@Serializable
enum class ItemRarity { COMMON, RARE, SUPER_RARE, LEGENDARY }

/**
 * A Driver (belt) that a Rider uses. One Driver can support multiple forms.
 */
@Serializable
data class Driver(
    val id: String,
    val name: String,
    val description: String,
    val imageAsset: String,
    val modelAsset: String,
    val supportedForms: List<String>,   // list of TransformationForm ids
    val insertSlots: Int = 1            // how many items can be inserted at once
)

/**
 * A Transformation Form – the complete definition of what happens when a
 * Rider transforms.
 */
@Serializable
data class TransformationForm(
    val id: String,
    val name: String,                   // e.g. "Kamen Rider Ex-Aid Level 2"
    val driverId: String,
    val requiredItemIds: List<String>,  // items that must be inserted
    val modelAsset: String,             // 3D model for this form
    val imageAsset: String,             // 2D artwork / thumbnail
    val soundSequence: List<SoundStep>,
    val animationSequence: List<AnimationStep>,
    val uiEffect: UiEffect
)

@Serializable
data class SoundStep(
    val soundId: String,
    val delayMs: Long = 0L,             // delay before playing
    val loop: Boolean = false
)

@Serializable
data class AnimationStep(
    val animationId: String,
    val delayMs: Long = 0L,
    val target: AnimationTarget = AnimationTarget.BELT_UI
)

@Serializable
enum class AnimationTarget { BELT_UI, RIDER_MODEL, SCREEN_EFFECT }

@Serializable
data class UiEffect(
    val flashColor: String = "#FFFFFF",  // hex color for screen flash
    val showParticles: Boolean = true,
    val screenShake: Boolean = true
)
