package com.denzelsloth.simpletimer.features.timer

import com.denzelsloth.simpletimer.SimpleTimerMod
import com.denzelsloth.simpletimer.config.TimerConfig
import com.denzelsloth.simpletimer.data.TimerManager
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object TimerKeybinds {
    private val DEFAULT_KEYS = intArrayOf(
        InputConstants.KEY_1, InputConstants.KEY_2, InputConstants.KEY_3,
        InputConstants.KEY_4, InputConstants.KEY_5, InputConstants.KEY_6,
        InputConstants.KEY_7, InputConstants.KEY_8, InputConstants.KEY_9,
        InputConstants.KEY_0
    )

    private val CATEGORY = KeyMapping.Category.register(SimpleTimerMod.id("timers"))
    private val RESET_KEYS = Array<KeyMapping?>(TimerManager.MAX_TIMERS) { null }

    fun register() {
        for (i in 0 until TimerManager.MAX_TIMERS) {
            val slot = i + 1
            RESET_KEYS[i] = KeyMappingHelper.registerKeyMapping(KeyMapping(
                "key.simpletimer.reset_$slot",
                InputConstants.Type.KEYSYM,
                DEFAULT_KEYS[i],
                CATEGORY
            ))
        }
        ClientTickEvents.END_CLIENT_TICK.register { onEndTick(it) }
    }

    private fun onEndTick(client: Minecraft) {
        TimerManager.tick(client)

        if (client.player == null || client.screen != null) return
        if (!TimerConfig.hotkeyResets) {
            for (key in RESET_KEYS) {
                while (key?.consumeClick() == true) { /* drain */ }
            }
            return
        }

        if (!client.hasControlDown()) {
            for (key in RESET_KEYS) {
                while (key?.consumeClick() == true) { /* drain */ }
            }
            return
        }

        for (i in RESET_KEYS.indices) {
            while (RESET_KEYS[i]?.consumeClick() == true) {
                val slot = i + 1
                if (TimerManager.reset(slot)) {
                    val timer = TimerManager.get(slot).orElseThrow()
                    client.gui.setOverlayMessage(
                        Component.literal("Reset \"${timer.displayName()}\" (slot $slot)"),
                        false
                    )
                }
            }
        }
    }
}
