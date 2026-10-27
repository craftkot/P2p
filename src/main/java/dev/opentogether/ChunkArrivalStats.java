package dev.opentogether;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;

/**
 * Samples client-side chunk load events for a rolling 10-second window.
 * This is a diagnostic metric, not a network throughput or packet counter:
 * cached/re-used chunks, server generation, and decoding cannot be separated
 * without additional instrumentation.
 */
@Mod.EventBusSubscriber(modid = "opentogether", value = Dist.CLIENT)
public final class ChunkArrivalStats {
    private static final long WINDOW_NANOS = 10_000_000_000L;
    private static final ArrayDeque<Long> LOADED_AT = new ArrayDeque<>();
    private static ClientLevel lastLevel;
    private static long mostRecentLoad = -1L;

    private ChunkArrivalStats() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ClientLevel level)) return;
        long now = System.nanoTime();
        synchronized (ChunkArrivalStats.class) {
            if (lastLevel != level) {
                LOADED_AT.clear();
                mostRecentLoad = -1L;
                lastLevel = level;
            }
            LOADED_AT.addLast(now);
            mostRecentLoad = now;
            trim(now);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ClientLevel level = Minecraft.getInstance().level;
        synchronized (ChunkArrivalStats.class) {
            if (level != lastLevel) {
                lastLevel = level;
                LOADED_AT.clear();
                mostRecentLoad = -1L;
            }
        }
    }

    public static synchronized Sample sample() {
        long now = System.nanoTime();
        trim(now);
        long lastLoadSeconds = mostRecentLoad < 0 ? -1
                : Math.max(0, (now - mostRecentLoad) / 1_000_000_000L);
        return new Sample(LOADED_AT.size(), lastLoadSeconds);
    }

    private static void trim(long now) {
        while (!LOADED_AT.isEmpty() && now - LOADED_AT.peekFirst() > WINDOW_NANOS)
            LOADED_AT.removeFirst();
    }

    public record Sample(int loadedInLast10s, long secondsSinceLastLoad) {}
}
