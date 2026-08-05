package com.denzelsloth.simpletimer.features.mobdetection

import net.minecraft.resources.Identifier

class DetectedMob(
    val name: String,
    val entityId: Int,
    val x: Double,
    val y: Double,
    val z: Double,
    val dimension: Identifier,
    val watchlistEntry: String = name
) {
    val detectedAtMillis: Long = System.currentTimeMillis()
    var isAlive: Boolean = true
        private set

    fun markDead() { isAlive = false }

    fun timeSinceDetected(): String {
        val elapsed = (System.currentTimeMillis() - detectedAtMillis) / 1000L
        val minutes = elapsed / 60L
        val seconds = elapsed % 60L
        return if (minutes > 0) "${minutes}m ${seconds}s ago" else "${seconds}s ago"
    }
}
