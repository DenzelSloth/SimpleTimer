package com.denzelsloth.simpletimer.data

import com.denzelsloth.simpletimer.SimpleTimerMod
import com.denzelsloth.simpletimer.config.TimerConfig
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
        val typeKey = watchlistEntry.lowercase()
        val instKey = instanceKey(typeKey, x, z)
        val now = System.currentTimeMillis()

        // Learn kill-to-spawn interval (shared per mob type)
        val kill = lastKills[instKey]
        if (kill != null) {
            val killToSpawn = now - kill.timeMillis
            if (killToSpawn > 1000L) {
                val previous = killToSpawnIntervals[typeKey]
                if (previous == null) {
                    killToSpawnIntervals[typeKey] = killToSpawn
                    save()
                    notifyLearnedInterval(watchlistEntry, killToSpawn)
                } else {
                    killToSpawnIntervals[typeKey] = (previous + killToSpawn) / 2
                    save()
                }
            }
        }

        // Learn spawn-to-spawn interval (shared per mob type)
        val lastSpawn = lastSpawns[instKey]
        if (lastSpawn != null) {
            val spawnToSpawn = now - lastSpawn
            if (spawnToSpawn > 1000L) {
                val previous = spawnToSpawnIntervals[typeKey]
                spawnToSpawnIntervals[typeKey] = if (previous == null) spawnToSpawn else (previous + spawnToSpawn) / 2
                save()
            }
        }
        lastSpawns[instKey] = now

        // Show "UP" marker while the mob is alive; countdown starts on kill
        createMarkerWaypoint(watchlistEntry, x, y, z, dimension)
    }

    fun onMobKilled(watchlistEntry: String, x: Double, y: Double, z: Double, dimension: Identifier) {
        val typeKey = watchlistEntry.lowercase()
        val instKey = instanceKey(typeKey, x, z)
        lastKills[instKey] = KillRecord(System.currentTimeMillis(), x, y, z, dimension)

        // Start respawn countdown using the best known interval
        val interval = killToSpawnIntervals[typeKey] ?: spawnToSpawnIntervals[typeKey]
        if (interval != null && interval > 0) {
            createSpawnTimer(watchlistEntry, interval, x, y, z, dimension)
        } else {
            if (lastSpawns.keys.any { it.startsWith("$typeKey@") }) {
                notifyNoInterval(watchlistEntry, x, y, z)
            }
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
        lastKills.keys.removeAll { it.startsWith("$key@") }
        lastSpawns.keys.removeAll { it.startsWith("$key@") }
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
        val displayName = instanceDisplayName(name, x, z)
        val slot = findAvailableSlot(displayName) ?: return

        val timer = ActiveTimer.createWithWaypoint(displayName, slot, seconds * 1000L, x, y, z, dimension)
        TimerManager.setTimerDirect(timer)

        Minecraft.getInstance().player?.sendSystemMessage(Component.literal(
            "[SimpleTimer] Spawn timer: \"$displayName\" ~${FormatUtils.formatInterval(intervalMillis)}"
        ))
    }

    private fun createMarkerWaypoint(name: String, x: Double, y: Double, z: Double, dimension: Identifier) {
        val displayName = instanceDisplayName(name, x, z)
        val slot = findAvailableSlot(displayName) ?: return

        val existing = TimerManager.get(slot)
        if (!existing.isEmpty) {
            val timer = existing.get()
            if (timer.name.equals(displayName, ignoreCase = true) && timer.isMarker) return
        }

        val marker = ActiveTimer.createMarkerWaypoint(displayName, slot, x, y, z, dimension)
        TimerManager.setTimerDirect(marker)
    }

    private fun findAvailableSlot(instanceName: String): Int? {
        var firstEmpty: Int? = null
        for (slot in 1..TimerManager.MAX_TIMERS) {
            val existing = TimerManager.get(slot)
            if (existing.isEmpty) {
                if (firstEmpty == null) firstEmpty = slot
            } else if (existing.get().name.equals(instanceName, ignoreCase = true)) {
                return slot
            }
        }
        return firstEmpty
    }

    private fun snapCoord(v: Double): Int {
        val grid = TimerConfig.spawnGridSize
        return Math.floorDiv(v.toInt(), grid) * grid
    }

    private fun instanceKey(typeKey: String, x: Double, z: Double): String {
        return "$typeKey@${snapCoord(x)},${snapCoord(z)}"
    }

    private fun instanceDisplayName(name: String, x: Double, z: Double): String {
        return "$name [${snapCoord(x)}, ${snapCoord(z)}]"
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
