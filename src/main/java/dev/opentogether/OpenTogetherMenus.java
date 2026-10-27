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

@Mod.EventBusSubscriber(modid="opentogether",value=Dist.CLIENT)
public final class OpenTogetherMenus {
    @SubscribeEvent
    public static void onScreen(ScreenEvent.Init.Post e) {
        var screen=e.getScreen();
        if(screen instanceof TitleScreen || screen instanceof JoinMultiplayerScreen) {
            e.addListener(Button.builder(Component.literal("Подключиться к другу"), b->Minecraft.getInstance().setScreen(new OpenTogetherScreen(screen,false)))
                    .bounds(screen.width-174,6,166,20).build());
            if (screen instanceof TitleScreen) {
                org.slf4j.LoggerFactory.getLogger("OpenTogether").info("[OpenTogether] Title-screen friend button registered");
            }
        }
        if(screen instanceof PauseScreen && Minecraft.getInstance().getSingleplayerServer() != null) {
            e.addListener(Button.builder(Component.literal("Пригласить друга"), b->Minecraft.getInstance().setScreen(new OpenTogetherScreen(screen,true)))
                    .bounds(screen.width-174,6,166,20).build());
        }
    }
}
