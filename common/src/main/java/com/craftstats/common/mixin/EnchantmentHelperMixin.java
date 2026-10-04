package com.craftstats.common.mixin;

import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
//#if MC < 1.21.2
import com.craftstats.common.stats.ItemOverrides;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.At;
//#endif

/** Enchantability is a data component from 1.21.2 on; before that it's a method on Item. */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    //#if MC < 1.21.2
    @ModifyExpressionValue(method = {"getEnchantmentCost", "selectEnchantment"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;getEnchantmentValue()I"), require = 0)
    private static int craftstats$enchantability(int original, @Local(argsOnly = true) ItemStack stack) {
        Integer value = ItemOverrides.enchantability(stack.getItem());
        return value != null ? Math.max(0, value) : original;
    }
    //#endif
}
