package com.denzelsloth.simpletimer.compat

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

object MissingClothConfigScreen {
    fun create(parent: Screen): Screen = ConfirmScreen(
        { _ -> Minecraft.getInstance().setScreen(parent) },
        Component.translatable("text.simpletimer.config.missingCloth.title"),
        Component.translatable("text.simpletimer.config.missingCloth.message"),
        CommonComponents.GUI_DONE,
        CommonComponents.GUI_CANCEL
    )
}
