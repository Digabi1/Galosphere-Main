package net.orcinus.galosphere.entities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.feature.DripstoneUtils;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.orcinus.galosphere.api.Spectatable;
import net.orcinus.galosphere.init.GEntityTypes;
import net.orcinus.galosphere.init.GItems;
import net.orcinus.galosphere.init.GNetwork;
import net.orcinus.galosphere.init.GSoundEvents;
import net.orcinus.galosphere.mixin.access.FireworkRocketEntityAccessor;
import org.jetbrains.annotations.Nullable;

public class SpectreFlare extends FireworkRocketEntity {
    public SpectreFlare(EntityType<? extends SpectreFlare> type, Level world) {
        super(type, world);
    }

    public SpectreFlare(Level world, ItemStack stack, Entity entity, double x, double y, double z, boolean shotAtAngle) {
        super(world, stack, entity, x, y, z, shotAtAngle);
    }

    public SpectreFlare(Level level, @Nullable Entity entity, double d, double e, double f, ItemStack itemStack) {
        this(level, d, e, f, itemStack);
        this.setOwner(entity);
    }

    public SpectreFlare(Level level, double d, double e, double f, ItemStack itemStack) {
        super(GEntityTypes.SPECTRE_FLARE, level);
        ((FireworkRocketEntityAccessor)this).setLife(0);
        this.setPos(d, e, f);
        if (!itemStack.isEmpty() && itemStack.hasTag()) {
            this.entityData.set(FireworkRocketEntityAccessor.getDATA_ID_FIREWORKS_ITEM(), itemStack.copy());
        }
        this.setDeltaMovement(this.random.triangle(0.0, 0.002297), 0.05, this.random.triangle(0.0, 0.002297));
        ((FireworkRocketEntityAccessor) this).setLifeTime(100);
    }

    public SpectreFlare(Level level, ItemStack itemStack, double d, double e, double f, boolean bl) {
        this(level, d, e, f, itemStack);
        this.entityData.set(FireworkRocketEntityAccessor.getDATA_SHOT_AT_ANGLE(), bl);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide && ((FireworkRocketEntityAccessor)this).getLife() > ((FireworkRocketEntityAccessor)this).getLifetime()) {
            this.spawnSpectatorVision(this.position());
            this.level.broadcastEntityEvent(this, (byte)17);
            this.gameEvent(GameEvent.EXPLODE, this.getOwner());
            this.discard();
        }
    }

    @Override
    public EntityType<?> getType() {
        return GEntityTypes.SPECTRE_FLARE;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.level.isClientSide()) {
            BlockPos hitPos = result.getBlockPos();
            BlockPos placePos = hitPos.relative(result.getDirection());
            Material material = this.level.getBlockState(placePos).getMaterial();
            if (this.level.getBlockState(hitPos).isCollisionShapeFullBlock(this.level, hitPos) && (material != Material.LAVA || this.level.isStateAtPosition(placePos, DripstoneUtils::isEmptyOrWater))) {
                this.spawnSpectatorVision(Vec3.atCenterOf(placePos));
            }
            this.discard();
        }
    }

    private void spawnSpectatorVision(Vec3 vec3) {
        if (this.getOwner() instanceof ServerPlayer serverPlayer) {
            if (!this.isCameraEntitySpectatorVision()) {
                SpectatorVision spectatorVision = SpectatorVision.create(this.level, vec3, serverPlayer, 120);
                serverPlayer.playNotifySound(GSoundEvents.SPECTRE_MANIPULATE_BEGIN, getSoundSource(), 1, 1);
                this.level.addFreshEntity(spectatorVision);
                FriendlyByteBuf buf = PacketByteBufs.create();
                buf.writeUUID(serverPlayer.getUUID());
                buf.writeInt(spectatorVision.getId());
                ServerPlayNetworking.send(serverPlayer, GNetwork.SEND_PERSPECTIVE, buf);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private boolean isCameraEntitySpectatorVision() {
        return Minecraft.getInstance().getCameraEntity() instanceof Spectatable;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(GItems.SPECTRE_FLARE);
    }

}