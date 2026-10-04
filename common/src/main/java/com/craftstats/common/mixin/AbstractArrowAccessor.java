package com.craftstats.common.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
//#if MC >= 1.21.11
//$ import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
//#else
import net.minecraft.world.entity.projectile.AbstractArrow;
//#endif

@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Invoker("setPierceLevel")
    void craftstats$setPierceLevel(byte level);
}
