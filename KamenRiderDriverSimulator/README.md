# Kamen Rider Driver Simulator

A lightweight Android game simulator for Kamen Rider Driver (belt) interactions.
Built with Kotlin, Jetpack Compose, MVVM, and Sceneform 3D.

---

## Architecture

```
com.kamenrider.simulator
├── common/
│   ├── base/          BaseManager interface
│   ├── event/         InputEvent, GameAction sealed classes
│   └── manager/
│       ├── InputManager          – gesture → InputEvent
│       ├── ActionManager         – InputEvent → GameAction pipeline
│       └── TransformationManager – henshin orchestrator
├── sound/
│   └── SoundManager              – SoundPool wrapper
├── animation/
│   └── AnimationManager          – animation command dispatcher
├── object3d/
│   ├── ModelLoader               – Sceneform asset loader + cache
│   └── SceneController           – SceneView lifecycle + model swap
├── data/
│   ├── model/   RiderItem, Driver, TransformationForm, etc.
│   ├── config/  GameConfig (all static data in one place)
│   └── repository/  DriverRepository
├── di/
│   └── AppModule / EngineInitializer
├── ui/theme/  Compose theme
└── view/
    ├── navigation/ AppNavigation + Routes
    ├── home/       HomeScreen + HomeViewModel
    ├── drivers/    DriversScreen + DriversViewModel
    ├── items/      ItemsScreen + ItemsViewModel
    ├── show/       ShowScreen + ShowViewModel  ← main game screen
    └── components/ GameButton, BeltView, ItemCard, DriverCard, ScreenEffects
```

---

## Input → Action → Execution Pipeline

```
User gesture
    │
    ▼
InputManager.onTap/onSwipeEnd/onDrag…
    │  emits InputEvent
    ▼
ActionManager (collects InputEvents)
    │  looks up mapping table
    │  emits GameAction(s)
    ▼
ActionFlow collectors:
    ├─ ShowViewModel    → calls TransformationManager
    ├─ SoundManager     → plays sound
    └─ AnimationManager → plays animation
```

---

## Transformation Flow (Ex-Aid Level 2 example)

```
1. User taps ItemCard → ItemsViewModel.onItemInserted("gashat_mighty_action_x", slot=0)
2. TransformationManager.insertItem()
3. User presses HENSHIN button on BeltView
4. ShowViewModel.onHenshinPressed()
   → ActionManager.dispatchAction(TriggerTransformation("exaid_level2"))
5. ShowViewModel.handleAction() resolves form from repository
6. TransformationManager.triggerTransformation("exaid_level2")
7. TransformationManager executes concurrently:
   ├─ SoundManager.play("gashat_insert", delayMs=0)
   ├─ SoundManager.play("belt_standby", delayMs=400)
   ├─ SoundManager.play("mighty_action_x_jingle", delayMs=800)
   ├─ SoundManager.play("exaid_henshin", delayMs=2200)
   ├─ AnimationManager.playSequence([belt_pulse, screen_flash, rider_appear])
   └─ SceneController.showModel("exaid_level2")
8. TransformationState → Completed
9. UI: screen flash (pink), BeltView glows, artwork fades in
```

---

## Adding a New Rider

1. Add entries to `GameConfig`:
   - `drivers` list
   - `items` list
   - `forms` list (with sound + animation sequences)

2. Place assets:
   - `assets/sound/<soundId>.ogg`
   - `assets/object3d/<modelId>.sfb` or `.glb`
   - `assets/image/<imageId>.png`

**No code changes required** in any manager or screen.

---

## Build & Run

```bash
# Prerequisites: Android Studio Hedgehog+, JDK 17, Android SDK 35

# Open KamenRiderDriverSimulator/ in Android Studio
# Connect device or start emulator (API 24+)
# Click Run ▶
```

### Key dependencies

| Library | Purpose |
|---------|---------|
| Jetpack Compose BOM 2024.11 | UI |
| Hilt 2.52 | Dependency injection |
| Navigation Compose 2.8 | Screen routing |
| Sceneform 1.17 (gorisse fork) | 3D rendering |
| Media3 ExoPlayer | Audio (upgrade from SoundPool if needed) |
| Coil 2.7 | Image loading |
| Kotlinx Serialization | JSON config |

---

## Asset Sources

Sound: Freesound.org, official Kamen Rider soundtracks (personal use only)
3D models: Sketchfab (check individual licences)
Images: Official artwork (personal/fan use only)
