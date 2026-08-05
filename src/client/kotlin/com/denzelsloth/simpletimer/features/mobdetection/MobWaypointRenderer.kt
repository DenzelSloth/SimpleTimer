package com.denzelsloth.simpletimer.features.mobdetection

import com.denzelsloth.simpletimer.SimpleTimerMod
import com.denzelsloth.simpletimer.config.TimerConfig
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.Camera
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Matrix4f
import org.joml.Vector4f
import kotlin.math.sqrt

object MobWaypointRenderer {
    private const val COLOR_NAME = 0xFFFFAA00.toInt()
    private const val COLOR_BACKGROUND = 0xC0200000.toInt()
    private const val PADDING_X = 4
    private const val PADDING_Y = 2

    private val VIEW_PROJ = Matrix4f()
    private val CLIP = Vector4f()

    fun register() {
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            SimpleTimerMod.id("mob_waypoints"),
            ::onRender
        )
    }

    private fun onRender(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        val client = Minecraft.getInstance()
        if (!TimerConfig.showWaypoints || client.player == null || client.level == null || client.options.hideGui) return

        val detectedMobs = EntityDetector.tracked()
        if (detectedMobs.isEmpty()) return

        val camera = client.gameRenderer.mainCamera
        if (!camera.isInitialized) return

        val dimension = client.level!!.dimension().identifier()
        val cameraPos = camera.position()
        camera.getViewRotationProjectionMatrix(VIEW_PROJ)

        val screenWidth = client.window.guiScaledWidth
        val screenHeight = client.window.guiScaledHeight
        val maxDistanceSq = TimerConfig.waypointDistanceSq
        val height = TimerConfig.waypointHeight

        for (mob in detectedMobs) {
            if (dimension != mob.dimension) continue

            val dx = mob.x - cameraPos.x
            val dy = (mob.y + height + 0.5) - cameraPos.y
            val dz = mob.z - cameraPos.z
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

            drawLabel(graphics, client, mob, screenX, screenY, sqrt(distSq))
        }
    }

    private fun drawLabel(
        graphics: GuiGraphicsExtractor, client: Minecraft,
        mob: DetectedMob, centerX: Int, centerY: Int, distance: Double
    ) {
        val label = "${mob.name}  ${distance.toInt()}m"
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
            TimerConfig.applyWaypointTextOpacity(COLOR_NAME), true
        )

        pose.popMatrix()
    }
}
