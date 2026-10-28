package dev.opentogether;

import java.util.Random;
import java.util.zip.Deflater;

public final class CompressionPolicyTest {
    public static void main(String[] args) {
        eq(256, CompressionPolicy.parse(null));
        eq(-1, CompressionPolicy.parse(" -1 "));
        for (int allowed : new int[]{128, 256, 512, 1024, 2048, 4096})
            eq(allowed, CompressionPolicy.parse(Integer.toString(allowed)));
        for (String bad : new String[]{"0", "-2", "1", "257", "999999", "NaN"})
            eq(256, CompressionPolicy.parse(bad));
        check(!CompressionPolicy.shouldCompress(100, 256));
        check(CompressionPolicy.shouldCompress(256, 256));
        check(!CompressionPolicy.shouldCompress(2000, -1));

        // Sanity benchmark with synthetic *illustrative* payloads only;
        // these are NOT real Minecraft chunk packets.
        byte[] easy = new byte[65536];
        for (int i = 0; i < easy.length; ++i) easy[i] = (byte) (i % 13);
        byte[] random = new byte[65536];
        new Random(42).nextBytes(random);
        int compressedEasy = sizeAfterZlib(easy);
        int compressedRandom = sizeAfterZlib(random);
        check(compressedEasy < easy.length / 10);
        check(compressedRandom >= random.length / 2);
        System.out.printf("Compression policy OK, synthetic 64KiB: repetitive=%dB incompressible=%dB%n",
                compressedEasy, compressedRandom);
    }

    private static int sizeAfterZlib(byte[] payload) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        try {
            deflater.setInput(payload);
            deflater.finish();
            byte[] out = new byte[payload.length + 512];
            int len = deflater.deflate(out);
            check(deflater.finished());
            return len;
        } finally {
            deflater.end();
        }
    }

    private static void check(boolean value) {
        if (!value) throw new AssertionError("Compression test failed");
    }

    private static void eq(int expected, int actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + " got " + actual);
    }
}
