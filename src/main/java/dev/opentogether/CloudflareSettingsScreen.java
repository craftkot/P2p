package dev.opentogether;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;

final class CloudflareSettingsScreen extends Screen {
    private final Screen previous;
    private EditBox url;
    private String status = "";

    CloudflareSettingsScreen(Screen previous) {
        super(Component.literal("OpenTogether — Cloudflare"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        int x = width / 2 - 140, y = height / 2 - 32;
        url = new EditBox(font, x, y, 280, 20, Component.literal("Worker HTTPS URL"));
        url.setMaxLength(220);
        url.setValue(CloudflareConfig.getUrl());
        url.setHint(Component.literal("https://example.workers.dev"));
        addRenderableWidget(url);
        addRenderableWidget(Button.builder(Component.literal("Сохранить"), b -> save())
                .bounds(x, y + 29, 137, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Назад"), b -> onClose())
                .bounds(x + 143, y + 29, 137, 20).build());
        setInitialFocus(url);
    }

    private void save() {
        try {
            CloudflareConfig.saveUrl(url.getValue());
            status = "Адрес сохранён. Вернись в меню приглашений.";
        } catch (IllegalArgumentException | IOException ex) {
            status = "Неверный HTTPS-адрес или нет доступа к config/";
            CloudflareApi.LOG.warn("[OpenTogether] Config save failed type={}",
                    ex.getClass().getSimpleName());
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        super.render(g, mx, my, partial);
        g.drawCenteredString(font, "Cloudflare Worker (только HTTPS, без /create)", width / 2,
                height / 2 - 55, 0xFFFFFF);
        g.drawCenteredString(font, status, width / 2, height / 2 + 35, 0xDDDDDD);
        g.drawCenteredString(font, "Не вводи API-ключи: здесь только публичный адрес Worker.",
                width / 2, height / 2 + 51, 0xFFBB77);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
