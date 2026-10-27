package dev.opentogether;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** String-only parsing: accepts nothing except the trusted e4mc domain suffix. */
public final class InviteAddress {
    private static final Pattern HOST = Pattern.compile(
            "(?i)(?<![a-z0-9.-])((?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.){1,5}e4mc\\.link)(?![a-z0-9.-])");

    private InviteAddress() { }

    /** Returns the hostname or null if no public e4mc invitation exists. */
    public static String extract(String message) {
        if (message == null || message.length() > 2048) return null;
        Matcher matcher = HOST.matcher(message);
        return matcher.find() ? matcher.group(1).toLowerCase(Locale.ROOT) : null;
    }

    public static boolean isTrusted(String host) {
        return host != null && host.equals(extract(host));
    }
}
