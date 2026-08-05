package com.denzelsloth.simpletimer.features.mobdetection

import com.denzelsloth.simpletimer.config.TimerConfig
import com.denzelsloth.simpletimer.data.MobWatchlist
import com.denzelsloth.simpletimer.data.SpawnTracker
import com.denzelsloth.simpletimer.utils.FormatUtils.stripFormatting
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.LivingEntity

object EntityDetector {
    private const val SCAN_INTERVAL_TICKS = 10
    private const val COOLDOWN_MILLIS = 30_000L
    private const val SPAWN_ALARM_SOUND_INTERVAL_MILLIS = 300L

    private var tickCounter = 0
    private var spawnAlarmEndsAt = 0L
    private var lastSpawnAlarmSoundAt = 0L
    private var spawnAlarmTick = false

    private val trackedMobs = LinkedHashMap<Int, DetectedMob>()
    private val detectionCooldowns = LinkedHashMap<String, Long>()

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { onTick(it) }
    }

    fun tracked(): List<DetectedMob> = trackedMobs.values.toList()

    fun clearTracked() { trackedMobs.clear() }

    fun removeTracked(entityId: Int) { trackedMobs.remove(entityId) }

    private fun onTick(client: Minecraft) {
        if (client.level == null || client.player == null) return

        tickSpawnAlarm(client)

        if (MobWatchlist.isEmpty) return

        pruneDeadMobs(client)

        tickCounter++
        if (tickCounter < SCAN_INTERVAL_TICKS) return
        tickCounter = 0

        scanEntities(client)
    }

    private fun scanEntities(client: Minecraft) {
        val player = client.player ?: return
        val level = client.level ?: return
        val dimension = level.dimension().identifier()
        val now = System.currentTimeMillis()

        for (entity in level.entitiesForRendering()) {
            if (entity !is LivingEntity) continue
            if (entity == player) continue
            if (!entity.isAlive) continue
            if (!player.hasLineOfSight(entity)) continue

            val name = entity.displayName.string.stripFormatting()
            if (name.isEmpty()) continue
            if (!MobWatchlist.matches(name)) continue

            val entityId = entity.id
            if (trackedMobs.containsKey(entityId)) continue

            val watchlistEntry = MobWatchlist.matchedEntry(name) ?: continue

            val grid = TimerConfig.spawnGridSize
            val gridX = Math.floorDiv(entity.x.toInt(), grid) * grid
            val gridZ = Math.floorDiv(entity.z.toInt(), grid) * grid
            val cooldownKey = "${watchlistEntry.lowercase()}@$dimension@$gridX,$gridZ"
            val lastDetection = detectionCooldowns[cooldownKey]
            if (lastDetection != null && now - lastDetection < COOLDOWN_MILLIS) continue

            val mob = DetectedMob(
                name = name,
                entityId = entityId,
                x = entity.x,
                y = entity.y,
                z = entity.z,
                dimension = dimension,
                watchlistEntry = watchlistEntry
            )
            trackedMobs[entityId] = mob
            detectionCooldowns[cooldownKey] = now

            SpawnTracker.onMobSpawned(watchlistEntry, entity.x, entity.y, entity.z, dimension)
            notifyPlayer(player, mob)
        }
    }

    private fun pruneDeadMobs(client: Minecraft) {
        val iterator = trackedMobs.entries.iterator()
        while (iterator.hasNext()) {
            val (_, mob) = iterator.next()
            if (mob.dimension != client.level!!.dimension().identifier()) continue

            val entity = client.level!!.getEntity(mob.entityId)
            if (entity == null || !entity.isAlive) {
                mob.markDead()
                iterator.remove()
                SpawnTracker.onMobKilled(mob.watchlistEntry, mob.x, mob.y, mob.z, mob.dimension)
            }
        }
    }

    private fun notifyPlayer(player: LocalPlayer, mob: DetectedMob) {
        player.sendSystemMessage(Component.literal(
            "[SimpleTimer] Detected \"${mob.watchlistEntry}\"!"
        ))

        FullScreenAlert.showSpawnAlert("${mob.watchlistEntry} SPAWNED!", mob.watchlistEntry)

        Minecraft.getInstance().gui.setOverlayMessage(
            Component.literal("Mob detected: ${mob.watchlistEntry}"),
            false
        )
        if (TimerConfig.volume > 0 && TimerConfig.spawnAlarmDurationSeconds > 0) {
            val now = System.currentTimeMillis()
            spawnAlarmEndsAt = now + TimerConfig.spawnAlarmDurationMillis()
            playSpawnAlarmSound(player)
            lastSpawnAlarmSoundAt = now
        }
    }

    private fun tickSpawnAlarm(client: Minecraft) {
        if (spawnAlarmEndsAt <= 0L) return
        val now = System.currentTimeMillis()
        if (now >= spawnAlarmEndsAt) {
            spawnAlarmEndsAt = 0L
            return
        }
        if (TimerConfig.volume <= 0) return
        val player = client.player ?: return
        if (now - lastSpawnAlarmSoundAt >= SPAWN_ALARM_SOUND_INTERVAL_MILLIS) {
            playSpawnAlarmSound(player)
            lastSpawnAlarmSoundAt = now
        }
    }

    private fun playSpawnAlarmSound(player: LocalPlayer) {
        spawnAlarmTick = !spawnAlarmTick
        val pitch = if (spawnAlarmTick) 1.8f else 1.4f
        player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), TimerConfig.volumeScale, pitch)
        player.playSound(SoundEvents.NOTE_BLOCK_BIT.value(), TimerConfig.volumeScale * 0.6f, pitch)
    }
}
