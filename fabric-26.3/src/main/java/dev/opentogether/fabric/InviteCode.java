package dev.opentogether.fabric;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

/**
 * Transport-neutral invitation containing a normal Minecraft TCP endpoint.
 *
 * OT1 is an address container, not an authentication credential or an
 * encryption key. A code alone cannot bypass NAT or make a private IP routable.
 */
public final class InviteCode {
    public static final String PREFIX = "OT1.";
    private static final int MAX_ENDPOINT = 255;
    private static final int MAX_CODE = 512;

    private InviteCode() {}

    public static String encode(String endpoint) {
        String normalized = normalizeEndpoint(endpoint);
        if (normalized.lastIndexOf(':') < 0)
            throw new IllegalArgumentException("Для приглашения укажи порт мира");
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(normalized.getBytes(StandardCharsets.UTF_8));
        return PREFIX + payload;
    }

    public static String resolve(String input) {
        if (input == null) throw new IllegalArgumentException("Адрес пуст");
        String raw = input.trim();
        if (raw.length() >= PREFIX.length() && raw.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            if (raw.length() > MAX_CODE)
                throw new IllegalArgumentException("Слишком длинный код");
            String data = raw.substring(PREFIX.length());
            if (data.isEmpty() || !data.matches("[A-Za-z0-9_-]+"))
                throw new IllegalArgumentException("Неверный код приглашения");
            try {
                byte[] decoded = Base64.getUrlDecoder().decode(data);
                if (decoded.length > MAX_ENDPOINT)
                    throw new IllegalArgumentException("Слишком длинный адрес в коде");
                String normalized = normalizeEndpoint(new String(decoded, StandardCharsets.UTF_8));
                if (!encode(normalized).substring(PREFIX.length()).equals(data))
                    throw new IllegalArgumentException("Неканонический код");
                return normalized;
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("Неверный код приглашения", error);
            }
        }
        return normalizeEndpoint(raw);
    }

    public static String normalizeEndpoint(String input) {
        if (input == null) throw new IllegalArgumentException("Адрес пуст");
        String raw = input.trim();
        if (raw.regionMatches(true, 0, "minecraft://", 0, 12))
            raw = raw.substring(12);
        if (raw.endsWith("/")) raw = raw.substring(0, raw.length() - 1);
        if (raw.isEmpty() || raw.length() > MAX_ENDPOINT || raw.contains("@")
                || raw.contains("/") || raw.contains(" ") || raw.contains("#")
                || raw.contains("?") || raw.contains("\\"))
            throw new IllegalArgumentException("Неверный адрес Minecraft");

        int colon = raw.indexOf(':');
        if (colon != raw.lastIndexOf(':'))
            throw new IllegalArgumentException("IPv6 пока не поддерживается кодом OT1");
        String host = colon < 0 ? raw : raw.substring(0, colon);
        if (host.length() > 253 || host.isEmpty())
            throw new IllegalArgumentException("Неверное имя хоста");
        if (colon >= 0) {
            String portText = raw.substring(colon + 1);
            if (!portText.matches("[0-9]{1,5}"))
                throw new IllegalArgumentException("Неверный порт");
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535)
                throw new IllegalArgumentException("Порт должен быть 1–65535");
            raw = host + ":" + port;
        }
        if (!host.matches("(?i)[a-z0-9.-]+"))
            throw new IllegalArgumentException("Неверные символы в хосте");
        for (String label : host.split("\\.", -1)) {
            if (label.isEmpty() || label.length() > 63
                    || !Character.isLetterOrDigit(label.charAt(0))
                    || !Character.isLetterOrDigit(label.charAt(label.length() - 1)))
                throw new IllegalArgumentException("Неверный домен");
        }
        return raw.toLowerCase(Locale.ROOT);
    }
}
