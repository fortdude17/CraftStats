package com.craftstats.common.mixin;

import com.craftstats.common.stats.EnchantmentStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Direct level lookups (silk touch, fortune in loot tables...) see the same adjusted level. */
@Mixin(ItemEnchantments.class)
public abstract class ItemEnchantmentsMixin {

    @Inject(method = "getLevel", at = @At("RETURN"), cancellable = true)
    private void craftstats$adjustLevel(Holder<Enchantment> enchantment, CallbackInfoReturnable<Integer> cir) {
        int level = cir.getReturnValueI();
        if (level <= 0 || !StatRegistry.hasEnchantmentOverrides()) return;
        EnchantmentStats s = StatRegistry.forEnchantment(enchantment);
        if (s == null) return;
        if (s.disabled) cir.setReturnValue(0);
        else if (s.levelBonus != null) cir.setReturnValue(Math.max(1, Math.min(255, level + s.levelBonus)));
    }
}
