package com.example.game.render

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.example.game.math.Math3D
import com.example.game.world.GameSimulation
import kotlin.math.sqrt

@SuppressLint("ViewConstructor")
class GameGLSurfaceView(
    context: Context,
    private val simulation: GameSimulation,
    val renderer: AetherfallRenderer
) : GLSurfaceView(context) {

    private var nativeW = 0
    private var nativeH = 0
    private var lastResScale = 1.0f

    private var dragPointerId = -1
    private var lastDragX = 0f
    private var lastDragY = 0f
    private var pinchStartDist = 0f
    private var pinchStartCamDist = 6.8f

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && nativeW == 0) {
            nativeW = w
            nativeH = h
            applyResolutionScale(simulation.settings.graphicsQuality.resolutionScale)
        }
    }

    fun applyResolutionScale(scale: Float) {
        val clamped = scale.coerceIn(0.55f, 1.0f)
        if (nativeW <= 0 || nativeH <= 0 || kotlin.math.abs(clamped - lastResScale) < 0.02f) return
        lastResScale = clamped
        val targetW = (nativeW * clamped).toInt().coerceAtLeast(480)
        val targetH = (nativeH * clamped).toInt().coerceAtLeast(270)
        holder.setFixedSize(targetW, targetH)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (renderer.isMenuPanoramaMode) return false
        val camSens = simulation.settings.cameraSensitivity * 0.0048f

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                if (event.pointerCount == 2) {
                    pinchStartDist = pointerDist(event)
                    pinchStartCamDist = simulation.cameraDistance
                } else if (dragPointerId == -1) {
                    dragPointerId = event.getPointerId(idx)
                    lastDragX = event.getX(idx)
                    lastDragY = event.getY(idx)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2 && pinchStartDist > 10f) {
                    val newDist = pointerDist(event)
                    val delta = (pinchStartDist - newDist) * 0.015f
                    simulation.cameraDistance = Math3D.clamp(pinchStartCamDist + delta, 3.6f, 12.2f)
                } else {
                    val idx = event.findPointerIndex(dragPointerId)
                    if (idx >= 0) {
                        val x = event.getX(idx)
                        val y = event.getY(idx)
                        val dx = x - lastDragX
                        val dy = y - lastDragY
                        lastDragX = x
                        lastDragY = y
                        simulation.cameraYaw -= dx * camSens
                        simulation.cameraPitch = Math3D.clamp(simulation.cameraPitch + dy * camSens * 0.8f, -0.15f, 1.15f)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val pid = event.getPointerId(event.actionIndex)
                if (pid == dragPointerId) dragPointerId = -1
                if (event.pointerCount <= 2) pinchStartDist = 0f
            }
        }
        return true
    }

    private fun pointerDist(e: MotionEvent): Float {
        if (e.pointerCount < 2) return 0f
        val dx = e.getX(0) - e.getX(1)
        val dy = e.getY(0) - e.getY(1)
        return sqrt(dx * dx + dy * dy)
    }
}
