package dev.opentogether;

public final class FriendServerAddressTest {
    public static void main(String[] args) {
        check("forest.eu.e4mc.link", " FOREST.eu.e4mc.link ");
        check("forest.eu.e4mc.link", "minecraft://forest.eu.e4mc.link/");
        check("example.net:25566", "EXAMPLE.NET:25566");
        check("localhost:25565", "localhost:25565");
        check("192.168.1.10:25565", "192.168.1.10:25565");
        for (String raw : new String[]{
                "", " ", "https://example.com", "good.e4mc.link/other",
                "x..com", "a.-bad.com", "-bad.com", "bad.com:",
                "bad.com:0", "bad.com:65536", "bad.com:123456",
                "bad.com other", "bad.com@evil", "bad.com?abc", "bad.com#frag",
                "bad.com\nevil", "bad.com.", "a".repeat(256)
        }) {
            try {
                FriendServerAddress.normalize(raw);
                throw new AssertionError("Accepted malformed address: " + raw);
            } catch (IllegalArgumentException expected) { }
        }
        System.out.println("FriendServerAddress tests: OK");
    }
    private static void check(String expected, String raw) {
        String got = FriendServerAddress.normalize(raw);
        if (!expected.equals(got)) throw new AssertionError(expected + " != " + got);
    }
}
