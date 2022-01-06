package io.github.boogiemonster1o1.discordwhitelisttool;

import io.github.boogiemonster1o1.discordwhitelisttool.commands.DiscordWhitelistCommand;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;

public class DiscordWhitelistTool implements DedicatedServerModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("modid");
	public static ConfigHolder<DiscordWhitelistToolConfig> CONFIG;

	@Override
	public void onInitializeServer() {
		LOGGER.info("Initializing Discord Whitelist Tool");
		CONFIG = AutoConfig.register(DiscordWhitelistToolConfig.class, JanksonConfigSerializer::new);
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			DiscordWhitelistCommand.register(dispatcher);
		});
	}
}
