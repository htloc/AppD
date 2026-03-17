package com.kamenrider.simulator.di

import com.kamenrider.simulator.animation.AnimationManager
import com.kamenrider.simulator.common.manager.ActionManager
import com.kamenrider.simulator.common.manager.InputManager
import com.kamenrider.simulator.common.manager.TransformationManager
import com.kamenrider.simulator.data.repository.DriverRepository
import com.kamenrider.simulator.object3d.ModelLoader
import com.kamenrider.simulator.object3d.SceneController
import com.kamenrider.simulator.sound.SoundManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module – provides all game-engine singletons.
 *
 * Most singletons are already annotated with [@Singleton] and [@Inject],
 * so Hilt handles them automatically. This module provides the dependency
 * graph wiring only where explicit factory methods are needed (i.e., when
 * initialization order matters or manual construction is required).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * EngineInitializer – responsible for calling [BaseManager.initialize]
     * on every manager at app startup.
     *
     * Called from [KamenRiderApp.onCreate].
     */
    @Provides
    @Singleton
    fun provideEngineInitializer(
        inputManager: InputManager,
        actionManager: ActionManager,
        soundManager: SoundManager,
        animationManager: AnimationManager,
        transformationManager: TransformationManager,
        sceneController: SceneController
    ): EngineInitializer = EngineInitializer(
        inputManager,
        actionManager,
        soundManager,
        animationManager,
        transformationManager,
        sceneController
    )
}

/**
 * Tiny helper that initialises all managers in the correct order.
 */
class EngineInitializer(
    private val inputManager: InputManager,
    private val actionManager: ActionManager,
    private val soundManager: SoundManager,
    private val animationManager: AnimationManager,
    private val transformationManager: TransformationManager,
    private val sceneController: SceneController
) {
    fun init() {
        inputManager.initialize()
        soundManager.initialize()
        animationManager.initialize()
        transformationManager.initialize()
        actionManager.initialize()      // must be after inputManager
        sceneController.initialize()
    }

    fun release() {
        sceneController.release()
        actionManager.release()
        transformationManager.release()
        animationManager.release()
        soundManager.release()
        inputManager.release()
    }
}
