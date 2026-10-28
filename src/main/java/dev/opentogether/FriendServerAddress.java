package dev.opentogether;

import java.util.Locale;

/** Validates the text pasted into Minecraft's vanilla multiplayer connector. */
public final class FriendServerAddress {
    private FriendServerAddress() {}

    public static String normalize(String raw) {
        if (raw == null) throw new IllegalArgumentException("Пустой адрес");
        String hostAndPort = raw.trim();
        if (hostAndPort.regionMatches(true, 0, "minecraft://", 0, 12))
            hostAndPort = hostAndPort.substring(12);
        if (hostAndPort.endsWith("/"))
            hostAndPort = hostAndPort.substring(0, hostAndPort.length() - 1);
        if (hostAndPort.length() < 1 || hostAndPort.length() > 255)
            throw new IllegalArgumentException("Адрес пустой или слишком длинный");
        if (!hostAndPort.matches("(?i)[a-z0-9](?:[a-z0-9.-]{0,251}[a-z0-9])?(?::[0-9]{1,5})?"))
            throw new IllegalArgumentException("Неверный адрес Minecraft");
        String host = hostAndPort;
        int colon = hostAndPort.lastIndexOf(':');
        if (colon >= 0) {
            int port;
            try { port = Integer.parseInt(hostAndPort.substring(colon + 1)); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("Неверный порт", ex); }
            if (port < 1 || port > 65535) throw new IllegalArgumentException("Неверный порт");
            host = hostAndPort.substring(0, colon);
        }
        for (String label : host.split("\\.", -1)) {
            if (label.isEmpty() || label.length() > 63 || label.startsWith("-") || label.endsWith("-"))
                throw new IllegalArgumentException("Неверное имя сервера");
        }
        return hostAndPort.toLowerCase(Locale.ROOT);
    }
}
