package dev.opentogether.mixin;

import dev.opentogether.CompressionTuning;
import dev.opentogether.NetworkSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.CompressionEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.zip.Deflater;

/**
 * Sets the zlib LEVEL for outgoing packets only when a LAN-hosting
 * integrated server exists. Never changes the on-wire zlib format.
 *
 * The game's existing compression encoder keeps the vanilla packet framing,
 * threshold, reset/flush and decompression behavior intact.
 *
 * Declared as a client-side mixin: it MUST NOT load on dedicated servers.
 */
@Mixin(CompressionEncoder.class)
public abstract class CompressionEncoderMixin {
    @Shadow @Final private Deflater deflater;
    @Unique private boolean opentogether$hostCompression;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void opentogether$init(int threshold, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        opentogether$hostCompression = server != null && server.isPublished() &&
                NetworkSettings.configuredThreshold() >= 0;
    }

    @Inject(method = "encode", at = @At("HEAD"))
    private void opentogether$configureLevel(ChannelHandlerContext ctx, ByteBuf input, ByteBuf output,
                                             CallbackInfo ci) {
        if (!opentogether$hostCompression) return;
        CompressionTuning.Mode mode = NetworkSettings.configuredMode();
        deflater.setLevel(CompressionTuning.levelForBytes(mode, input.readableBytes()));
    }
}
