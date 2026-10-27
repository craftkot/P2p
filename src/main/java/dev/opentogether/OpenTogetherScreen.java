package dev.opentogether;

import com.google.gson.JsonObject;
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

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Invitations work with an externally reachable TCP endpoint; P2P is not implemented yet. */
final class OpenTogetherScreen extends Screen {
    private final Screen previous;
    private final boolean inviting;
    private EditBox entry;
    private Button sendButton;
    private Button probeButton;
    private String entryText = "";
    private String status = "";
    private boolean requestBusy;
    private boolean probeBusy;

    OpenTogetherScreen(Screen previous, boolean inviting) {
        super(Component.literal("OpenTogether"));
        this.previous = previous;
        this.inviting = inviting;
    }

    @Override
    protected void init() {
        if (entry != null) entryText = entry.getValue();
        int x = width / 2 - 140, y = height / 2 - 68;

        entry = new EditBox(font, x, y, 280, 20,
                Component.literal(inviting ? "Публичный адрес:порт" : "Код приглашения"));
        entry.setMaxLength(254);
        entry.setValue(entryText);
        entry.setHint(Component.literal(inviting ? "example.org:25565" : "ABCD-EFGH"));
        addRenderableWidget(entry);

        sendButton = addRenderableWidget(Button.builder(
                Component.literal(inviting ? "Создать код приглашения" : "Подключиться"),
                button -> action()).bounds(x, y + 26, 280, 20).build());
        sendButton.active = !requestBusy;

        if (inviting && !isLanOpen()) {
            probeButton = addRenderableWidget(Button.builder(
                    Component.literal("Открыть мир для LAN"),
                    button -> minecraft.setScreen(new ShareToLanScreen(previous)))
                    .bounds(x, y + 51, 280, 20).build());
        } else {
            probeButton = addRenderableWidget(Button.builder(
                    Component.literal("Проверить доступность STUN / UDP"),
                    button -> probeNetwork()).bounds(x, y + 51, 280, 20).build());
            probeButton.active = !probeBusy;
        }

        addRenderableWidget(Button.builder(Component.literal("Настроить Cloudflare"),
                button -> minecraft.setScreen(new CloudflareSettingsScreen(this)))
                .bounds(x, y + 76, 280, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Назад"), button -> onClose())
                .bounds(x, y + 101, 280, 20).build());

        setInitialFocus(entry);
        if (!CloudflareApi.configured()) {
            status = "Укажи URL Cloudflare Worker в настройках";
        } else if (inviting && !isLanOpen()) {
            status = "Открой мир для сети (LAN), затем создай приглашение";
        }
    }

    private boolean isLanOpen() {
        IntegratedServer server = minecraft.getSingleplayerServer();
        return server != null && server.isPublished();
    }

    private void action() {
        if (requestBusy) return;
        if (!CloudflareApi.configured()) {
            status = "Сначала настрой Cloudflare Worker";
            return;
        }
        if (inviting && !isLanOpen()) {
            status = "Открой мир для сети через меню Minecraft";
            return;
        }

        String value = entry.getValue().trim();
        if (value.isEmpty()) {
            status = inviting ? "Укажи публичный адрес и порт" : "Введи код приглашения";
            return;
        }
        if (!inviting) {
            try {
                value = InviteCode.normalize(value);
            } catch (IllegalArgumentException ex) {
                status = "Неверный код: нужно 8 букв/цифр без I, O, 0, 1";
                return;
            }
        }

        requestBusy = true;
        sendButton.active = false;
        status = "Запрос к Cloudflare...";
        final String endpointOrCode = value;

        CompletableFuture<JsonObject> future = inviting
                ? CloudflareApi.call("POST", "/create", "{\"endpoint\":\"" + jsonEscape(value) + "\"}")
                : CloudflareApi.call("GET", "/resolve/" + value, "");

        future.whenComplete((json, error) -> Minecraft.getInstance().execute(() -> {
            requestBusy = false;
            if (sendButton != null) sendButton.active = true;
            // A stale HTTP response must NEVER connect after the user closes this screen.
            if (minecraft.screen != this) return;

            if (error != null) {
                Throwable root = rootCause(error);
                String message = root.getMessage();
                if (message != null && message.contains("HTTP 404")) status = "Код не найден или истёк";
                else if (message != null && message.contains("HTTP 429")) status = "Слишком много запросов — попробуй позже";
                else if (message != null && message.contains("HTTP 400")) status = "Некорректный адрес или код";
                else status = "Ошибка Cloudflare. Подробности в latest.log";
                CloudflareApi.LOG.warn("[OpenTogether] Request failed type={}",
                        root.getClass().getSimpleName());
                return;
            }

            try {
                if (inviting) {
                    String code = InviteCode.normalize(json.get("code").getAsString());
                    minecraft.keyboardHandler.setClipboard(code);
                    status = "Код " + code + " скопирован. Действует 10 минут.";
                } else {
                    String endpoint = json.get("endpoint").getAsString();
                    if (!endpoint.contains(":")) throw new IllegalArgumentException("Missing TCP port");
                    ServerAddress address = ServerAddress.parseString(endpoint);
                    ServerData data = new ServerData("OpenTogether friend", endpoint, false);
                    ConnectScreen.startConnecting(previous, minecraft, address, data, false);
                }
            } catch (RuntimeException ex) {
                status = "Ошибка ответа сервера или подключения";
                CloudflareApi.LOG.warn("[OpenTogether] Invalid invitation response type={}",
                        ex.getClass().getSimpleName());
            }
        }));
    }

    private void probeNetwork() {
        if (probeBusy) return;
        probeBusy = true;
        probeButton.active = false;
        status = "Проверка STUN/UDP...";
        CompletableFuture.supplyAsync(() -> {
            try {
                return StunProbe.probe();
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        }).whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
            probeBusy = false;
            if (probeButton != null) probeButton.active = true;
            if (minecraft.screen != this) return;

            if (error != null) {
                status = "STUN недоступен: проверь VPN и UDP";
                CloudflareApi.LOG.warn("[OpenTogether] STUN probe failed type={}",
                        rootCause(error).getClass().getSimpleName());
            } else {
                status = "STUN доступен, RTT " + result.rttMs() + " мс (это ещё не P2P)";
                CloudflareApi.LOG.info("[OpenTogether] STUN RTT={}ms", result.rttMs());
            }
        }));
    }

    private static Throwable rootCause(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        return root;
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        g.drawCenteredString(font, inviting ? "Приглашение в одиночный мир" : "Подключение к другу",
                width / 2, height / 2 - 89, 0xFFFFFF);
        String message = status.length() > 52 ? status.substring(0, 52) : status;
        g.drawCenteredString(font, message, width / 2, height / 2 + 62, 0xDDDDDD);
        if (inviting && isLanOpen()) {
            g.drawCenteredString(font, "LAN-порт: " + minecraft.getSingleplayerServer().getPort()
                    + " (должен быть доступен извне)", width / 2, height / 2 + 79, 0xFFBB77);
        } else if (inviting) {
            g.drawCenteredString(font, "Обычное LAN не пробивает NAT: потребуется открытый TCP-порт.",
                    width / 2, height / 2 + 79, 0xFFBB77);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
