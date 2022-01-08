package io.github.boogiemonster1o1.discordwhitelisttool.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.GameProfile;
import discord4j.common.util.Snowflake;
import io.github.boogiemonster1o1.discordwhitelisttool.DiscordWhitelistTool;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;

import net.fabricmc.fabric.api.util.TriState;

public class UserList {
	private final BiMap<UuidName, Snowflake> users;
	private final BiMap<UuidName, String> states;
	private final BiMap<UuidName, String> tokens;
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Supplier<HttpClient> HTTP_CLIENT = () -> HttpClient.create().compress(true).followRedirect(true).secure();
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);

	public UserList() {
		this.users = HashBiMap.create();
		this.states = HashBiMap.create();
		this.tokens = HashBiMap.create();
	}

	public String getState(UuidName uuidName) {
		return this.states.get(uuidName);
	}

	@NotNull
	public TriState isAuthorized(GameProfile profile) {
		UuidName uuidName = UuidName.from(profile);
		Snowflake e = this.users.get(uuidName);
		if (e == null) {
			byte[] bytes = new byte[28];
			RANDOM.nextBytes(bytes);
			this.states.putIfAbsent(uuidName, new String(Hex.encodeHex(bytes)));
			return TriState.DEFAULT;
		}
		return TriState.of(hasRole(DiscordWhitelistTool.CONFIG.get().roleId, e));
	}

	@Nullable
	public Boolean hasRole(String roleId, Snowflake userId) {
		String token = this.tokens.get(this.users.inverse().get(userId));
		Optional<JsonNode> thing = HTTP_CLIENT.get()
				.headers(headers -> {
					headers.set("Authorization", "Bearer " + token);
				})
				.get()
				.uri("/users/@me/guilds/" + DiscordWhitelistTool.CONFIG.getConfig().guildId + "/member")
				.responseSingle((response, content) -> {
					if (response.status().code() != HttpResponseStatus.OK.code()) {
						return Mono.empty();
					}
					return content.map(ByteBuf::retain).map(buf -> buf.toString(StandardCharsets.UTF_8)).map(UserList::toTree);
				}).blockOptional();
		if (thing.isEmpty()) {
			return null;
		}
		return StreamSupport.stream(thing.get().get("roles").spliterator(), false).map(JsonNode::asText).toList().contains(roleId);
	}

	public LiteralText getOauthUrl(GameProfile profile) {
		String url = DiscordWhitelistTool.OAUTH.getAuthorizationURL(this.states.computeIfAbsent(UuidName.from(profile), uuidName -> {
			byte[] bytes = new byte[28];
			RANDOM.nextBytes(bytes);
			return new String(Hex.encodeHex(bytes));
		}));
		LiteralText text = new LiteralText(url);
		text.setStyle(text.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
		return text;
	}

	public void addToken(String state, String token) {
		JsonNode node = HTTP_CLIENT.get()
				.headers(headers -> {
					headers.set("Authorization", "Bearer " + token);
				})
				.get()
				.uri("https://discord.com/api/v9/users/@me")
				.responseSingle((resp, mono) -> {
					if (resp.status().code() != HttpResponseStatus.OK.code()) {
						return Mono.empty();
					}
					return mono.map(ByteBuf::retain).map(buf -> buf.toString(StandardCharsets.UTF_8)).map(UserList::toTree);
				})
				.block();
		String id = node.get("id").asText();
		this.tokens.put(this.states.inverse().get(state), token);
		this.users.put(this.states.inverse().get(state), Snowflake.of(id));
	}

	public static JsonNode toTree(String s) {
		try {
			return OBJECT_MAPPER.readTree(s);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public void load(Path statesPath, Path usersPath, Path tokensPath) {
		try {
			if (Files.exists(statesPath)) {
				NbtCompound nbt  = NbtIo.readCompressed(Files.newInputStream(statesPath));
				nbt.getKeys().forEach(key -> {
					NbtCompound uuidNameNbt = nbt.getCompound(key);
					UuidName uuidName = new UuidName(UUID.fromString(uuidNameNbt.getString("uuid")), uuidNameNbt.getString("name"));
					this.states.put(uuidName, key);
				});
			} else {
				Files.createFile(statesPath);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		try {
			if (Files.exists(usersPath)) {
				NbtCompound nbt  = NbtIo.readCompressed(Files.newInputStream(usersPath));
				nbt.getKeys().forEach(key -> {
					NbtCompound uuidNameNbt = nbt.getCompound(key);
					UuidName uuidName = new UuidName(UUID.fromString(uuidNameNbt.getString("uuid")), uuidNameNbt.getString("name"));
					this.users.put(uuidName, Snowflake.of(key));
				});
			} else {
				Files.createFile(usersPath);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		try {
			if (Files.exists(tokensPath)) {
				NbtCompound nbt  = NbtIo.readCompressed(Files.newInputStream(tokensPath));
				nbt.getKeys().forEach(key -> {
					NbtCompound uuidNameNbt = nbt.getCompound(key);
					UuidName uuidName = new UuidName(UUID.fromString(uuidNameNbt.getString("uuid")), uuidNameNbt.getString("name"));
					this.tokens.put(uuidName, key);
				});
			} else {
				Files.createFile(tokensPath);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void save(Path statesPath, Path usersPath, Path tokensPath) {
		try {
			NbtCompound nbt = new NbtCompound();
			this.states.forEach((uuidName, state) -> {
				NbtCompound uuidNameNbt = new NbtCompound();
				uuidNameNbt.putString("uuid", uuidName.getUuid().toString());
				uuidNameNbt.putString("name", uuidName.getName());
				nbt.put(state, uuidNameNbt);
			});
			NbtIo.writeCompressed(nbt, Files.newOutputStream(statesPath));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		try {
			NbtCompound nbt = new NbtCompound();
			this.users.forEach((uuidName, snowflake) -> {
				NbtCompound uuidNameNbt = new NbtCompound();
				uuidNameNbt.putString("uuid", uuidName.getUuid().toString());
				uuidNameNbt.putString("name", uuidName.getName());
				nbt.put(snowflake.asString(), uuidNameNbt);
			});
			NbtIo.writeCompressed(nbt, Files.newOutputStream(usersPath));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		try {
			NbtCompound nbt = new NbtCompound();
			this.tokens.forEach((uuidName, token) -> {
				NbtCompound uuidNameNbt = new NbtCompound();
				uuidNameNbt.putString("uuid", uuidName.getUuid().toString());
				uuidNameNbt.putString("name", uuidName.getName());
				nbt.put(token, uuidNameNbt);
			});
			NbtIo.writeCompressed(nbt, Files.newOutputStream(tokensPath));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static class UuidName {
		private final UUID uuid;
		private final String name;

		public UuidName(UUID uuid, String name) {
			this.uuid = uuid;
			this.name = name;
		}

		public UUID getUuid() {
			return uuid;
		}

		public String getName() {
			return name;
		}

		@Override
		public boolean equals(Object obj) {
			return obj.getClass() == UuidName.class && (((UuidName) obj).uuid.equals(uuid) || ((UuidName) obj).name.equals(name));
		}

		public static UuidName from(GameProfile profile) {
			return new UuidName(profile.getId(), profile.getName());
		}
	}
}
