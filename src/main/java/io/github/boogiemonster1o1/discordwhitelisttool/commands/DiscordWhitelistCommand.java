package io.github.boogiemonster1o1.discordwhitelisttool.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import io.github.boogiemonster1o1.discordwhitelisttool.DiscordWhitelistTool;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

import static net.minecraft.server.command.CommandManager.literal;

public class DiscordWhitelistCommand {
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				literal("discordwhitelist")
						.requires(source -> source.hasPermissionLevel(2))
						.then(
								literal("enable")
										.executes(ctx -> {
											boolean prev = DiscordWhitelistTool.CONFIG.getConfig().enabled;
											DiscordWhitelistTool.CONFIG.getConfig().enabled = true;
											ctx.getSource().sendFeedback(new LiteralText("Discord whitelist is now enabled."), true);
											if (!prev) {
												DiscordWhitelistTool.refreshBot();
											}
											return Command.SINGLE_SUCCESS;
										})
						)
						.then(
								literal("disable")
										.executes(ctx -> {
											boolean prev = DiscordWhitelistTool.CONFIG.getConfig().enabled;
											DiscordWhitelistTool.CONFIG.getConfig().enabled = false;
											ctx.getSource().sendFeedback(new LiteralText("Discord whitelist is now disabled."), false);
											if (prev) {
												DiscordWhitelistTool.destroyBot();
											}
											return Command.SINGLE_SUCCESS;
										})
						)
		);
	}
}
