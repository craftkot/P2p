package dev.opentogether;

import java.util.Locale;

/** Client-side validation for human-shareable Cloudflare invitation codes. */
public final class InviteCode {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private InviteCode() {}

    public static String normalize(String input) {
        if (input == null) throw new IllegalArgumentException("Missing invite code");
        String upper = input.toUpperCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(8);
        for (int i = 0; i < upper.length(); i++) {
            char ch = upper.charAt(i);
            if (Character.isWhitespace(ch) || Character.isSpaceChar(ch) || ch == '-') continue;
            if (ALPHABET.indexOf(ch) < 0 || out.length() == 8)
                throw new IllegalArgumentException("Invalid invite code");
            out.append(ch);
        }
        if (out.length() != 8) throw new IllegalArgumentException("Invite code must contain eight characters");
        return out.substring(0, 4) + "-" + out.substring(4);
    }
}
