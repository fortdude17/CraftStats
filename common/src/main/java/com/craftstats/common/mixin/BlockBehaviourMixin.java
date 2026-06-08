package com.craftstats.common.mixin;

import com.craftstats.common.stats.BlockStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {

    private BlockStats craftstats$getStats() {
        if (!(((Object) this) instanceof Block self)) return null;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(self);
        if (id == null) return null;
        return StatRegistry.getBlock(id);
    }

    @Inject(method = "onPlace", at = @At("TAIL"), require = 0)
    private void craftstats$scheduleOnPlace(BlockState state, Level level, BlockPos pos,
                                             BlockState oldState, boolean isMoving, CallbackInfo ci) {
        if (level.isClientSide) return;
        BlockStats stats = craftstats$getStats();
        if (stats != null && stats.canFall && ((Object) this) instanceof Block self) {
            level.scheduleTick(pos, self, 2);
        }
    }

    @Inject(method = "neighborChanged", at = @At("TAIL"), require = 0)
    private void craftstats$scheduleOnNeighbor(BlockState state, Level level, BlockPos pos,
                                                Block neighborBlock, BlockPos neighborPos,
                                                boolean movedByPiston, CallbackInfo ci) {
        if (level.isClientSide) return;
        BlockStats stats = craftstats$getStats();
        if (stats != null && stats.canFall && ((Object) this) instanceof Block self) {
            level.scheduleTick(pos, self, 2);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void craftstats$doFall(BlockState state, ServerLevel level, BlockPos pos,
                                    RandomSource random, CallbackInfo ci) {
        BlockStats stats = craftstats$getStats();
        if (stats == null || !stats.canFall) return;
        if (!(((Object) this) instanceof Block self)) return;
        if (!level.getBlockState(pos).is(self)) return;
        BlockState below = level.getBlockState(pos.below());
        if (below.isAir() || below.liquid() || below.canBeReplaced()) {
            FallingBlockEntity.fall(level, pos, state);
        }
    }
}
