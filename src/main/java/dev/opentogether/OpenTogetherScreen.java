package dev.opentogether;

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

/** One-JAR invitation screen. e4mc is bundled; users never install a second mod. */
final class OpenTogetherScreen extends Screen {
    private final Screen previous;
    private final boolean inviting;
    private EditBox addressField;
    private Button copyButton;
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
        int y = height / 2 - 67;
        if (inviting) {
            if (!isLanOpen()) {
                addRenderableWidget(Button.builder(
                        Component.literal("Открыть мир для друзей"),
                        b -> {
                            FriendInvites.requestCopy();
                            minecraft.setScreen(new ShareToLanScreen(previous));
                        }).bounds(x, y + 28, 280, 20).build());
                status = "Открой LAN — адрес появится и скопируется автоматически";
            } else {
                copyButton = addRenderableWidget(Button.builder(
                        Component.literal("Скопировать приглашение"),
                        b -> {
                            FriendInvites.requestCopy();
                            status = FriendInvites.getPublishedDomain() != null
                                    ? "Адрес скопирован. Отправь его другу."
                                    : "Ожидаем адрес от встроенного e4mc...";
                        }).bounds(x, y + 28, 280, 20).build());
                copyButton.active = FriendInvites.getPublishedDomain() != null;
                if (copyButton.active) {
                    FriendInvites.requestCopy();
                    status = "Адрес скопирован. Отправь его другу.";
                } else {
                    status = "Подключаемся к сети e4mc...";
                }
            }
            addRenderableWidget(Button.builder(Component.literal("Вернуться в игру"),
                    b -> minecraft.setScreen(null)).bounds(x, y + 55, 280, 20).build());
        } else {
            addressField = new EditBox(font, x, y + 28, 280, 20,
                    Component.literal("Адрес друга"));
            addressField.setMaxLength(255);
            addressField.setValue(rememberedAddress);
            addressField.setHint(Component.literal("example.eu.e4mc.link"));
            addRenderableWidget(addressField);
            addRenderableWidget(Button.builder(Component.literal("Подключиться"),
                    b -> connect()).bounds(x, y + 55, 280, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Вставить приглашение"),
                    b -> {
                        String clipboard = minecraft.keyboardHandler.getClipboard();
                        if (clipboard != null) addressField.setValue(clipboard.trim());
                    }).bounds(x, y + 82, 280, 20).build());
            status = "Вставь адрес, который прислал друг";
            setInitialFocus(addressField);
        }

        addRenderableWidget(Button.builder(Component.literal("Назад"),
                b -> onClose()).bounds(x, y + 109, 280, 20).build());
    }

    private boolean isLanOpen() {
        IntegratedServer server = minecraft.getSingleplayerServer();
        return server != null && server.isPublished();
    }

    private void connect() {
        String endpoint = addressField.getValue().trim();
        rememberedAddress = endpoint;
        if (!endpoint.matches("(?i)^[a-z0-9][a-z0-9.-]{0,252}(?::[0-9]{1,5})?$")) {
            status = "Введи адрес Minecraft, без https://";
            return;
        }
        int colon = endpoint.lastIndexOf(':');
        if (colon >= 0) {
            try {
                int port = Integer.parseInt(endpoint.substring(colon + 1));
                if (port < 1 || port > 65535) {
                    status = "Некорректный номер порта";
                    return;
                }
            } catch (NumberFormatException ex) {
                status = "Некорректный номер порта";
                return;
            }
        }
        try {
            ServerAddress address = ServerAddress.parseString(endpoint);
            ServerData data = new ServerData("OpenTogether friend", endpoint, false);
            ConnectScreen.startConnecting(previous, minecraft, address, data, false);
        } catch (RuntimeException ex) {
            status = "Проверь адрес и соединение";
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float tick) {
        renderBackground(g);
        super.render(g, mx, my, tick);
        int mid = width / 2;
        g.drawCenteredString(font, inviting ? "Пригласить друга" : "Подключиться к другу",
                mid, height / 2 - 88, 0xFFFFFF);
        if (inviting && isLanOpen()) {
            String domain = FriendInvites.getPublishedDomain();
            if (copyButton != null) copyButton.active = domain != null;
            if (domain != null) {
                g.drawCenteredString(font, domain, mid, height / 2 - 21, 0x99FF99);
                status = "Нажми «Скопировать» и отправь другу";
            }
        }
        g.drawCenteredString(font, status.length() > 68 ? status.substring(0, 68) : status,
                mid, height / 2 + 76, 0xDDDDDD);
        if (inviting) {
            g.drawCenteredString(font, "Встроенный e4mc • один JAR • без Cloudflare",
                    mid, height / 2 + 94, 0xFFBB77);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
