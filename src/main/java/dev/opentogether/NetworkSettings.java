package dev.opentogether;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Read-only-at-login networking settings for the local Minecraft LAN host. */
public final class NetworkSettings {
    private static final Logger LOG = LoggerFactory.getLogger("OpenTogether");

    private NetworkSettings() {}

    public static int configuredThreshold() {
        return Holder.SETTINGS.threshold();
    }

    public static CompressionTuning.Mode configuredMode() {
        return Holder.SETTINGS.mode();
    }

    private record Settings(int threshold, CompressionTuning.Mode mode) {}

    private static final class Holder {
        private static final Settings SETTINGS = load();
    }

    static int parseThreshold(String text) {
        return CompressionPolicy.parse(text);
    }

    private static Settings load() {
        Path file = FMLPaths.CONFIGDIR.get().resolve("opentogether-network.properties");
        Properties properties = new Properties();

        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException ex) {
                LOG.warn("[OpenTogether] Could not read network config type={}", ex.getClass().getSimpleName());
            }
        } else {
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, """
                        # OpenTogether networking, only for singleplayer worlds shared over LAN/e4mc.
                        # Valid thresholds: -1 (no compression), 128, 256, 512, 1024, 2048, 4096.
                        # 256 bytes recommended: compresses chunks but avoids tiny voice-sensitive traffic.
                        compressionThreshold=256
                        # adaptive: zlib level 1 (<4KiB), 6 (<16KiB), 9 (large chunk-sized packets)
                        # maximum: zlib level 9 for all compressed packets (may add packet latency)
                        # vanilla: zlib level 6
                        compressionMode=adaptive
                        # Restart Minecraft to change these settings.
                        """, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                LOG.warn("[OpenTogether] Could not create network config type={}", ex.getClass().getSimpleName());
            }
        }

        String rawThreshold = properties.getProperty("compressionThreshold", "256");
        int threshold = parseThreshold(rawThreshold);
        String rawMode = properties.getProperty("compressionMode", "adaptive");
        CompressionTuning.Mode mode = CompressionTuning.parseMode(rawMode);
        LOG.info("[OpenTogether] LAN compression threshold={} bytes; mode={}", threshold, mode);
        return new Settings(threshold, mode);
    }
}
