package dev.opentogether;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** A noninvasive view of chunk loading, TPS and Minecraft connection health. */
final class ChunkDiagnosticsScreen extends Screen {
    private final Screen previous;
    private String feedback = "Сделай замер при движении по знакомой местности";

    ChunkDiagnosticsScreen(Screen previous) {
        super(Component.literal("OpenTogether — диагностика"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        int left = FriendsUiStyle.panelLeft(width), top = FriendsUiStyle.panelTop(height);
        int w = FriendsUiStyle.panelWidth(width);
        addRenderableWidget(Button.builder(Component.literal("Скопировать отчёт"),
                b -> {
                    minecraft.keyboardHandler.setClipboard(report());
                    feedback = "Отчёт скопирован, отправь его другу";
                }).bounds(left + 16, top + 175, w - 32, 24).build());

        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .bounds(left + w - 30, top + 10, 18, 18).build());
    }

    private String report() {
        if (minecraft.level == null) return "OpenTogether: мир не открыт";
        var sample = ChunkArrivalStats.sample();
        var server = minecraft.getSingleplayerServer();
        StringBuilder out = new StringBuilder("OpenTogether | Диагностика загрузки чанков\n");
        out.append("Клиент, дальность прорисовки: ").append(minecraft.options.renderDistance().get()).append('\n');
        out.append("Кеш чанков: ").append(minecraft.level.getChunkSource().getLoadedChunksCount()).append('\n');
        out.append("Событий загрузки за 10 секунд: ").append(sample.loadedInLast10s()).append('\n');
        out.append("Время с последней загрузки: ").append(sample.secondsSinceLastLoad()).append(" с\n");
        if (server != null) {
            out.append("Хост, отправка: ").append(server.getPlayerList().getViewDistance()).append(" чанков\n");
            out.append("Хост, симуляция: ").append(server.getPlayerList().getSimulationDistance()).append(" чанков\n");
            out.append(String.format(Locale.ROOT, "Среднее время тика: %.1f мс\n", server.getAverageTickTime()));
            out.append("Порог сжатия: ").append(server.getCompressionThreshold()).append(" байт\n");
            out.append("Режим: ").append(NetworkSettings.configuredMode()).append('\n');
        } else {
            out.append("Транспорт: ").append(transportLabel()).append('\n');
            out.append("Пинг: ").append(latency()).append(" мс\n");
        }
        out.append("Количество новых чанков — не скорость сети. При стоянке оно может быть нулевым.");
        return out.toString();
    }

    private int latency() {
        if (minecraft.getConnection() == null || minecraft.player == null) return -1;
        PlayerInfo info = minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID());
        return info == null ? -1 : info.getLatency();
    }

    private String transportLabel() {
        ClientPacketListener listener = minecraft.getConnection();
        if (listener == null || listener.getConnection() == null) return "нет соединения";
        java.net.SocketAddress remote = listener.getConnection().getRemoteAddress();
        if (remote == null) return "неизвестен";
        if ("link.e4mc.dialtone.DialtoneAddress".equals(remote.getClass().getName()))
            return "e4mc Dialtone (прямой или relay)";
        return "Обычный TCP / ретранслятор";
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        FriendsUiStyle.drawPanel(g, width, height, "", "");
        FriendsUiStyle.heading(g, font, width, height,
                "ДИАГНОСТИКА СЕТИ", "Загрузка чанков и состояние соединения");

        int left = FriendsUiStyle.panelLeft(width), top = FriendsUiStyle.panelTop(height);
        int w = FriendsUiStyle.panelWidth(width);
        if (minecraft.level == null) {
            metric(g, left, top + 77, w, "Нет открытого мира", FriendsUiStyle.WARNING);
        } else {
            var sample = ChunkArrivalStats.sample();
            metric(g, left, top + 65, w,
                    "Кеш чанков: " + minecraft.level.getChunkSource().getLoadedChunksCount(),
                    FriendsUiStyle.TEXT);
            metric(g, left, top + 82, w,
                    "Новые загрузки (10 с): " + sample.loadedInLast10s(),
                    FriendsUiStyle.ACCENT);
            metric(g, left, top + 99, w,
                    "Прорисовка клиента: " + minecraft.options.renderDistance().get() + " чанков",
                    FriendsUiStyle.TEXT);
            IntegratedServer server = minecraft.getSingleplayerServer();
            if (server != null) {
                metric(g, left, top + 116, w, "Хост / отправка: " +
                        server.getPlayerList().getViewDistance() + " чанков", FriendsUiStyle.TEXT);
                metric(g, left, top + 133, w, String.format(Locale.ROOT,
                        "Серверный тик: %.1f / 50 мс", server.getAverageTickTime()), FriendsUiStyle.TEXT);
                metric(g, left, top + 150, w, "Сжатие: " + server.getCompressionThreshold() +
                        " B, " + NetworkSettings.configuredMode(), FriendsUiStyle.MUTED);
            } else {
                metric(g, left, top + 116, w, "Пинг Minecraft: " + latency() + " мс",
                        FriendsUiStyle.TEXT);
                metric(g, left, top + 133, w, "Маршрут: " + transportLabel(),
                        FriendsUiStyle.TEXT);
                metric(g, left, top + 150, w, "Сравни с отчётом хозяина мира",
                        FriendsUiStyle.MUTED);
            }
        }
        metric(g, left, top + 208, w, feedback, FriendsUiStyle.MUTED);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void metric(GuiGraphics g, int left, int y, int w, String label, int color) {
        FriendsUiStyle.text(g, font, label, left + 17, y, color, w - 34);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
