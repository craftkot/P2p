package dev.opentogether;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Captures only e4mc's local "domainAssigned" announcement, never player chat.
 * Does not retain secrets or traffic and clears its cached address on world exit.
 */
@Mod.EventBusSubscriber(modid = "opentogether", value = Dist.CLIENT)
public final class FriendInvites {
    private static final Logger LOG = LoggerFactory.getLogger("OpenTogether");
    private static volatile String publishedDomain;
    private static boolean copyWhenReady;
    private static long copyRequestExpiresAt;
    private static Object armedServer;

    private FriendInvites() {}

    public static String getPublishedDomain() {
        return publishedDomain;
    }

    /** Called only when the host explicitly opens the invitation workflow. */
    public static void requestCopy() {
        copyWhenReady = true;
        copyRequestExpiresAt = System.nanoTime() + java.util.concurrent.TimeUnit.MINUTES.toNanos(3);
        armedServer = Minecraft.getInstance().getSingleplayerServer();
        String existing = publishedDomain;
        if (existing != null) copyNow(existing);
    }

    /** Chat message is trusted by its translatable key, not by plain-text contents. */
    public static void onLocalChat(Component component) {
        if (component == null) return;
        if (!(component.getContents() instanceof TranslatableContents translatable)) return;
        String key = translatable.getKey();
        if ("text.e4mc_minecraft.closeServer".equals(key) ||
                "text.e4mc_minecraft.error".equals(key)) {
            clear();
            return;
        }
        if (!"text.e4mc_minecraft.domainAssigned".equals(key)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() == null || !mc.getSingleplayerServer().isPublished()) return;
        String domain = InviteAddress.extract(component.getString());
        if (domain == null) return;
        publishedDomain = domain;
        LOG.info("[OpenTogether] Friend invite address detected and ready to share");
        if (copyWhenReady && System.nanoTime() < copyRequestExpiresAt && armedServer == mc.getSingleplayerServer()) copyNow(domain);
    }

    private static void copyNow(String domain) {
        Minecraft.getInstance().keyboardHandler.setClipboard(domain);
        copyWhenReady = false;
        armedServer = null;
        LOG.info("[OpenTogether] Friend invitation copied to clipboard");
    }

    public static void clear() {
        publishedDomain = null;
        copyWhenReady = false;
        armedServer = null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (copyWhenReady && (System.nanoTime() >= copyRequestExpiresAt || armedServer != mc.getSingleplayerServer())) {
            copyWhenReady = false;
            armedServer = null;
        }
        if (mc.getSingleplayerServer() == null || !mc.getSingleplayerServer().isPublished()) {
            if (publishedDomain != null) clear();
        }
    }
}
