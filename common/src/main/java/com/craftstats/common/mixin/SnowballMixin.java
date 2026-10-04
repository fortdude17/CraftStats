package com.craftstats.common.mixin;

import com.craftstats.common.logic.ItemHooks;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC >= 1.21.11
//$ import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
//#else
import net.minecraft.world.entity.projectile.Snowball;
//#endif

/** Thrown CraftStats items ride on a snowball entity; they handle their own hits. */
@Mixin(Snowball.class)
public abstract class SnowballMixin {

    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void craftstats$thrownItemHit(HitResult hit, CallbackInfo ci) {
        if (ItemHooks.onThrownHit((Snowball) (Object) this, hit)) ci.cancel();
    }
}
