package com.craftstats.common.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockBehaviour.Properties.class)
public interface BlockBehaviourPropertiesAccessor {
    @Accessor("destroyTime")
    float craftstats$getDestroyTime();

    @Accessor("hasCollision")
    boolean craftstats$getHasCollision();
}
