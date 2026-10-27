package dev.opentogether;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;

/** RFC 5389 IPv4 STUN binding probe. Diagnostics only; not NAT traversal. */
public final class StunProbe {
    private static final int MAGIC = 0x2112A442;
    private static final int RESPONSE = 0x0101;
    private static final int XOR_MAPPED_ADDRESS = 0x0020;
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Result(String mappedIp, int mappedPort, long rttMs) {}

    private StunProbe() {}

    public static Result probe() throws IOException {
        return probe("stun.l.google.com", 19302, 2500);
    }

    public static Result probe(String server, int port, int timeoutMs) throws IOException {
        if (port < 1 || port > 65535 || timeoutMs < 200 || timeoutMs > 10000)
            throw new IllegalArgumentException("Invalid STUN configuration");
        InetAddress address = InetAddress.getByName(server);
        byte[] txid = new byte[12];
        RANDOM.nextBytes(txid);
        byte[] packet = new byte[20];
        ByteBuffer request = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN);
        request.putShort((short) 0x0001).putShort((short) 0).putInt(MAGIC).put(txid);
        long start = System.nanoTime();
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(timeoutMs);
            socket.send(new DatagramPacket(packet, packet.length, new InetSocketAddress(address, port)));
            byte[] reply = new byte[1500];
            DatagramPacket response = new DatagramPacket(reply, reply.length);
            long deadline = System.nanoTime() + Duration.ofMillis(timeoutMs).toNanos();
            while (true) {
                if (System.nanoTime() >= deadline) throw new SocketTimeoutException("STUN response timeout");
                socket.receive(response);
                if (!response.getAddress().equals(address) || response.getPort() != port) continue;
                try {
                    Result parsed = parse(response.getData(), response.getLength(), txid);
                    return new Result(parsed.mappedIp(), parsed.mappedPort(),
                            Duration.ofNanos(System.nanoTime() - start).toMillis());
                } catch (IllegalArgumentException badPacket) {
                    // Ignore malformed/unsolicited responses while timeout remains.
                }
            }
        }
    }

    static Result parse(byte[] packet, int length, byte[] txid) {
        if (length < 20 || txid.length != 12) throw new IllegalArgumentException("Short STUN packet");
        ByteBuffer b = ByteBuffer.wrap(packet, 0, length).order(ByteOrder.BIG_ENDIAN);
        int type = Short.toUnsignedInt(b.getShort());
        int attributesLength = Short.toUnsignedInt(b.getShort());
        if (type != RESPONSE || b.getInt() != MAGIC || attributesLength > length - 20 ||
                !Arrays.equals(Arrays.copyOfRange(packet, 8, 20), txid))
            throw new IllegalArgumentException("Unexpected STUN response");
        int end = 20 + attributesLength;
        for (int pos = 20; pos + 4 <= end;) {
            int attrType = Short.toUnsignedInt(b.getShort(pos));
            int size = Short.toUnsignedInt(b.getShort(pos + 2));
            if (pos + 4 + size > end) throw new IllegalArgumentException("Truncated STUN attribute");
            if (attrType == XOR_MAPPED_ADDRESS && size >= 8 && packet[pos + 5] == 0x01) {
                int port = ((packet[pos + 6] & 255) << 8 | packet[pos + 7] & 255) ^ (MAGIC >>> 16);
                if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid mapped port");
                byte[] ip = new byte[4];
                for (int i = 0; i < 4; i++) ip[i] = (byte) (packet[pos + 8 + i] ^ (MAGIC >>> (24 - 8*i)));
                try {
                    return new Result(InetAddress.getByAddress(ip).getHostAddress(), port, 0);
                } catch (IOException ex) {throw new IllegalArgumentException(ex);}
            }
            pos += 4 + ((size + 3) & ~3);
        }
        throw new IllegalArgumentException("No IPv4 XOR-MAPPED-ADDRESS attribute");
    }
}
