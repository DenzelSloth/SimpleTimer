package com.denzelsloth.simpletimer.data

import com.denzelsloth.simpletimer.config.TimerConfig
import com.denzelsloth.simpletimer.features.mobdetection.FullScreenAlert
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import java.util.*

object TimerManager {
    const val MAX_TIMERS = 999
    const val MAX_HOTKEY_SLOT = 10

    private const val ALARM_SOUND_INTERVAL_MILLIS = 500L
    private const val WARNING_BEEP_COUNT = 2

    private val timersBySlot = LinkedHashMap<Int, ActiveTimer>()
    private val lastAlarmSoundAtMillis = HashMap<Int, Long>()
    private val warningBeepsPlayed = HashMap<Int, Int>()
    private val lastWarningSoundAtMillis = HashMap<Int, Long>()

    fun loadPersisted() {
        timersBySlot.clear()
        clearAllSoundState()
        for (timer in TimerPersistence.load()) {
            timersBySlot[timer.slot] = timer
        }
    }

    fun savePersisted() {
        TimerPersistence.save(timers())
    }

    fun timers(): List<ActiveTimer> =
        timersBySlot.values.sortedBy { it.slot }

    fun get(slot: Int): Optional<ActiveTimer> =
        Optional.ofNullable(timersBySlot[slot])

    fun setTimer(name: String, seconds: Int, slot: Int): ActiveTimer =
        setTimer(name, seconds, slot, waypoint = false, player = null)

    fun setTimer(name: String, seconds: Int, slot: Int, waypoint: Boolean, player: LocalPlayer?): ActiveTimer {
        require(slot in 1..MAX_TIMERS) { "Slot must be between 1 and $MAX_TIMERS" }
        require(seconds >= 1) { "Timer duration must be at least 1 second" }

        val timer = if (waypoint && player?.level() != null) {
            ActiveTimer.createWithWaypoint(
                name, slot, seconds * 1000L,
                player.x, player.y, player.z,
                player.level().dimension().identifier()
            )
        } else {
            ActiveTimer.create(name, slot, seconds * 1000L)
        }
        timersBySlot[slot] = timer
        clearSoundState(slot)
        savePersisted()
        return timer
    }

    fun setTimerDirect(timer: ActiveTimer) {
        val previous = timersBySlot[timer.slot]
        if (previous != null && previous.isMuted) {
            timer.applyMute(true)
        }
        timersBySlot[timer.slot] = timer
        clearSoundState(timer.slot)
        savePersisted()
    }

    fun remove(slot: Int): Boolean {
        clearSoundState(slot)
        val removed = timersBySlot.remove(slot) != null
        if (removed) savePersisted()
        return removed
    }

    fun removeByName(name: String): List<ActiveTimer> {
        val removed = mutableListOf<ActiveTimer>()
        val iterator = timersBySlot.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val timer = entry.value
            if (timer.name.equals(name, ignoreCase = true) || timer.baseName().equals(name, ignoreCase = true)) {
                clearSoundState(entry.key)
                removed.add(timer)
                iterator.remove()
            }
        }
        removed.sortBy { it.slot }
        if (removed.isNotEmpty()) savePersisted()
        return removed
    }

    fun reset(slot: Int): Boolean {
        val timer = timersBySlot[slot] ?: return false
        timer.restart()
        clearSoundState(slot)
        savePersisted()
        return true
    }

    fun toggleMute(slot: Int): Boolean? {
        val timer = timersBySlot[slot] ?: return null
        val muted = timer.toggleMute()
        if (muted) clearSoundState(slot)
        return muted
    }

    fun tick(client: Minecraft) {
        val player = client.player ?: return
        val now = System.currentTimeMillis()

        for (timer in timersBySlot.values) {
            if (!timer.isMuted) tickWarningSounds(player, timer, now)

            if (timer.consumeExpiryNotification()) {
                player.sendSystemMessage(Component.literal("Timer \"${timer.displayName()}\" is up!"))
                if (!timer.isMuted && TimerConfig.alarmDurationSeconds > 0 && TimerConfig.volume > 0) {
                    playAlarmBeep(player)
                    lastAlarmSoundAtMillis[timer.slot] = now
                }
            } else if (timer.isAlarming) {
                if (TimerConfig.volume <= 0) continue
                val lastSound = lastAlarmSoundAtMillis.getOrDefault(timer.slot, 0L)
                if (now - lastSound >= ALARM_SOUND_INTERVAL_MILLIS) {
                    playAlarmBeep(player)
                    lastAlarmSoundAtMillis[timer.slot] = now
                }
            } else {
                lastAlarmSoundAtMillis.remove(timer.slot)
            }
        }
    }

    private fun tickWarningSounds(player: LocalPlayer, timer: ActiveTimer, now: Long) {
        if (TimerConfig.volume <= 0) {
            if (timer.consumeWarningNotification() && TimerConfig.showWarningMessage) {
                FullScreenAlert.showWarningAlert("${timer.displayName()} — ${TimerConfig.warningThresholdSeconds}s!", timer.baseName())
            }
            return
        }

        val slot = timer.slot
        if (timer.consumeWarningNotification()) {
            if (TimerConfig.showWarningMessage) {
                FullScreenAlert.showWarningAlert("${timer.displayName()} — ${TimerConfig.warningThresholdSeconds}s!", timer.baseName())
            }
            playAlarmBeep(player)
            warningBeepsPlayed[slot] = 1
            lastWarningSoundAtMillis[slot] = now
            return
        }

        val played = warningBeepsPlayed[slot] ?: return
        if (played >= WARNING_BEEP_COUNT) return

        val lastSound = lastWarningSoundAtMillis.getOrDefault(slot, 0L)
        if (now - lastSound < ALARM_SOUND_INTERVAL_MILLIS) return

        playAlarmBeep(player)
        val nextPlayed = played + 1
        if (nextPlayed >= WARNING_BEEP_COUNT) {
            warningBeepsPlayed.remove(slot)
            lastWarningSoundAtMillis.remove(slot)
        } else {
            warningBeepsPlayed[slot] = nextPlayed
            lastWarningSoundAtMillis[slot] = now
        }
    }

    private fun clearSoundState(slot: Int) {
        lastAlarmSoundAtMillis.remove(slot)
        warningBeepsPlayed.remove(slot)
        lastWarningSoundAtMillis.remove(slot)
    }

    private fun clearAllSoundState() {
        lastAlarmSoundAtMillis.clear()
        warningBeepsPlayed.clear()
        lastWarningSoundAtMillis.clear()
    }

    private fun playAlarmBeep(player: LocalPlayer) {
        player.playSound(
            SoundEvents.NOTE_BLOCK_PLING.value(),
            TimerConfig.volumeScale,
            1.5f
        )
    }
}
