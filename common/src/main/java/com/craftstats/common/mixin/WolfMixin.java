package com.craftstats.common.mixin;

import com.craftstats.common.stats.ItemStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#if MC >= 1.21.11
//$ import net.minecraft.world.entity.animal.wolf.Wolf;
//#else
import net.minecraft.world.entity.animal.Wolf;
//#endif

/** "Wolves Eat It": wolves accept the item as food (taming, healing, breeding). */
@Mixin(Wolf.class)
public abstract class WolfMixin {

    @Inject(method = "isFood", at = @At("HEAD"), cancellable = true)
    private void craftstats$wolfFood(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        ItemStats s = StatRegistry.forItem(stack.getItem());
        if (s != null && s.wolfFood) cir.setReturnValue(true);
    }
}
