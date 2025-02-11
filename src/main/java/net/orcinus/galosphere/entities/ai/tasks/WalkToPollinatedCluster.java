package net.orcinus.galosphere.entities.ai.tasks;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.orcinus.galosphere.blocks.PollinatedClusterBlock;
import net.orcinus.galosphere.entities.Sparkle;
import net.orcinus.galosphere.init.GMemoryModuleTypes;

import java.util.Optional;

public class WalkToPollinatedCluster extends Behavior<Sparkle> {
    private int sniffingTicks;
    private int stuckTicks;
    private boolean setCooldownOnly;

    public WalkToPollinatedCluster() {
        super(ImmutableMap.of(GMemoryModuleTypes.NEAREST_POLLINATED_CLUSTER.get(), MemoryStatus.VALUE_PRESENT, GMemoryModuleTypes.POLLINATED_COOLDOWN.get(), MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        this.setCooldownOnly = false;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Sparkle entity) {
        return !entity.isBaby() && this.getNearestCluster(entity).isPresent() && !entity.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET) && !entity.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
    }

    @Override
    protected boolean canStillUse(ServerLevel world, Sparkle entity, long p_22547_) {
        if (this.stuckTicks > 200) {
            this.setCooldownOnly = true;
            return false;
        } else {
            return this.sniffingTicks > 0 && this.getNearestCluster(entity).map(world::getBlockState).filter(this::isPollinatedCluster).isPresent();
        }
    }

    private boolean isPollinatedCluster(BlockState state) {
        return state.is(Blocks.AMETHYST_CLUSTER) || (state.getBlock() instanceof PollinatedClusterBlock && !state.getValue(PollinatedClusterBlock.POLLINATED));
    }

    @Override
    protected void start(ServerLevel world, Sparkle entity, long p_22542_) {
        this.sniffingTicks = 100;
    }

    @Override
    protected void tick(ServerLevel p_22551_, Sparkle entity, long p_22553_) {
        this.getNearestCluster(entity).ifPresent(blockPos -> {
            boolean flag = entity.blockPosition().distManhattan(blockPos) <= (entity.isInWaterOrBubble() ? 2.0F : 1.0F);
            if (flag) {
                entity.getNavigation().stop();
                this.sniffingTicks--;
            } else {
                this.stuckTicks++;
            }
            BehaviorUtils.setWalkAndLookTargetMemories(entity, blockPos, 2.0F, 0);
        });
    }

    @Override
    protected void stop(ServerLevel world, Sparkle entity, long p_22550_) {
        this.getNearestCluster(entity).filter(blockPos -> this.isPollinatedCluster(world.getBlockState(blockPos))).ifPresent(blockPos -> {
            entity.getBrain().eraseMemory(GMemoryModuleTypes.NEAREST_POLLINATED_CLUSTER.get());
            if (!this.setCooldownOnly) {
                BlockState state = world.getBlockState(blockPos);
                Block placeState = entity.getClustersToGlinted().get(state.getBlock());
                world.setBlock(blockPos, placeState.withPropertiesOf(state), 2);
                world.playSound(null, blockPos, placeState.defaultBlockState().getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                world.levelEvent(2005, blockPos, 0);
                return;
            }
            entity.getBrain().setMemory(GMemoryModuleTypes.POLLINATED_COOLDOWN.get(), 100);
        });
        this.stuckTicks = 0;
        this.setCooldownOnly = false;
    }

    @Override
    protected boolean timedOut(long p_22537_) {
        return false;
    }

    private Optional<BlockPos> getNearestCluster(Sparkle entity) {
        return entity.getBrain().getMemory(GMemoryModuleTypes.NEAREST_POLLINATED_CLUSTER.get());
    }
}