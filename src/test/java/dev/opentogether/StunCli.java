package dev.opentogether;

import java.io.IOException;

/** Standalone network diagnostic; this is NOT a Minecraft mod. */
public final class StunCli {
    public static void main(String[] args) {
        if (args.length > 0 && "--selftest".equals(args[0])) {
            try {
                Class<?> tester = Class.forName("dev.opentogether.StunProbeTest");
                tester.getMethod("main", String[].class).invoke(null, (Object) new String[0]);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Self-test missing", e);
            }
            return;
        }
        System.out.println("OpenTogether UDP/STUN network test (NOT a Minecraft mod)");
        System.out.println("Checking UDP/STUN reachability...");
        try {
            var r = StunProbe.probe();
            System.out.println("RESULT: STUN_OK");
            System.out.println("STUN RTT: " + r.rttMs() + " ms");
            System.out.println("Mapped UDP port: " + r.mappedPort());
            System.out.println("Note: STUN reachability does NOT prove direct P2P connectivity.");
        } catch (IOException e) {
            System.out.println("RESULT: STUN_FAILED (" + e.getClass().getSimpleName() + ")");
            System.out.println("Check UDP filtering, VPN and DNS. This is not a direct P2P test.");
            System.exit(2);
        }
    }
}
