package com.craftstats.common.mixin;

import com.craftstats.common.logic.BlockHooks;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Block breaking effects: remember the block before it's broken, act once it's gone. */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;
    @Unique private BlockState craftstats$breaking;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void craftstats$beforeBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        craftstats$breaking = StatRegistry.hasBlockOverrides() ? level.getBlockState(pos) : null;
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void craftstats$afterBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = craftstats$breaking;
        craftstats$breaking = null;
        if (state != null && cir.getReturnValueZ()) BlockHooks.afterPlayerBreak(level, pos, state, player);
    }
}
