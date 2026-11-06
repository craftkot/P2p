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

/** e4mc-free UI: vanilla LAN/TCP transport with optional address-based invite codes. */
public final class FriendsScreen extends Screen {
    private final Screen previous;
    private EditBox address;
    private String feedback = "";
    private int tab; // 0 = hub; 1 = join; 2 = host code
    private String lastAddress = "";
    private String hostAddress = "";
    private int hostPort = -1;

    public FriendsScreen(Screen previous) {
        super(Component.literal("OpenTogether Direct 26.3"));
        this.previous = previous;
    }

    private boolean published() {
        var server = minecraft.getSingleplayerServer();
        return server != null && server.isPublished();
    }

    @Override
    protected void init() {
        int w = Math.min(330, width - 30);
        int x = (width - w) / 2;
        int y = Math.max(12, (height - 220) / 2);
        if (tab == 0) {
            boolean hosting = minecraft.getSingleplayerServer() != null;
            Button share = addRenderableWidget(Button.builder(
                Component.literal(published() ? "Скопировать приглашение" : "Открыть свой мир по LAN"),
                b -> {
                    if (published()) {
                        tab = 2;
                        rebuildWidgets();
                    } else {
                        minecraft.gui.setScreen(new WorldOptionsScreen(previous, minecraft.level));
                    }
                }).bounds(x, y + 74, w, 24).build());
            share.active = hosting;
            addRenderableWidget(Button.builder(Component.literal("Подключиться к другу"),
                b -> { tab = 1; rebuildWidgets(); })
                .bounds(x, y + 104, w, 24).build());
            addRenderableWidget(Button.builder(Component.literal("Диагностика"),
                b -> minecraft.gui.setScreen(new ChunkDiagnosticsScreen(this)))
                .bounds(x, y + 141, (w - 6) / 2, 24).build());
            addRenderableWidget(Button.builder(Component.literal("Закрыть"),
                b -> onClose()).bounds(x + (w + 6) / 2, y + 141, (w - 6) / 2, 24).build());
            feedback = published() ? "Мир опубликован: код содержит IP и порт"
                    : hosting ? "Сначала открой мир для локальной сети"
                    : "Чтобы пригласить, открой одиночный мир.";
        } else if (tab == 1) {
            address = new EditBox(font, x, y + 73, w, 22,
                    Component.literal("Код или адрес друга"));
            address.setMaxLength(512);
            address.setValue(lastAddress);
            address.setHint(Component.literal("OT1... / 192.168.1.10:12345"));
            addRenderableWidget(address);
            addRenderableWidget(Button.builder(Component.literal("Вставить код"),
                b -> {
                    String value = minecraft.keyboardHandler.getClipboard();
                    if (value != null) address.setValue(value.strip());
                }).bounds(x, y + 106, (w - 6) / 2, 23).build());
            addRenderableWidget(Button.builder(Component.literal("Подключиться"),
                b -> connect()).bounds(x + (w + 6) / 2, y + 106, (w - 6) / 2, 23).build());
            addRenderableWidget(Button.builder(Component.literal("Назад"),
                b -> { lastAddress = address.getValue(); tab = 0; rebuildWidgets(); })
                .bounds(x, y + 142, w, 23).build());
            feedback = "Код OT1 содержит обычный IP:порт";
            setInitialFocus(address);
        } else {
            if (!published()) {
                tab = 0;
                rebuildWidgets();
                return;
            }
            int port = minecraft.getSingleplayerServer().getPort();
            if (port != hostPort) {
                hostPort = port;
                hostAddress = LanAddress.suggest(port);
            }
            address = new EditBox(font, x, y + 73, w, 22,
                    Component.literal("IP:порт доступного мира"));
            address.setMaxLength(255);
            address.setValue(hostAddress);
            address.setHint(Component.literal("192.168.1.10:" + port));
            addRenderableWidget(address);
            addRenderableWidget(Button.builder(Component.literal("Скопировать OT1-код"),
                b -> copyCode()).bounds(x, y + 106, w, 23).build());
            addRenderableWidget(Button.builder(Component.literal("Назад"),
                b -> { hostAddress = address.getValue(); tab = 0; rebuildWidgets(); })
                .bounds(x, y + 142, w, 23).build());
            feedback = "LAN-код не обходит CGNAT и закрытый порт";
            setInitialFocus(address);
        }
        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .bounds(x + w - 24, y + 12, 18, 18).build());
    }

    private void copyCode() {
        try {
            hostAddress = address.getValue();
            String code = InviteCode.encode(hostAddress);
            minecraft.keyboardHandler.setClipboard(code);
            feedback = "Код скопирован. Для Интернета нужен доступный порт";
        } catch (IllegalArgumentException e) {
            feedback = "Укажи правильный IP или домен с портом";
        }
    }

    private void connect() {
        String target;
        try {
            lastAddress = address.getValue().strip();
            target = InviteCode.resolve(lastAddress);
            ServerAddress addressToConnect = ServerAddress.parseString(target);
            ServerData data = new ServerData("OpenTogether friend", target, ServerData.Type.OTHER);
            ConnectScreen.startConnecting(this, minecraft, addressToConnect, data, false, null);
        } catch (IllegalArgumentException e) {
            feedback = "Неверный код или адрес Minecraft";
        } catch (RuntimeException e) {
            feedback = "Не удалось запустить подключение";
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int w = Math.min(330, width - 30);
        int x = (width - w) / 2;
        int y = Math.max(12, (height - 220) / 2);
        g.fill(0, 0, width, height, 0xCC0B121D);
        g.fill(x - 8, y, x + w + 8, y + 185, 0xEE15202F);
        g.fill(x - 8, y, x + w + 8, y + 2, 0xFF67DDC2);
        g.text(font, "OPENTOGETHER DIRECT", x + 10, y + 16, 0xFFEAF7FF, false);
        g.text(font, tab == 0 ? "Играй по LAN или напрямую через Интернет"
                : tab == 1 ? "Вставь код друга или IP:порт"
                : "Код приглашения из адреса и порта", x + 10, y + 38, 0xFFB4C7D4, false);
        g.text(font, "Прямой Minecraft TCP • без e4mc", x + 10, y + 56, 0xFF67DDC2, false);
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
