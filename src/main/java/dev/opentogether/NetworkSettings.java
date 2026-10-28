package dev.opentogether;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Login-time compression tuning for published integrated servers only.
 * Never changes a live connection's packet format: vanilla negotiates it.
 */
public final class NetworkSettings {
    private static final Logger LOG = LoggerFactory.getLogger("OpenTogether");
    private static final int DEFAULT = CompressionPolicy.DEFAULT_THRESHOLD;

    private NetworkSettings() {}

    public static int configuredThreshold() {
        return Holder.THRESHOLD;
    }

    private static final class Holder {
        private static final int THRESHOLD = load();
    }

    static int parseThreshold(String text) {
        return CompressionPolicy.parse(text);
    }

    private static int load() {
        Path file = FMLPaths.CONFIGDIR.get().resolve("opentogether-network.properties");
        Properties props = new Properties();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException ex) {
                LOG.warn("[OpenTogether] Network configuration unavailable ({})", ex.getClass().getSimpleName());
            }
        } else {
            try {
                Files.createDirectories(file.getParent());
                // Properties.store is not needed: keep comments human-readable in UTF-8.
                Files.writeString(file, """
                        # OpenTogether published-world network tuning
                        # Compress remote Minecraft packets from this size (in bytes).
                        # Valid: -1 (vanilla/off), 128, 256, 512, 1024, 2048, 4096.
                        # Default 256: saves relay upload, at some CPU cost.
                        # Restart the world/connection to apply changes.
                        compressionThreshold=256
                        """, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                LOG.warn("[OpenTogether] Could not create network configuration ({})", ex.getClass().getSimpleName());
            }
        }
        String configured = props.getProperty("compressionThreshold", Integer.toString(DEFAULT));
        int result = parseThreshold(configured);
        if (result != -1 && !Integer.toString(result).equals(configured.trim()))
            LOG.warn("[OpenTogether] Invalid compressionThreshold. Using 256.");
        LOG.info("[OpenTogether] Integrated LAN compression setting: {}", result);
        return result;
    }
}
