package dev.nobothehobo.afkaway.net;

import dev.nobothehobo.afkaway.AfkAway;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PermissionRequestPayload() implements CustomPacketPayload {
    public static final PermissionRequestPayload INSTANCE = new PermissionRequestPayload();

    public static final Type<PermissionRequestPayload> TYPE =
            new Type<>(AfkAway.id("permission_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PermissionRequestPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
