package dev.opentogether;

public final class InviteCodeTest {
    public static void main(String[] args) {
        expect("ABCD-EFGH", InviteCode.normalize("abcd-efgh"));
        expect("ABCD-EFGH", InviteCode.normalize(" aBcD  efGH "));
        expect("ABCD-EFGH", InviteCode.normalize("ABCD\u00a0EFGH"));
        for (String s : new String[]{null, "", "ABCD", "ABCDEF1I", "ABCD/EFGH", "ABCDEFGHI", "ABCD-EFG0", "ABCD-EFG1"}) {
            try {
                InviteCode.normalize(s);
                throw new AssertionError("Accepted malformed code: " + s);
            } catch (IllegalArgumentException expected) { }
        }
        System.out.println("Invite code tests: OK");
    }
    private static void expect(String want, String actual) {
        if (!want.equals(actual)) throw new AssertionError("Expected " + want + ", got " + actual);
    }
}
