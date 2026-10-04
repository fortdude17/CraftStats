package com.craftstats.common.mixin;

import com.craftstats.common.stats.EnchantmentStats;
import com.craftstats.common.stats.StatRegistry;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import it.unimi.dsi.fastutil.objects.AbstractObject2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.LinkedHashSet;
import java.util.Set;
//#if MC < 1.21.2
import com.craftstats.common.stats.ItemOverrides;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.ItemStack;
//#endif

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    /**
     * Every enchantment effect (damage, protection, fortune...) is applied by iterating an
     * item's enchantments here. Disabled enchantments are skipped; the level bonus is added.
     */
    @ModifyExpressionValue(method = "runIterationOnItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/ItemEnchantments;entrySet()Ljava/util/Set;"))
    private static Set<Object2IntMap.Entry<Holder<Enchantment>>> craftstats$adjustLevels(
            Set<Object2IntMap.Entry<Holder<Enchantment>>> original) {
        if (!StatRegistry.hasEnchantmentOverrides() || original.isEmpty()) return original;
        Set<Object2IntMap.Entry<Holder<Enchantment>>> out = new LinkedHashSet<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> e : original) {
            EnchantmentStats s = StatRegistry.forEnchantment(e.getKey());
            if (s == null) { out.add(e); continue; }
            if (s.disabled) continue;
            int level = s.levelBonus == null ? e.getIntValue() : Math.max(1, Math.min(255, e.getIntValue() + s.levelBonus));
            out.add(new AbstractObject2IntMap.BasicEntry<>(e.getKey(), level));
        }
        return out;
    }

    //#if MC < 1.21.2
    /** Enchantability is a data component from 1.21.2 on; before that it's a method on Item. */
    @ModifyExpressionValue(method = {"getEnchantmentCost", "selectEnchantment"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;getEnchantmentValue()I"), require = 0)
    private static int craftstats$enchantability(int original, @Local(argsOnly = true) ItemStack stack) {
        Integer value = ItemOverrides.enchantability(stack.getItem());
        return value != null ? Math.max(0, value) : original;
    }
    //#endif
}
