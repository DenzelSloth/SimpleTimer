package com.denzelsloth.simpletimer.data

import com.denzelsloth.simpletimer.SimpleTimerMod
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.Identifier
import java.io.IOException
import java.nio.file.Files
import java.util.Properties

object TimerPersistence {
    private val PATH = FabricLoader.getInstance()
        .configDir
        .resolve("simpletimer-timers.properties")

    fun save(timers: Collection<ActiveTimer>) {
        val properties = Properties()
        val list = timers.toList()
        properties.setProperty("count", list.size.toString())

        list.forEachIndexed { i, timer ->
            val prefix = "timer.$i."
            properties.setProperty("${prefix}name", timer.name)
            properties.setProperty("${prefix}slot", timer.slot.toString())
            properties.setProperty("${prefix}durationMillis", timer.durationMillis.toString())
            properties.setProperty("${prefix}endsAtMillis", timer.endsAtMillis.toString())
            properties.setProperty("${prefix}muted", timer.isMuted.toString())
            properties.setProperty("${prefix}waypoint", timer.hasWaypoint().toString())
            if (timer.hasWaypoint()) {
                properties.setProperty("${prefix}x", timer.waypointX.toString())
                properties.setProperty("${prefix}y", timer.waypointY.toString())
                properties.setProperty("${prefix}z", timer.waypointZ.toString())
                properties.setProperty("${prefix}dimension", timer.waypointDimension.toString())
            }
        }

        try {
            Files.createDirectories(PATH.parent)
            Files.newOutputStream(PATH).use { out ->
                properties.store(out, "Simple Timer active timers (wall-clock)")
            }
        } catch (e: IOException) {
            SimpleTimerMod.LOGGER.warn("Failed to save timers", e)
        }
    }

    fun load(): List<ActiveTimer> {
        val loaded = mutableListOf<ActiveTimer>()
        if (!Files.isRegularFile(PATH)) return loaded

        val properties = Properties()
        try {
            Files.newInputStream(PATH).use { properties.load(it) }
        } catch (e: IOException) {
            SimpleTimerMod.LOGGER.warn("Failed to load timers", e)
            return loaded
        }

        val count = properties.getProperty("count", "0").toIntOrNull() ?: return loaded

        for (i in 0 until count) {
            try {
                val prefix = "timer.$i."
                val name = properties.getProperty("${prefix}name") ?: continue
                val slot = properties.getProperty("${prefix}slot", "0").toInt()
                val durationMillis = properties.getProperty("${prefix}durationMillis", "0").toLong()
                val endsAtMillis = properties.getProperty("${prefix}endsAtMillis", "0").toLong()
                var waypoint = properties.getProperty("${prefix}waypoint", "false").toBoolean()
                var x = 0.0; var y = 0.0; var z = 0.0
                var dimension: Identifier? = null
                if (waypoint) {
                    x = properties.getProperty("${prefix}x", "0").toDouble()
                    y = properties.getProperty("${prefix}y", "0").toDouble()
                    z = properties.getProperty("${prefix}z", "0").toDouble()
                    val dim = properties.getProperty("${prefix}dimension")
                    if (!dim.isNullOrBlank()) {
                        dimension = Identifier.parse(dim)
                    } else {
                        waypoint = false
                    }
                }
                if (name.isBlank() || slot < 1 || slot > TimerManager.MAX_TIMERS || durationMillis < 1L) continue
                val muted = properties.getProperty("${prefix}muted", "false").toBoolean()
                val timer = ActiveTimer.restore(name, slot, durationMillis, endsAtMillis, waypoint, x, y, z, dimension)
                timer.applyMute(muted)
                loaded.add(timer)
            } catch (_: IllegalArgumentException) {
                // Skip bad entries
            }
        }
        return loaded
    }
}
