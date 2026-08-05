package com.denzelsloth.simpletimer.data

import com.denzelsloth.simpletimer.SimpleTimerMod
import com.denzelsloth.simpletimer.utils.FormatUtils
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import java.io.IOException
import java.nio.file.Files
import java.util.Properties

object SpawnTracker {
    private val PATH = FabricLoader.getInstance()
        .configDir
        .resolve("simpletimer-spawntimes.properties")

    private val killToSpawnIntervals = LinkedHashMap<String, Long>()
    private val spawnToSpawnIntervals = LinkedHashMap<String, Long>()
    private val lastKills = LinkedHashMap<String, KillRecord>()
    private val lastSpawns = LinkedHashMap<String, Long>()

    fun load() {
        killToSpawnIntervals.clear()
        spawnToSpawnIntervals.clear()
        if (!Files.isRegularFile(PATH)) return
        val properties = Properties()
        try {
            Files.newInputStream(PATH).use { properties.load(it) }
            val count = properties.getProperty("count", "0").toInt()
            for (i in 0 until count) {
                val name = properties.getProperty("spawn.$i.name") ?: continue
                val killToSpawn = properties.getProperty("spawn.$i.killToSpawn", "0").toLong()
                val spawnToSpawn = properties.getProperty("spawn.$i.spawnToSpawn", "0").toLong()
                val key = name.lowercase()
                if (name.isNotBlank() && killToSpawn > 0) killToSpawnIntervals[key] = killToSpawn
                if (name.isNotBlank() && spawnToSpawn > 0) spawnToSpawnIntervals[key] = spawnToSpawn
            }
        } catch (e: Exception) {
            SimpleTimerMod.LOGGER.warn("Failed to load spawn times", e)
        }
    }

    fun save() {
        val properties = Properties()
        val allKeys = (killToSpawnIntervals.keys + spawnToSpawnIntervals.keys).distinct()
        properties.setProperty("count", allKeys.size.toString())
        allKeys.forEachIndexed { i, key ->
            properties.setProperty("spawn.$i.name", key)
            properties.setProperty("spawn.$i.killToSpawn", (killToSpawnIntervals[key] ?: 0).toString())
            properties.setProperty("spawn.$i.spawnToSpawn", (spawnToSpawnIntervals[key] ?: 0).toString())
        }
        try {
            Files.createDirectories(PATH.parent)
            Files.newOutputStream(PATH).use { out ->
                properties.store(out, "Simple Timer learned spawn intervals")
            }
        } catch (e: IOException) {
            SimpleTimerMod.LOGGER.warn("Failed to save spawn times", e)
        }
    }

    fun onMobSpawned(watchlistEntry: String, x: Double, y: Double, z: Double, dimension: Identifier) {
        val key = watchlistEntry.lowercase()
        val now = System.currentTimeMillis()

        // Learn kill-to-spawn interval (respawn cooldown)
        val kill = lastKills[key]
        if (kill != null) {
            val killToSpawn = now - kill.timeMillis
            if (killToSpawn > 1000L) {
                val previous = killToSpawnIntervals[key]
                if (previous == null) {
                    killToSpawnIntervals[key] = killToSpawn
                    save()
                    notifyLearnedInterval(watchlistEntry, killToSpawn)
                } else {
                    killToSpawnIntervals[key] = (previous + killToSpawn) / 2
                    save()
                }
            }
        }

        // Learn spawn-to-spawn interval (full cycle)
        val lastSpawn = lastSpawns[key]
        if (lastSpawn != null) {
            val spawnToSpawn = now - lastSpawn
            if (spawnToSpawn > 1000L) {
                val previous = spawnToSpawnIntervals[key]
                spawnToSpawnIntervals[key] = if (previous == null) spawnToSpawn else (previous + spawnToSpawn) / 2
                save()
            }
        }
        lastSpawns[key] = now

        // If we know spawn-to-spawn, start timer immediately for NEXT spawn
        val cycle = spawnToSpawnIntervals[key]
        if (cycle != null && cycle > 0) {
            createSpawnTimer(watchlistEntry, cycle, x, y, z, dimension)
        }
    }

    fun onMobKilled(watchlistEntry: String, x: Double, y: Double, z: Double, dimension: Identifier) {
        val key = watchlistEntry.lowercase()
        lastKills[key] = KillRecord(System.currentTimeMillis(), x, y, z, dimension)

        // Refine the timer using the more accurate kill-to-spawn interval
        val killToSpawn = killToSpawnIntervals[key]
        if (killToSpawn != null && killToSpawn > 0) {
            createSpawnTimer(watchlistEntry, killToSpawn, x, y, z, dimension)
        } else if (lastSpawns.containsKey(key)) {
            notifyNoInterval(watchlistEntry, x, y, z)
        }
    }

    fun getLearnedInterval(watchlistEntry: String): Long? =
        killToSpawnIntervals[watchlistEntry.lowercase()]

    fun allLearnedIntervals(): Map<String, Long> {
        val all = LinkedHashMap<String, Long>()
        for (key in (killToSpawnIntervals.keys + spawnToSpawnIntervals.keys).distinct()) {
            all[key] = killToSpawnIntervals[key] ?: spawnToSpawnIntervals[key] ?: continue
        }
        return all
    }

    fun clearInterval(watchlistEntry: String) {
        val key = watchlistEntry.lowercase()
        killToSpawnIntervals.remove(key)
        spawnToSpawnIntervals.remove(key)
        lastKills.remove(key)
        lastSpawns.remove(key)
        save()
    }

    fun clearAll() {
        killToSpawnIntervals.clear()
        spawnToSpawnIntervals.clear()
        lastKills.clear()
        lastSpawns.clear()
        save()
    }

    private fun createSpawnTimer(name: String, intervalMillis: Long, x: Double, y: Double, z: Double, dimension: Identifier) {
        val seconds = maxOf(1, (intervalMillis / 1000L).toInt())
        val slot = findAvailableSlot(name) ?: return

        val timer = ActiveTimer.createWithWaypoint(name, slot, seconds * 1000L, x, y, z, dimension)
        TimerManager.setTimerDirect(timer)

        Minecraft.getInstance().player?.sendSystemMessage(Component.literal(
            "[SimpleTimer] Spawn timer: \"$name\" ~${FormatUtils.formatInterval(intervalMillis)}"
        ))
    }

    private fun findAvailableSlot(name: String): Int? {
        for (slot in 1..TimerManager.MAX_TIMERS) {
            val existing = TimerManager.get(slot)
            if (existing.isEmpty) return slot
            if (existing.get().name.equals(name, ignoreCase = true)) return slot
        }
        return null
    }

    private fun notifyLearnedInterval(name: String, intervalMillis: Long) {
        Minecraft.getInstance().player?.sendSystemMessage(Component.literal(
            "[SimpleTimer] Learned respawn time for \"$name\": ${FormatUtils.formatInterval(intervalMillis)}"
        ))
    }

    private fun notifyNoInterval(name: String, x: Double, y: Double, z: Double) {
        Minecraft.getInstance().player?.sendSystemMessage(Component.literal(
            "[SimpleTimer] \"$name\" killed — waiting for respawn to learn interval..."
        ))
    }

    private data class KillRecord(val timeMillis: Long, val x: Double, val y: Double, val z: Double, val dimension: Identifier)
}
