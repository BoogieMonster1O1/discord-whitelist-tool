package io.github.boogiemonster1o1.discordwhitelisttool;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import io.github.boogiemonster1o1.discordwhitelisttool.commands.DiscordWhitelistCommand;
import io.github.boogiemonster1o1.discordwhitelisttool.io.DiscordWhitelistToolConfig;
import io.github.boogiemonster1o1.discordwhitelisttool.io.UserList;
import io.github.boogiemonster1o1.discordwhitelisttool.server.RedirectServer;
import io.mokulu.discord.oauth.DiscordOAuth;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

public class DiscordWhitelistTool implements DedicatedServerModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("modid");
	public static ConfigHolder<DiscordWhitelistToolConfig> CONFIG;
	public static UserList USER_LIST;
	public static final MutableText NOT_WHITELISTED_MESSAGE;
	public static final MutableText NOT_AUTHORIZED_MESSAGE;
	public static final Path WHITELIST_DATA_PATH = FabricLoader.getInstance().getGameDir().resolve("discordwhitelisttool");
	public static GatewayDiscordClient BOT;
	public static DiscordOAuth OAUTH;

	public static void refreshBot() {
		destroyBot();
		OAUTH = new DiscordOAuth(CONFIG.get().clientId, CONFIG.get().clientSecret, CONFIG.get().redirectUri, new String[]{"identify", "guilds.members.read"});
	}

	public static void destroyBot() {
	}

	@Override
	public void onInitializeServer() {
		LOGGER.info("Initializing Discord Whitelist Tool");
		CONFIG = AutoConfig.register(DiscordWhitelistToolConfig.class, JanksonConfigSerializer::new);
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			DiscordWhitelistCommand.register(dispatcher);
		});
		USER_LIST = new UserList();
		if (!Files.exists(WHITELIST_DATA_PATH)) {
			try {
				Files.createDirectories(WHITELIST_DATA_PATH);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		USER_LIST.load(WHITELIST_DATA_PATH.resolve("states.dat"), WHITELIST_DATA_PATH.resolve("users.dat"), WHITELIST_DATA_PATH.resolve("tokens.dat"));
		if (CONFIG.get().enabled) {
			refreshBot();
		}
		RedirectServer.init(CONFIG.get().host, CONFIG.get().port, false);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			USER_LIST.save(WHITELIST_DATA_PATH.resolve("states.dat"), WHITELIST_DATA_PATH.resolve("users.dat"), WHITELIST_DATA_PATH.resolve("tokens.dat"));
			RedirectServer.SERVER.dispose();
		});
	}

	static {
		NOT_WHITELISTED_MESSAGE = new LiteralText("You are not whitelisted on this server.");
		NOT_WHITELISTED_MESSAGE.setStyle(NOT_WHITELISTED_MESSAGE.getStyle().withColor(TextColor.fromFormatting(Formatting.AQUA)).withUnderline(Boolean.TRUE));
		NOT_AUTHORIZED_MESSAGE = new LiteralText("You are not authorized to join the server. Please verify your discord account: ");
	}
}
