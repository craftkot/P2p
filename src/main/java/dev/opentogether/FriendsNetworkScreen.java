package dev.opentogether;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Client GUI for the existing on-disk network tuning; restart required. */
final class FriendsNetworkScreen extends Screen {
    private static final int[] THRESHOLDS = {256, 128, 512, 1024, 2048, 4096, -1};
    private static final String[] MODES = {"adaptive", "maximum", "vanilla"};
    private static final String[] NAMES = {"Адаптивное", "Максимальное", "Обычное"};

    private final Screen previous;
    private Button modeButton;
    private Button thresholdButton;
    private int threshold = 256;
    private int mode;
    private String feedback = "Сохранённые изменения вступают в силу после перезапуска";

    FriendsNetworkScreen(Screen previous) {
        super(Component.literal("OpenTogether — сеть"));
        this.previous = previous;
        readFile();
    }

    @Override
    protected void init() {
        int left = FriendsUiStyle.panelLeft(width), top = FriendsUiStyle.panelTop(height);
        int w = FriendsUiStyle.panelWidth(width), bx = left + 16, bw = w - 32;

        modeButton = addRenderableWidget(Button.builder(Component.literal(modeLabel()), b -> {
            mode = (mode + 1) % MODES.length;
            modeButton.setMessage(Component.literal(modeLabel()));
            feedback = "Нажми «Сохранить», затем перезапусти игру";
        }).bounds(bx, top + 85, bw, 24).build());

        thresholdButton = addRenderableWidget(Button.builder(Component.literal(thresholdLabel()), b -> {
            int pos = 0;
            while (pos < THRESHOLDS.length && THRESHOLDS[pos] != threshold) pos++;
            threshold = THRESHOLDS[(pos + 1) % THRESHOLDS.length];
            thresholdButton.setMessage(Component.literal(thresholdLabel()));
            feedback = "Нажми «Сохранить», затем перезапусти игру";
        }).bounds(bx, top + 114, bw, 24).build());

        addRenderableWidget(Button.builder(Component.literal("Сохранить настройки"), b -> save())
                .bounds(bx, top + 150, bw, 24).build());

        addRenderableWidget(Button.builder(Component.literal("Вернуть стандартные"), b -> {
            threshold = 256;
            mode = 0;
            thresholdButton.setMessage(Component.literal(thresholdLabel()));
            modeButton.setMessage(Component.literal(modeLabel()));
            feedback = "Стандартные значения выбраны, сохрани изменения";
        }).bounds(bx, top + 179, bw, 20).build());

        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .bounds(left + w - 30, top + 10, 18, 18).build());
    }

    private String thresholdLabel() {
        return "Порог: " + (threshold < 0 ? "Без сжатия" : threshold + " байт");
    }

    private String modeLabel() {
        return "Режим: " + NAMES[mode];
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("opentogether-network.properties");
    }

    private void readFile() {
        Properties props = new Properties();
        if (Files.isRegularFile(path())) {
            try (Reader r = Files.newBufferedReader(path(), StandardCharsets.UTF_8)) {
                props.load(r);
            } catch (IOException ex) {
                feedback = "Не удалось прочитать настройки. Проверь папку config.";
            }
        }
        threshold = CompressionPolicy.parse(props.getProperty("compressionThreshold", "256"));
        String selected = CompressionTuning.parseMode(props.getProperty("compressionMode", "adaptive"))
                .name().toLowerCase(java.util.Locale.ROOT);
        for (int i = 0; i < MODES.length; i++) if (MODES[i].equals(selected)) mode = i;
    }

    private void save() {
        try {
            Path p = path();
            Files.createDirectories(p.getParent());
            Files.writeString(p, """
                    # OpenTogether network settings. Restart Minecraft after changing.
                    # adaptive=fast small packets, strong big packets
                    # maximum=strongest zlib; vanilla=default zlib
                    compressionMode=%s
                    # 256 recommended. -1 = disable negotiated compression.
                    compressionThreshold=%d
                    """.formatted(MODES[mode], threshold), StandardCharsets.UTF_8);
            feedback = "Сохранено. Полностью перезапусти Minecraft.";
        } catch (IOException | SecurityException e) {
            feedback = "Не удалось сохранить: проверь доступ к config.";
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        FriendsUiStyle.drawPanel(g, width, height, "", "");
        FriendsUiStyle.heading(g, font, width, height, "НАСТРОЙКИ СЕТИ",
                "Экономия трафика при игре через интернет");
        int left = FriendsUiStyle.panelLeft(width), top = FriendsUiStyle.panelTop(height);
        int w = FriendsUiStyle.panelWidth(width);
        FriendsUiStyle.field(g, left + 15, top + 64, w - 30, 16, false);
        FriendsUiStyle.text(g, font, "Малые пакеты — быстро, большие — сильнее",
                left + 22, top + 68, FriendsUiStyle.MUTED, w - 44);
        FriendsUiStyle.text(g, font, feedback,
                left + 16, top + 210, FriendsUiStyle.MUTED, w - 32);
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
