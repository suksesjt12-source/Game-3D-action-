# AETHERFALL: THE LOST FRONTIER

**Aetherfall: The Lost Frontier** is an offline, single-player 3D Open World Action Adventure RPG built natively for Android (OpenGL ES 3.0 + Jetpack Compose + Kotlin Coroutines).

> **Runtime Note:** Built directly for the native Android Gradle / Kotlin / OpenGL ES 3.0 toolchain so the game compiles immediately into a playable Android APK/AAB without requiring external desktop engine binaries.

## Implemented World & Gameplay Systems

- **2 km × 2 km Open World with Chunk Streaming** (`100m × 100m` dynamic chunks loaded/unloaded around the player):
  - **Dawnreach Village** — Timber houses, Blacksmith forge, Merchant stalls, Waypoint Shrine, 7 NPCs with patrol/talking animations.
  - **Greenveil Forest** — Dense pine & broadleaf canopy, river & waterfall cascade, mossy ruins, Forest Goblins, Wild Beasts, Shadow Archers, and **Boss 1: The Forest Tyrant**.
  - **Ashen Highlands** — Volcanic crags & glowing caldera, igneous ore veins, Signal Beacon, Dark Knights, and Ancient Guardians.
  - **Silverlake Valley** — Shimmering water-shader lake, fisher village & wooden docks, boardable **Rowboat**, and central Treasure Island.
  - **Forgotten Kingdom** — Ruined royal avenue, Castle Keep towers, enterable **Catacomb Dungeon Vault**, **Boss 2: The Ancient Colossus**, and **The Fracture Core Altar**.
- **Third-Person Character & 21 Skeletal Animations**: Idle, Walking, Running, Sprinting, Jumping (Double Jump), Falling, Landing, Dodging, Rolling, Light/Heavy/Combo Attacks, Block, Parry, Hit Reaction, Stagger, Death, Climbing, Swimming, Interacting, and Collecting.
- **Combat & 4 Weapon Types**: Sword, Great Sword, Dual Blades, and Bow (3D arrows), featuring 3-hit combos, charged/heavy blows, dodge i-frames, **Perfect Parry** stagger, critical hits, hit-stop, screen shake, slash trails, and 3D->2D floating damage numbers.
- **RPG & Exploration**: 8 Main Quests, 10 Side Quests, 5 Exploration Objectives, 6-branch Skill Tree, 6 Equipment Slots (with visible weapon/helmet/armor changes), Shop & Blacksmith weapon upgrades, World Map with Fast Travel.
- **Audio & Environment**: Real-time procedural PCM Music (5 adaptive themes), biome Ambience, SFX bank, Day/Night Cycle (Morning/Afternoon/Evening/Night + stars), and Weather (Clear/Cloudy/Rain/Fog).
- **Save System & Android Controls**: Crash-safe atomic JSON Save Slots (3 manual + Auto-Save with `.tmp` and `.bak` recovery), multi-touch 360° Virtual Joystick + Camera Drag + Pinch Zoom + Hardware Gamepad support, and Graphics Presets (`Low`, `Medium`, `High` + `30/60 FPS` pacing).

## How to Open & Run

1. Open the project root in **Android Studio** (or build via Gradle CLI).
2. Connect an Android 10+ device (ARM64-v8a / landscape) or launch an emulator.
3. Run `gradle :app:assembleDebug` or click **Run** in Android Studio.
