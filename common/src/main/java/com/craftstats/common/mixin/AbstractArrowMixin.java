package com.craftstats.common.mixin;

import com.craftstats.common.stats.ItemStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {

    @Inject(method = "getBaseDamage", at = @At("HEAD"), cancellable = true)
    private void craftstats$arrowDamage(CallbackInfoReturnable<Double> cir) {
        AbstractArrow self = (AbstractArrow) (Object) this;

        ItemStack weapon = self.getWeaponItem();
        if (weapon != null && !weapon.isEmpty()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(weapon.getItem());
            ItemStats stats = StatRegistry.getItem(id);
            if (stats != null && stats.attackDamage > 0) {
                cir.setReturnValue(stats.attackDamage);
            }
        }
    }
}
