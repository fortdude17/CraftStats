package com.craftstats.common.mixin;

import com.craftstats.common.stats.ItemOverrides;
import com.mojang.serialization.DataResult;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow public abstract Item getItem();
    @Shadow public abstract net.minecraft.core.component.DataComponentPatch getComponentsPatch();

    /** Layers item-type overrides (durability, stack size, damage, food...) over the stack's components. */
    @Inject(method = "getComponents", at = @At("RETURN"), cancellable = true)
    private void craftstats$overrideComponents(CallbackInfoReturnable<DataComponentMap> cir) {
        Item item = getItem();
        if (!ItemOverrides.isOverridden(item)) return;
        cir.setReturnValue(ItemOverrides.apply(item, cir.getReturnValue(), getComponentsPatch()));
    }

    /**
     * Stacks of an edited item may legitimately exceed a lowered stack size. Don't let
     * vanilla's strict validation delete them when a world or inventory is loaded.
     */
    @Inject(method = "validateStrict", at = @At("HEAD"), cancellable = true, require = 0)
    private static void craftstats$lenientValidation(ItemStack stack, CallbackInfoReturnable<DataResult<ItemStack>> cir) {
        if (!stack.isEmpty() && ItemOverrides.isOverridden(stack.getItem())) cir.setReturnValue(DataResult.success(stack));
    }
}
