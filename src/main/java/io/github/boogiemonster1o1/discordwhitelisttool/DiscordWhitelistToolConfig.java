package io.github.boogiemonster1o1.discordwhitelisttool;

import blue.endless.jankson.Comment;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "discord-whitelist-tool")
public class DiscordWhitelistToolConfig implements ConfigData {
	@Comment("Whether Discord Whitelist Tool should be enabled")
	public boolean enabled = true;

	@Comment("The Guild ID of the Discord server to use for whitelist")
	public String guildId = "";

	@Comment("The role ID of the role to use for whitelist")
	public String roleId = "";

	@Comment("The application's client ID")
	public String clientId = "";

	@Comment("The application's client secret")
	public String clientSecret = "";
}
