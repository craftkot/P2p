package dev.opentogether.fabric;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WorldOptionsScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

/** Native Minecraft GUI with no Cloudflare account or second relay download. */
public final class FriendsScreen extends Screen {
    private final Screen previous;
    private EditBox address;
    private String feedback = "Открой мир по LAN или введи адрес друга";
    private int tab; // 0 = hub; 1 = join
    private String lastAddress = "";

    public FriendsScreen(Screen previous) {
        super(Component.literal("OpenTogether 26.3"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        int w = Math.min(310, width - 30);
        int x = (width - w) / 2;
        int y = Math.max(12, (height - 220) / 2);
        if (tab == 0) {
            boolean hosting = minecraft.getSingleplayerServer() != null;
            Button share = addRenderableWidget(Button.builder(
                Component.literal("Открыть свой мир для друзей"),
                b -> minecraft.gui.setScreen(new WorldOptionsScreen(previous, minecraft.level)))
                .bounds(x, y + 74, w, 24).build());
            share.active = hosting;
            addRenderableWidget(Button.builder(Component.literal("Подключиться к другу"),
                b -> { tab = 1; rebuildWidgets(); })
                .bounds(x, y + 104, w, 24).build());
            addRenderableWidget(Button.builder(Component.literal("Вернуться в Minecraft"),
                b -> onClose()).bounds(x, y + 141, w, 24).build());
            feedback = hosting ? "После открытия LAN адрес появится в чате e4mc."
                    : "Чтобы пригласить, зайди в одиночный мир.";
        } else {
            address = new EditBox(font, x, y + 73, w, 22,
                    Component.literal("Адрес друга"));
            address.setMaxLength(255);
            address.setValue(lastAddress);
            address.setHint(Component.literal("sunny.eu.e4mc.link"));
            addRenderableWidget(address);
            addRenderableWidget(Button.builder(Component.literal("Вставить адрес"),
                b -> {
                    String value = minecraft.keyboardHandler.getClipboard();
                    if (value != null) address.setValue(value.strip());
                }).bounds(x, y + 106, (w - 6) / 2, 23).build());
            addRenderableWidget(Button.builder(Component.literal("Подключиться"),
                b -> connect()).bounds(x + (w + 6) / 2, y + 106, (w - 6) / 2, 23).build());
            addRenderableWidget(Button.builder(Component.literal("Назад"),
                b -> { lastAddress = address.getValue(); tab = 0; rebuildWidgets(); })
                .bounds(x, y + 142, w, 23).build());
            setInitialFocus(address);
        }
        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .bounds(x + w - 24, y + 12, 18, 18).build());
    }

    private void connect() {
        String value = address.getValue().strip();
        if (value.length() > 255 || !value.matches("(?i)[a-z0-9][a-z0-9.\\-]{0,252}(:[0-9]{1,5})?")) {
            feedback = "Неверный адрес Minecraft. Убери https:// и пробелы.";
            return;
        }
        lastAddress = value;
        ServerAddress target = ServerAddress.parseString(value);
        ServerData data = new ServerData("OpenTogether friend", value, ServerData.Type.OTHER);
        ConnectScreen.startConnecting(this, minecraft, target, data, false, null);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int w = Math.min(310, width - 30);
        int x = (width - w) / 2;
        int y = Math.max(12, (height - 220) / 2);
        g.fill(0, 0, width, height, 0xCC0B121D);
        g.fill(x - 8, y, x + w + 8, y + 185, 0xEE15202F);
        g.fill(x - 8, y, x + w + 8, y + 2, 0xFF67DDC2);
        g.text(font, "OPENTOGETHER 26.3", x + 10, y + 16, 0xFFEAF7FF, false);
        g.text(font, tab == 0 ? "Играй с друзьями без проброса портов"
                : "Подключение по адресу Minecraft", x + 10, y + 38, 0xFFB4C7D4, false);
        g.text(font, "Встроен e4mc 6.2.3 / Fabric", x + 10, y + 56, 0xFF67DDC2, false);
        if (feedback != null) {
            String line = feedback;
            while (font.width(line) > w && line.length() > 4)
                line = line.substring(0, line.length() - 1);
            g.text(font, line, x + 4, y + 174, 0xFFFFD59C, false);
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(previous);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
