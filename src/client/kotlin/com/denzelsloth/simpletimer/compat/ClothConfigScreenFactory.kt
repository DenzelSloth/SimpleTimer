package com.denzelsloth.simpletimer.compat

import com.denzelsloth.simpletimer.config.TimerConfig
import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

object ClothConfigScreenFactory {
    fun create(parent: Screen): Screen {
        val builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("text.simpletimer.config.title"))
            .setSavingRunnable { TimerConfig.save() }

        val entry = builder.entryBuilder()

        val hud = builder.getOrCreateCategory(Component.translatable("text.simpletimer.config.category.hud"))
        hud.addEntry(entry.startBooleanToggle(Component.translatable("text.simpletimer.config.showKeybinds"), TimerConfig.showKeybinds)
            .setDefaultValue(true)
            .setTooltip(Component.translatable("text.simpletimer.config.showKeybinds.tooltip"))
            .setSaveConsumer { TimerConfig.applyShowKeybinds(it) }
            .build())
        hud.addEntry(entry.startBooleanToggle(Component.translatable("text.simpletimer.config.draggable"), TimerConfig.draggable)
            .setDefaultValue(true)
            .setTooltip(Component.translatable("text.simpletimer.config.draggable.tooltip"))
            .setSaveConsumer { TimerConfig.applyDraggable(it) }
            .build())
        hud.addEntry(entry.startBooleanToggle(Component.translatable("text.simpletimer.config.showBackground"), TimerConfig.showBackground)
            .setDefaultValue(true)
            .setTooltip(Component.translatable("text.simpletimer.config.showBackground.tooltip"))
            .setSaveConsumer { TimerConfig.applyShowBackground(it) }
            .build())
        hud.addEntry(entry.startDoubleField(Component.translatable("text.simpletimer.config.size"), TimerConfig.size.toDouble())
            .setDefaultValue(TimerConfig.DEFAULT_SIZE.toDouble())
            .setMin(TimerConfig.MIN_SIZE.toDouble())
            .setMax(TimerConfig.MAX_SIZE.toDouble())
            .setTooltip(Component.translatable("text.simpletimer.config.size.tooltip"))
            .setSaveConsumer { TimerConfig.applySize(it.toFloat()) }
            .build())
        hud.addEntry(entry.startIntSlider(Component.translatable("text.simpletimer.config.textOpacity"), TimerConfig.textOpacity, TimerConfig.MIN_TEXT_OPACITY, TimerConfig.MAX_OPACITY)
            .setDefaultValue(TimerConfig.DEFAULT_TEXT_OPACITY)
            .setTooltip(Component.translatable("text.simpletimer.config.textOpacity.tooltip"))
            .setSaveConsumer { TimerConfig.applyTextOpacitySetting(it) }
            .build())
        hud.addEntry(entry.startIntSlider(Component.translatable("text.simpletimer.config.backgroundOpacity"), TimerConfig.backgroundOpacity, TimerConfig.MIN_BACKGROUND_OPACITY, TimerConfig.MAX_OPACITY)
            .setDefaultValue(TimerConfig.DEFAULT_BACKGROUND_OPACITY)
            .setTooltip(Component.translatable("text.simpletimer.config.backgroundOpacity.tooltip"))
            .setSaveConsumer { TimerConfig.applyBackgroundOpacitySetting(it) }
            .build())

        val sound = builder.getOrCreateCategory(Component.translatable("text.simpletimer.config.category.sound"))
        sound.addEntry(entry.startIntSlider(Component.translatable("text.simpletimer.config.warningThreshold"), TimerConfig.warningThresholdSeconds, TimerConfig.MIN_WARNING_THRESHOLD_SECONDS, TimerConfig.MAX_WARNING_THRESHOLD_SECONDS)
            .setDefaultValue(TimerConfig.DEFAULT_WARNING_THRESHOLD_SECONDS)
            .setTooltip(Component.translatable("text.simpletimer.config.warningThreshold.tooltip"))
            .setSaveConsumer { TimerConfig.applyWarningThresholdSeconds(it) }
            .build())
        sound.addEntry(entry.startBooleanToggle(Component.translatable("text.simpletimer.config.showWarningMessage"), TimerConfig.showWarningMessage)
            .setDefaultValue(true)
            .setTooltip(Component.translatable("text.simpletimer.config.showWarningMessage.tooltip"))
            .setSaveConsumer { TimerConfig.applyShowWarningMessage(it) }
            .build())
        sound.addEntry(entry.startIntSlider(Component.translatable("text.simpletimer.config.alarmDuration"), TimerConfig.alarmDurationSeconds, TimerConfig.MIN_ALARM_DURATION_SECONDS, TimerConfig.MAX_ALARM_DURATION_SECONDS)
            .setDefaultValue(TimerConfig.DEFAULT_ALARM_DURATION_SECONDS)
            .setTooltip(Component.translatable("text.simpletimer.config.alarmDuration.tooltip"))
            .setSaveConsumer { TimerConfig.applyAlarmDurationSeconds(it) }
            .build())
        sound.addEntry(entry.startIntSlider(Component.translatable("text.simpletimer.config.spawnAlarmDuration"), TimerConfig.spawnAlarmDurationSeconds, TimerConfig.MIN_SPAWN_ALARM_DURATION_SECONDS, TimerConfig.MAX_SPAWN_ALARM_DURATION_SECONDS)
            .setDefaultValue(TimerConfig.DEFAULT_SPAWN_ALARM_DURATION_SECONDS)
            .setTooltip(Component.translatable("text.simpletimer.config.spawnAlarmDuration.tooltip"))
            .setSaveConsumer { TimerConfig.applySpawnAlarmDurationSeconds(it) }
            .build())
        sound.addEntry(entry.startIntSlider(Component.translatable("text.simpletimer.config.volume"), TimerConfig.volume, TimerConfig.MIN_VOLUME, TimerConfig.MAX_VOLUME)
            .setDefaultValue(TimerConfig.DEFAULT_VOLUME)
            .setTooltip(Component.translatable("text.simpletimer.config.volume.tooltip"))
            .setSaveConsumer { TimerConfig.applyVolume(it) }
            .build())

        val waypoints = builder.getOrCreateCategory(Component.translatable("text.simpletimer.config.category.waypoints"))
        waypoints.addEntry(entry.startBooleanToggle(Component.translatable("text.simpletimer.config.showWaypoints"), TimerConfig.showWaypoints)
            .setDefaultValue(true)
            .setTooltip(Component.translatable("text.simpletimer.config.showWaypoints.tooltip"))
            .setSaveConsumer { TimerConfig.applyShowWaypoints(it) }
            .build())
        waypoints.addEntry(entry.startIntSlider(Component.translatable("text.simpletimer.config.waypointDistance"), TimerConfig.waypointDistance, TimerConfig.MIN_WAYPOINT_DISTANCE, TimerConfig.MAX_WAYPOINT_DISTANCE)
            .setDefaultValue(TimerConfig.DEFAULT_WAYPOINT_DISTANCE)
            .setTooltip(Component.translatable("text.simpletimer.config.waypointDistance.tooltip"))
            .setSaveConsumer { TimerConfig.applyWaypointDistance(it) }
            .build())
        waypoints.addEntry(entry.startDoubleField(Component.translatable("text.simpletimer.config.waypointSize"), TimerConfig.waypointSize.toDouble())
            .setDefaultValue(TimerConfig.DEFAULT_SIZE.toDouble())
            .setMin(TimerConfig.MIN_SIZE.toDouble())
            .setMax(TimerConfig.MAX_SIZE.toDouble())
            .setTooltip(Component.translatable("text.simpletimer.config.waypointSize.tooltip"))
            .setSaveConsumer { TimerConfig.applyWaypointSize(it.toFloat()) }
            .build())
        waypoints.addEntry(entry.startBooleanToggle(Component.translatable("text.simpletimer.config.waypointShowBackground"), TimerConfig.waypointShowBackground)
            .setDefaultValue(true)
            .setTooltip(Component.translatable("text.simpletimer.config.waypointShowBackground.tooltip"))
            .setSaveConsumer { TimerConfig.applyWaypointShowBackground(it) }
            .build())
        waypoints.addEntry(entry.startIntSlider(Component.translatable("text.simpletimer.config.waypointTextOpacity"), TimerConfig.waypointTextOpacity, TimerConfig.MIN_TEXT_OPACITY, TimerConfig.MAX_OPACITY)
            .setDefaultValue(TimerConfig.DEFAULT_TEXT_OPACITY)
            .setTooltip(Component.translatable("text.simpletimer.config.waypointTextOpacity.tooltip"))
            .setSaveConsumer { TimerConfig.applyWaypointTextOpacitySetting(it) }
            .build())
        waypoints.addEntry(entry.startIntSlider(Component.translatable("text.simpletimer.config.waypointBackgroundOpacity"), TimerConfig.waypointBackgroundOpacity, TimerConfig.MIN_BACKGROUND_OPACITY, TimerConfig.MAX_OPACITY)
            .setDefaultValue(TimerConfig.DEFAULT_BACKGROUND_OPACITY)
            .setTooltip(Component.translatable("text.simpletimer.config.waypointBackgroundOpacity.tooltip"))
            .setSaveConsumer { TimerConfig.applyWaypointBackgroundOpacitySetting(it) }
            .build())
        waypoints.addEntry(entry.startDoubleField(Component.translatable("text.simpletimer.config.waypointHeight"), TimerConfig.waypointHeight.toDouble())
            .setDefaultValue(TimerConfig.DEFAULT_WAYPOINT_HEIGHT.toDouble())
            .setMin(TimerConfig.MIN_WAYPOINT_HEIGHT.toDouble())
            .setMax(TimerConfig.MAX_WAYPOINT_HEIGHT.toDouble())
            .setTooltip(Component.translatable("text.simpletimer.config.waypointHeight.tooltip"))
            .setSaveConsumer { TimerConfig.applyWaypointHeight(it.toFloat()) }
            .build())

        return builder.build()
    }
}
