package com.craftstats.common.mixin;

import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.stats.WorldStats;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** World setting "Mob Spawn Amount": scales how many mobs of each kind may exist. */
@Mixin(MobCategory.class)
public abstract class MobCategoryMixin {

    @Inject(method = "getMaxInstancesPerChunk", at = @At("RETURN"), cancellable = true)
    private void craftstats$spawnCap(CallbackInfoReturnable<Integer> cir) {
        WorldStats w = StatRegistry.world();
        if (w == null || w.spawnCapMultiplier == null || w.spawnCapMultiplier == 1.0) return;
        cir.setReturnValue((int) Math.round(cir.getReturnValueI() * Math.max(0, Math.min(20, w.spawnCapMultiplier))));
    }
}
