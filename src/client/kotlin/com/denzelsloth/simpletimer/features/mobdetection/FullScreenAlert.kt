package com.denzelsloth.simpletimer.features.mobdetection

import com.denzelsloth.simpletimer.SimpleTimerMod
import com.denzelsloth.simpletimer.config.TimerConfig
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

object FullScreenAlert {
    private const val COLOR_SPAWN = 0xFFFF5555.toInt()
    private const val COLOR_WARNING = 0xFFFFFF55.toInt()
    private const val FADE_DURATION_MILLIS = 500L
    private const val SPAWN_SCALE = 3.0f
    private const val WARNING_SCALE = 2.0f

    private var spawnText: String? = null
    private var spawnExpiresAt = 0L
    private var spawnSource: String = ""

    private var warningText: String? = null
    private var warningExpiresAt = 0L
    private var warningSource: String = ""

    fun register() {
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            SimpleTimerMod.id("fullscreen_alert"),
            ::onRender
        )
    }

    fun showSpawnAlert(text: String, source: String = text) {
        spawnText = text
        spawnExpiresAt = System.currentTimeMillis() + 3000L
        spawnSource = source
        if (warningSource.equals(source, ignoreCase = true)) {
            warningText = null
        }
    }

    fun showWarningAlert(text: String, source: String = text) {
        warningText = text
        warningExpiresAt = System.currentTimeMillis() + 3000L
        warningSource = source
        if (spawnSource.equals(source, ignoreCase = true)) {
            warningText = null
        }
    }

    private fun onRender(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        if (!TimerConfig.showFullScreenAlert) return
        val client = Minecraft.getInstance()
        if (client.options.hideGui) return

        val screenWidth = client.window.guiScaledWidth
        val screenHeight = client.window.guiScaledHeight
        val now = System.currentTimeMillis()

        val spawnY = screenHeight * 0.30f
        val warningY = screenHeight * 0.22f

        renderAlert(graphics, client, spawnText, spawnExpiresAt, now, COLOR_SPAWN, SPAWN_SCALE, screenWidth, spawnY)?.let {
            spawnText = null
        }
        renderAlert(graphics, client, warningText, warningExpiresAt, now, COLOR_WARNING, WARNING_SCALE, screenWidth, warningY)?.let {
            warningText = null
        }
    }

    private fun renderAlert(
        graphics: GuiGraphicsExtractor, client: Minecraft,
        text: String?, expiresAt: Long, now: Long,
        baseColor: Int, scale: Float, screenWidth: Int, centerY: Float
    ): Unit? {
        text ?: return null
        val remaining = expiresAt - now
        if (remaining <= 0) return Unit

        val fade = if (remaining < FADE_DURATION_MILLIS) remaining / FADE_DURATION_MILLIS.toFloat() else 1f
        val alpha = (255 * fade).toInt()
        val color = (alpha shl 24) or (baseColor and 0x00FFFFFF)

        val textWidth = client.font.width(text)
        val scaledWidth = textWidth * scale
        val scaledHeight = client.font.lineHeight * scale
        val x = (screenWidth - scaledWidth) / 2f
        val y = centerY - scaledHeight / 2f

        val pose = graphics.pose()
        pose.pushMatrix()
        pose.translate(x, y)
        pose.scale(scale, scale)
        graphics.text(client.font, text, 0, 0, color, true)
        pose.popMatrix()
        return null
    }
}
