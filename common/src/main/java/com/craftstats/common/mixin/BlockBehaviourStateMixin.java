package com.craftstats.common.mixin;

import com.craftstats.common.stats.BlockStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockBehaviourStateMixin {

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void craftstats$hardness(BlockGetter level, BlockPos pos,
                                      CallbackInfoReturnable<Float> cir) {
        BlockState state = (BlockState)(Object)this;

        if (level instanceof Level lv) {
            String posKey = StatRegistry.makePosKey(lv.dimension(), pos);
            BlockStats posStats = StatRegistry.getBlockAt(posKey);
            if (posStats != null && posStats.hardness >= 0) { cir.setReturnValue(posStats.hardness); return; }
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        BlockStats stats = StatRegistry.getBlock(id);
        if (stats != null && stats.hardness >= 0) cir.setReturnValue(stats.hardness);
    }

    @Inject(method = "getLightEmission", at = @At("HEAD"), cancellable = true)
    private void craftstats$lightEmission(CallbackInfoReturnable<Integer> cir) {
        BlockState state = (BlockState)(Object)this;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        BlockStats stats = StatRegistry.getBlock(id);
        if (stats != null && stats.lightEmission >= 0) cir.setReturnValue(stats.lightEmission);
    }

    @Inject(
        method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("HEAD"), cancellable = true, require = 0
    )
    private void craftstats$noCollision(BlockGetter level, BlockPos pos, CollisionContext ctx,
                                         CallbackInfoReturnable<VoxelShape> cir) {
        BlockState state = (BlockState)(Object)this;

        if (level instanceof Level lv) {
            String posKey = StatRegistry.makePosKey(lv.dimension(), pos);
            BlockStats ps = StatRegistry.getBlockAt(posKey);
            if (ps != null && ps.noCollision) { cir.setReturnValue(Shapes.empty()); return; }
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        BlockStats stats = StatRegistry.getBlock(id);
        if (stats != null && stats.noCollision) cir.setReturnValue(Shapes.empty());
    }

    @Inject(method = "getPistonPushReaction", at = @At("HEAD"), cancellable = true, require = 0)
    private void craftstats$pushReaction(CallbackInfoReturnable<PushReaction> cir) {
        BlockState state = (BlockState)(Object)this;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        BlockStats stats = StatRegistry.getBlock(id);
        if (stats == null || stats.pushReaction.equals("normal")) return;
        PushReaction reaction = switch (stats.pushReaction) {
            case "destroy"   -> PushReaction.DESTROY;
            case "block"     -> PushReaction.BLOCK;
            case "ignore"    -> PushReaction.IGNORE;
            case "push_only" -> PushReaction.PUSH_ONLY;
            default          -> PushReaction.NORMAL;
        };
        cir.setReturnValue(reaction);
    }

    @Inject(method = "requiresCorrectToolForDrops", at = @At("HEAD"), cancellable = true, require = 0)
    private void craftstats$requiresCorrectTool(CallbackInfoReturnable<Boolean> cir) {
        BlockState state = (BlockState)(Object)this;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        BlockStats stats = StatRegistry.getBlock(id);
        if (stats != null && stats.requiresCorrectTool) cir.setReturnValue(true);
    }

    @Inject(method = "spawnAfterBreak", at = @At("HEAD"), cancellable = true, require = 0)
    private void craftstats$dropXp(ServerLevel level, BlockPos pos, ItemStack tool,
                                    boolean dropExperience, CallbackInfo ci) {
        if (!dropExperience) return;
        BlockState state = (BlockState)(Object)this;

        String posKey = StatRegistry.makePosKey(level.dimension(), pos);
        BlockStats ps = StatRegistry.getBlockAt(posKey);
        if (ps != null && ps.dropXp >= 0) {
            if (ps.dropXp > 0) ExperienceOrb.award(level, Vec3.atCenterOf(pos), ps.dropXp);
            ci.cancel();
            return;
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) return;
        BlockStats stats = StatRegistry.getBlock(id);
        if (stats != null && stats.dropXp >= 0) {
            if (stats.dropXp > 0) ExperienceOrb.award(level, Vec3.atCenterOf(pos), stats.dropXp);
            ci.cancel();
        }
    }
}
