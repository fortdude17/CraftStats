package com.craftstats.common.mixin;

import com.craftstats.common.stats.BlockStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
//#if MC >= 1.21.2
//$ import net.minecraft.world.level.LevelReader;
//$ import net.minecraft.world.level.ScheduledTickAccess;
//#else
import net.minecraft.world.level.LevelAccessor;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks on BlockState rather than Block: these methods are always called, even for blocks
 * that override the corresponding Block method.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockBehaviourStateMixin {

    @Shadow public abstract Block getBlock();

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void craftstats$hardness(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        BlockStats s = StatRegistry.forBlockAt(getBlock(), level, pos);
        if (s != null && s.hardness != null) cir.setReturnValue(s.hardness);
    }

    @Inject(method = "getLightEmission", at = @At("HEAD"), cancellable = true)
    private void craftstats$lightEmission(CallbackInfoReturnable<Integer> cir) {
        BlockStats s = StatRegistry.forBlock(getBlock());
        if (s != null && s.lightEmission != null) cir.setReturnValue(Math.max(0, Math.min(15, s.lightEmission)));
    }

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void craftstats$noCollision(BlockGetter level, BlockPos pos, CollisionContext ctx,
                                        CallbackInfoReturnable<VoxelShape> cir) {
        BlockStats s = StatRegistry.forBlockAt(getBlock(), level, pos);
        if (s != null && s.noCollision) cir.setReturnValue(Shapes.empty());
    }

    @Inject(method = "getPistonPushReaction", at = @At("HEAD"), cancellable = true)
    private void craftstats$pushReaction(CallbackInfoReturnable<PushReaction> cir) {
        BlockStats s = StatRegistry.forBlock(getBlock());
        if (s == null || s.pushReaction == null) return;
        switch (s.pushReaction) {
            case "destroy"   -> cir.setReturnValue(PushReaction.DESTROY);
            case "block"     -> cir.setReturnValue(PushReaction.BLOCK);
            case "ignore"    -> cir.setReturnValue(PushReaction.IGNORE);
            case "push_only" -> cir.setReturnValue(PushReaction.PUSH_ONLY);
            default -> {}
        }
    }

    @Inject(method = "requiresCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
    private void craftstats$requiresCorrectTool(CallbackInfoReturnable<Boolean> cir) {
        BlockStats s = StatRegistry.forBlock(getBlock());
        if (s != null && s.requiresCorrectTool) cir.setReturnValue(true);
    }

    /** Replaces vanilla XP with the configured amount, keeping the block's other after-break behaviour. */
    @ModifyVariable(method = "spawnAfterBreak", at = @At("HEAD"), argsOnly = true)
    private boolean craftstats$dropXp(boolean dropExperience, @Local(argsOnly = true) ServerLevel level,
                                      @Local(argsOnly = true) BlockPos pos) {
        BlockStats s = StatRegistry.forBlockAt(getBlock(), level, pos);
        if (s == null || s.dropXp == null) return dropExperience;
        if (dropExperience && s.dropXp > 0) ExperienceOrb.award(level, Vec3.atCenterOf(pos), s.dropXp);
        return false;
    }

    // ---- "Can Fall": behave like sand -------------------------------------------------

    @Inject(method = "onPlace", at = @At("TAIL"))
    private void craftstats$scheduleFallOnPlace(Level level, BlockPos pos, BlockState oldState, boolean movedByPiston,
                                                CallbackInfo ci) {
        if (!level.isClientSide() && craftstats$canFall()) level.scheduleTick(pos, getBlock(), 2);
    }

    //#if MC >= 1.21.2
    //$ @Inject(method = "updateShape", at = @At("HEAD"), require = 0)
    //$ private void craftstats$scheduleFallOnUpdate(LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
    //$                                              Direction direction, BlockPos neighborPos, BlockState neighbor,
    //$                                              RandomSource random, CallbackInfoReturnable<BlockState> cir) {
    //$     if (craftstats$canFall()) ticks.scheduleTick(pos, getBlock(), 2);
    //$ }
    //#else
    @Inject(method = "updateShape", at = @At("HEAD"), require = 0)
    private void craftstats$scheduleFallOnUpdate(Direction direction, BlockState neighbor, LevelAccessor level,
                                                 BlockPos pos, BlockPos neighborPos,
                                                 CallbackInfoReturnable<BlockState> cir) {
        if (craftstats$canFall()) level.scheduleTick(pos, getBlock(), 2);
    }
    //#endif

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void craftstats$fall(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!craftstats$canFall()) return;
        BlockState self = (BlockState) (Object) this;
        BlockState below = level.getBlockState(pos.below());
        //#if MC >= 1.21.2
        //$ int minY = level.getMinY();
        //#else
        int minY = level.getMinBuildHeight();
        //#endif
        if (pos.getY() > minY && (below.isAir() || below.liquid() || below.canBeReplaced())) {
            FallingBlockEntity.fall(level, pos, self);
            ci.cancel();
        }
    }

    private boolean craftstats$canFall() {
        BlockStats s = StatRegistry.forBlock(getBlock());
        return s != null && s.canFall;
    }
}
