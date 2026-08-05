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

    private var alertText: String? = null
    private var alertExpiresAt = 0L
    private var alertColor = COLOR_SPAWN

    fun register() {
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            SimpleTimerMod.id("fullscreen_alert"),
            ::onRender
        )
    }

    fun show(text: String, durationMillis: Long, color: Int = COLOR_SPAWN) {
        alertText = text
        alertExpiresAt = System.currentTimeMillis() + durationMillis
        alertColor = color
    }

    fun showSpawnAlert(text: String) {
        show(text, 3000L, COLOR_SPAWN)
    }

    fun showWarningAlert(text: String) {
        show(text, 3000L, COLOR_WARNING)
    }

    private fun onRender(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        if (!TimerConfig.showFullScreenAlert) return
        val text = alertText ?: return
        val client = Minecraft.getInstance()
        if (client.options.hideGui) return

        val remaining = alertExpiresAt - System.currentTimeMillis()
        if (remaining <= 0) {
            alertText = null
            return
        }

        val fade = if (remaining < FADE_DURATION_MILLIS) remaining / FADE_DURATION_MILLIS.toFloat() else 1f

        val screenWidth = client.window.guiScaledWidth
        val screenHeight = client.window.guiScaledHeight
        val alpha = (255 * fade).toInt()
        val textColor = (alpha shl 24) or (alertColor and 0x00FFFFFF)

        val scale = 3.0f
        val pose = graphics.pose()
        pose.pushMatrix()

        val textWidth = client.font.width(text)
        val scaledTextWidth = textWidth * scale
        val scaledTextHeight = client.font.lineHeight * scale
        val x = (screenWidth - scaledTextWidth) / 2f
        val y = screenHeight * 0.3f - scaledTextHeight / 2f

        pose.translate(x, y)
        pose.scale(scale, scale)
        graphics.text(client.font, text, 0, 0, textColor, true)
        pose.popMatrix()
    }
}
