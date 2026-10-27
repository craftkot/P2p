package dev.opentogether;

import net.minecraftforge.fml.loading.FMLPaths;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Optional HTTPS worker URL persisted in the normal Forge config directory. */
final class CloudflareConfig {
    private static final String KEY = "workerUrl";
    private CloudflareConfig() {}

    static String getUrl() {
        Path path = configPath();
        if (Files.isRegularFile(path)) {
            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
                String configured = properties.getProperty(KEY, "").trim();
                if (isValidUrl(configured)) return stripTrailingSlash(configured);
            } catch (IOException ex) {
                CloudflareApi.LOG.warn("[OpenTogether] Could not read Cloudflare configuration ({})",
                        ex.getClass().getSimpleName());
            }
        }
        String legacy = System.getProperty("opentogether.api", "").trim();
        return isValidUrl(legacy) ? stripTrailingSlash(legacy) : "";
    }

    static void saveUrl(String url) throws IOException {
        String trimmed = url.trim();
        if (!trimmed.isEmpty() && !isValidUrl(trimmed))
            throw new IllegalArgumentException("Only an HTTPS Worker URL is allowed (without a path or query)");
        Path path = configPath();
        Files.createDirectories(path.getParent());
        Properties properties = new Properties();
        properties.setProperty(KEY, stripTrailingSlash(trimmed));
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            properties.store(writer, "OpenTogether invite broker. Do not put tokens or secrets here.");
        }
    }

    static boolean isValidUrl(String value) {
        if (value == null || value.isBlank() || value.length() > 220) return false;
        try {
            URI uri = URI.create(value.trim());
            String path = uri.getRawPath();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getUserInfo() == null
                    && uri.getRawQuery() == null
                    && uri.getRawFragment() == null
                    && (path == null || path.isEmpty() || path.equals("/"));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve("opentogether.properties");
    }
}
