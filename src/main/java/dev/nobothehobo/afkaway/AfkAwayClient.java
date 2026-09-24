package dev.nobothehobo.afkaway;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;

public final class AfkAwayClient implements ClientModInitializer {
	private static final String MOD_ID = "afkaway";
	private static final int TICKS_PER_SECOND = 20;

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
		Identifier.fromNamespaceAndPath(MOD_ID, "controls")
	);

	private static final KeyMapping TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping(
			"key.afkaway.toggle",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_F8,
			CATEGORY
		)
	);

	private final Config config = Config.load();
	private boolean sessionEnabled = true;
	private boolean blockedMessageShown = false;
	private int idleTicks = 0;
	private int phaseTicks = 0;
	private Phase phase = Phase.IDLE;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
	}

	private void onClientTick(Minecraft client) {
		while (TOGGLE_KEY.consumeClick()) {
			sessionEnabled = !sessionEnabled;
			cancelMovement(client, true);

			if (client.player != null) {
				client.player.sendSystemMessage(Component.literal(
					"AFK Away: " + (sessionEnabled ? "enabled" : "disabled")
				));
			}
		}

		if (client.player == null || client.level == null) {
			cancelMovement(client, true);
			blockedMessageShown = false;
			return;
		}

		if (!config.enabled || !sessionEnabled) {
			cancelMovement(client, false);
			return;
		}

		if (!isAllowedContext(client)) {
			cancelMovement(client, false);

			if (!blockedMessageShown) {
				client.player.sendSystemMessage(Component.literal(
					"AFK Away is blocked on this multiplayer server. "
					+ "Only single-player or explicitly allowlisted servers are supported."
				));
				blockedMessageShown = true;
			}

			return;
		}

		blockedMessageShown = false;

		if (client.screen != null || !client.player.isAlive()) {
			cancelMovement(client, true);
			return;
		}

		switch (phase) {
			case IDLE -> tickIdle(client);
			case FORWARD -> tickForward(client);
			case BACKWARD -> tickBackward(client);
		}
	}

	private void tickIdle(Minecraft client) {
		if (isPlayerMoving(client)) {
			idleTicks = 0;
			return;
		}

		if (!client.player.onGround()) {
			return;
		}

		idleTicks++;

		if (idleTicks >= config.intervalSeconds * TICKS_PER_SECOND) {
			phase = Phase.FORWARD;
			phaseTicks = config.moveTicks;
			client.options.keyUp.setDown(true);
		}
	}

	private void tickForward(Minecraft client) {
		if (!client.player.onGround()) {
			cancelMovement(client, true);
			return;
		}

		phaseTicks--;

		if (phaseTicks <= 0) {
			client.options.keyUp.setDown(false);
			client.options.keyDown.setDown(true);
			phase = Phase.BACKWARD;
			phaseTicks = config.moveTicks;
		}
	}

	private void tickBackward(Minecraft client) {
		if (!client.player.onGround()) {
			cancelMovement(client, true);
			return;
		}

		phaseTicks--;

		if (phaseTicks <= 0) {
			client.options.keyDown.setDown(false);
			phase = Phase.IDLE;
			phaseTicks = 0;
			idleTicks = 0;
		}
	}

	private static boolean isPlayerMoving(Minecraft client) {
		return client.options.keyUp.isDown()
			|| client.options.keyDown.isDown()
			|| client.options.keyLeft.isDown()
			|| client.options.keyRight.isDown();
	}

	private void cancelMovement(Minecraft client, boolean resetTimer) {
		if (client.options != null) {
			client.options.keyUp.setDown(false);
			client.options.keyDown.setDown(false);
		}

		phase = Phase.IDLE;
		phaseTicks = 0;

		if (resetTimer) {
			idleTicks = 0;
		}
	}

	private boolean isAllowedContext(Minecraft client) {
		if (client.getCurrentServer() == null) {
			return true;
		}

		String actual = normalizeServer(client.getCurrentServer().ip);

		for (String allowed : config.allowedServers) {
			if (serverMatches(actual, allowed)) {
				return true;
			}
		}

		return false;
	}

	private static boolean serverMatches(String actual, String allowed) {
		if (actual.equals(allowed)) {
			return true;
		}

		if (!allowed.contains(":")) {
			return stripPort(actual).equals(allowed);
		}

		return false;
	}

	private static String stripPort(String address) {
		if (address.startsWith("[")) {
			int bracket = address.indexOf(']');
			if (bracket >= 0) {
				return address.substring(0, bracket + 1);
			}
		}

		int firstColon = address.indexOf(':');
		int lastColon = address.lastIndexOf(':');

		if (firstColon > 0 && firstColon == lastColon) {
			String maybePort = address.substring(lastColon + 1);
			if (!maybePort.isEmpty() && maybePort.chars().allMatch(Character::isDigit)) {
				return address.substring(0, lastColon);
			}
		}

		return address;
	}

	private static String normalizeServer(String value) {
		String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);

		while (normalized.endsWith(".")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}

		return normalized;
	}

	private enum Phase {
		IDLE,
		FORWARD,
		BACKWARD
	}

	private static final class Config {
		private static final int DEFAULT_INTERVAL_SECONDS = 180;
		private static final int DEFAULT_MOVE_TICKS = 12;

		private final boolean enabled;
		private final int intervalSeconds;
		private final int moveTicks;
		private final Set<String> allowedServers;

		private Config(boolean enabled, int intervalSeconds, int moveTicks, Set<String> allowedServers) {
			this.enabled = enabled;
			this.intervalSeconds = intervalSeconds;
			this.moveTicks = moveTicks;
			this.allowedServers = allowedServers;
		}

		private static Config load() {
			Path path = FabricLoader.getInstance().getConfigDir().resolve("afk-away.properties");
			Properties properties = new Properties();

			if (Files.notExists(path)) {
				properties.setProperty("enabled", "true");
				properties.setProperty("intervalSeconds", Integer.toString(DEFAULT_INTERVAL_SECONDS));
				properties.setProperty("moveTicks", Integer.toString(DEFAULT_MOVE_TICKS));
				properties.setProperty("allowedServers", "localhost,127.0.0.1");

				try {
					Files.createDirectories(path.getParent());
					try (OutputStream output = Files.newOutputStream(path)) {
						properties.store(output,
							"AFK Away: single-player is always permitted. "
							+ "Add only multiplayer servers where you have permission to automate movement."
						);
					}
				} catch (IOException ignored) {
					// Defaults below keep the mod functional even if the config cannot be written.
				}
			} else {
				try (InputStream input = Files.newInputStream(path)) {
					properties.load(input);
				} catch (IOException ignored) {
					// Fall back to defaults.
				}
			}

			boolean enabled = Boolean.parseBoolean(properties.getProperty("enabled", "true"));
			int intervalSeconds = clamp(
				parseInt(properties.getProperty("intervalSeconds"), DEFAULT_INTERVAL_SECONDS),
				30,
				86_400
			);
			int moveTicks = clamp(
				parseInt(properties.getProperty("moveTicks"), DEFAULT_MOVE_TICKS),
				2,
				40
			);

			Set<String> allowedServers = Arrays.stream(
				properties.getProperty("allowedServers", "localhost,127.0.0.1").split(",")
			)
				.map(AfkAwayClient::normalizeServer)
				.filter(value -> !value.isBlank())
				.collect(Collectors.toUnmodifiableSet());

			return new Config(enabled, intervalSeconds, moveTicks, allowedServers);
		}

		private static int parseInt(String value, int fallback) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException ignored) {
				return fallback;
			}
		}

		private static int clamp(int value, int min, int max) {
			return Math.max(min, Math.min(max, value));
		}
	}
}
