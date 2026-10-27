package dev.opentogether;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;

final class CloudflareSettingsScreen extends Screen {
    private final Screen previous;
    private EditBox url;
    private Button testButton;
    private String status = "";
    private boolean healthBusy;

    CloudflareSettingsScreen(Screen previous) {
        super(Component.literal("OpenTogether — Cloudflare"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        String current = url != null ? url.getValue() : CloudflareConfig.getUrl();
        int x = width / 2 - 140, y = height / 2 - 48;

        url = new EditBox(font, x, y, 280, 20, Component.literal("Worker HTTPS URL"));
        url.setMaxLength(220);
        url.setValue(current);
        url.setHint(Component.literal("https://example.workers.dev"));
        addRenderableWidget(url);

        addRenderableWidget(Button.builder(Component.literal("Сохранить"), b -> save())
                .bounds(x, y + 27, 137, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Назад"), b -> onClose())
                .bounds(x + 143, y + 27, 137, 20).build());

        testButton = addRenderableWidget(Button.builder(
                Component.literal("Проверить доступность Worker"), b -> checkHealth())
                .bounds(x, y + 54, 280, 20).build());
        testButton.active = !healthBusy;
        setInitialFocus(url);
    }

    private void save() {
        try {
            CloudflareConfig.saveUrl(url.getValue());
            status = "Адрес сохранён в config/opentogether.properties";
        } catch (IllegalArgumentException | IOException ex) {
            status = "Неверный HTTPS-адрес или нет доступа к config/";
            CloudflareApi.LOG.warn("[OpenTogether] Config save failed type={}",
                    ex.getClass().getSimpleName());
        }
    }

    private void checkHealth() {
        if (healthBusy) return;
        healthBusy = true;
        testButton.active = false;
        status = "Проверка Worker...";
        CloudflareApi.health(url.getValue()).whenComplete((result, error) ->
                Minecraft.getInstance().execute(() -> {
                    healthBusy = false;
                    if (testButton != null) testButton.active = true;
                    if (minecraft.screen != this) return;
                    if (error != null) {
                        status = "Worker недоступен или ответил ошибкой";
                        CloudflareApi.LOG.warn("[OpenTogether] Worker health failed type={}",
                                error.getClass().getSimpleName());
                    } else {
                        status = Boolean.TRUE.equals(result)
                                ? "Worker отвечает. Можно сохранять адрес."
                                : "Адрес работает, но это не OpenTogether Worker.";
                    }
                }));
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        super.render(g, mx, my, partial);
        g.drawCenteredString(font, "Cloudflare Worker (HTTPS, без /create)", width / 2,
                height / 2 - 68, 0xFFFFFF);
        g.drawCenteredString(font, status, width / 2, height / 2 + 45, 0xDDDDDD);
        g.drawCenteredString(font, "Не вводи ключи: здесь только публичный адрес Worker.",
                width / 2, height / 2 + 62, 0xFFBB77);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
