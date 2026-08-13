package com.denzelsloth.simpletimer.data

import com.denzelsloth.simpletimer.SimpleTimerMod
import net.fabricmc.loader.api.FabricLoader
import java.io.IOException
import java.nio.file.Files
import java.util.Properties

object MobWatchlist {
    private val PATH = FabricLoader.getInstance()
        .configDir
        .resolve("simpletimer-watchlist.properties")

    private val names = LinkedHashSet<String>()

    fun load() {
        names.clear()
        if (!Files.isRegularFile(PATH)) return
        val properties = Properties()
        try {
            Files.newInputStream(PATH).use { properties.load(it) }
            val count = properties.getProperty("count", "0").toInt()
            for (i in 0 until count) {
                val name = properties.getProperty("name.$i")
                if (!name.isNullOrBlank()) names.add(name)
            }
        } catch (e: Exception) {
            SimpleTimerMod.LOGGER.warn("Failed to load mob watchlist", e)
        }
    }

    fun save() {
        val properties = Properties()
        val list = names.toList()
        properties.setProperty("count", list.size.toString())
        list.forEachIndexed { i, name -> properties.setProperty("name.$i", name) }
        try {
            Files.createDirectories(PATH.parent)
            Files.newOutputStream(PATH).use { out ->
                properties.store(out, "Simple Spawn Timer mob watchlist")
            }
        } catch (e: IOException) {
            SimpleTimerMod.LOGGER.warn("Failed to save mob watchlist", e)
        }
    }

    fun add(name: String): Boolean {
        if (names.add(name)) { save(); return true }
        return false
    }

    fun remove(name: String): Boolean {
        val removed = names.removeAll { it.equals(name, ignoreCase = true) }
        if (removed) save()
        return removed
    }

    fun clear() { names.clear(); save() }

    fun entries(): List<String> = names.toList()

    val isEmpty: Boolean get() = names.isEmpty()

    val size: Int get() = names.size

    fun matches(strippedName: String): Boolean {
        val lower = strippedName.lowercase()
        return names.any { lower.contains(it.lowercase()) }
    }

    fun matchedEntry(strippedName: String): String? {
        val lower = strippedName.lowercase()
        return names.firstOrNull { lower.contains(it.lowercase()) }
    }
}
