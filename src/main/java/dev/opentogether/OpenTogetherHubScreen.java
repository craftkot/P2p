package dev.opentogether;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

/** Main friend dashboard: the same entry point from title, multiplayer and pause. */
final class OpenTogetherHubScreen extends Screen {
    private final Screen previous;
    private String feedback = "";

    OpenTogetherHubScreen(Screen previous) {
        super(Component.literal("OpenTogether — друзья"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        int left = FriendsUiStyle.panelLeft(width), top = FriendsUiStyle.panelTop(height);
        int w = FriendsUiStyle.panelWidth(width);
        int bx = left + 16, bw = w - 32;
        IntegratedServer local = minecraft.getSingleplayerServer();

        Button host = addRenderableWidget(Button.builder(Component.literal("Пригласить в свой мир"),
                b -> minecraft.setScreen(new OpenTogetherScreen(this, true)))
                .bounds(bx, top + 80, bw, 24).build());
        host.active = local != null;

        addRenderableWidget(Button.builder(Component.literal("Подключиться к другу"),
                b -> minecraft.setScreen(new OpenTogetherScreen(this, false)))
                .bounds(bx, top + 110, bw, 24).build());

        addRenderableWidget(Button.builder(Component.literal("Настройки передачи"),
                b -> minecraft.setScreen(new FriendsNetworkScreen(this)))
                .bounds(bx, top + 140, bw, 24).build());

        Button diagnostics = addRenderableWidget(Button.builder(Component.literal("Диагностика чанков"),
                b -> minecraft.setScreen(new ChunkDiagnosticsScreen(this)))
                .bounds(bx, top + 170, bw, 24).build());
        diagnostics.active = minecraft.level != null;

        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .bounds(left + w - 30, top + 10, 18, 18).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        FriendsUiStyle.drawPanel(g, width, height, "OpenTogether", "Играй с друзьями без настройки роутера");
        FriendsUiStyle.heading(g, font, width, height,
                "OPENTOGETHER", "Друзья и совместная игра");

        IntegratedServer local = minecraft.getSingleplayerServer();
        int left = FriendsUiStyle.panelLeft(width), top = FriendsUiStyle.panelTop(height);
        int w = FriendsUiStyle.panelWidth(width);

        String state = local == null ? "Вне одиночного мира"
                : local.isPublished() ? "Твой мир открыт для друзей" : "Одиночный мир готов";
        FriendsUiStyle.field(g, left + 15, top + 62, w - 30, 15, local != null && local.isPublished());
        FriendsUiStyle.text(g, font, state, left + 24, top + 65,
                local != null && local.isPublished() ? FriendsUiStyle.ACCENT : FriendsUiStyle.MUTED, w - 52);

        String foot = local == null ? "Чтобы пригласить, сначала открой свой мир"
                : "Встроенный e4mc | один JAR | без Cloudflare";
        FriendsUiStyle.text(g, font, feedback.isEmpty() ? foot : feedback,
                left + 16, top + 208, FriendsUiStyle.MUTED, w - 32);
        super.render(g, mouseX, mouseY, partialTick);
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
