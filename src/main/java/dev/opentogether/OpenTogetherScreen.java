package dev.opentogether;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

/**
 * Simple friends-only workflow backed by an optional trusted LAN tunnel.
 * e4mc handles the relay; OpenTogether never connects to a third-party
 * service or automatically installs other mods.
 */
final class OpenTogetherScreen extends Screen {
    private static final String MOD_PAGE = "https://modrinth.com/mod/e4mc";
    private final Screen previous;
    private final boolean inviting;
    private EditBox addressField;
    private String status = "";
    private String rememberedAddress = "";

    OpenTogetherScreen(Screen previous, boolean inviting) {
        super(Component.literal("OpenTogether"));
        this.previous = previous;
        this.inviting = inviting;
    }

    @Override
    protected void init() {
        int x = width / 2 - 140;
        int y = height / 2 - 65;

        if (inviting) {
            boolean hasTunnelMod = ModList.get().isLoaded("e4mc");
            boolean open = isLanOpen();
            if (!open) {
                addRenderableWidget(Button.builder(
                        Component.literal("1. Открыть мир для сети (LAN)"),
                        b -> minecraft.setScreen(new ShareToLanScreen(previous)))
                        .bounds(x, y + 26, 280, 20).build());
                status = "Сначала открой мир для сети";
            } else {
                addRenderableWidget(Button.builder(
                        Component.literal("Мир уже открыт для LAN"),
                        b -> {}).bounds(x, y + 26, 280, 20).build()).active = false;
                status = hasTunnelMod
                        ? "Адрес e4mc появится в чате; щёлкни по нему"
                        : "Нужен e4mc, чтобы друзья подключились извне";
            }

            addRenderableWidget(Button.builder(
                    Component.literal(hasTunnelMod ? "e4mc установлен" : "2. Скачать e4mc (хозяину мира)"),
                    b -> Util.getPlatform().openUri(MOD_PAGE))
                    .bounds(x, y + 51, 280, 20).build());

            addRenderableWidget(Button.builder(
                    Component.literal("Вернуться в игру / посмотреть чат"),
                    b -> minecraft.setScreen(null)).bounds(x, y + 76, 280, 20).build());
        } else {
            addressField = new EditBox(font, x, y + 26, 280, 20,
                    Component.literal("Адрес сервера друга"));
            addressField.setMaxLength(255);
            addressField.setValue(rememberedAddress);
            addressField.setHint(Component.literal("пример: sunny-fox.eu.e4mc.link"));
            addRenderableWidget(addressField);
            addRenderableWidget(Button.builder(Component.literal("Подключиться к другу"),
                    b -> connect()).bounds(x, y + 52, 280, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Вставить из буфера"),
                    b -> {
                        String text = minecraft.keyboardHandler.getClipboard();
                        if (text != null) addressField.setValue(text.trim());
                    }).bounds(x, y + 77, 280, 20).build());
            status = "Введи адрес e4mc или обычного сервера";
            setInitialFocus(addressField);
        }

        addRenderableWidget(Button.builder(Component.literal("Назад"), b -> onClose())
                .bounds(x, y + 106, 280, 20).build());
    }

    private boolean isLanOpen() {
        IntegratedServer server = minecraft.getSingleplayerServer();
        return server != null && server.isPublished();
    }

    private void connect() {
        String endpoint = addressField.getValue().trim();
        rememberedAddress = endpoint;
        // Explicitly disallow pasted URLs, spaces and unexpected URL schemes.
        if (!endpoint.matches("(?i)^[a-z0-9][a-z0-9.-]{0,252}(?::[0-9]{1,5})?$")) {
            status = "Нужен адрес сервера, не ссылка https:// или код";
            return;
        }
        int colon = endpoint.lastIndexOf(':');
        if (colon >= 0) {
            try {
                int port = Integer.parseInt(endpoint.substring(colon + 1));
                if (port < 1 || port > 65535) {
                    status = "Неверный номер TCP-порта";
                    return;
                }
            } catch (NumberFormatException ex) {
                status = "Неверный TCP-порт";
                return;
            }
        }
        try {
            ServerAddress address = ServerAddress.parseString(endpoint);
            ServerData data = new ServerData("OpenTogether friend", endpoint, false);
            ConnectScreen.startConnecting(previous, minecraft, address, data, false);
        } catch (RuntimeException ex) {
            status = "Подключиться не удалось; проверь адрес и Minecraft";
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        int center = width / 2;
        g.drawCenteredString(font, inviting ? "Пригласить друга (через e4mc)"
                : "Подключиться к другу", center, height / 2 - 86, 0xFFFFFF);
        g.drawCenteredString(font, status, center, height / 2 + 73, 0xDDDDDD);
        if (inviting) {
            g.drawCenteredString(font,
                    ModList.get().isLoaded("e4mc")
                            ? "Бесплатный временный туннель без проброса портов"
                            : "Без e4mc мир доступен только в локальной сети",
                    center, height / 2 + 91, 0xFFBB66);
        } else {
            g.drawCenteredString(font,
                    "Cloudflare, код и аккаунт не нужны",
                    center, height / 2 + 91, 0xAADD99);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
