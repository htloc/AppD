package com.kamenrider.simulator.data.config

import com.kamenrider.simulator.data.model.*

/**
 * Static game configuration – all data is defined here, making it easy to
 * add new Riders / Forms without touching any logic code.
 *
 * In a production app this would be loaded from JSON assets or a remote CMS,
 * but keeping it in-code keeps the sample self-contained.
 */
object GameConfig {

    // ------------------------------------------------------------------
    // Sound IDs – keys used throughout the system.
    // Real files go in app/src/main/assets/sound/
    // ------------------------------------------------------------------
    object Sounds {
        const val HENSHIN_EXAID         = "exaid_henshin"
        const val LEVEL_UP              = "level_up"
        const val GASHAT_INSERT         = "gashat_insert"
        const val BELT_STANDBY          = "belt_standby"
        const val MIGHTY_ACTION_X       = "mighty_action_x_jingle"
        const val RIDER_KICK            = "rider_kick"
        const val CRITICAL_STRIKE       = "critical_strike"
        const val GAME_CLEAR            = "game_clear"
        const val MENU_SELECT           = "menu_select"
        const val MENU_BACK             = "menu_back"
    }

    // ------------------------------------------------------------------
    // Animation IDs
    // ------------------------------------------------------------------
    object Animations {
        const val BELT_PULSE            = "belt_pulse"
        const val BELT_SPIN             = "belt_spin"
        const val SCREEN_FLASH          = "screen_flash"
        const val RIDER_APPEAR          = "rider_appear"
        const val ITEM_INSERT           = "item_insert"
        const val LEVEL_UP_BURST        = "level_up_burst"
        const val IDLE_LOOP             = "idle_loop"
    }

    // ------------------------------------------------------------------
    // Model IDs
    // ------------------------------------------------------------------
    object Models {
        const val EXAID_BASE            = "exaid_base"
        const val EXAID_LEVEL2          = "exaid_level2"
        const val EXAID_LEVEL5          = "exaid_level5"
        const val GASHAT_MIGHTY         = "gashat_mighty_action_x"
        const val GASHAT_TADDLE         = "gashat_taddle_quest"
        const val MIGHTY_DRIVER         = "mighty_driver"
    }

    // ------------------------------------------------------------------
    // Drivers
    // ------------------------------------------------------------------
    val drivers: List<Driver> = listOf(
        Driver(
            id = "mighty_driver",
            name = "Mighty Action X Driver",
            description = "The Gamer Driver used by Kamen Rider Ex-Aid",
            imageAsset = "image/driver_mighty.png",
            modelAsset = "object3d/mighty_driver.sfb",
            supportedForms = listOf("exaid_level2", "exaid_level5"),
            insertSlots = 2
        )
    )

    // ------------------------------------------------------------------
    // Collectible Items
    // ------------------------------------------------------------------
    val items: List<RiderItem> = listOf(
        RiderItem(
            id = "gashat_mighty_action_x",
            name = "Mighty Action X Gashat",
            description = "A Gashat for Mighty Action X, Ex-Aid's primary game.",
            imageAsset = "image/gashat_mighty_action_x.png",
            modelAsset = "object3d/gashat_mighty.sfb",
            linkedFormId = "exaid_level2",
            rarity = ItemRarity.RARE
        ),
        RiderItem(
            id = "gashat_taddle_quest",
            name = "Taddle Quest Gashat",
            description = "A Gashat for the fantasy RPG Taddle Quest.",
            imageAsset = "image/gashat_taddle_quest.png",
            modelAsset = "object3d/gashat_taddle.sfb",
            linkedFormId = "exaid_level5",
            rarity = ItemRarity.SUPER_RARE
        )
    )

    // ------------------------------------------------------------------
    // Transformation Forms
    // ------------------------------------------------------------------
    val forms: List<TransformationForm> = listOf(
        TransformationForm(
            id = "exaid_level2",
            name = "Kamen Rider Ex-Aid Action Gamer Level 2",
            driverId = "mighty_driver",
            requiredItemIds = listOf("gashat_mighty_action_x"),
            modelAsset = "object3d/exaid_level2.sfb",
            imageAsset = "image/exaid_level2.png",
            soundSequence = listOf(
                SoundStep(Sounds.GASHAT_INSERT, delayMs = 0),
                SoundStep(Sounds.BELT_STANDBY, delayMs = 400),
                SoundStep(Sounds.MIGHTY_ACTION_X, delayMs = 800),
                SoundStep(Sounds.HENSHIN_EXAID, delayMs = 2200)
            ),
            animationSequence = listOf(
                AnimationStep(Animations.ITEM_INSERT, delayMs = 0, AnimationTarget.BELT_UI),
                AnimationStep(Animations.BELT_PULSE, delayMs = 400, AnimationTarget.BELT_UI),
                AnimationStep(Animations.SCREEN_FLASH, delayMs = 800, AnimationTarget.SCREEN_EFFECT),
                AnimationStep(Animations.RIDER_APPEAR, delayMs = 2200, AnimationTarget.RIDER_MODEL)
            ),
            uiEffect = UiEffect(
                flashColor = "#FF69B4",
                showParticles = true,
                screenShake = true
            )
        ),
        TransformationForm(
            id = "exaid_level5",
            name = "Kamen Rider Ex-Aid Wizard Gamer Level 5",
            driverId = "mighty_driver",
            requiredItemIds = listOf("gashat_mighty_action_x", "gashat_taddle_quest"),
            modelAsset = "object3d/exaid_level5.sfb",
            imageAsset = "image/exaid_level5.png",
            soundSequence = listOf(
                SoundStep(Sounds.GASHAT_INSERT, delayMs = 0),
                SoundStep(Sounds.GASHAT_INSERT, delayMs = 300),
                SoundStep(Sounds.BELT_STANDBY, delayMs = 600),
                SoundStep(Sounds.LEVEL_UP, delayMs = 1000),
                SoundStep(Sounds.MIGHTY_ACTION_X, delayMs = 1400),
                SoundStep(Sounds.HENSHIN_EXAID, delayMs = 2800)
            ),
            animationSequence = listOf(
                AnimationStep(Animations.ITEM_INSERT, delayMs = 0, AnimationTarget.BELT_UI),
                AnimationStep(Animations.ITEM_INSERT, delayMs = 300, AnimationTarget.BELT_UI),
                AnimationStep(Animations.BELT_SPIN, delayMs = 600, AnimationTarget.BELT_UI),
                AnimationStep(Animations.LEVEL_UP_BURST, delayMs = 1000, AnimationTarget.SCREEN_EFFECT),
                AnimationStep(Animations.SCREEN_FLASH, delayMs = 1400, AnimationTarget.SCREEN_EFFECT),
                AnimationStep(Animations.RIDER_APPEAR, delayMs = 2800, AnimationTarget.RIDER_MODEL)
            ),
            uiEffect = UiEffect(
                flashColor = "#FFD700",
                showParticles = true,
                screenShake = true
            )
        )
    )
}
