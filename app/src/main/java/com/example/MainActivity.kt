package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.model.ScreenMode
import com.example.game.render.GameGLSurfaceView
import com.example.game.ui.*
import com.example.ui.theme.AetherfallTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()
    private var glView: GameGLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyImmersiveFullscreen()

        setContent {
            AetherfallTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                // Handle Android Back button across all game screens
                BackHandler(enabled = state.screenMode != ScreenMode.MAIN_MENU) {
                    when (state.screenMode) {
                        ScreenMode.PLAYING -> viewModel.openScreen(ScreenMode.PAUSED)
                        ScreenMode.PAUSED,
                        ScreenMode.INVENTORY,
                        ScreenMode.EQUIPMENT,
                        ScreenMode.SKILLS,
                        ScreenMode.QUESTS,
                        ScreenMode.WORLD_MAP,
                        ScreenMode.DIALOGUE,
                        ScreenMode.SHOP -> viewModel.resumeGame()
                        ScreenMode.SETTINGS -> viewModel.openScreen(
                            if (viewModel.renderer.isMenuPanoramaMode) ScreenMode.MAIN_MENU else ScreenMode.PAUSED
                        )
                        ScreenMode.CREDITS -> viewModel.openScreen(ScreenMode.MAIN_MENU)
                        ScreenMode.GAME_OVER -> viewModel.respawnPlayer()
                        ScreenMode.VICTORY -> viewModel.resumeGame()
                        ScreenMode.MAIN_MENU, ScreenMode.LOADING -> Unit
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Base Native OpenGL ES 3.0 3D World Surface
                    AndroidView(
                        factory = { ctx ->
                            GameGLSurfaceView(ctx, viewModel.simulation, viewModel.renderer).also { surface ->
                                glView = surface
                                viewModel.onResolutionScaleChanged = { scale ->
                                    surface.post { surface.applyResolutionScale(scale) }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Compose Fantasy RPG UI & Virtual Touch Controls
                    when (state.screenMode) {
                        ScreenMode.MAIN_MENU -> MainMenuScreen(viewModel, state)
                        ScreenMode.LOADING -> LoadingScreenOverlay(state.loadingProgress)
                        ScreenMode.PLAYING -> {
                            VirtualGamepadOverlay(viewModel, state)
                            HudOverlay(viewModel, state)
                        }
                        ScreenMode.PAUSED -> PauseMenuScreen(viewModel, state)
                        ScreenMode.INVENTORY -> InventoryScreen(viewModel, state)
                        ScreenMode.EQUIPMENT -> EquipmentScreen(viewModel, state)
                        ScreenMode.SKILLS -> SkillTreeScreen(viewModel, state)
                        ScreenMode.QUESTS -> QuestJournalScreen(viewModel, state)
                        ScreenMode.WORLD_MAP -> WorldMapScreen(viewModel, state)
                        ScreenMode.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            state = state,
                            returnTo = if (viewModel.renderer.isMenuPanoramaMode) ScreenMode.MAIN_MENU else ScreenMode.PAUSED
                        )
                        ScreenMode.DIALOGUE -> DialogueOverlay(viewModel, state)
                        ScreenMode.SHOP -> ShopOverlay(viewModel, state)
                        ScreenMode.CREDITS -> CreditsScreen { viewModel.openScreen(ScreenMode.MAIN_MENU) }
                        ScreenMode.GAME_OVER -> EndStateOverlay(
                            isVictory = false,
                            level = state.level,
                            onPrimaryAction = { viewModel.respawnPlayer() },
                            onMainMenu = { viewModel.openScreen(ScreenMode.MAIN_MENU) }
                        )
                        ScreenMode.VICTORY -> EndStateOverlay(
                            isVictory = true,
                            level = state.level,
                            onPrimaryAction = { viewModel.resumeGame() },
                            onMainMenu = { viewModel.openScreen(ScreenMode.MAIN_MENU) }
                        )
                    }
                }
            }
        }
    }

    private fun applyImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveFullscreen()
    }

    override fun onPause() {
        super.onPause()
        glView?.onPause()
        viewModel.onAppPause()
    }

    override fun onResume() {
        super.onResume()
        glView?.onResume()
        viewModel.onAppResume()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
            if (viewModel.handleGamepadKey(event.keyCode, event.action == KeyEvent.ACTION_DOWN)) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (viewModel.handleGamepadMotion(event)) return true
        return super.dispatchGenericMotionEvent(event)
    }
}
