package dev.opentogether;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/** An RFC5389 IPv4 Binding probe. This only diagnoses UDP reachability, not P2P. */
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

        Inet4Address address = null;
        for (InetAddress resolved : InetAddress.getAllByName(server)) {
            if (resolved instanceof Inet4Address ipv4) {
                address = ipv4;
                break;
            }
        }
        if (address == null) throw new IOException("STUN has no IPv4 address");

        byte[] txid = new byte[12];
        RANDOM.nextBytes(txid);
        byte[] requestBytes = new byte[20];
        ByteBuffer request = ByteBuffer.wrap(requestBytes).order(ByteOrder.BIG_ENDIAN);
        request.putShort((short) 0x0001).putShort((short) 0).putInt(MAGIC).put(txid);

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.send(new DatagramPacket(requestBytes, requestBytes.length,
                    new InetSocketAddress(address, port)));
            long started = System.nanoTime();
            long deadline = started + Duration.ofMillis(timeoutMs).toNanos();
            byte[] receivedBytes = new byte[1500];
            DatagramPacket response = new DatagramPacket(receivedBytes, receivedBytes.length);

            while (true) {
                long leftNanos = deadline - System.nanoTime();
                if (leftNanos <= 0) throw new SocketTimeoutException("STUN response timeout");
                socket.setSoTimeout((int) Math.min(timeoutMs,
                        Math.max(1L, TimeUnit.NANOSECONDS.toMillis(leftNanos))));
                // DatagramPacket length shrinks after receiving, so reset before the next call.
                response.setLength(receivedBytes.length);
                try {
                    socket.receive(response);
                } catch (SocketTimeoutException ex) {
                    if (System.nanoTime() >= deadline) throw ex;
                    continue;
                }
                if (!response.getAddress().equals(address) || response.getPort() != port) continue;

                try {
                    Result mapped = parse(response.getData(), response.getLength(), txid);
                    return new Result(mapped.mappedIp(), mapped.mappedPort(),
                            Duration.ofNanos(System.nanoTime() - started).toMillis());
                } catch (IllegalArgumentException ignored) {
                    // An invalid datagram cannot extend the overall timeout.
                }
            }
        }
    }

    static Result parse(byte[] packet, int length, byte[] txid) {
        if (packet == null || txid == null || txid.length != 12 ||
                length < 20 || length > packet.length)
            throw new IllegalArgumentException("Invalid STUN packet length");

        ByteBuffer b = ByteBuffer.wrap(packet, 0, length).order(ByteOrder.BIG_ENDIAN);
        int type = Short.toUnsignedInt(b.getShort());
        int attributesLength = Short.toUnsignedInt(b.getShort());
        if (type != RESPONSE || b.getInt() != MAGIC || attributesLength > length - 20 ||
                (attributesLength & 3) != 0 ||
                !Arrays.equals(Arrays.copyOfRange(packet, 8, 20), txid))
            throw new IllegalArgumentException("Unexpected STUN response");

        int end = 20 + attributesLength;
        for (int pos = 20; pos + 4 <= end;) {
            int attrType = Short.toUnsignedInt(b.getShort(pos));
            int size = Short.toUnsignedInt(b.getShort(pos + 2));
            if (pos + 4 + size > end)
                throw new IllegalArgumentException("Truncated STUN attribute");

            if (attrType == XOR_MAPPED_ADDRESS && size >= 8 && packet[pos + 5] == 1) {
                int port = (((packet[pos + 6] & 255) << 8) | (packet[pos + 7] & 255))
                        ^ (MAGIC >>> 16);
                if (port < 1 || port > 65535)
                    throw new IllegalArgumentException("Invalid mapped port");

                byte[] ip = new byte[4];
                for (int i = 0; i < 4; i++)
                    ip[i] = (byte) (packet[pos + 8 + i] ^ (MAGIC >>> (24 - 8 * i)));

                try {
                    return new Result(InetAddress.getByAddress(ip).getHostAddress(), port, 0);
                } catch (IOException ex) {
                    throw new IllegalArgumentException("Invalid mapped IP", ex);
                }
            }

            int padded = (size + 3) & ~3;
            if (pos + 4 + padded > end) throw new IllegalArgumentException("Invalid STUN padding");
            pos += 4 + padded;
        }

        throw new IllegalArgumentException("No IPv4 XOR-MAPPED-ADDRESS attribute");
    }
}
