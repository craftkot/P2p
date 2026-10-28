package dev.opentogether;

import java.util.Locale;

/**
 * Selects vanilla-compatible zlib compression levels without changing
 * the Minecraft packet format or requiring a modded receiving client.
 *
 * Mode "adaptive": faster on small packets, stronger for chunk-sized data.
 * Mode "maximum": strongest zlib for every compressed packet (may add latency).
 * Mode "vanilla": Java Deflater default level 6.
 */
public final class CompressionTuning {
    public enum Mode { ADAPTIVE, MAXIMUM, VANILLA }

    private CompressionTuning() {}

    public static Mode parseMode(String input) {
        if (input == null) return Mode.ADAPTIVE;
        return switch (input.trim().toLowerCase(Locale.ROOT)) {
            case "adaptive" -> Mode.ADAPTIVE;
            case "maximum", "max", "9" -> Mode.MAXIMUM;
            case "vanilla", "default", "6" -> Mode.VANILLA;
            default -> Mode.ADAPTIVE;
        };
    }

    public static int levelForBytes(Mode mode, int packetBytes) {
        if (packetBytes < 0) throw new IllegalArgumentException("Invalid packet length");
        return switch (mode) {
            case MAXIMUM -> 9;
            case VANILLA -> 6;
            case ADAPTIVE -> {
                if (packetBytes < 4096) yield 1;
                if (packetBytes < 16384) yield 6;
                yield 9;
            }
        };
    }
}
