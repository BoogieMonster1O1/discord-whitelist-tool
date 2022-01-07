package io.github.boogiemonster1o1.discordwhitelisttool.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.GameProfile;
import discord4j.common.util.Snowflake;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import net.fabricmc.fabric.api.util.TriState;

public class UserList {
	private final BiMap<UuidName, Snowflake> users;
	private final BiMap<UuidName, String> states;

	public UserList() {
		this.users = HashBiMap.create();
		this.states = HashBiMap.create();
	}

	public TriState isAuthorized(GameProfile profile) {
		UuidName uuidName = new UuidName(profile.getId(), profile.getName());
		Snowflake e = this.users.get(uuidName);
		if (e == null) {
			this.states.putIfAbsent(uuidName, null /* TODO: state generator*/);
			return TriState.DEFAULT;
		}
		// TODO: Return whether role exists on user
		return TriState.FALSE;
	}

	public void load(Path statesPath, Path usersPath) {
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
	}

	public void save(Path statesPath, Path usersPath) {
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
	}

	private static class UuidName {
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
			return obj instanceof UuidName && (((UuidName) obj).uuid.equals(uuid) || ((UuidName) obj).name.equals(name));
		}

		public static UuidName from(GameProfile profile) {
			return new UuidName(profile.getId(), profile.getName());
		}
	}
}
