package dev.opentogether.fabric;

import java.util.ArrayDeque;

/**
 * Client chunk-load event counter with a rolling 10-second window.
 *
 * This counts client-side load events, NOT packets, bandwidth, chunk generation
 * or a server-side chunk send rate. A standing player can legitimately see 0.
 * World identity is tracked to avoid mixing dimension/session measurements.
 */
public final class ChunkArrivalStats {
    public static final ChunkArrivalStats INSTANCE = new ChunkArrivalStats();

    private static final long WINDOW_NANOS = 10_000_000_000L;
    private final ArrayDeque<Long> loadTimes = new ArrayDeque<>();
    private Object currentWorld;
    private long latestLoadNanos = -1;

    private ChunkArrivalStats() {}

    public synchronized void setWorld(Object world) {
        if (world == currentWorld) return;
        currentWorld = world;
        loadTimes.clear();
        latestLoadNanos = -1;
    }

    public synchronized void onChunkLoaded(Object world) {
        if (world == null) return;
        setWorld(world);
        long now = System.nanoTime();
        loadTimes.addLast(now);
        latestLoadNanos = now;
        trim(now);
    }

    public synchronized Sample snapshot(Object world) {
        setWorld(world);
        long now = System.nanoTime();
        trim(now);
        long since = latestLoadNanos == -1 ? -1 :
                Math.max(0L, (now - latestLoadNanos) / 1_000_000_000L);
        return new Sample(loadTimes.size(), since);
    }

    private void trim(long now) {
        while (!loadTimes.isEmpty() && now - loadTimes.peekFirst() > WINDOW_NANOS) {
            loadTimes.removeFirst();
        }
    }

    public record Sample(int eventsIn10Seconds, long secondsSinceLastEvent) {}
}
