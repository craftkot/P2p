package dev.opentogether.fabric;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InviteCodeTest {
    @Test
    void hostAndPortRoundTrip() {
        String code = InviteCode.encode("Minecraft://192.168.1.44:34567");
        assertTrue(code.startsWith("OT1."));
        assertEquals("192.168.1.44:34567", InviteCode.resolve(code));
    }

    @Test
    void publicDomainRoundTripAndNormalization() {
        String code = InviteCode.encode("Play.Example.org:25565");
        assertEquals("play.example.org:25565", InviteCode.resolve(code));
        assertEquals("play.example.org:25565", InviteCode.resolve("play.example.org:25565"));
    }

    @Test
    void plainMinecraftServerMayUseDefaultPort() {
        assertEquals("server.example.org", InviteCode.resolve("server.example.org"));
        assertThrows(IllegalArgumentException.class, () -> InviteCode.encode("server.example.org"));
    }

    @Test
    void rejectUnsafeAddressAndBadPort() {
        for (String input : new String[] {
                "", "https://example.com", "a.example:0", "a.example:65536",
                "a.example:-1", "example.com:abc", "example..com:25565",
                "example.com/path:25565", "user@example.com:25565",
                "example.com:25565#evil", "::1", "example.com:25 565"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> InviteCode.normalizeEndpoint(input), input);
        }
    }

    @Test
    void rejectInvalidCodes() {
        for (String code : new String[] {
                "OT1.", "OT1.not*base64", "OT1.YQ", "OT1.eA",
                "OT1." + "A".repeat(510)}) {
            assertThrows(IllegalArgumentException.class, () -> InviteCode.resolve(code), code);
        }
    }

    @Test
    void canonicalCodesAreCaseSensitive() {
        String code = InviteCode.encode("host.example:25565");
        assertThrows(IllegalArgumentException.class, () -> InviteCode.resolve(code.toLowerCase()));
    }
}
