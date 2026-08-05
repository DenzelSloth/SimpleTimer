package com.denzelsloth.simpletimer.features.timer

import com.denzelsloth.simpletimer.SimpleTimerMod
import com.denzelsloth.simpletimer.config.TimerConfig
import com.denzelsloth.simpletimer.data.ActiveTimer
import com.denzelsloth.simpletimer.data.TimerManager
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.Camera
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Matrix4f
import org.joml.Vector4f
import kotlin.math.sqrt

object TimerWaypointRenderer {
    private const val COLOR_ACTIVE = 0xFFFFFFFF.toInt()
    private const val COLOR_WARNING = 0xFFFFFF55.toInt()
    private const val COLOR_EXPIRED = 0xFF55FF55.toInt()
    private const val COLOR_LIVE = 0xFFFF5555.toInt()
    private const val COLOR_BACKGROUND = 0xC0100010.toInt()
    private const val PADDING_X = 3
    private const val PADDING_Y = 2

    private val VIEW_PROJ = Matrix4f()
    private val CLIP = Vector4f()

    fun register() {
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            SimpleTimerMod.id("timer_waypoints"),
            ::onRender
        )
    }

    private fun onRender(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        val client = Minecraft.getInstance()
        if (!TimerConfig.showWaypoints || client.player == null || client.level == null || client.options.hideGui) return

        val camera = client.gameRenderer.mainCamera
        if (!camera.isInitialized) return

        val dimension = client.level!!.dimension().identifier()
        val cameraPos = camera.position()
        camera.getViewRotationProjectionMatrix(VIEW_PROJ)

        val screenWidth = client.window.guiScaledWidth
        val screenHeight = client.window.guiScaledHeight
        val maxDistanceSq = TimerConfig.waypointDistanceSq
        val height = TimerConfig.waypointHeight

        for (timer in TimerManager.timers()) {
            if (!timer.hasWaypoint() || dimension != timer.waypointDimension) continue

            val dx = timer.waypointX - cameraPos.x
            val dy = (timer.waypointY + height) - cameraPos.y
            val dz = timer.waypointZ - cameraPos.z
            val distSq = dx * dx + dy * dy + dz * dz
            if (distSq > maxDistanceSq) continue

            CLIP.set(dx.toFloat(), dy.toFloat(), dz.toFloat(), 1.0f)
            VIEW_PROJ.transform(CLIP)
            if (CLIP.w <= 0.05f) continue

            val ndcX = CLIP.x / CLIP.w
            val ndcY = CLIP.y / CLIP.w
            if (ndcX < -1.2f || ndcX > 1.2f || ndcY < -1.2f || ndcY > 1.2f) continue

            val screenX = ((ndcX + 1.0f) * 0.5f * screenWidth).toInt()
            val screenY = ((1.0f - ndcY) * 0.5f * screenHeight).toInt()

            drawLabel(graphics, client, timer, screenX, screenY, sqrt(distSq))
        }
    }

    private fun drawLabel(graphics: GuiGraphicsExtractor, client: Minecraft, timer: ActiveTimer, centerX: Int, centerY: Int, distance: Double) {
        val distSuffix = if (TimerConfig.waypointShowDistance) "  ${distance.toInt()}m" else ""
        val label = timer.waypointLabel() + distSuffix
        val textWidth = client.font.width(label)
        val textHeight = client.font.lineHeight
        val scale = TimerConfig.waypointSize

        val pose = graphics.pose()
        pose.pushMatrix()
        pose.translate(centerX.toFloat(), centerY.toFloat())
        pose.scale(scale, scale)

        if (TimerConfig.waypointShowBackground) {
            graphics.fill(
                -textWidth / 2 - PADDING_X, -textHeight / 2 - PADDING_Y,
                textWidth / 2 + PADDING_X, textHeight / 2 + PADDING_Y,
                TimerConfig.applyWaypointBackgroundOpacity(COLOR_BACKGROUND)
            )
        }
        graphics.text(
            client.font, label, -textWidth / 2, -textHeight / 2,
            TimerConfig.applyWaypointTextOpacity(colorFor(timer)), true
        )

        pose.popMatrix()
    }

    private fun colorFor(timer: ActiveTimer): Int = when {
        timer.isMarker -> COLOR_LIVE
        timer.isExpired -> COLOR_EXPIRED
        timer.isWarning -> COLOR_WARNING
        else -> COLOR_ACTIVE
    }
}
