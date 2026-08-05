package com.denzelsloth.simpletimer.config

import com.denzelsloth.simpletimer.SimpleTimerMod
import com.denzelsloth.simpletimer.utils.FormatUtils
import net.fabricmc.loader.api.FabricLoader
import java.io.IOException
import java.nio.file.Files
import java.util.Properties

object TimerConfig {
    const val DEFAULT_X = 8
    const val DEFAULT_Y = 8
    const val DEFAULT_SIZE = 1.0f
    const val DEFAULT_TEXT_OPACITY = 100
    const val DEFAULT_BACKGROUND_OPACITY = 100
    const val DEFAULT_WARNING_THRESHOLD_SECONDS = 10
    const val DEFAULT_ALARM_DURATION_SECONDS = 10
    const val DEFAULT_SPAWN_ALARM_DURATION_SECONDS = 3
    const val DEFAULT_VOLUME = 100
    const val DEFAULT_WAYPOINT_DISTANCE = 200
    const val DEFAULT_WAYPOINT_HEIGHT = 1.2f
    const val MIN_SIZE = 0.5f
    const val MAX_SIZE = 3.0f
    const val MIN_TEXT_OPACITY = 10
    const val MAX_OPACITY = 100
    const val MIN_BACKGROUND_OPACITY = 0
    const val MIN_WARNING_THRESHOLD_SECONDS = 1
    const val MAX_WARNING_THRESHOLD_SECONDS = 60
    const val MIN_ALARM_DURATION_SECONDS = 0
    const val MAX_ALARM_DURATION_SECONDS = 60
    const val MIN_SPAWN_ALARM_DURATION_SECONDS = 0
    const val MAX_SPAWN_ALARM_DURATION_SECONDS = 30
    const val MIN_VOLUME = 0
    const val MAX_VOLUME = 100
    const val MIN_WAYPOINT_DISTANCE = 8
    const val MAX_WAYPOINT_DISTANCE = 500
    const val MIN_WAYPOINT_HEIGHT = 0.0f
    const val MAX_WAYPOINT_HEIGHT = 5.0f

    private val CONFIG_PATH = FabricLoader.getInstance()
        .configDir
        .resolve("simpletimer-hud.properties")

    var x: Int = DEFAULT_X; private set
    var y: Int = DEFAULT_Y; private set
    var showKeybinds: Boolean = true; private set
    var draggable: Boolean = true; private set
    var showBackground: Boolean = true; private set
    var size: Float = DEFAULT_SIZE; private set
    var textOpacity: Int = DEFAULT_TEXT_OPACITY; private set
    var backgroundOpacity: Int = DEFAULT_BACKGROUND_OPACITY; private set
    var warningThresholdSeconds: Int = DEFAULT_WARNING_THRESHOLD_SECONDS; private set
    var alarmDurationSeconds: Int = DEFAULT_ALARM_DURATION_SECONDS; private set
    var spawnAlarmDurationSeconds: Int = DEFAULT_SPAWN_ALARM_DURATION_SECONDS; private set
    var volume: Int = DEFAULT_VOLUME; private set
    var showWaypoints: Boolean = true; private set
    var waypointDistance: Int = DEFAULT_WAYPOINT_DISTANCE; private set
    var waypointSize: Float = DEFAULT_SIZE; private set
    var waypointShowBackground: Boolean = true; private set
    var waypointTextOpacity: Int = DEFAULT_TEXT_OPACITY; private set
    var waypointBackgroundOpacity: Int = DEFAULT_BACKGROUND_OPACITY; private set
    var waypointHeight: Float = DEFAULT_WAYPOINT_HEIGHT; private set
    var showFullScreenAlert: Boolean = true; private set
    var showWarningMessage: Boolean = true; private set

    val volumeScale: Float get() = volume / 100.0f
    val waypointDistanceSq: Double get() = waypointDistance.toDouble() * waypointDistance.toDouble()

    val warningThresholdMillis: Long get() = warningThresholdSeconds * 1000L
    fun alarmDurationMillis(): Long = alarmDurationSeconds * 1000L
    fun spawnAlarmDurationMillis(): Long = spawnAlarmDurationSeconds * 1000L

    fun load() {
        if (!Files.isRegularFile(CONFIG_PATH)) return
        val properties = Properties()
        try {
            Files.newInputStream(CONFIG_PATH).use { properties.load(it) }
            x = properties.parseInt("x", DEFAULT_X)
            y = properties.parseInt("y", DEFAULT_Y)
            showKeybinds = properties.parseBool("showKeybinds", true)
            draggable = properties.parseBool("draggable", true)
            showBackground = properties.parseBool("showBackground", true)
            size = properties.parseFloat("size", DEFAULT_SIZE).coerceIn(MIN_SIZE, MAX_SIZE)
            val legacyOpacity = properties.parseInt("opacity", DEFAULT_TEXT_OPACITY)
            textOpacity = properties.parseInt("textOpacity", legacyOpacity).coerceIn(MIN_TEXT_OPACITY, MAX_OPACITY)
            backgroundOpacity = properties.parseInt("backgroundOpacity", DEFAULT_BACKGROUND_OPACITY).coerceIn(MIN_BACKGROUND_OPACITY, MAX_OPACITY)
            warningThresholdSeconds = properties.parseInt("warningThreshold", DEFAULT_WARNING_THRESHOLD_SECONDS).coerceIn(MIN_WARNING_THRESHOLD_SECONDS, MAX_WARNING_THRESHOLD_SECONDS)
            alarmDurationSeconds = properties.parseInt("alarmDuration", DEFAULT_ALARM_DURATION_SECONDS).coerceIn(MIN_ALARM_DURATION_SECONDS, MAX_ALARM_DURATION_SECONDS)
            spawnAlarmDurationSeconds = properties.parseInt("spawnAlarmDuration", DEFAULT_SPAWN_ALARM_DURATION_SECONDS).coerceIn(MIN_SPAWN_ALARM_DURATION_SECONDS, MAX_SPAWN_ALARM_DURATION_SECONDS)
            volume = properties.parseInt("volume", DEFAULT_VOLUME).coerceIn(MIN_VOLUME, MAX_VOLUME)
            showWaypoints = properties.parseBool("showWaypoints", true)
            waypointDistance = properties.parseInt("waypointDistance", DEFAULT_WAYPOINT_DISTANCE).coerceIn(MIN_WAYPOINT_DISTANCE, MAX_WAYPOINT_DISTANCE)
            waypointSize = properties.parseFloat("waypointSize", DEFAULT_SIZE).coerceIn(MIN_SIZE, MAX_SIZE)
            waypointShowBackground = properties.parseBool("waypointShowBackground", true)
            waypointTextOpacity = properties.parseInt("waypointTextOpacity", DEFAULT_TEXT_OPACITY).coerceIn(MIN_TEXT_OPACITY, MAX_OPACITY)
            waypointBackgroundOpacity = properties.parseInt("waypointBackgroundOpacity", DEFAULT_BACKGROUND_OPACITY).coerceIn(MIN_BACKGROUND_OPACITY, MAX_OPACITY)
            waypointHeight = properties.parseFloat("waypointHeight", DEFAULT_WAYPOINT_HEIGHT).coerceIn(MIN_WAYPOINT_HEIGHT, MAX_WAYPOINT_HEIGHT)
            showFullScreenAlert = properties.parseBool("showFullScreenAlert", true)
            showWarningMessage = properties.parseBool("showWarningMessage", true)
        } catch (e: IOException) {
            SimpleTimerMod.LOGGER.warn("Failed to load config", e)
        }
    }

    fun save() {
        val properties = Properties()
        properties.setProperty("x", x.toString())
        properties.setProperty("y", y.toString())
        properties.setProperty("showKeybinds", showKeybinds.toString())
        properties.setProperty("draggable", draggable.toString())
        properties.setProperty("showBackground", showBackground.toString())
        properties.setProperty("size", size.toString())
        properties.setProperty("textOpacity", textOpacity.toString())
        properties.setProperty("backgroundOpacity", backgroundOpacity.toString())
        properties.setProperty("warningThreshold", warningThresholdSeconds.toString())
        properties.setProperty("alarmDuration", alarmDurationSeconds.toString())
        properties.setProperty("spawnAlarmDuration", spawnAlarmDurationSeconds.toString())
        properties.setProperty("volume", volume.toString())
        properties.setProperty("showWaypoints", showWaypoints.toString())
        properties.setProperty("waypointDistance", waypointDistance.toString())
        properties.setProperty("waypointSize", waypointSize.toString())
        properties.setProperty("waypointShowBackground", waypointShowBackground.toString())
        properties.setProperty("waypointTextOpacity", waypointTextOpacity.toString())
        properties.setProperty("waypointBackgroundOpacity", waypointBackgroundOpacity.toString())
        properties.setProperty("waypointHeight", waypointHeight.toString())
        properties.setProperty("showFullScreenAlert", showFullScreenAlert.toString())
        properties.setProperty("showWarningMessage", showWarningMessage.toString())
        try {
            Files.createDirectories(CONFIG_PATH.parent)
            Files.newOutputStream(CONFIG_PATH).use { out ->
                properties.store(out, "Simple Timer HUD config")
            }
        } catch (e: IOException) {
            SimpleTimerMod.LOGGER.warn("Failed to save config", e)
        }
    }

    fun setPosition(newX: Int, newY: Int) { x = newX; y = newY }
    fun setPositionAndSave(newX: Int, newY: Int) { setPosition(newX, newY); save() }

    fun setShowKeybinds(value: Boolean) { showKeybinds = value; save() }
    fun applyShowKeybinds(value: Boolean) { showKeybinds = value }

    fun setDraggable(value: Boolean) { draggable = value; save() }
    fun applyDraggable(value: Boolean) { draggable = value }

    fun setShowBackground(value: Boolean) { showBackground = value; save() }
    fun applyShowBackground(value: Boolean) { showBackground = value }

    fun setSize(value: Float) { size = value.coerceIn(MIN_SIZE, MAX_SIZE); save() }
    fun applySize(value: Float) { size = value.coerceIn(MIN_SIZE, MAX_SIZE) }

    fun setTextOpacity(value: Int) { textOpacity = value.coerceIn(MIN_TEXT_OPACITY, MAX_OPACITY); save() }
    fun applyTextOpacitySetting(value: Int) { textOpacity = value.coerceIn(MIN_TEXT_OPACITY, MAX_OPACITY) }

    fun setBackgroundOpacity(value: Int) { backgroundOpacity = value.coerceIn(MIN_BACKGROUND_OPACITY, MAX_OPACITY); save() }
    fun applyBackgroundOpacitySetting(value: Int) { backgroundOpacity = value.coerceIn(MIN_BACKGROUND_OPACITY, MAX_OPACITY) }

    fun setWarningThresholdSeconds(value: Int) { warningThresholdSeconds = value.coerceIn(MIN_WARNING_THRESHOLD_SECONDS, MAX_WARNING_THRESHOLD_SECONDS); save() }
    fun applyWarningThresholdSeconds(value: Int) { warningThresholdSeconds = value.coerceIn(MIN_WARNING_THRESHOLD_SECONDS, MAX_WARNING_THRESHOLD_SECONDS) }

    fun setAlarmDurationSeconds(value: Int) { alarmDurationSeconds = value.coerceIn(MIN_ALARM_DURATION_SECONDS, MAX_ALARM_DURATION_SECONDS); save() }
    fun applyAlarmDurationSeconds(value: Int) { alarmDurationSeconds = value.coerceIn(MIN_ALARM_DURATION_SECONDS, MAX_ALARM_DURATION_SECONDS) }

    fun setSpawnAlarmDurationSeconds(value: Int) { spawnAlarmDurationSeconds = value.coerceIn(MIN_SPAWN_ALARM_DURATION_SECONDS, MAX_SPAWN_ALARM_DURATION_SECONDS); save() }
    fun applySpawnAlarmDurationSeconds(value: Int) { spawnAlarmDurationSeconds = value.coerceIn(MIN_SPAWN_ALARM_DURATION_SECONDS, MAX_SPAWN_ALARM_DURATION_SECONDS) }

    fun setVolume(value: Int) { volume = value.coerceIn(MIN_VOLUME, MAX_VOLUME); save() }
    fun applyVolume(value: Int) { volume = value.coerceIn(MIN_VOLUME, MAX_VOLUME) }

    fun setShowWaypoints(value: Boolean) { showWaypoints = value; save() }
    fun applyShowWaypoints(value: Boolean) { showWaypoints = value }

    fun setWaypointDistance(value: Int) { waypointDistance = value.coerceIn(MIN_WAYPOINT_DISTANCE, MAX_WAYPOINT_DISTANCE); save() }
    fun applyWaypointDistance(value: Int) { waypointDistance = value.coerceIn(MIN_WAYPOINT_DISTANCE, MAX_WAYPOINT_DISTANCE) }

    fun setWaypointSize(value: Float) { waypointSize = value.coerceIn(MIN_SIZE, MAX_SIZE); save() }
    fun applyWaypointSize(value: Float) { waypointSize = value.coerceIn(MIN_SIZE, MAX_SIZE) }

    fun setWaypointShowBackground(value: Boolean) { waypointShowBackground = value; save() }
    fun applyWaypointShowBackground(value: Boolean) { waypointShowBackground = value }

    fun setWaypointTextOpacity(value: Int) { waypointTextOpacity = value.coerceIn(MIN_TEXT_OPACITY, MAX_OPACITY); save() }
    fun applyWaypointTextOpacitySetting(value: Int) { waypointTextOpacity = value.coerceIn(MIN_TEXT_OPACITY, MAX_OPACITY) }

    fun setWaypointBackgroundOpacity(value: Int) { waypointBackgroundOpacity = value.coerceIn(MIN_BACKGROUND_OPACITY, MAX_OPACITY); save() }
    fun applyWaypointBackgroundOpacitySetting(value: Int) { waypointBackgroundOpacity = value.coerceIn(MIN_BACKGROUND_OPACITY, MAX_OPACITY) }

    fun setWaypointHeight(value: Float) { waypointHeight = value.coerceIn(MIN_WAYPOINT_HEIGHT, MAX_WAYPOINT_HEIGHT); save() }
    fun applyWaypointHeight(value: Float) { waypointHeight = value.coerceIn(MIN_WAYPOINT_HEIGHT, MAX_WAYPOINT_HEIGHT) }

    fun setShowFullScreenAlert(value: Boolean) { showFullScreenAlert = value; save() }

    fun setShowWarningMessage(value: Boolean) { showWarningMessage = value; save() }
    fun applyShowWarningMessage(value: Boolean) { showWarningMessage = value }

    fun applyTextOpacity(argb: Int): Int = withOpacity(argb, textOpacity)
    fun applyBackgroundOpacity(argb: Int): Int = withOpacity(argb, backgroundOpacity)
    fun applyWaypointTextOpacity(argb: Int): Int = withOpacity(argb, waypointTextOpacity)
    fun applyWaypointBackgroundOpacity(argb: Int): Int = withOpacity(argb, waypointBackgroundOpacity)

    fun summarize(): String = buildString {
        append("showKeybinds=$showKeybinds")
        append(", draggable=$draggable")
        append(", showBackground=$showBackground")
        append(", size=${FormatUtils.formatSize(size)}")
        append(", textOpacity=$textOpacity%")
        append(", backgroundOpacity=$backgroundOpacity%")
        append(", alarmDuration=${alarmDurationSeconds}s")
        append(", volume=$volume%")
        append(", showWaypoints=$showWaypoints")
        append(", waypointDistance=$waypointDistance")
        append(", waypointSize=${FormatUtils.formatSize(waypointSize)}")
        append(", waypointShowBackground=$waypointShowBackground")
        append(", waypointTextOpacity=$waypointTextOpacity%")
        append(", waypointBackgroundOpacity=$waypointBackgroundOpacity%")
        append(", waypointHeight=${FormatUtils.formatSize(waypointHeight)}")
    }

    private fun withOpacity(argb: Int, opacityPercent: Int): Int {
        val alpha = (255.0f * opacityPercent / 100.0f).toInt()
        return (alpha shl 24) or (argb and 0x00FFFFFF)
    }

    private fun Properties.parseInt(key: String, fallback: Int): Int =
        getProperty(key, fallback.toString()).toIntOrNull() ?: fallback

    private fun Properties.parseFloat(key: String, fallback: Float): Float =
        getProperty(key, fallback.toString()).toFloatOrNull() ?: fallback

    private fun Properties.parseBool(key: String, fallback: Boolean): Boolean {
        val value = getProperty(key) ?: return fallback
        return value.toBoolean()
    }
}
