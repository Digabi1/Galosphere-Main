package net.orcinus.galosphere.entities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.orcinus.galosphere.api.SpectreBoundSpyglass;
import net.orcinus.galosphere.init.GEntityTypes;
import net.orcinus.galosphere.init.GNetwork;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class SpectatorVision extends Entity implements SpectreBoundSpyglass {
    private static final EntityDataAccessor<Optional<UUID>> MANIPULATOR = SynchedEntityData.defineId(SpectatorVision.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(SpectatorVision.class, EntityDataSerializers.INT);
    private int spectatingTicks;

    public SpectatorVision(Level level) {
        super(GEntityTypes.SPECTATOR_VISION, level);
    }

    public SpectatorVision(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(MANIPULATOR, Optional.empty());
        this.entityData.define(PHASE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        UUID manipulatorUUID = this.getManipulatorUUID();
        Optional<Player> player = Optional.ofNullable(manipulatorUUID).map(this.level::getPlayerByUUID);
        if (!this.level.isClientSide) {
            player.ifPresent(this::handlePerspective);
        }
        if (!this.isRemoved()) {
            player.ifPresent(this::copyPlayerRotation);
        }
    }

    @Environment(EnvType.CLIENT)
    public boolean matchesClientPlayerUUID() {
        return Minecraft.getInstance().player != null && Minecraft.getInstance().player.getUUID().equals(this.getManipulatorUUID());
    }

    @Environment(EnvType.CLIENT)
    public boolean checkIfSpectating() {
        return Minecraft.getInstance().player instanceof SpectreBoundSpyglass spectreBoundSpyglass && spectreBoundSpyglass.isUsingSpectreBoundedSpyglass();
    }

    private void copyPlayerRotation(Player player) {
        this.setYRot(player.getYRot());
        this.setXRot(player.getXRot() * 0.5F);
        this.setRot(this.getYRot(), this.getXRot());
    }

    private void handlePerspective(Player player) {
        if (this.getPhase() < 12 && this.tickCount % 5 == 0) {
            this.setPhase(this.getPhase() + 1);
        }
        if (this.getSpectatableTime() > 0) {
            this.setSpectatableTime(this.getSpectatableTime() - 1);
            this.sendPerspective(player, this.getId());
        } else {
            this.resetPerspective(player);
            this.discard();
        }
    }

    private void sendPerspective(Player player, int Id) {
        if (player instanceof ServerPlayer serverPlayer) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeUUID(player.getUUID());
            buf.writeInt(Id);
            ServerPlayNetworking.send(serverPlayer, GNetwork.SEND_PERSPECTIVE, buf);
        }
    }

    private void resetPerspective(Player player) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(player.getUUID());
        ServerPlayNetworking.send((ServerPlayer) player, GNetwork.RESET_PERSPECTIVE, buf);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setSpectatableTime(tag.getInt("SpectatingTicks"));
        this.setPhase(tag.getInt("Phase"));
        UUID uuid;
        if (tag.hasUUID("Manipulator")) {
            uuid = tag.getUUID("Manipulator");
        } else {
            String s = tag.getString("Manipulator");
            uuid = OldUsersConverter.convertMobOwnerIfNecessary(this.getServer(), s);
        }
        if (uuid != null) {
            this.setManipulatorUUID(uuid);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.getManipulatorUUID() != null) {
            tag.putUUID("Manipulator", this.getManipulatorUUID());
        }
        tag.putInt("SpectatingTicks", this.getSpectatableTime());
        tag.putInt("Phase", this.getPhase());
    }

    @Nullable
    public UUID getManipulatorUUID() {
        return this.entityData.get(MANIPULATOR).orElse(null);
    }

    public void setManipulatorUUID(@Nullable UUID uuid) {
        this.entityData.set(MANIPULATOR, Optional.ofNullable(uuid));
    }

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    public void setPhase(int phase) {
        this.entityData.set(PHASE, phase);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    public static SpectatorVision create(Level world, Vec3 blockPos, int ticks) {
        SpectatorVision spectatorVision = new SpectatorVision(world);
        spectatorVision.moveTo(blockPos.x, blockPos.y, blockPos.z);
        spectatorVision.setSpectatableTime(ticks);
        return spectatorVision;
    }

    public int getSpectatableTime() {
        return this.spectatingTicks;
    }

    public void setSpectatableTime(int spectatableTime) {
        this.spectatingTicks = spectatableTime;
    }

    @Override
    public boolean isUsingSpectreBoundedSpyglass() {
        return this.isAlive() && this.getManipulatorUUID() != null;
    }

    @Override
    public void setUsingSpectreBoundedSpyglass(boolean usingSpectreBoundedSpyglass) {
    }

}
