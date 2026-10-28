package dev.opentogether;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "opentogether", value = Dist.CLIENT)
public final class OpenTogetherMenus {
    private OpenTogetherMenus() {}

    @SubscribeEvent
    public static void onScreen(ScreenEvent.Init.Post event) {
        var screen = event.getScreen();
        if (screen instanceof TitleScreen || screen instanceof JoinMultiplayerScreen ||
                screen instanceof PauseScreen) {
            int buttonWidth = Math.min(130, Math.max(88, screen.width / 3));
            event.addListener(Button.builder(Component.literal("OpenTogether"),
                    button -> Minecraft.getInstance().setScreen(new OpenTogetherHubScreen(screen)))
                    .bounds(screen.width - buttonWidth - 7, 7, buttonWidth, 20).build());
            if (screen instanceof TitleScreen) {
                org.slf4j.LoggerFactory.getLogger("OpenTogether")
                        .info("[OpenTogether] Title-screen friend button registered");
            }
        }
    }
}
