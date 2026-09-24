package dev.nobothehobo.afkaway;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nobothehobo.afkaway.net.PermissionRequestPayload;
import dev.nobothehobo.afkaway.net.PermissionResponsePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class AfkAwayClient implements ClientModInitializer {
    private static final int TICKS_PER_SECOND = 20;
    private static final int DEFAULT_INTERVAL_SECONDS = 180;
    private static final int DEFAULT_MOVE_TICKS = 12;

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(AfkAway.MOD_ID, "controls")
    );

    private static final KeyMapping TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.afkaway.toggle",
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_F8,
                    CATEGORY
            )
    );

    private boolean active;
    private volatile boolean serverAllowsMovement;
    private int idleTicks;
    private int phaseTicks;
    private Phase phase = Phase.IDLE;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                PermissionResponsePayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    serverAllowsMovement = payload.allowed();

                    if (context.client().player != null && active) {
                        context.client().player.sendSystemMessage(Component.literal(
                                payload.allowed()
                                        ? "AFK Away: this server explicitly permits the movement pulse."
                                        : "AFK Away: this server does not permit automated movement; display protection remains active."
                        ));
                    }
                })
        );

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(AfkAway.MOD_ID, "display_protection"),
                (graphics, deltaTracker) -> renderOverlay(graphics)
        );

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(Minecraft client) {
        while (TOGGLE_KEY.consumeClick()) {
            active = !active;
            cancelMovement(client, true);

            if (client.player == null) {
                active = false;
                continue;
            }

            if (active) {
                requestPermission(client);
                client.player.sendSystemMessage(Component.literal(
                        "AFK Away: active. Press F8 again to return."
                ));
            } else {
                serverAllowsMovement = false;
                client.player.sendSystemMessage(Component.literal("AFK Away: disabled."));
            }
        }

        if (!active || client.player == null || client.level == null) {
            cancelMovement(client, false);
            return;
        }

        if (client.screen != null || !client.player.isAlive()) {
            cancelMovement(client, true);
            return;
        }

        if (!movementPermitted(client)) {
            cancelMovement(client, false);
            return;
        }

        switch (phase) {
            case IDLE -> tickIdle(client);
            case FORWARD -> tickForward(client);
            case BACKWARD -> tickBackward(client);
        }
    }

    private void requestPermission(Minecraft client) {
        serverAllowsMovement = false;

        if (client.getCurrentServer() == null) {
            return;
        }

        if (ClientPlayNetworking.canSend(PermissionRequestPayload.TYPE)) {
            ClientPlayNetworking.send(PermissionRequestPayload.INSTANCE);
        } else if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(
                    "AFK Away: the server has not provided an AFK Away permission channel, so automated movement is disabled."
            ));
        }
    }

    private boolean movementPermitted(Minecraft client) {
        return client.getCurrentServer() == null || serverAllowsMovement;
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

        if (idleTicks >= DEFAULT_INTERVAL_SECONDS * TICKS_PER_SECOND) {
            phase = Phase.FORWARD;
            phaseTicks = DEFAULT_MOVE_TICKS;
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
            phaseTicks = DEFAULT_MOVE_TICKS;
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

    private void renderOverlay(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        if (!active) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.screen != null) {
            return;
        }

        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();

        graphics.fill(0, 0, width, height, 0xFF050505);

        String permission = movementPermitted(client)
                ? "movement permitted"
                : "movement disabled by server";
        Component status = Component.literal("AFK Away • " + permission + " • F8 to return");

        int textWidth = client.font.width(status);
        long phaseTime = System.currentTimeMillis() / 55L;
        int travelX = Math.max(1, width - textWidth - 24);
        int travelY = Math.max(1, height - 48);
        int x = 12 + bounce((int) (phaseTime % (travelX * 2L)), travelX);
        int y = 12 + bounce((int) ((phaseTime / 2L) % (travelY * 2L)), travelY);

        graphics.text(client.font, status, x, y, 0xFFE5E7EB, true);
    }

    private static int bounce(int value, int max) {
        return value <= max ? value : (max * 2) - value;
    }

    private enum Phase {
        IDLE,
        FORWARD,
        BACKWARD
    }
}
