package com.craftstats.common.mixin;

import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.stats.WorldStats;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** World setting "Explosion Power": every explosion goes through ServerLevel#explode. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @ModifyVariable(method = "explode", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float craftstats$explosionPower(float radius) {
        WorldStats w = StatRegistry.world();
        if (w == null || w.explosionPowerMultiplier == null) return radius;
        return (float) Math.min(64, radius * Math.max(0, w.explosionPowerMultiplier));
    }
}
