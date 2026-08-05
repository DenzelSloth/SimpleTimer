package com.denzelsloth.simpletimer.compat

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.fabricmc.loader.api.FabricLoader

class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return if (isClothConfigLoaded()) {
            ConfigScreenFactory { parent -> ClothConfigScreenFactory.create(parent) }
        } else {
            ConfigScreenFactory { parent -> MissingClothConfigScreen.create(parent) }
        }
    }

    companion object {
        fun isClothConfigLoaded(): Boolean {
            val loader = FabricLoader.getInstance()
            return loader.isModLoaded("cloth-config2") || loader.isModLoaded("cloth-config")
        }
    }
}
