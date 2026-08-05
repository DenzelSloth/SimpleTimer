package com.denzelsloth.simpletimer.features.timer

import com.denzelsloth.simpletimer.config.TimerConfig
import com.denzelsloth.simpletimer.data.ActiveTimer
import com.denzelsloth.simpletimer.data.MobWatchlist
import com.denzelsloth.simpletimer.data.TimerManager
import com.denzelsloth.simpletimer.features.mobdetection.EntityDetector
import com.denzelsloth.simpletimer.utils.FormatUtils
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component
import java.util.Locale

object STimerCommand {
    private const val MAX_DURATION_SECONDS = 24 * 60 * 60
    private val INVALID_UNIT = SimpleCommandExceptionType(
        Component.literal("Unknown time unit. Use seconds, minutes, or hours (or s/m/h).")
    )
    private val DURATION_OUT_OF_RANGE = SimpleCommandExceptionType(
        Component.literal("Duration must be between 1 second and 24 hours.")
    )
    private val TIME_UNIT_SUGGESTIONS = { context: CommandContext<FabricClientCommandSource>, builder: com.mojang.brigadier.suggestion.SuggestionsBuilder ->
        SharedSuggestionProvider.suggest(listOf("seconds", "minutes", "hours", "s", "m", "h"), builder)
    }

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            val stimer = dispatcher.register(buildCommand())
            dispatcher.register(literal("stimer").redirect(stimer))
        }
    }

    private fun buildCommand(): LiteralArgumentBuilder<FabricClientCommandSource> =
        literal("STimer")
            .then(literal("set")
                .then(argument("name", StringArgumentType.string())
                    .then(argument("amount", IntegerArgumentType.integer(1, MAX_DURATION_SECONDS))
                        .then(argument("hotkey", IntegerArgumentType.integer(1, TimerManager.MAX_TIMERS))
                            .executes { executeSetSeconds(it) }
                            .then(argument("waypoint", BoolArgumentType.bool())
                                .executes { executeSetSecondsWithWaypoint(it) }))
                        .then(argument("unit", StringArgumentType.word())
                            .suggests(TIME_UNIT_SUGGESTIONS)
                            .then(argument("hotkey", IntegerArgumentType.integer(1, TimerManager.MAX_TIMERS))
                                .executes { executeSetWithUnit(it) }
                                .then(argument("waypoint", BoolArgumentType.bool())
                                    .executes { executeSetWithUnitAndWaypoint(it) }))))))
            .then(literal("remove")
                .then(argument("hotkey", IntegerArgumentType.integer(1, TimerManager.MAX_TIMERS))
                    .executes { executeRemoveByHotkey(it) })
                .then(literal("name")
                    .then(argument("name", StringArgumentType.string())
                        .executes { executeRemoveByName(it) })))
            .then(buildWatchCommand())
            .then(buildConfigCommand())

    private fun buildWatchCommand(): LiteralArgumentBuilder<FabricClientCommandSource> =
        literal("watch")
            .executes { executeWatchList(it) }
            .then(literal("add")
                .then(argument("name", StringArgumentType.string())
                    .executes { executeWatchAdd(it) }))
            .then(literal("remove")
                .then(argument("name", StringArgumentType.string())
                    .executes { executeWatchRemove(it) }))
            .then(literal("list").executes { executeWatchList(it) })
            .then(literal("clear").executes { executeWatchClear(it) })
            .then(literal("alert").executes { executeWatchAlertToggle(it) })

    private fun buildConfigCommand(): LiteralArgumentBuilder<FabricClientCommandSource> =
        literal("config")
            .executes { ctx -> ctx.source.sendFeedback(Component.literal("STimer config: ${TimerConfig.summarize()}")); 1 }
            .then(literal("showKeybinds").then(argument("enabled", BoolArgumentType.bool()).executes { ctx ->
                TimerConfig.setShowKeybinds(BoolArgumentType.getBool(ctx, "enabled"))
                ctx.source.sendFeedback(Component.literal("HUD keybinds ${if (TimerConfig.showKeybinds) "shown" else "hidden"}")); 1
            }))
            .then(literal("draggable").then(argument("enabled", BoolArgumentType.bool()).executes { ctx ->
                TimerConfig.setDraggable(BoolArgumentType.getBool(ctx, "enabled"))
                ctx.source.sendFeedback(Component.literal("HUD dragging ${if (TimerConfig.draggable) "enabled" else "disabled"}")); 1
            }))
            .then(literal("showBackground").then(argument("enabled", BoolArgumentType.bool()).executes { ctx ->
                TimerConfig.setShowBackground(BoolArgumentType.getBool(ctx, "enabled"))
                ctx.source.sendFeedback(Component.literal("HUD background ${if (TimerConfig.showBackground) "shown" else "hidden"}")); 1
            }))
            .then(literal("size").then(argument("scale", FloatArgumentType.floatArg(TimerConfig.MIN_SIZE, TimerConfig.MAX_SIZE)).executes { ctx ->
                TimerConfig.setSize(FloatArgumentType.getFloat(ctx, "scale"))
                ctx.source.sendFeedback(Component.literal("HUD size set to ${FormatUtils.formatSize(TimerConfig.size)}x")); 1
            }))
            .then(literal("textOpacity").then(argument("percent", IntegerArgumentType.integer(TimerConfig.MIN_TEXT_OPACITY, TimerConfig.MAX_OPACITY)).executes { ctx ->
                TimerConfig.setTextOpacity(IntegerArgumentType.getInteger(ctx, "percent"))
                ctx.source.sendFeedback(Component.literal("Text opacity set to ${TimerConfig.textOpacity}%")); 1
            }))
            .then(literal("backgroundOpacity").then(argument("percent", IntegerArgumentType.integer(TimerConfig.MIN_BACKGROUND_OPACITY, TimerConfig.MAX_OPACITY)).executes { ctx ->
                TimerConfig.setBackgroundOpacity(IntegerArgumentType.getInteger(ctx, "percent"))
                ctx.source.sendFeedback(Component.literal("Background opacity set to ${TimerConfig.backgroundOpacity}%")); 1
            }))
            .then(literal("warningThreshold").then(argument("seconds", IntegerArgumentType.integer(TimerConfig.MIN_WARNING_THRESHOLD_SECONDS, TimerConfig.MAX_WARNING_THRESHOLD_SECONDS)).executes { ctx ->
                TimerConfig.setWarningThresholdSeconds(IntegerArgumentType.getInteger(ctx, "seconds"))
                ctx.source.sendFeedback(Component.literal("Warning threshold set to ${TimerConfig.warningThresholdSeconds}s (yellow + sound)")); 1
            }))
            .then(literal("alarmDuration").then(argument("seconds", IntegerArgumentType.integer(TimerConfig.MIN_ALARM_DURATION_SECONDS, TimerConfig.MAX_ALARM_DURATION_SECONDS)).executes { ctx ->
                TimerConfig.setAlarmDurationSeconds(IntegerArgumentType.getInteger(ctx, "seconds"))
                ctx.source.sendFeedback(Component.literal("Timer alarm duration set to ${TimerConfig.alarmDurationSeconds}s")); 1
            }))
            .then(literal("spawnAlarmDuration").then(argument("seconds", IntegerArgumentType.integer(TimerConfig.MIN_SPAWN_ALARM_DURATION_SECONDS, TimerConfig.MAX_SPAWN_ALARM_DURATION_SECONDS)).executes { ctx ->
                TimerConfig.setSpawnAlarmDurationSeconds(IntegerArgumentType.getInteger(ctx, "seconds"))
                ctx.source.sendFeedback(Component.literal("Spawn alarm duration set to ${TimerConfig.spawnAlarmDurationSeconds}s")); 1
            }))
            .then(literal("volume").then(argument("percent", IntegerArgumentType.integer(TimerConfig.MIN_VOLUME, TimerConfig.MAX_VOLUME)).executes { ctx ->
                TimerConfig.setVolume(IntegerArgumentType.getInteger(ctx, "percent"))
                ctx.source.sendFeedback(Component.literal("Timer volume set to ${TimerConfig.volume}%")); 1
            }))
            .then(buildWaypointConfigCommand())

    private fun buildWaypointConfigCommand(): LiteralArgumentBuilder<FabricClientCommandSource> =
        literal("waypoint")
            .executes { ctx ->
                ctx.source.sendFeedback(Component.literal(
                    "STimer waypoint config: enabled=${TimerConfig.showWaypoints}, distance=${TimerConfig.waypointDistance}" +
                    ", size=${FormatUtils.formatSize(TimerConfig.waypointSize)}, showBackground=${TimerConfig.waypointShowBackground}" +
                    ", textOpacity=${TimerConfig.waypointTextOpacity}%, backgroundOpacity=${TimerConfig.waypointBackgroundOpacity}%" +
                    ", height=${FormatUtils.formatSize(TimerConfig.waypointHeight)}"
                )); 1
            }
            .then(literal("enabled").then(argument("enabled", BoolArgumentType.bool()).executes { ctx ->
                TimerConfig.setShowWaypoints(BoolArgumentType.getBool(ctx, "enabled"))
                ctx.source.sendFeedback(Component.literal("Waypoint labels ${if (TimerConfig.showWaypoints) "enabled" else "disabled"}")); 1
            }))
            .then(literal("distance").then(argument("blocks", IntegerArgumentType.integer(TimerConfig.MIN_WAYPOINT_DISTANCE, TimerConfig.MAX_WAYPOINT_DISTANCE)).executes { ctx ->
                TimerConfig.setWaypointDistance(IntegerArgumentType.getInteger(ctx, "blocks"))
                ctx.source.sendFeedback(Component.literal("Waypoint distance set to ${TimerConfig.waypointDistance} blocks")); 1
            }))
            .then(literal("size").then(argument("scale", FloatArgumentType.floatArg(TimerConfig.MIN_SIZE, TimerConfig.MAX_SIZE)).executes { ctx ->
                TimerConfig.setWaypointSize(FloatArgumentType.getFloat(ctx, "scale"))
                ctx.source.sendFeedback(Component.literal("Waypoint size set to ${FormatUtils.formatSize(TimerConfig.waypointSize)}x")); 1
            }))
            .then(literal("showBackground").then(argument("enabled", BoolArgumentType.bool()).executes { ctx ->
                TimerConfig.setWaypointShowBackground(BoolArgumentType.getBool(ctx, "enabled"))
                ctx.source.sendFeedback(Component.literal("Waypoint background ${if (TimerConfig.waypointShowBackground) "shown" else "hidden"}")); 1
            }))
            .then(literal("textOpacity").then(argument("percent", IntegerArgumentType.integer(TimerConfig.MIN_TEXT_OPACITY, TimerConfig.MAX_OPACITY)).executes { ctx ->
                TimerConfig.setWaypointTextOpacity(IntegerArgumentType.getInteger(ctx, "percent"))
                ctx.source.sendFeedback(Component.literal("Waypoint text opacity set to ${TimerConfig.waypointTextOpacity}%")); 1
            }))
            .then(literal("backgroundOpacity").then(argument("percent", IntegerArgumentType.integer(TimerConfig.MIN_BACKGROUND_OPACITY, TimerConfig.MAX_OPACITY)).executes { ctx ->
                TimerConfig.setWaypointBackgroundOpacity(IntegerArgumentType.getInteger(ctx, "percent"))
                ctx.source.sendFeedback(Component.literal("Waypoint background opacity set to ${TimerConfig.waypointBackgroundOpacity}%")); 1
            }))
            .then(literal("height").then(argument("blocks", FloatArgumentType.floatArg(TimerConfig.MIN_WAYPOINT_HEIGHT, TimerConfig.MAX_WAYPOINT_HEIGHT)).executes { ctx ->
                TimerConfig.setWaypointHeight(FloatArgumentType.getFloat(ctx, "blocks"))
                ctx.source.sendFeedback(Component.literal("Waypoint height set to ${FormatUtils.formatSize(TimerConfig.waypointHeight)} blocks above feet")); 1
            }))

    // --- Set commands ---

    private fun executeSetSeconds(ctx: CommandContext<FabricClientCommandSource>): Int {
        val amount = IntegerArgumentType.getInteger(ctx, "amount")
        return setTimer(ctx, amount, "seconds", waypoint = false)
    }

    private fun executeSetSecondsWithWaypoint(ctx: CommandContext<FabricClientCommandSource>): Int {
        val amount = IntegerArgumentType.getInteger(ctx, "amount")
        return setTimer(ctx, amount, "seconds", BoolArgumentType.getBool(ctx, "waypoint"))
    }

    private fun executeSetWithUnit(ctx: CommandContext<FabricClientCommandSource>): Int {
        val amount = IntegerArgumentType.getInteger(ctx, "amount")
        val unit = StringArgumentType.getString(ctx, "unit")
        return setTimer(ctx, amount, unit, waypoint = false)
    }

    private fun executeSetWithUnitAndWaypoint(ctx: CommandContext<FabricClientCommandSource>): Int {
        val amount = IntegerArgumentType.getInteger(ctx, "amount")
        val unit = StringArgumentType.getString(ctx, "unit")
        return setTimer(ctx, amount, unit, BoolArgumentType.getBool(ctx, "waypoint"))
    }

    private fun setTimer(ctx: CommandContext<FabricClientCommandSource>, amount: Int, unit: String, waypoint: Boolean): Int {
        val name = StringArgumentType.getString(ctx, "name")
        val seconds = toSeconds(amount, unit)
        val hotkey = IntegerArgumentType.getInteger(ctx, "hotkey")

        val timer = TimerManager.setTimer(name, seconds, hotkey, waypoint, ctx.source.player)
        var feedback = "Timer \"${timer.name}\" set to ${formatDuration(amount, unit)} on slot $hotkey (reset with Ctrl/Cmd+$hotkey)"
        if (timer.hasWaypoint()) {
            feedback += " [waypoint @ ${timer.waypointX.toInt()} ${timer.waypointY.toInt()} ${timer.waypointZ.toInt()}]"
        }
        ctx.source.sendFeedback(Component.literal(feedback))
        return 1
    }

    private fun toSeconds(amount: Int, unit: String): Int {
        val multiplier = when (unit.lowercase(Locale.ROOT)) {
            "s", "sec", "secs", "second", "seconds" -> 1
            "m", "min", "mins", "minute", "minutes" -> 60
            "h", "hr", "hrs", "hour", "hours" -> 3600
            else -> throw INVALID_UNIT.create()
        }
        val total = amount.toLong() * multiplier
        if (total < 1L || total > MAX_DURATION_SECONDS) throw DURATION_OUT_OF_RANGE.create()
        return total.toInt()
    }

    private fun formatDuration(amount: Int, unit: String): String {
        val normalized = when (unit.lowercase(Locale.ROOT)) {
            "s", "sec", "secs", "second", "seconds" -> if (amount == 1) "second" else "seconds"
            "m", "min", "mins", "minute", "minutes" -> if (amount == 1) "minute" else "minutes"
            "h", "hr", "hrs", "hour", "hours" -> if (amount == 1) "hour" else "hours"
            else -> unit
        }
        return "$amount $normalized"
    }

    // --- Remove commands ---

    private fun executeRemoveByHotkey(ctx: CommandContext<FabricClientCommandSource>): Int {
        val hotkey = IntegerArgumentType.getInteger(ctx, "hotkey")
        val existing = TimerManager.get(hotkey)
        if (existing.isEmpty) {
            ctx.source.sendError(Component.literal("No timer on slot $hotkey"))
            return 0
        }
        val timer = existing.get()
        TimerManager.remove(hotkey)
        ctx.source.sendFeedback(Component.literal("Removed timer \"${timer.name}\" from slot $hotkey"))
        return 1
    }

    private fun executeRemoveByName(ctx: CommandContext<FabricClientCommandSource>): Int {
        val name = StringArgumentType.getString(ctx, "name")
        val removed = TimerManager.removeByName(name)
        if (removed.isEmpty()) {
            ctx.source.sendError(Component.literal("No timer named \"$name\""))
            return 0
        }
        val slots = removed.joinToString(", ") { it.slot.toString() }
        val label = if (removed.size == 1) "timer" else "timers"
        val slotLabel = if (removed.size == 1) "slot " else "slots "
        ctx.source.sendFeedback(Component.literal("Removed ${removed.size} $label named \"${removed.first().name}\" from $slotLabel$slots"))
        return removed.size
    }

    // --- Watch commands ---

    private fun executeWatchAdd(ctx: CommandContext<FabricClientCommandSource>): Int {
        val name = StringArgumentType.getString(ctx, "name")
        if (MobWatchlist.add(name)) {
            ctx.source.sendFeedback(Component.literal("Added \"$name\" to mob watchlist (${MobWatchlist.size} entries)"))
        } else {
            ctx.source.sendError(Component.literal("\"$name\" is already in the watchlist"))
            return 0
        }
        return 1
    }

    private fun executeWatchRemove(ctx: CommandContext<FabricClientCommandSource>): Int {
        val name = StringArgumentType.getString(ctx, "name")
        if (MobWatchlist.remove(name)) {
            ctx.source.sendFeedback(Component.literal("Removed \"$name\" from mob watchlist"))
        } else {
            ctx.source.sendError(Component.literal("\"$name\" is not in the watchlist"))
            return 0
        }
        return 1
    }

    private fun executeWatchList(ctx: CommandContext<FabricClientCommandSource>): Int {
        val entries = MobWatchlist.entries()
        if (entries.isEmpty()) {
            ctx.source.sendFeedback(Component.literal("Mob watchlist is empty. Add mobs with: /STimer watch add \"Mob Name\""))
        } else {
            val sb = StringBuilder("Mob watchlist (${entries.size}):")
            entries.forEach { sb.append("\n  - $it") }
            ctx.source.sendFeedback(Component.literal(sb.toString()))
        }

        val tracked = EntityDetector.tracked()
        if (tracked.isNotEmpty()) {
            val sb = StringBuilder("Currently tracked (${tracked.size}):")
            tracked.forEach { mob ->
                sb.append("\n  - ${mob.name} at ${mob.x.toInt()}, ${mob.y.toInt()}, ${mob.z.toInt()} (${mob.timeSinceDetected()})")
            }
            ctx.source.sendFeedback(Component.literal(sb.toString()))
        }
        return 1
    }

    private fun executeWatchClear(ctx: CommandContext<FabricClientCommandSource>): Int {
        val count = MobWatchlist.size
        MobWatchlist.clear()
        EntityDetector.clearTracked()
        ctx.source.sendFeedback(Component.literal("Cleared mob watchlist ($count entries removed)"))
        return 1
    }

    private fun executeWatchAlertToggle(ctx: CommandContext<FabricClientCommandSource>): Int {
        val newState = !TimerConfig.showFullScreenAlert
        TimerConfig.setShowFullScreenAlert(newState)
        ctx.source.sendFeedback(Component.literal("Full-screen alert ${if (newState) "ON" else "OFF"}"))
        return 1
    }
}
