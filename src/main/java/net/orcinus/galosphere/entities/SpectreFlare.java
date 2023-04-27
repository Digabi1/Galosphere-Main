package net.orcinus.galosphere.entities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
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
import net.orcinus.galosphere.api.SpectreBoundSpyglass;
import net.orcinus.galosphere.init.GEntityTypes;
import net.orcinus.galosphere.init.GItems;
import net.orcinus.galosphere.init.GSoundEvents;
import net.orcinus.galosphere.mixin.access.FireworkRocketEntityAccessor;

public class SpectreFlare extends FireworkRocketEntity {
    public SpectreFlare(EntityType<? extends SpectreFlare> type, Level world) {
        super(type, world);
    }

    public SpectreFlare(Level world, ItemStack stack, Entity entity, double x, double y, double z, boolean shotAtAngle) {
        super(world, stack, entity, x, y, z, shotAtAngle);
    }

    public SpectreFlare(Level level, @org.jetbrains.annotations.Nullable Entity entity, double d, double e, double f, ItemStack itemStack) {
        this(level, d, e, f, itemStack);
        this.setOwner(entity);
    }

    public SpectreFlare(Level level, double d, double e, double f, ItemStack itemStack) {
        super(GEntityTypes.GLOW_FLARE, level);
        ((FireworkRocketEntityAccessor)this).setLife(0);
        this.setPos(d, e, f);
        if (!itemStack.isEmpty() && itemStack.hasTag()) {
            this.entityData.set(FireworkRocketEntityAccessor.getDATA_ID_FIREWORKS_ITEM(), itemStack.copy());
        }
        this.setDeltaMovement(this.random.triangle(0.0, 0.002297), 0.05, this.random.triangle(0.0, 0.002297));
        ((FireworkRocketEntityAccessor) this).setLifeTime(400);
    }

    public SpectreFlare(Level level, ItemStack itemStack, double d, double e, double f, boolean bl) {
        this(level, d, e, f, itemStack);
        this.entityData.set(FireworkRocketEntityAccessor.getDATA_SHOT_AT_ANGLE(), bl);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide && ((FireworkRocketEntityAccessor)this).getLife() > ((FireworkRocketEntityAccessor)this).getLifetime()) {
            if (this.getOwner() instanceof ServerPlayer serverPlayer) {
                SpectatorVision spectatorVision = SpectatorVision.create(this.level, this.position(), 120);
                spectatorVision.setManipulatorUUID(serverPlayer.getUUID());
                serverPlayer.playNotifySound(GSoundEvents.SPECTRE_MANIPULATE_BEGIN, getSoundSource(), 1, 1);
                this.level.addFreshEntity(spectatorVision);
            }
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
        if (this.checkIfSpectating()) {
            this.discard();
            return;
        }
        if (!this.level.isClientSide()) {
            BlockPos hitPos = result.getBlockPos();
            BlockPos placePos = hitPos.relative(result.getDirection());
            Material material = this.level.getBlockState(placePos).getMaterial();
            if (this.level.getBlockState(hitPos).isCollisionShapeFullBlock(this.level, hitPos) && (material != Material.LAVA || this.level.isStateAtPosition(placePos, DripstoneUtils::isEmptyOrWater))) {
                Vec3 vec3 = Vec3.atCenterOf(placePos);
                if (this.getOwner() instanceof ServerPlayer serverPlayer) {
                    SpectatorVision spectatorVision = SpectatorVision.create(this.level, vec3, 120);
                    spectatorVision.setManipulatorUUID(serverPlayer.getUUID());
                    serverPlayer.playNotifySound(GSoundEvents.SPECTRE_MANIPULATE_BEGIN, getSoundSource(), 1, 1);
                    this.level.addFreshEntity(spectatorVision);
                }
            }
            this.discard();
        }
    }

    @Environment(EnvType.CLIENT)
    public boolean checkIfSpectating() {
        return Minecraft.getInstance().player instanceof SpectreBoundSpyglass spectreBoundSpyglass && Minecraft.getInstance().getCameraEntity() instanceof SpectatorVision;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(GItems.SPECTRE_FLARE);
    }
}
