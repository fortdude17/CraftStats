package com.craftstats.common.mixin;

import com.craftstats.common.stats.BlockStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockMixin {

    @Inject(method = "getFriction", at = @At("HEAD"), cancellable = true, require = 0)
    private void craftstats$friction(CallbackInfoReturnable<Float> cir) {
        Block self = (Block)(Object)this;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(self);
        if (id == null) return;
        BlockStats stats = StatRegistry.getBlock(id);
        if (stats != null && stats.slipperiness >= 0) cir.setReturnValue(stats.slipperiness);
    }

    @Inject(method = "getExplosionResistance", at = @At("HEAD"), cancellable = true, require = 0)
    private void craftstats$blastResistance(CallbackInfoReturnable<Float> cir) {
        Block self = (Block)(Object)this;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(self);
        if (id == null) return;
        BlockStats stats = StatRegistry.getBlock(id);
        if (stats != null && stats.blastResistance >= 0) cir.setReturnValue(stats.blastResistance);
    }
}
