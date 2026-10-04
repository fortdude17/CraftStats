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
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
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

    // ---- replaceable, redstone, sound, looks, spawning, growth -------------------------

    @Inject(method = "canBeReplaced()Z", at = @At("HEAD"), cancellable = true)
    private void craftstats$replaceable(CallbackInfoReturnable<Boolean> cir) {
        BlockStats s = StatRegistry.forBlock(getBlock());
        if (s != null && s.replaceable) cir.setReturnValue(true);
    }

    @Inject(method = "canBeReplaced(Lnet/minecraft/world/item/context/BlockPlaceContext;)Z", at = @At("HEAD"), cancellable = true)
    private void craftstats$replaceableByPlacing(BlockPlaceContext ctx, CallbackInfoReturnable<Boolean> cir) {
        BlockStats s = StatRegistry.forBlock(getBlock());
        if (s != null && s.replaceable) cir.setReturnValue(true);
    }

    @Inject(method = "isSignalSource", at = @At("HEAD"), cancellable = true)
    private void craftstats$signalSource(CallbackInfoReturnable<Boolean> cir) {
        BlockStats s = StatRegistry.forBlock(getBlock());
        if (s != null && s.redstonePower != null && s.redstonePower > 0) cir.setReturnValue(true);
    }

    @Inject(method = "getSignal", at = @At("HEAD"), cancellable = true)
    private void craftstats$signal(BlockGetter level, BlockPos pos, Direction dir, CallbackInfoReturnable<Integer> cir) {
        BlockStats s = StatRegistry.forBlock(getBlock());
        if (s != null && s.redstonePower != null && s.redstonePower > 0) cir.setReturnValue(Math.min(15, s.redstonePower));
    }

    @Inject(method = "getSoundType", at = @At("HEAD"), cancellable = true)
    private void craftstats$sound(CallbackInfoReturnable<SoundType> cir) {
        BlockStats s = StatRegistry.forBlock(getBlock());
        if (s == null || s.soundType == null) return;
        SoundType t = switch (s.soundType) {
            case "stone" -> SoundType.STONE;
            case "wood" -> SoundType.WOOD;
            case "grass" -> SoundType.GRASS;
            case "gravel" -> SoundType.GRAVEL;
            case "sand" -> SoundType.SAND;
            case "snow" -> SoundType.SNOW;
            case "wool" -> SoundType.WOOL;
            case "glass" -> SoundType.GLASS;
            case "metal" -> SoundType.METAL;
            case "slime" -> SoundType.SLIME_BLOCK;
            case "honey" -> SoundType.HONEY_BLOCK;
            case "amethyst" -> SoundType.AMETHYST;
            case "bone" -> SoundType.BONE_BLOCK;
            case "netherrack" -> SoundType.NETHERRACK;
            default -> null;
        };
        if (t != null) cir.setReturnValue(t);
    }

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void craftstats$invisible(CallbackInfoReturnable<RenderShape> cir) {
        BlockStats s = StatRegistry.forBlock(getBlock());
        if (s != null && s.invisible) cir.setReturnValue(RenderShape.INVISIBLE);
    }

    @Inject(method = "isValidSpawn", at = @At("HEAD"), cancellable = true)
    private void craftstats$mobSpawning(BlockGetter level, BlockPos pos, EntityType<?> type, CallbackInfoReturnable<Boolean> cir) {
        BlockStats s = StatRegistry.forBlockAt(getBlock(), level, pos);
        if (s == null || s.mobSpawning == null) return;
        if (s.mobSpawning.equals("always")) cir.setReturnValue(true);
        else if (s.mobSpawning.equals("never")) cir.setReturnValue(false);
    }

    @Unique private static final ThreadLocal<Boolean> craftstats$extraTicks = ThreadLocal.withInitial(() -> false);

    /** Growth speed: random ticks (crop growth, saplings, ice melting...) happen n times as often. */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void craftstats$growthSpeed(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (craftstats$extraTicks.get()) return;
        BlockStats s = StatRegistry.forBlock(getBlock());
        if (s == null || s.randomTickMultiplier == null || s.randomTickMultiplier == 1) return;
        if (s.randomTickMultiplier <= 0) { ci.cancel(); return; }
        craftstats$extraTicks.set(true);
        try {
            BlockState self = (BlockState) (Object) this;
            for (int i = 1; i < Math.min(64, s.randomTickMultiplier); i++) {
                if (level.getBlockState(pos) != self) break; // it grew or changed
                self.randomTick(level, pos, random);
            }
        } finally {
            craftstats$extraTicks.set(false);
        }
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
