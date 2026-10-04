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
import com.craftstats.common.logic.ItemHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
//#if MC < 1.21.2
import net.minecraft.world.InteractionResultHolder;
//#endif

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

    // ---- using -----------------------------------------------------------------------------

    /** Throwable items: right-click throws instead of the normal use. */
    //#if MC >= 1.21.2
    //$ @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    //$ private void craftstats$throw(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
    //$     ItemStack self = (ItemStack) (Object) this;
    //$     if (ItemHooks.tryThrow(level, player, hand, self)) cir.setReturnValue(InteractionResult.SUCCESS);
    //$ }
    //$
    //$ @Inject(method = "use", at = @At("RETURN"))
    //$ private void craftstats$useCooldown(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
    //$     ItemHooks.afterUse(player, (ItemStack) (Object) this, cir.getReturnValue().consumesAction());
    //$ }
    //#else
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void craftstats$throw(Level level, Player player, InteractionHand hand,
                                  CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (ItemHooks.tryThrow(level, player, hand, self))
            cir.setReturnValue(InteractionResultHolder.sidedSuccess(self, level.isClientSide()));
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void craftstats$useCooldown(Level level, Player player, InteractionHand hand,
                                        CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemHooks.afterUse(player, (ItemStack) (Object) this, cir.getReturnValue().getResult().consumesAction());
    }
    //#endif

    /** Extra healing and XP when an edited food is eaten. */
    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void craftstats$eatExtras(Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (ItemOverrides.isOverridden(getItem())) ItemHooks.afterFinishUsing(level, entity, getItem());
    }
}
