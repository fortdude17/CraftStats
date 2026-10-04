package com.craftstats.common.mixin;

import com.craftstats.common.stats.EnchantmentStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Max level, table weight, anvil cost, "works on any item" and "ignores conflicts". */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {

    private EnchantmentStats craftstats$stats() {
        return StatRegistry.forEnchantment((Enchantment) (Object) this);
    }

    @Inject(method = "getMaxLevel", at = @At("HEAD"), cancellable = true)
    private void craftstats$maxLevel(CallbackInfoReturnable<Integer> cir) {
        EnchantmentStats s = craftstats$stats();
        if (s != null && s.maxLevel != null) cir.setReturnValue(Math.max(1, Math.min(255, s.maxLevel)));
    }

    @Inject(method = "getWeight", at = @At("HEAD"), cancellable = true)
    private void craftstats$weight(CallbackInfoReturnable<Integer> cir) {
        EnchantmentStats s = craftstats$stats();
        if (s != null && s.weight != null) cir.setReturnValue(Math.max(1, s.weight));
    }

    @Inject(method = "getAnvilCost", at = @At("HEAD"), cancellable = true)
    private void craftstats$anvilCost(CallbackInfoReturnable<Integer> cir) {
        EnchantmentStats s = craftstats$stats();
        if (s != null && s.anvilCost != null) cir.setReturnValue(Math.max(0, s.anvilCost));
    }

    @Inject(method = {"canEnchant", "isSupportedItem"}, at = @At("HEAD"), cancellable = true)
    private void craftstats$anyItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        EnchantmentStats s = craftstats$stats();
        if (s != null && s.anyItem) cir.setReturnValue(true);
    }

    @Inject(method = "areCompatible", at = @At("HEAD"), cancellable = true)
    private static void craftstats$ignoreConflicts(Holder<Enchantment> a, Holder<Enchantment> b, CallbackInfoReturnable<Boolean> cir) {
        if (!StatRegistry.hasEnchantmentOverrides() || a.equals(b)) return;
        EnchantmentStats sa = StatRegistry.forEnchantment(a), sb = StatRegistry.forEnchantment(b);
        if ((sa != null && sa.ignoreConflicts) || (sb != null && sb.ignoreConflicts)) cir.setReturnValue(true);
    }
}
