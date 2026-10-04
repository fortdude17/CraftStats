package com.craftstats.common.mixin;

import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
//#if MC < 1.21.2
import com.craftstats.common.stats.ItemStats;
import com.craftstats.common.stats.StatRegistry;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.At;
//#endif

/**
 * "Repaired With" before 1.21.2, where repair materials are hard-coded per item class.
 * Newer versions use the repairable data component (see ItemOverrides).
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    //#if MC < 1.21.2
    @WrapOperation(method = "createResult", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;isValidRepairItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean craftstats$repairMaterial(Item item, ItemStack tool, ItemStack material, Operation<Boolean> original) {
        ItemStats s = StatRegistry.forItem(item);
        if (s != null && !s.repairMaterial.isBlank())
            return BuiltInRegistries.ITEM.getKey(material.getItem()).toString().equals(s.repairMaterial.trim());
        return original.call(item, tool, material);
    }
    //#endif
}
