package dev.opentogether;

/** Pure-Java validated configuration for the Minecraft login packet compressor. */
public final class CompressionPolicy {
    public static final int DEFAULT_THRESHOLD = 256;

    private CompressionPolicy() {}

    public static int parse(String value) {
        if (value == null) return DEFAULT_THRESHOLD;
        try {
            int n = Integer.parseInt(value.trim());
            if (n == -1 || n == 128 || n == 256 || n == 512 ||
                    n == 1024 || n == 2048 || n == 4096) return n;
        } catch (NumberFormatException ignored) { }
        return DEFAULT_THRESHOLD;
    }

    public static boolean shouldCompress(int bytes, int threshold) {
        return threshold >= 0 && bytes >= threshold;
    }
}
