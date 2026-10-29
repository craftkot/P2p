package dev.opentogether.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

/** Client-only bridge for Minecraft 26.3 Fabric. */
public final class OpenTogetherFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen || screen instanceof JoinMultiplayerScreen
                    || screen instanceof PauseScreen) {
                Button button = Button.builder(Component.literal("OpenTogether"),
                        b -> client.gui.setScreen(new FriendsScreen(screen)))
                        .bounds(Math.max(4, scaledWidth - 124), 7, 116, 20).build();
                Screens.getWidgets(screen).add(button);
            }
        });
    }
}
