package dev.nobothehobo.afkaway;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

final class ServerConfig {
    private static Boolean cachedAllowMovement;

    private ServerConfig() {
    }

    static synchronized boolean allowAutomatedMovement() {
        if (cachedAllowMovement != null) {
            return cachedAllowMovement;
        }

        Path path = FabricLoader.getInstance().getConfigDir().resolve("afk-away-server.properties");
        Properties properties = new Properties();

        if (Files.exists(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException ignored) {
                // Safe default below remains false.
            }
        } else {
            properties.setProperty("allowAutomatedMovement", "false");
            try {
                Files.createDirectories(path.getParent());
                try (OutputStream output = Files.newOutputStream(path)) {
                    properties.store(
                            output,
                            "AFK Away server permission. Set true only if automated idle movement is allowed by this server."
                    );
                }
            } catch (IOException ignored) {
                // Safe default below remains false.
            }
        }

        cachedAllowMovement = Boolean.parseBoolean(
                properties.getProperty("allowAutomatedMovement", "false")
        );
        return cachedAllowMovement;
    }
}
