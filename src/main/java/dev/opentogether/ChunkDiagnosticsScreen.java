package dev.opentogether;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * A zero-packet inspection tool. It does not increase view distance or
 * manipulate chunk send order, avoiding additional stress on slow relays.
 */
final class ChunkDiagnosticsScreen extends Screen {
    private final Screen previous;
    private String copied = "";

    ChunkDiagnosticsScreen(Screen previous) {
        super(Component.literal("OpenTogether — диагностика чанков"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        int x = width / 2 - 132;
        int y = height / 2 + 70;
        addRenderableWidget(Button.builder(Component.literal("Скопировать показатели"), b -> {
            minecraft.keyboardHandler.setClipboard(report());
            copied = "Показатели скопированы. Отправь их хозяину мира.";
        }).bounds(x, y, 264, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Назад"), b -> onClose())
                .bounds(x, y + 26, 264, 20).build());
    }

    private String report() {
        if (minecraft.level == null) return "OpenTogether: мир не открыт";
        int loaded = minecraft.level.getChunkSource().getLoadedChunksCount();
        int clientRender = minecraft.options.renderDistance().get();
        var sample = ChunkArrivalStats.sample();
        var server = minecraft.getSingleplayerServer();
        StringBuilder output = new StringBuilder("OpenTogether 0.7 / диагностика загрузки чанков\n");
        output.append("Клиент, дальность прорисовки: ").append(clientRender).append(" чанков\n");
        output.append("Чанков в клиентском кеше: ").append(loaded).append('\n');
        output.append("Событий загрузки чанков за 10 секунд: ").append(sample.loadedInLast10s()).append('\n');
        output.append("С последней загрузки прошло: ").append(sample.secondsSinceLastLoad())
                .append(" с (-1 = не наблюдали)\n");
        if (server != null) {
            output.append("Локальный хост: да\n");
            output.append("Сервер, дальность отправки: ")
                    .append(server.getPlayerList().getViewDistance()).append(" чанков\n");
            output.append("Сервер, дальность симуляции: ")
                    .append(server.getPlayerList().getSimulationDistance()).append(" чанков\n");
            output.append(String.format(Locale.ROOT, "Среднее время тика: %.1f мс\n",
                    server.getAverageTickTime()));
        } else {
            output.append("Локальный хост: нет\n");
            ClientPacketListener connection = minecraft.getConnection();
            if (connection != null && minecraft.player != null) {
                PlayerInfo info = connection.getPlayerInfo(minecraft.player.getUUID());
                if (info != null) output.append("Пинг (по Minecraft): ")
                        .append(info.getLatency()).append(" мс\n");
            }
        }
        if (server != null) output.append("Сжатие Minecraft при входе гостя: ").append(server.getCompressionThreshold()).append(" байт (-1 = выключено)\n");
        if (server == null) output.append("Транспорт: ").append(transportLabel()).append("\n");
        output.append("Важно: события загрузки — не число полученных сетевых пакетов.\n");
        output.append("Низкое значение после остановки движения нормально.\n");
        return output.toString();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        if (minecraft.level == null) return;
        int x = width / 2, y = height / 2 - 91;
        g.drawCenteredString(font, "OpenTogether | Диагностика чанков", x, y, 0xFFFFFF);
        int loaded = minecraft.level.getChunkSource().getLoadedChunksCount();
        var sample = ChunkArrivalStats.sample();
        line(g, x, y + 22, "Чанков в клиентском кеше: " + loaded, 0xFFFFFF);
        line(g, x, y + 36, "Новые загрузки за 10 с: " + sample.loadedInLast10s(), 0xAADDFF);
        line(g, x, y + 50, "Дальность прорисовки: " +
                minecraft.options.renderDistance().get() + " чанков", 0xFFFFFF);
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server != null) {
            line(g, x, y + 66, "Хост: отправка до " + server.getPlayerList().getViewDistance() +
                    " ч. / симуляция " + server.getPlayerList().getSimulationDistance() + " ч.", 0xCCFFCC);
            line(g, x, y + 80, String.format(Locale.ROOT, "Время тика: %.1f мс / предел 50 мс",
                    server.getAverageTickTime()), 0xCCFFCC);
            line(g, x, y + 126, "Сжатие для входящих гостей: " + server.getCompressionThreshold() + " байт", 0xAADDFF);
        } else {
            line(g, x, y + 66, "Другой игрок: сравни результаты с хостом", 0xCCFFCC);
            line(g, x, y + 126, "Транспорт: " + transportLabel(), 0xAADDFF);
            ClientPacketListener conn = minecraft.getConnection();
            if (conn != null && minecraft.player != null) {
                PlayerInfo info = conn.getPlayerInfo(minecraft.player.getUUID());
                if (info != null) line(g, x, y + 80, "Пинг: " + info.getLatency() + " мс", 0xCCFFCC);
            }
        }
        line(g, x, y + 98, "Замеряй при движении в уже созданном мире.", 0xFFCC88);
        line(g, x, y + 110, "Рост загрузок без роста видимости? Проверь F3.", 0xFFCC88);
        if (!copied.isEmpty()) line(g, x, y + 143, copied, 0x99FF99);
    }

    /** Only inspects the transport class name; never reads/shares peer addresses. */
    private String transportLabel() {
        ClientPacketListener listener = minecraft.getConnection();
        if (listener == null || listener.getConnection() == null) return "нет соединения";
        java.net.SocketAddress remote = listener.getConnection().getRemoteAddress();
        if (remote == null) return "адрес неизвестен";
        if ("link.e4mc.dialtone.DialtoneAddress".equals(remote.getClass().getName()))
            return "e4mc Dialtone (P2P или резервный relay)";
        return "TCP/ретранслятор (не Dialtone)";
    }

    private void line(GuiGraphics graphics, int center, int y, String message, int color) {
        graphics.drawCenteredString(font, message, center, y, color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
