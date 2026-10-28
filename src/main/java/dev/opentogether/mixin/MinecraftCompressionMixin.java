package dev.opentogether.mixin;

import dev.opentogether.NetworkSettings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's LoginCompression packet is sent before gameplay.
 * Override only the login-time threshold for published integrated worlds,
 * so distant chunks use the standard Minecraft compression codec.
 * Dedicated servers are untouched, as are local in-memory connections.
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftCompressionMixin {
    @Inject(method = "getCompressionThreshold", at = @At("HEAD"), cancellable = true)
    private void opentogether$wanCompression(CallbackInfoReturnable<Integer> cir) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        if (server.isDedicatedServer()) return;
        if (!(server instanceof IntegratedServer local) || !local.isPublished()) return;
        int threshold = NetworkSettings.configuredThreshold();
        // -1 is an explicit opt-out, not a fallback to an unknown vanilla value.
        cir.setReturnValue(threshold);
    }
}
