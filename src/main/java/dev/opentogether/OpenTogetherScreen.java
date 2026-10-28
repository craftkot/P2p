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

/**
 * Friend invitation and joining UI. All controls use vanilla widgets; no
 * assets, shaders, custom rendering engines or external services required.
 */
final class OpenTogetherScreen extends Screen {
    private final Screen previous;
    private final boolean inviting;
    private EditBox addressField;
    private Button copyButton;
    private Button connectButton;
    private String status = "";
    private String rememberedAddress = "";
    private boolean copiedDuringSession;

    OpenTogetherScreen(Screen previous, boolean inviting) {
        super(Component.literal("OpenTogether"));
        this.previous = previous;
        this.inviting = inviting;
    }

    @Override
    protected void init() {
        int left = FriendsUiStyle.panelLeft(width);
        int top = FriendsUiStyle.panelTop(height);
        int w = FriendsUiStyle.panelWidth(width);
        int bx = left + 16, bw = w - 32;

        if (inviting) {
            initHosting(bx, bw, top);
        } else {
            initJoining(bx, bw, top);
        }

        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .bounds(left + w - 30, top + 10, 18, 18).build());
    }

    private boolean isLanOpen() {
        IntegratedServer server = minecraft.getSingleplayerServer();
        return server != null && server.isPublished();
    }

    private void initHosting(int x, int w, int y) {
        if (!isLanOpen()) {
            addRenderableWidget(Button.builder(Component.literal("Открыть мир для друзей"),
                    b -> {
                        FriendInvites.requestCopy();
                        minecraft.setScreen(new ShareToLanScreen(previous));
                    }).bounds(x, y + 122, w, 24).build());
            status = "Шаг 1: открой LAN. Шаг 2: отправь адрес другу.";
        } else {
            copyButton = addRenderableWidget(Button.builder(Component.literal("Скопировать адрес"),
                    b -> {
                        FriendInvites.requestCopy();
                        copiedDuringSession = FriendInvites.getPublishedDomain() != null;
                        status = copiedDuringSession
                                ? "Адрес скопирован. Вставь его другу."
                                : "Ожидаем адрес от встроенного e4mc...";
                    }).bounds(x, y + 122, w, 24).build());
            FriendInvites.requestCopy();
            copiedDuringSession = FriendInvites.getPublishedDomain() != null;
            status = copiedDuringSession
                    ? "Адрес автоматически скопирован."
                    : "Подключаемся к сети, ждём публичный адрес...";
            copyButton.active = copiedDuringSession;
        }

        addRenderableWidget(Button.builder(Component.literal("Вернуться в игру"),
                b -> minecraft.setScreen(null)).bounds(x, y + 153, w, 24).build());
    }

    private void initJoining(int x, int w, int y) {
        addressField = new EditBox(font, x, y + 84, w, 23, Component.literal("Адрес Minecraft"));
        addressField.setMaxLength(255);
        addressField.setValue(rememberedAddress);
        addressField.setHint(Component.literal("пример: sunny.eu.e4mc.link"));
        addRenderableWidget(addressField);

        connectButton = addRenderableWidget(Button.builder(Component.literal("Подключиться"),
                b -> connect()).bounds(x, y + 122, w, 24).build());
        connectButton.active = !rememberedAddress.isBlank();
        addressField.setResponder(value -> {
            rememberedAddress = value;
            connectButton.active = !value.trim().isEmpty();
            status = "Проверь адрес и нажми «Подключиться»";
        });

        addRenderableWidget(Button.builder(Component.literal("Вставить из буфера"),
                b -> pasteAddress()).bounds(x, y + 153, w, 24).build());
        setInitialFocus(addressField);
        status = "Введи адрес, который прислал хозяин мира.";
    }

    private void pasteAddress() {
        String clipboard = minecraft.keyboardHandler.getClipboard();
        if (clipboard == null || clipboard.isBlank()) {
            status = "Буфер обмена пуст.";
            return;
        }
        if (clipboard.length() > 255) {
            status = "В буфере слишком длинный текст.";
            return;
        }
        addressField.setValue(clipboard.trim());
        status = "Адрес вставлен. Можно подключаться.";
    }

    private void connect() {
        String endpoint;
        try {
            endpoint = FriendServerAddress.normalize(addressField.getValue());
        } catch (IllegalArgumentException ex) {
            status = "Неверный адрес. Нужен домен или IP с портом.";
            return;
        }
        rememberedAddress = endpoint;
        try {
            ServerAddress address = ServerAddress.parseString(endpoint);
            ServerData data = new ServerData("OpenTogether friend", endpoint, false);
            ConnectScreen.startConnecting(this, minecraft, address, data, false);
        } catch (RuntimeException ex) {
            status = "Не получилось подключиться. Проверь адрес.";
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!inviting && (keyCode == 257 || keyCode == 335) &&
                addressField != null && !addressField.getValue().trim().isEmpty()) {
            connect();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        FriendsUiStyle.drawPanel(g, width, height, "", "");
        FriendsUiStyle.heading(g, font, width, height,
                inviting ? "ПРИГЛАСИТЬ ДРУГА" : "ПОДКЛЮЧИТЬСЯ",
                inviting ? "Открой мир и поделись адресом"
                        : "Вставь адрес друга и начинай играть");

        int left = FriendsUiStyle.panelLeft(width);
        int top = FriendsUiStyle.panelTop(height);
        int w = FriendsUiStyle.panelWidth(width);

        if (inviting) {
            String domain = isLanOpen() ? FriendInvites.getPublishedDomain() : null;
            String stage = !isLanOpen() ? "МИР ЗАКРЫТ"
                    : domain == null ? "ОЖИДАЕМ АДРЕС" : "МИР ДОСТУПЕН";
            FriendsUiStyle.text(g, font, stage, left + 16, top + 67,
                    domain != null ? FriendsUiStyle.ACCENT : FriendsUiStyle.WARNING, w - 32);

            FriendsUiStyle.field(g, left + 15, top + 83, w - 30, 31, domain != null);
            FriendsUiStyle.text(g, font, domain == null ? "Адрес появится здесь..."
                            : domain, left + 25, top + 94,
                    domain == null ? FriendsUiStyle.MUTED : FriendsUiStyle.TEXT, w - 50);

            if (copyButton != null) {
                copyButton.active = domain != null && isLanOpen();
                if (domain != null && !copiedDuringSession) {
                    // The request to copy was armed by initHosting(). The
                    // bridge copies the address as soon as e4mc announces it.
                    status = "Адрес появился и готов к отправке.";
                }
            }
        } else {
            FriendsUiStyle.text(g, font, "АДРЕС СЕРВЕРА", left + 16, top + 68,
                    FriendsUiStyle.MUTED, w - 32);
        }

        FriendsUiStyle.text(g, font, status, left + 16, top + 190,
                FriendsUiStyle.MUTED, w - 32);
        FriendsUiStyle.text(g, font, inviting ? "Адрес действует, пока открыт твой мир."
                        : "Работает со встроенным e4mc и обычными серверами.",
                left + 16, top + 207, FriendsUiStyle.ACCENT, w - 32);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
