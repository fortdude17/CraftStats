package com.craftstats.common.stats;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

/** Mob stats that map one-to-one onto an attribute. */
public final class MobAttributes {

    public record Link(String field, Holder<Attribute> attribute) {}

    public static final List<Link> LINKS = List.of(
            new Link("maxHealth", Attributes.MAX_HEALTH),
            new Link("attackDamage", Attributes.ATTACK_DAMAGE),
            new Link("attackKnockback", Attributes.ATTACK_KNOCKBACK),
            new Link("armor", Attributes.ARMOR),
            new Link("armorToughness", Attributes.ARMOR_TOUGHNESS),
            new Link("knockbackResist", Attributes.KNOCKBACK_RESISTANCE),
            new Link("absorption", Attributes.MAX_ABSORPTION),
            new Link("moveSpeed", Attributes.MOVEMENT_SPEED),
            new Link("flyingSpeed", Attributes.FLYING_SPEED),
            new Link("jumpForce", Attributes.JUMP_STRENGTH),
            new Link("stepHeight", Attributes.STEP_HEIGHT),
            new Link("gravity", Attributes.GRAVITY),
            new Link("safeFallDistance", Attributes.SAFE_FALL_DISTANCE),
            new Link("fallDamageMultiplier", Attributes.FALL_DAMAGE_MULTIPLIER),
            new Link("waterMovement", Attributes.WATER_MOVEMENT_EFFICIENCY),
            new Link("movementEfficiency", Attributes.MOVEMENT_EFFICIENCY),
            new Link("oxygenBonus", Attributes.OXYGEN_BONUS),
            new Link("followRange", Attributes.FOLLOW_RANGE),
            new Link("sizeScale", Attributes.SCALE),
            new Link("burningTime", Attributes.BURNING_TIME),
            new Link("explosionKbResist", Attributes.EXPLOSION_KNOCKBACK_RESISTANCE));

    private MobAttributes() {}

    public static Holder<Attribute> forField(String field) {
        for (Link l : LINKS) if (l.field().equals(field)) return l.attribute();
        return null;
    }
}
