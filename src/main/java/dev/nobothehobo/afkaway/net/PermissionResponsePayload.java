package dev.nobothehobo.afkaway.net;

import dev.nobothehobo.afkaway.AfkAway;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PermissionResponsePayload(boolean allowed) implements CustomPacketPayload {
    public static final Type<PermissionResponsePayload> TYPE =
            new Type<>(AfkAway.id("permission_response"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PermissionResponsePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    PermissionResponsePayload::allowed,
                    PermissionResponsePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
