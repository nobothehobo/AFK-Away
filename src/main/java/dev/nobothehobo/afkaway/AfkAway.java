package dev.nobothehobo.afkaway;

import dev.nobothehobo.afkaway.net.PermissionRequestPayload;
import dev.nobothehobo.afkaway.net.PermissionResponsePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;

public final class AfkAway implements ModInitializer {
    public static final String MOD_ID = "afkaway";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(
                PermissionRequestPayload.TYPE,
                PermissionRequestPayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
                PermissionResponsePayload.TYPE,
                PermissionResponsePayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(PermissionRequestPayload.TYPE, (payload, context) -> {
            boolean allowed = ServerConfig.allowAutomatedMovement();
            ServerPlayNetworking.send(context.player(), new PermissionResponsePayload(allowed));
        });
    }
}
