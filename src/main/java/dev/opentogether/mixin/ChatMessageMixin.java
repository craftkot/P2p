package dev.opentogether.mixin;

import dev.opentogether.FriendInvites;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * e4mc writes locally to ChatComponent, bypassing Forge's network chat event.
 * Never changes, cancels, or suppresses chat messages.
 */
@Mixin(ChatComponent.class)
public abstract class ChatMessageMixin {
    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void opentogether$onLocalChat(Component message, CallbackInfo ci) {
        FriendInvites.onLocalChat(message);
    }
}
