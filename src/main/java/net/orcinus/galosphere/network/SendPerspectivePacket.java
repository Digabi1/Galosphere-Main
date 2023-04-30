package net.orcinus.galosphere.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

public class SendPerspectivePacket implements ClientPlayNetworking.PlayChannelHandler {

    @Override
    public void receive(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
        UUID uuid = buf.readUUID();
        int id = buf.readInt();
        client.execute(() -> {
            Level world = client.level;
            if (world != null) {
                Player player = world.getPlayerByUUID(uuid);
                if (player != null && player.equals(client.player)) {
                    Entity e = client.level.getEntity(id);
                    if (e != null) {
                        if (client.getCameraEntity() != e) {
                            client.setCameraEntity(e);
                            if (!client.options.getCameraType().isFirstPerson()) {
                                client.options.setCameraType(CameraType.FIRST_PERSON);
                            }
                        }
                    }
                }
//                Optional.ofNullable(world.getPlayerByUUID(uuid)).filter(player -> player.equals(client.player)).flatMap(player -> Optional.ofNullable(client.level.getEntity(id))).ifPresent(entity -> {
//                    if (client.getCameraEntity() != entity) {
//                        client.setCameraEntity(entity);
//                        if (!client.options.getCameraType().isFirstPerson()) {
//                            client.options.setCameraType(CameraType.FIRST_PERSON);
//                        }
//                    }
//                });
            }
        });
    }

}
