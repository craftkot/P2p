package dev.opentogether;

import java.util.Random;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

public final class CompressionTuningTest {
    public static void main(String[] args) throws Exception {
        eq(CompressionTuning.Mode.ADAPTIVE, CompressionTuning.parseMode(null));
        eq(CompressionTuning.Mode.ADAPTIVE, CompressionTuning.parseMode(" ADAPTIVE "));
        eq(CompressionTuning.Mode.ADAPTIVE, CompressionTuning.parseMode("garbage"));
        eq(CompressionTuning.Mode.MAXIMUM, CompressionTuning.parseMode("max"));
        eq(CompressionTuning.Mode.MAXIMUM, CompressionTuning.parseMode("maximum"));
        eq(CompressionTuning.Mode.VANILLA, CompressionTuning.parseMode("vanilla"));
        eq(1, CompressionTuning.levelForBytes(CompressionTuning.Mode.ADAPTIVE, 1024));
        eq(1, CompressionTuning.levelForBytes(CompressionTuning.Mode.ADAPTIVE, 4095));
        eq(6, CompressionTuning.levelForBytes(CompressionTuning.Mode.ADAPTIVE, 4096));
        eq(6, CompressionTuning.levelForBytes(CompressionTuning.Mode.ADAPTIVE, 16383));
        eq(9, CompressionTuning.levelForBytes(CompressionTuning.Mode.ADAPTIVE, 16384));
        eq(9, CompressionTuning.levelForBytes(CompressionTuning.Mode.MAXIMUM, 10));
        eq(6, CompressionTuning.levelForBytes(CompressionTuning.Mode.VANILLA, 100000));
        boolean rejected = false;
        try { CompressionTuning.levelForBytes(CompressionTuning.Mode.MAXIMUM, -1); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected);

        byte[] chunkLike = new byte[64 * 1024];
        Random rand = new Random(0x1eaf);
        // Synthetic palette-like byte distribution; NOT a real Minecraft chunk capture.
        for (int i = 0; i < chunkLike.length; i++)
            chunkLike[i] = (byte) ((i % 32 == 0) ? rand.nextInt(16) : (i / 1024) % 7);
        byte[] random = new byte[64 * 1024];
        rand.nextBytes(random);

        for (byte[] input : new byte[][]{chunkLike, random}) {
            for (CompressionTuning.Mode mode : CompressionTuning.Mode.values()) {
                int level = CompressionTuning.levelForBytes(mode, input.length);
                byte[] packed = compress(input, level);
                byte[] unpacked = decompress(packed, input.length);
                check(java.util.Arrays.equals(input, unpacked));
                if (input == chunkLike) {
                    System.out.printf("Synthetic palette-like 64KiB: %s level=%d -> %d B%n", mode, level, packed.length);
                }
            }
        }
        System.out.println("Adaptive vanilla-zlib compression policy: OK");
    }

    private static byte[] compress(byte[] payload, int level) {
        Deflater d = new Deflater(level);
        try {
            d.setInput(payload);
            d.finish();
            byte[] result = new byte[payload.length + 512];
            int bytes = d.deflate(result);
            check(d.finished());
            return java.util.Arrays.copyOf(result, bytes);
        } finally { d.end(); }
    }

    private static byte[] decompress(byte[] packet, int expected) throws DataFormatException {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(packet);
            byte[] result = new byte[expected];
            int amount = inflater.inflate(result);
            check(amount == expected && inflater.finished());
            return result;
        } finally { inflater.end(); }
    }

    private static void eq(Object want, Object got) {
        if (!want.equals(got)) throw new AssertionError(want + " != " + got);
    }
    private static void check(boolean result) {
        if (!result) throw new AssertionError("Compression tuning assertion failed");
    }
}
