package dev.opentogether;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/** Runs with Java 17; no Forge or game installation required. */
public final class StunProbeTest {
    public static void main(String[] args) {
        byte[] txid = new byte[12];
        Arrays.fill(txid, (byte) 7);
        byte[] raw = new byte[32];
        ByteBuffer b = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN);
        b.putShort((short) 0x0101).putShort((short) 12).putInt(0x2112A442).put(txid);
        b.putShort((short) 0x0020).putShort((short) 8).put((byte) 0).put((byte) 1);
        b.putShort((short) (25565 ^ 0x2112));
        byte[] ip = {(byte) 203, 0, 113, 7};
        for (int i = 0; i < 4; i++)
            b.put((byte) (ip[i] ^ (0x2112A442 >>> (24 - 8*i))));
        var result = StunProbe.parse(raw, raw.length, txid);
        check(result.mappedIp().equals("203.0.113.7"), "wrong IP");
        check(result.mappedPort() == 25565, "wrong port");
        byte[] wrong = txid.clone();
        wrong[0]++;
        try {
            StunProbe.parse(raw, raw.length, wrong);
            throw new AssertionError("accepts unexpected transaction ID");
        } catch (IllegalArgumentException expected) {}
        try {
            StunProbe.parse(raw, 27, txid);
            throw new AssertionError("accepts truncated packet");
        } catch (IllegalArgumentException expected) {}
        System.out.println("STUN parse test OK");
    }
    private static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
}
