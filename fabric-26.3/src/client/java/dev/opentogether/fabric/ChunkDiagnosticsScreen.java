package dev.opentogether.fabric;

import java.net.SocketAddress;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

/** Live read-only client diagnostics. The screen must never alter network traffic. */
public final class ChunkDiagnosticsScreen extends Screen {
    private final Screen previous;
    private String feedback = "Сравни отчёты хоста и клиента при движении";

    public ChunkDiagnosticsScreen(Screen previous) {
        super(Component.literal("OpenTogether — диагностика"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        int w = Math.min(354, width - 24);
        int x = (width - w) / 2;
        int y = Math.max(8, (height - 256) / 2);
        addRenderableWidget(Button.builder(Component.literal("Скопировать отчёт"),
                b -> {
                    minecraft.keyboardHandler.setClipboard(report());
                    feedback = "Отчёт скопирован в буфер обмена";
                }).bounds(x + 8, y + 187, w - 16, 23).build());
        addRenderableWidget(Button.builder(Component.literal("Назад"),
                b -> onClose()).bounds(x + 8, y + 217, w - 16, 23).build());
        addRenderableWidget(Button.builder(Component.literal("X"),
                b -> onClose()).bounds(x + w - 23, y + 8, 18, 18).build());
    }

    private int ping() {
        if (minecraft.getConnection() == null || minecraft.player == null) return -1;
        PlayerInfo info = minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID());
        return info == null ? -1 : info.getLatency();
    }

    private String transport() {
        if (minecraft.getSingleplayerServer() != null) return "Локальный сервер (хост)";
        if (minecraft.getConnection() == null) return "Нет соединения";
        SocketAddress remote = minecraft.getConnection().getConnection().getRemoteAddress();
        if (remote == null) return "Неизвестен";
        String className = remote.getClass().getName();
        if (className.contains("e4mc") || className.contains("Dialtone"))
            return "e4mc (direct/relay неизвестно)";
        return "TCP / другой транспорт";
    }

    private String report() {
        if (minecraft.level == null) return "OpenTogether | мир не открыт";
        ChunkArrivalStats.Sample sample = ChunkArrivalStats.INSTANCE.snapshot(minecraft.level);
        var server = minecraft.getSingleplayerServer();
        StringBuilder s = new StringBuilder("OpenTogether | Диагностика чанков Fabric 26.3\n");
        s.append("Клиент, дальность прорисовки: ")
                .append(minecraft.options.renderDistance().get()).append("\n");
        s.append("Кеш чанков: ")
                .append(minecraft.level.getChunkSource().getLoadedChunksCount()).append("\n");
        s.append("Событий загрузки за 10 секунд: ")
                .append(sample.eventsIn10Seconds()).append("\n");
        s.append("Время с последней загрузки: ")
                .append(sample.secondsSinceLastEvent() < 0 ? "нет данных"
                        : sample.secondsSinceLastEvent() + " с").append("\n");
        if (server != null) {
            s.append("Хост, отправка: ").append(server.getPlayerList().getViewDistance()).append(" чанков\n");
            s.append("Хост, симуляция: ").append(server.getPlayerList().getSimulationDistance()).append(" чанков\n");
            s.append(String.format(Locale.ROOT, "Среднее время тика: %.1f мс\n",
                    server.getAverageTickTimeNanos() / 1_000_000.0));
        } else {
            s.append("Пинг Minecraft: ").append(ping()).append(" мс\n");
        }
        s.append("Транспорт: ").append(transport()).append("\n");
        s.append("Число загрузок — не скорость сети. На месте оно может быть 0.\n");
        s.append("e4mc transport не позволяет надёжно различить direct и relay.");
        return s.toString();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int w = Math.min(354, width - 24);
        int x = (width - w) / 2;
        int y = Math.max(8, (height - 256) / 2);
        g.fill(0, 0, width, height, 0xCC0B121D);
        g.fill(x - 8, y, x + w + 8, y + 253, 0xEE15202F);
        g.fill(x - 8, y, x + w + 8, y + 2, 0xFF67DDC2);
        line(g, "ДИАГНОСТИКА СЕТИ", x + 8, y + 13, 0xFFEAF7FF, w - 40);
        line(g, "Сравни измерения на хосте и клиенте", x + 8, y + 36, 0xFFB4C7D4, w - 16);
        if (minecraft.level == null) {
            line(g, "Нет открытого мира", x + 8, y + 65, 0xFFFFD59C, w - 16);
        } else {
            ChunkArrivalStats.Sample s = ChunkArrivalStats.INSTANCE.snapshot(minecraft.level);
            line(g, "Кеш чанков: " + minecraft.level.getChunkSource().getLoadedChunksCount(),
                    x + 8, y + 64, 0xFFEAF7FF, w - 16);
            line(g, "Новые загрузки (10 с): " + s.eventsIn10Seconds(),
                    x + 8, y + 84, 0xFF67DDC2, w - 16);
            line(g, "С последней загрузки: " +
                    (s.secondsSinceLastEvent() < 0 ? "нет данных" : s.secondsSinceLastEvent() + " с"),
                    x + 8, y + 104, 0xFFEAF7FF, w - 16);
            line(g, "Прорисовка клиента: " + minecraft.options.renderDistance().get() + " чанков",
                    x + 8, y + 124, 0xFFEAF7FF, w - 16);
            var server = minecraft.getSingleplayerServer();
            if (server != null) {
                line(g, String.format(Locale.ROOT, "Тик хоста: %.1f мс",
                                server.getAverageTickTimeNanos() / 1_000_000.0),
                        x + 8, y + 144, 0xFFEAF7FF, w - 16);
            } else {
                line(g, "Пинг Minecraft: " + ping() + " мс",
                        x + 8, y + 144, 0xFFEAF7FF, w - 16);
            }
            line(g, "Маршрут: " + transport(), x + 8, y + 164, 0xFFB4C7D4, w - 16);
        }
        line(g, feedback, x + 8, y + 244, 0xFFFFD59C, w - 16);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void line(GuiGraphicsExtractor g, String message, int x, int y, int color, int maxWidth) {
        String text = message;
        while (text.length() > 4 && font.width(text) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        g.text(font, text, x, y, color, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(previous);
    }
}
