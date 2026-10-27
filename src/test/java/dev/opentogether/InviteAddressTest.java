package dev.opentogether;

public final class InviteAddressTest {
    public static void main(String[] args) {
        check("freestyle-platform.na.e4mc.link",
                InviteAddress.extract("Local game hosted on domain [freestyle-platform.na.e4mc.link]"));
        check("fun.eu.e4mc.link", InviteAddress.extract("Игра доступна: FUN.EU.E4MC.LINK"));
        check(null, InviteAddress.extract("e4mc.link"));
        check(null, InviteAddress.extract("fake.e4mc.link.evil.com"));
        check(null, InviteAddress.extract("https://not-e4mc.example.org"));
        check(null, InviteAddress.extract("user@evile4mc.link.bad.host"));
        check(null, InviteAddress.extract(null));
        check(null, InviteAddress.extract("x".repeat(2049)));
        assertTrue(InviteAddress.isTrusted("town.eu.e4mc.link"));
        assertFalse(InviteAddress.isTrusted("town.eu.e4mc.link:25565"));
        System.out.println("InviteAddress tests OK");
    }

    private static void check(String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual))
            throw new AssertionError("Expected " + expected + " but got " + actual);
    }
    private static void assertTrue(boolean result) {if (!result) throw new AssertionError("Expected true");}
    private static void assertFalse(boolean result) {if (result) throw new AssertionError("Expected false");}
}
