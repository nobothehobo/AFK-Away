# AFK Away

AFK Away is a Fabric mod for **Minecraft Java Edition 26.1.2** designed for long passive waits such as crop growth or farms.

It has two pieces:

- a client-side dark, animated AFK overlay that avoids leaving a static Minecraft HUD/world on the display, and
- a tiny forward/back idle movement pulse every 180 seconds.

## Multiplayer permission model

The movement pulse is intentionally not hidden from a server.

- **Single-player:** movement works automatically.
- **Multiplayer with only the client mod installed:** the display-protection overlay works, but automated movement is disabled.
- **Multiplayer with AFK Away installed on the server:** the client asks the server for permission. Movement only runs when the server explicitly replies that it is allowed.

This keeps the mod multiplayer-compatible without trying to disguise traffic or circumvent an AFK policy.

### Server setup

Install the same jar on the Fabric server. The first permission request creates:

```text
config/afk-away-server.properties
```

Its safe default is:

```properties
allowAutomatedMovement=false
```

A server owner who permits the feature can change it to:

```properties
allowAutomatedMovement=true
```

Restart the server after changing the setting.

## Controls

- **F8** — toggle AFK Away.

When active, the client draws a near-black overlay with a slowly moving status line. The game continues running underneath. Press F8 again to return.

On Steam Deck, bind a rear paddle or another Steam Input button to **F8**.

## Movement

When movement is permitted, AFK Away waits 180 seconds, walks forward briefly, then backward for the same duration. It only starts a pulse while the player is alive, on the ground, and no menu is open.

## Requirements

- Minecraft Java Edition **26.1.2**
- Java **25**
- Fabric Loader **0.19.5+**
- Fabric API **0.146.1+26.1.2** or a compatible newer 26.1.2 build

Minecraft 26.1 uses Fabric's unobfuscated Mojang-name toolchain and Java 25. citeturn893418search0turn893418search4

## Steam Deck / Linux

There is no Windows-specific code. Install it like any Fabric jar in the instance's `mods` folder. For the vanilla launcher that is commonly:

```text
~/.minecraft/mods
```

Third-party launchers normally use a per-instance mods folder.

## Build

GitHub Actions builds every push. With Java 25 and Gradle 9.6 installed locally:

```bash
gradle build
```

The mod jar is written to `build/libs/`.

## License

MIT
