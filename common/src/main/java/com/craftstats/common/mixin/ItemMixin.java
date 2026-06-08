package com.craftstats.common.mixin;

import com.craftstats.common.stats.ItemStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemMixin {

    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void craftstats$stackSize(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(self.getItem());
        ItemStats stats = StatRegistry.getItem(id);
        if (stats != null) cir.setReturnValue(stats.stackSize);
    }

    @Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
    private void craftstats$maxDurability(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(self.getItem());
        ItemStats stats = StatRegistry.getItem(id);
        if (stats != null && stats.maxDurability > 0) cir.setReturnValue(stats.maxDurability);
    }
}
