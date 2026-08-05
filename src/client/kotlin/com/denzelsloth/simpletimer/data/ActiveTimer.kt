package com.denzelsloth.simpletimer.data

import com.denzelsloth.simpletimer.config.TimerConfig
import net.minecraft.resources.Identifier

class ActiveTimer private constructor(
    val name: String,
    val slot: Int,
    val durationMillis: Long,
    private val waypoint: Boolean,
    val waypointX: Double,
    val waypointY: Double,
    val waypointZ: Double,
    val waypointDimension: Identifier?,
    endsAtMillis: Long?
) {
    companion object {
        fun create(name: String, slot: Int, durationMillis: Long): ActiveTimer =
            ActiveTimer(name, slot, durationMillis, false, 0.0, 0.0, 0.0, null, null)

        fun createWithWaypoint(
            name: String, slot: Int, durationMillis: Long,
            x: Double, y: Double, z: Double, dimension: Identifier
        ): ActiveTimer =
            ActiveTimer(name, slot, durationMillis, true, x, y, z, dimension, null)

        fun restore(
            name: String, slot: Int, durationMillis: Long, endsAtMillis: Long,
            waypoint: Boolean, x: Double, y: Double, z: Double, dimension: Identifier?
        ): ActiveTimer =
            ActiveTimer(name, slot, durationMillis, waypoint, x, y, z, dimension, endsAtMillis)
    }

    var endsAtMillis: Long = endsAtMillis ?: (System.currentTimeMillis() + durationMillis)
        private set

    private var warningNotified: Boolean
    private var expiredNotified: Boolean
    private var alarmEndsAtMillis: Long = 0L

    init {
        if (endsAtMillis != null) {
            val remaining = remainingMillis()
            when {
                remaining <= 0L -> {
                    warningNotified = true
                    expiredNotified = true
                }
                remaining <= TimerConfig.warningThresholdMillis -> {
                    warningNotified = true
                    expiredNotified = false
                }
                else -> {
                    warningNotified = false
                    expiredNotified = false
                }
            }
        } else {
            warningNotified = false
            expiredNotified = false
        }
    }

    fun hasWaypoint(): Boolean = waypoint && waypointDimension != null

    val isExpired: Boolean get() = remainingMillis() <= 0L

    val isWarning: Boolean get() = !isExpired && remainingMillis() <= TimerConfig.warningThresholdMillis

    val isAlarming: Boolean get() = isExpired && System.currentTimeMillis() < alarmEndsAtMillis

    fun remainingMillis(): Long = maxOf(0L, endsAtMillis - System.currentTimeMillis())

    fun remainingFraction(): Float {
        if (durationMillis <= 0L) return 0f
        return remainingMillis() / durationMillis.toFloat()
    }

    fun consumeWarningNotification(): Boolean {
        if (!isWarning || warningNotified) return false
        warningNotified = true
        return true
    }

    fun consumeExpiryNotification(): Boolean {
        if (!isExpired || expiredNotified) return false
        expiredNotified = true
        alarmEndsAtMillis = System.currentTimeMillis() + TimerConfig.alarmDurationMillis()
        return true
    }

    fun restart() {
        endsAtMillis = System.currentTimeMillis() + durationMillis
        warningNotified = false
        expiredNotified = false
        alarmEndsAtMillis = 0L
    }

    fun formattedRemaining(): String {
        if (isExpired) return "UP"
        val totalSeconds = (remainingMillis() + 999L) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
        else String.format("%d:%02d", minutes, seconds)
    }

    fun waypointLabel(): String = "$name  ${formattedRemaining()}"
}
