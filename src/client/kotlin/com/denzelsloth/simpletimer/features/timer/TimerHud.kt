package com.denzelsloth.simpletimer.features.timer

import com.denzelsloth.simpletimer.SimpleTimerMod
import com.denzelsloth.simpletimer.config.TimerConfig
import com.denzelsloth.simpletimer.data.ActiveTimer
import com.denzelsloth.simpletimer.data.TimerManager
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

object TimerHud {
    private const val COLOR_ACTIVE = 0xFFFFFFFF.toInt()
    private const val COLOR_WARNING = 0xFFFFFF55.toInt()
    private const val COLOR_EXPIRED = 0xFF55FF55.toInt()
    private const val COLOR_LIVE = 0xFFFF5555.toInt()
    private const val COLOR_LABEL = 0xFFAAAAAA.toInt()
    private const val COLOR_BAR_BACKGROUND = 0xC0100010.toInt()
    private const val COLOR_BAR_ACTIVE = 0x8844AAFF.toInt()
    private const val COLOR_BAR_WARNING = 0x88FFCC33.toInt()
    private const val COLOR_BAR_EXPIRED = 0x8855FF55.toInt()
    private const val COLOR_BAR_LIVE = 0x88FF4444.toInt()
    private const val COLOR_MUTED = 0xFFAAAAAA.toInt()
    private const val COLOR_BAR_MUTED = 0x88666666.toInt()

    private const val ROW_HEIGHT_WITH_KEYBINDS = 28
    private const val ROW_HEIGHT_COMPACT = 16
    private const val ROW_PADDING_X = 3
    private const val ROW_PADDING_Y = 4
    private const val MIN_BAR_WIDTH = 90
    private const val GAP = 8
    private const val HIT_PADDING = 4
    private const val DRAG_THRESHOLD_PX = 3
    private const val DOUBLE_CLICK_MILLIS = 400L

    private var pressed = false
    private var pressedSlot = -1
    private var dragging = false
    private var dragMoved = false
    private var dragOffsetX = 0
    private var dragOffsetY = 0
    private var pressHudX = 0
    private var pressHudY = 0
    private var lastClickSlot = -1
    private var lastClickAtMillis = 0L

    fun register() {
        TimerConfig.load()
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            SimpleTimerMod.id("timers"),
            ::onRender
        )
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen is ChatScreen) {
                ScreenMouseEvents.allowMouseClick(screen).register { _, event -> allowMouseClick(event) }
                ScreenMouseEvents.allowMouseRelease(screen).register { _, event -> allowMouseRelease(event) }
            }
        }
    }

    private fun allowMouseClick(event: MouseButtonEvent): Boolean {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true
        return !handleMousePress(event.x(), event.y())
    }

    private fun allowMouseRelease(event: MouseButtonEvent): Boolean {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true
        if (!pressed) return true
        handleRelease()
        return false
    }

    private fun onRender(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        val client = Minecraft.getInstance()
        if (client.player == null || client.options.hideGui) return

        val timers = TimerManager.timers()
        if (timers.isEmpty()) {
            if (dragging) handleRelease()
            return
        }

        val chatOpen = client.screen is ChatScreen
        if (dragging) {
            if (!chatOpen || !TimerConfig.draggable || !isLeftMouseDown(client)) {
                handleRelease()
            } else {
                updateDragPosition(client)
            }
        }

        val scale = TimerConfig.size
        val hudX = TimerConfig.x
        val hudY = TimerConfig.y
        val showKeybinds = TimerConfig.showKeybinds
        val rowHeight = rowHeight(showKeybinds)

        val pose = graphics.pose()
        pose.pushMatrix()
        pose.translate(hudX.toFloat(), hudY.toFloat())
        pose.scale(scale, scale)

        var y = 0
        val barWidth = unscaledContentWidth(client, timers)
        for (timer in timers) {
            drawTimerRow(graphics, client, timer, y, barWidth, showKeybinds)
            y += rowHeight
        }

        pose.popMatrix()
    }

    private fun drawTimerRow(
        graphics: GuiGraphicsExtractor, client: Minecraft,
        timer: ActiveTimer, y: Int, barWidth: Int, showKeybinds: Boolean
    ) {
        val rowHeight = rowHeight(showKeybinds)
        val bottom = y + rowHeight - 1

        if (TimerConfig.showBackground) {
            graphics.fill(0, y, barWidth, bottom, TimerConfig.applyBackgroundOpacity(COLOR_BAR_BACKGROUND))
            val fraction = if (timer.isMarker) 1.0f else if (timer.isExpired) 1.0f else timer.remainingFraction()
            val progressWidth = (barWidth * fraction.coerceIn(0f, 1f)).toInt()
            if (progressWidth > 0) {
                graphics.fill(0, y, progressWidth, bottom, TimerConfig.applyBackgroundOpacity(barColorFor(timer)))
            }
        }

        val left = formatLeft(timer)
        val right = formatRight(timer)
        val textColor = TimerConfig.applyTextOpacity(textColorFor(timer))
        val rightX = barWidth - ROW_PADDING_X - client.font.width(right)

        graphics.text(client.font, left, ROW_PADDING_X, y + ROW_PADDING_Y, textColor, true)
        graphics.text(client.font, right, rightX, y + ROW_PADDING_Y, textColor, true)

        if (showKeybinds) {
            graphics.text(
                client.font, resetHint(timer), ROW_PADDING_X, y + ROW_PADDING_Y + 10,
                TimerConfig.applyTextOpacity(COLOR_LABEL), false
            )
        }
    }

    private fun handleMousePress(mouseX: Double, mouseY: Double): Boolean {
        val client = Minecraft.getInstance()
        val slot = hitTestSlot(client, mouseX, mouseY)
        val bounds = computeBounds(client) ?: return false
        if (!bounds.contains(mouseX, mouseY)) return false

        pressed = true
        pressedSlot = slot

        if (TimerConfig.draggable) {
            dragging = true
            dragMoved = false
            pressHudX = TimerConfig.x
            pressHudY = TimerConfig.y
            dragOffsetX = mouseX.toInt() - TimerConfig.x
            dragOffsetY = mouseY.toInt() - TimerConfig.y
        }
        return true
    }

    private fun updateDragPosition(client: Minecraft) {
        val mouseX = scaledMouseX(client)
        val mouseY = scaledMouseY(client)
        val content = computeContentSize(client) ?: return

        val maxX = maxOf(0, client.window.guiScaledWidth - content.width)
        val maxY = maxOf(0, client.window.guiScaledHeight - content.height)
        val nextX = (mouseX.toInt() - dragOffsetX).coerceIn(0, maxX)
        val nextY = (mouseY.toInt() - dragOffsetY).coerceIn(0, maxY)

        if (Math.abs(nextX - pressHudX) >= DRAG_THRESHOLD_PX || Math.abs(nextY - pressHudY) >= DRAG_THRESHOLD_PX) {
            dragMoved = true
        }
        TimerConfig.setPosition(nextX, nextY)
    }

    private fun handleRelease() {
        val wasDragging = dragging
        dragging = false
        val slot = pressedSlot
        pressed = false
        pressedSlot = -1

        if (wasDragging && dragMoved) {
            TimerConfig.save()
            return
        }

        if (wasDragging) {
            TimerConfig.setPosition(pressHudX, pressHudY)
        }

        if (slot > 0) {
            handleClick(slot)
        }
    }

    private fun handleClick(slot: Int) {
        val now = System.currentTimeMillis()
        val client = Minecraft.getInstance()

        if (slot == lastClickSlot && now - lastClickAtMillis <= DOUBLE_CLICK_MILLIS) {
            lastClickSlot = -1
            lastClickAtMillis = 0L
            if (TimerManager.reset(slot)) {
                val timer = TimerManager.get(slot).orElseThrow()
                client.gui.setOverlayMessage(
                    Component.literal("Reset \"${timer.displayName()}\" (slot $slot)"), false
                )
            }
        } else {
            lastClickSlot = slot
            lastClickAtMillis = now
            val muted = TimerManager.toggleMute(slot)
            if (muted != null) {
                val timer = TimerManager.get(slot).orElseThrow()
                client.gui.setOverlayMessage(
                    Component.literal("${if (muted) "Muted" else "Unmuted"} \"${timer.displayName()}\" (slot $slot)"), false
                )
            }
        }
    }

    private fun hitTestSlot(client: Minecraft, mouseX: Double, mouseY: Double): Int {
        val timers = TimerManager.timers()
        if (timers.isEmpty()) return -1

        val scale = TimerConfig.size
        val localX = (mouseX - TimerConfig.x) / scale
        val localY = (mouseY - TimerConfig.y) / scale
        val width = unscaledContentWidth(client, timers)
        val rowHeight = rowHeight(TimerConfig.showKeybinds)

        var y = 0
        for (timer in timers) {
            val inside = localX >= -HIT_PADDING && localX < width + HIT_PADDING
                && localY >= y - HIT_PADDING && localY < y + rowHeight
            if (inside) return timer.slot
            y += rowHeight
        }
        return -1
    }

    private fun computeBounds(client: Minecraft): HudBounds? {
        val content = computeContentSize(client) ?: return null
        val pad = (HIT_PADDING * TimerConfig.size).toInt()
        return HudBounds(TimerConfig.x - pad, TimerConfig.y - pad, content.width + pad * 2, content.height + pad * 2)
    }

    private fun computeContentSize(client: Minecraft): HudBounds? {
        val timers = TimerManager.timers()
        if (timers.isEmpty()) return null
        val scale = TimerConfig.size
        val width = (unscaledContentWidth(client, timers) * scale).toInt()
        val height = (timers.size * rowHeight(TimerConfig.showKeybinds) * scale).toInt()
        return HudBounds(0, 0, width, height)
    }

    private fun unscaledContentWidth(client: Minecraft, timers: Collection<ActiveTimer>): Int {
        val showKeybinds = TimerConfig.showKeybinds
        var maxLeft = 0
        var maxRight = 0
        var maxHint = 0
        for (timer in timers) {
            maxLeft = maxOf(maxLeft, client.font.width(formatLeft(timer)))
            maxRight = maxOf(maxRight, client.font.width(formatRight(timer)))
            if (showKeybinds) maxHint = maxOf(maxHint, client.font.width(resetHint(timer)))
        }
        val contentWidth = maxLeft + GAP + maxRight + ROW_PADDING_X * 2
        return maxOf(MIN_BAR_WIDTH, contentWidth, maxHint + ROW_PADDING_X * 2)
    }

    private fun rowHeight(showKeybinds: Boolean): Int =
        if (showKeybinds) ROW_HEIGHT_WITH_KEYBINDS else ROW_HEIGHT_COMPACT

    private fun formatLeft(timer: ActiveTimer): String =
        "[${timer.slot}] ${timer.displayName()}"

    private fun formatRight(timer: ActiveTimer): String {
        val mute = if (timer.isMuted) " [M]" else ""
        return "${timer.formattedRemaining()}$mute"
    }

    private fun resetHint(timer: ActiveTimer): String =
        if (TimerConfig.showClickHints) "Click mute | Double-click reset"
        else "Ctrl/Cmd+${timer.slot} reset"

    private fun textColorFor(timer: ActiveTimer): Int =
        if (timer.isMuted) COLOR_MUTED
        else if (TimerConfig.showBackground) COLOR_ACTIVE
        else colorFor(timer)

    private fun colorFor(timer: ActiveTimer): Int = when {
        timer.isMarker -> COLOR_LIVE
        timer.isExpired -> COLOR_EXPIRED
        timer.isWarning -> COLOR_WARNING
        else -> COLOR_ACTIVE
    }

    private fun barColorFor(timer: ActiveTimer): Int = when {
        timer.isMarker -> COLOR_BAR_LIVE
        timer.isExpired -> COLOR_BAR_EXPIRED
        timer.isWarning -> COLOR_BAR_WARNING
        else -> COLOR_BAR_ACTIVE
    }

    private fun isLeftMouseDown(client: Minecraft): Boolean =
        GLFW.glfwGetMouseButton(client.window.handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS

    private fun scaledMouseX(client: Minecraft): Double =
        client.mouseHandler.xpos() * client.window.guiScaledWidth / client.window.width

    private fun scaledMouseY(client: Minecraft): Double =
        client.mouseHandler.ypos() * client.window.guiScaledHeight / client.window.height

    private data class HudBounds(val x: Int, val y: Int, val width: Int, val height: Int) {
        fun contains(mouseX: Double, mouseY: Double): Boolean =
            mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
    }
}
