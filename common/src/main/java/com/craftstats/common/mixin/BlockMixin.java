package com.craftstats.common.mixin;

import com.craftstats.common.logic.BlockHooks;
import com.craftstats.common.stats.BlockStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockMixin {

    @Inject(method = "getFriction", at = @At("HEAD"), cancellable = true)
    private void craftstats$friction(CallbackInfoReturnable<Float> cir) {
        BlockStats s = StatRegistry.forBlock((Block) (Object) this);
        if (s != null && s.slipperiness != null) cir.setReturnValue(s.slipperiness);
    }

    @Inject(method = "getExplosionResistance", at = @At("HEAD"), cancellable = true)
    private void craftstats$blastResistance(CallbackInfoReturnable<Float> cir) {
        BlockStats s = StatRegistry.forBlock((Block) (Object) this);
        if (s != null && s.blastResistance != null) cir.setReturnValue(Math.max(0f, s.blastResistance));
    }

    @Inject(method = "getJumpFactor", at = @At("HEAD"), cancellable = true)
    private void craftstats$jumpFactor(CallbackInfoReturnable<Float> cir) {
        BlockStats s = StatRegistry.forBlock((Block) (Object) this);
        if (s != null && s.jumpFactor != null) cir.setReturnValue(Math.max(0f, s.jumpFactor));
    }

    @Inject(method = "getSpeedFactor", at = @At("HEAD"), cancellable = true)
    private void craftstats$speedFactor(CallbackInfoReturnable<Float> cir) {
        BlockStats s = StatRegistry.forBlock((Block) (Object) this);
        if (s != null && s.speedFactor != null) cir.setReturnValue(Math.max(0f, s.speedFactor));
    }

    /** Bounciness: landing bounces you back up like a slime block. Runs on both sides. */
    //#if MC >= 1.21.5
    //$ @Inject(method = "updateEntityMovementAfterFallOn", at = @At("HEAD"), cancellable = true)
    //#else
    @Inject(method = "updateEntityAfterFallOn", at = @At("HEAD"), cancellable = true)
    //#endif
    private void craftstats$bounce(BlockGetter level, Entity entity, CallbackInfo ci) {
        BlockStats s = StatRegistry.forBlock((Block) (Object) this);
        if (s == null || s.bounciness == null || s.bounciness <= 0 || entity.isSuppressingBounce()) return;
        Vec3 v = entity.getDeltaMovement();
        if (v.y < 0) {
            double factor = entity instanceof LivingEntity ? 1.0 : 0.8;
            entity.setDeltaMovement(v.x, -v.y * Math.min(2, s.bounciness) * factor, v.z);
            ci.cancel();
        }
    }

    /** Landing damage multiplier (hay bales are 0.2). Bounce pads don't hurt either. */
    //#if MC >= 1.21.11
    //$ @Inject(method = "fallOn", at = @At("HEAD"), cancellable = true)
    //$ private void craftstats$landing(Level level, BlockState state, BlockPos pos, Entity entity, double distance, CallbackInfo ci) {
    //#else
    @Inject(method = "fallOn", at = @At("HEAD"), cancellable = true)
    private void craftstats$landing(Level level, BlockState state, BlockPos pos, Entity entity, float distance, CallbackInfo ci) {
    //#endif
        BlockStats s = StatRegistry.forBlockAt((Block) (Object) this, level, pos);
        if (s == null) return;
        boolean bouncy = s.bounciness != null && s.bounciness > 0 && !entity.isSuppressingBounce();
        if (s.landingDamageMultiplier == null && !bouncy) return;
        float mult = bouncy ? 0f : Math.max(0f, s.landingDamageMultiplier);
        if (mult > 0) entity.causeFallDamage(distance, mult, entity.damageSources().fall());
        ci.cancel();
    }

    // ---- drops when a player breaks the block --------------------------------------------

    private static final String DROP_RESOURCES = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;"
            + "Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V";

    @Inject(method = DROP_RESOURCES, at = @At("HEAD"), cancellable = true)
    private static void craftstats$replaceDrops(BlockState state, Level level, BlockPos pos, BlockEntity blockEntity,
                                                Entity entity, ItemStack tool, CallbackInfo ci) {
        if (!StatRegistry.hasBlockOverrides() || !(entity instanceof Player)) return;
        if (BlockHooks.replaceDrops(state, level, pos, tool)) ci.cancel();
    }

    @Inject(method = DROP_RESOURCES, at = @At("TAIL"))
    private static void craftstats$extraDrops(BlockState state, Level level, BlockPos pos, BlockEntity blockEntity,
                                              Entity entity, ItemStack tool, CallbackInfo ci) {
        if (!StatRegistry.hasBlockOverrides() || !(entity instanceof Player) || !(level instanceof ServerLevel server)) return;
        int extra = BlockHooks.extraDropRolls(state, level, pos);
        for (int i = 0; i < extra; i++)
            Block.getDrops(state, server, pos, blockEntity, entity, tool).forEach(stack -> Block.popResource(level, pos, stack));
    }
}
