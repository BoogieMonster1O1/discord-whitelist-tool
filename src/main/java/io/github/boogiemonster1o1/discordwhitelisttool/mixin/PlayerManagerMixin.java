package io.github.boogiemonster1o1.discordwhitelisttool.mixin;

import java.net.SocketAddress;

import com.mojang.authlib.GameProfile;
import io.github.boogiemonster1o1.discordwhitelisttool.DiscordWhitelistTool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;

import net.fabricmc.fabric.api.util.TriState;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
	@Inject(method = "checkCanJoin", at = @At("TAIL"), cancellable = true)
	public void interceptCanJoin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir) {
		if (DiscordWhitelistTool.CONFIG.getConfig().enabled) {
			TriState state = DiscordWhitelistTool.USER_LIST.isAuthorized(profile);
			if (state == TriState.FALSE) {
				cir.setReturnValue(DiscordWhitelistTool.NOT_WHITELISTED_MESSAGE);
			} else if (state == TriState.DEFAULT) {
				cir.setReturnValue(DiscordWhitelistTool.NOT_AUTHORIZED_MESSAGE); // TODO: Send oauth link
			}
		}
	}
}
