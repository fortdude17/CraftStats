package com.craftstats.common.mixin;

import com.craftstats.common.stats.BlockStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}
