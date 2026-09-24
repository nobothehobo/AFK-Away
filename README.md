# AFK Away

A tiny Fabric client mod for Minecraft Java Edition **26.1.2** that performs a brief, visible forward/back movement pulse on a timer.

## Important scope

AFK Away is intentionally **not a stealth or anti-detection mod**. It does not spoof packets, hide itself, bypass anti-cheat, or attempt to defeat a server's AFK policy.

For safety, it runs automatically in:

- single-player worlds, or
- multiplayer servers that you explicitly add to the local allowlist and have permission to automate on.

On any other multiplayer server, the movement routine is blocked.

## Controls

- **F8** — toggle AFK Away for the current game session.

The default interval is 180 seconds. When the timer fires, the mod briefly walks forward and then backward so you end up close to the starting spot.

The movement pulse only starts while you are on the ground and no menu is open.

## Configuration

On first launch, the mod creates:

```
.minecraft/config/afk-away.properties
```

Example:

```properties
enabled=true
intervalSeconds=180
moveTicks=12
allowedServers=localhost,127.0.0.1
```

`allowedServers` is a comma-separated list. Exact hostnames are accepted, with or without a port. Wildcards are deliberately unsupported.

After editing the file, restart Minecraft.

## Minecraft / Fabric target

Minecraft 26.1 and later use Fabric's unobfuscated toolchain and Java 25. The project compiles against the 26.1 game/API surface and its mod metadata is locked to **Minecraft 26.1.2**. This avoids relying on the unavailable public `com.mojang:minecraft:26.1.2` Maven artifact while still targeting the 26.1.2 client.

Requirements:

- Minecraft Java Edition 26.1.2
- Fabric Loader 0.19.5+
- Fabric API
- Java 25 for development/building

## Build

If you have Gradle 9.5.1 and JDK 25 installed:

```bash
gradle build
```

The jar will be in `build/libs/`.

A GitHub Actions workflow is included and also uploads the built jar as a workflow artifact.

## Installation

1. Install Fabric Loader for Minecraft 26.1.2.
2. Install the matching Fabric API.
3. Put the AFK Away jar in your Minecraft `mods` folder.
4. Launch the game.
