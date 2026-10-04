package com.craftstats.common.stats;

import com.craftstats.common.util.BlockStatExtractor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;

/**
 * The unmodified ("vanilla") values of a target, as fully filled-in stat objects. Used for
 * editor hints and as the baseline for randomizing.
 */
public final class VanillaStats {

    private VanillaStats() {}

    @SuppressWarnings("unchecked")
    public static MobStats mob(EntityType<?> type) {
        MobStats s = new MobStats();
        if (!DefaultAttributes.hasSupplier(type)) return s;
        AttributeSupplier sup = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
        s.maxHealth       = base(sup, Attributes.MAX_HEALTH);
        s.attackDamage    = base(sup, Attributes.ATTACK_DAMAGE);
        s.armor           = base(sup, Attributes.ARMOR);
        s.knockbackResist = base(sup, Attributes.KNOCKBACK_RESISTANCE);
        s.moveSpeed       = base(sup, Attributes.MOVEMENT_SPEED);
        s.jumpForce       = base(sup, Attributes.JUMP_STRENGTH);
        s.followRange     = base(sup, Attributes.FOLLOW_RANGE);
        s.sizeScale       = base(sup, Attributes.SCALE);
        return s;
    }

    private static Double base(AttributeSupplier sup, Holder<Attribute> attr) {
        return sup.hasAttribute(attr) ? sup.getBaseValue(attr) : null;
    }

    public static BlockStats block(Block block) {
        return BlockStatExtractor.extractFrom(block);
    }

    public static ItemStats item(Item item) {
        DataComponentMap c = item.components();
        ItemStats s = new ItemStats();
        s.stackSize = c.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
        Integer maxDamage = c.get(DataComponents.MAX_DAMAGE);
        s.maxDurability = maxDamage;
        ItemAttributeModifiers mods = c.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        double damage = 1.0, speed = 4.0;
        for (ItemAttributeModifiers.Entry e : mods.modifiers()) {
            if (!e.slot().test(EquipmentSlot.MAINHAND)) continue;
            if (e.attribute().equals(Attributes.ATTACK_DAMAGE)) damage += e.modifier().amount();
            if (e.attribute().equals(Attributes.ATTACK_SPEED))  speed  += e.modifier().amount();
        }
        s.attackDamage = damage;
        s.attackSpeed = speed;
        Tool tool = c.get(DataComponents.TOOL);
        if (tool != null) {
            float best = tool.defaultMiningSpeed();
            for (Tool.Rule r : tool.rules()) if (r.speed().isPresent()) best = Math.max(best, r.speed().get());
            s.miningSpeed = best;
        }
        FoodProperties food = c.get(DataComponents.FOOD);
        if (food != null) {
            s.nutrition = food.nutrition();
            s.saturation = food.saturation();
        }
        return s;
    }

    public static PlayerStats player() {
        return new PlayerStats();
    }
}
