package com.craftstats.common.mixin;

import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.stats.WorldStats;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** World setting "XP Orbs": scales every XP drop (mobs, blocks, smelting, breeding...). */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {

    @ModifyVariable(method = "award", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int craftstats$xpMultiplier(int amount) {
        WorldStats w = StatRegistry.world();
        if (w == null || w.xpMultiplier == null || amount <= 0) return amount;
        return (int) Math.min(Integer.MAX_VALUE, Math.round(amount * Math.max(0, w.xpMultiplier)));
    }
}
