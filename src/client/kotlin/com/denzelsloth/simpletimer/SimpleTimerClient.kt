package com.denzelsloth.simpletimer

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import com.denzelsloth.simpletimer.config.TimerConfig
import com.denzelsloth.simpletimer.data.MobWatchlist
import com.denzelsloth.simpletimer.data.SpawnTracker
import com.denzelsloth.simpletimer.data.TimerManager
import com.denzelsloth.simpletimer.features.mobdetection.EntityDetector
import com.denzelsloth.simpletimer.features.mobdetection.FullScreenAlert
import com.denzelsloth.simpletimer.features.mobdetection.MobWaypointRenderer
import com.denzelsloth.simpletimer.features.timer.STimerCommand
import com.denzelsloth.simpletimer.features.timer.TimerHud
import com.denzelsloth.simpletimer.features.timer.TimerKeybinds
import com.denzelsloth.simpletimer.features.timer.TimerWaypointRenderer

object SimpleTimerClient : ClientModInitializer {
    override fun onInitializeClient() {
        TimerManager.loadPersisted()
        MobWatchlist.load()
        SpawnTracker.load()
        STimerCommand.register()
        TimerKeybinds.register()
        TimerHud.register()
        TimerWaypointRenderer.register()
        EntityDetector.register()
        MobWaypointRenderer.register()
        FullScreenAlert.register()
        ClientLifecycleEvents.CLIENT_STOPPING.register { TimerManager.savePersisted() }
    }
}
