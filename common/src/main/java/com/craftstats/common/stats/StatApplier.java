package com.craftstats.common.stats;

import com.craftstats.common.CraftStats;
import com.craftstats.common.config.CraftStatsConfig;
import com.craftstats.common.util.Compat;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

/**
 * Applies mob and player overrides as transient attribute modifiers. Nothing is written
 * into the entity's saved attributes, so removing an override (or the mod) restores the
 * vanilla values exactly.
 */
public final class StatApplier {

    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(CraftStats.MOD_ID, "override");

    private static final PlayerStats VANILLA_PLAYER = new PlayerStats();

    /** Every player attribute CraftStats manages. */
    private static final List<Holder<Attribute>> PLAYER_ATTRIBUTES = List.of(
            Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED,
            Attributes.BLOCK_INTERACTION_RANGE, Attributes.ENTITY_INTERACTION_RANGE,
            Attributes.ATTACK_KNOCKBACK, Attributes.SWEEPING_DAMAGE_RATIO,
            Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH, Attributes.STEP_HEIGHT,
            Attributes.GRAVITY, Attributes.SNEAKING_SPEED, Attributes.MINING_EFFICIENCY,
            Attributes.MOVEMENT_EFFICIENCY, Attributes.SUBMERGED_MINING_SPEED,
            Attributes.WATER_MOVEMENT_EFFICIENCY, Attributes.OXYGEN_BONUS,
            Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS, Attributes.KNOCKBACK_RESISTANCE,
            Attributes.MAX_ABSORPTION, Attributes.LUCK, Attributes.BURNING_TIME,
            Attributes.FALL_DAMAGE_MULTIPLIER, Attributes.SAFE_FALL_DISTANCE,
            Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);

    private StatApplier() {}

    // ---- mobs --------------------------------------------------------------------------

    public static void applyMob(LivingEntity e) {
        if (e instanceof Player) return;
        MobStats s = StatRegistry.forEntity(e);
        boolean wasFullHealth = e.getHealth() >= e.getMaxHealth() - 1.0E-4f;

        set(e, Attributes.MAX_HEALTH,           s == null ? null : s.maxHealth);
        set(e, Attributes.ATTACK_DAMAGE,        s == null ? null : s.attackDamage);
        set(e, Attributes.ARMOR,                s == null ? null : s.armor);
        set(e, Attributes.KNOCKBACK_RESISTANCE, s == null ? null : s.knockbackResist);
        set(e, Attributes.MOVEMENT_SPEED,       s == null ? null : s.moveSpeed);
        set(e, Attributes.JUMP_STRENGTH,        s == null ? null : s.jumpForce);
        set(e, Attributes.FOLLOW_RANGE,         s == null ? null : s.followRange);
        Double scale = s == null || s.sizeScale == null ? null
                : Math.min(s.sizeScale, CraftStatsConfig.get().maxScaleCap);
        set(e, Attributes.SCALE, scale);

        if (wasFullHealth || e.getHealth() > e.getMaxHealth()) e.setHealth(e.getMaxHealth());
    }

    /** Re-applies overrides to every loaded mob of a type (or all mobs when typeId is null). */
    public static void refreshMobs(MinecraftServer server, ResourceLocation typeId) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living && !(living instanceof Player)
                        && (typeId == null || typeId.equals(EntityType.getKey(living.getType()))))
                    applyMob(living);
            }
        }
    }

    public static void refreshMob(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof LivingEntity living) { applyMob(living); return; }
        }
    }

    // ---- players -----------------------------------------------------------------------

    public static void applyPlayer(ServerPlayer p) {
        resetLegacyBaseValues(p);
        PlayerStats s = StatRegistry.forPlayer(p);
        PlayerStats v = s != null ? s : VANILLA_PLAYER;

        set(p, Attributes.MAX_HEALTH,                    target(s, v.maxHealth));
        set(p, Attributes.ATTACK_DAMAGE,                 target(s, v.baseDamage));
        set(p, Attributes.ATTACK_SPEED,                  target(s, v.attackSpeed));
        set(p, Attributes.BLOCK_INTERACTION_RANGE,       target(s, v.reachDistance));
        set(p, Attributes.ENTITY_INTERACTION_RANGE,      target(s, v.entityReach));
        set(p, Attributes.ATTACK_KNOCKBACK,              target(s, v.attackKnockback));
        set(p, Attributes.SWEEPING_DAMAGE_RATIO,         target(s, v.sweepingDamageRatio));
        set(p, Attributes.MOVEMENT_SPEED,                target(s, v.walkSpeed));
        set(p, Attributes.JUMP_STRENGTH,                 target(s, v.jumpForce));
        set(p, Attributes.STEP_HEIGHT,                   target(s, v.stepHeight));
        set(p, Attributes.GRAVITY,                       target(s, v.gravity));
        set(p, Attributes.SNEAKING_SPEED,                target(s, v.sneakingSpeed));
        set(p, Attributes.MINING_EFFICIENCY,             target(s, v.miningEfficiency));
        set(p, Attributes.MOVEMENT_EFFICIENCY,           target(s, v.movementEfficiency));
        set(p, Attributes.SUBMERGED_MINING_SPEED,        target(s, v.submergedMiningSpeed));
        set(p, Attributes.WATER_MOVEMENT_EFFICIENCY,     target(s, v.waterMovementEfficiency));
        set(p, Attributes.OXYGEN_BONUS,                  s != null && s.drownImmune ? 1024.0 : null);
        set(p, Attributes.ARMOR,                         target(s, v.armor));
        set(p, Attributes.ARMOR_TOUGHNESS,               target(s, v.armorToughness));
        set(p, Attributes.KNOCKBACK_RESISTANCE,          target(s, v.knockbackResistance));
        set(p, Attributes.MAX_ABSORPTION,                target(s, v.maxAbsorption));
        set(p, Attributes.LUCK,                          target(s, v.luck));
        set(p, Attributes.BURNING_TIME,                  target(s, v.burningTime));
        set(p, Attributes.FALL_DAMAGE_MULTIPLIER,        target(s, v.fallDamageMultiplier));
        set(p, Attributes.SAFE_FALL_DISTANCE,            target(s, v.safeFallDistance));
        set(p, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, target(s, v.explosionKbResistance));

        // Creative flight speed is an ability, not an attribute.
        float flySpeed = (float) v.flySpeed;
        if (p.getAbilities().getFlyingSpeed() != flySpeed) {
            p.getAbilities().setFlyingSpeed(flySpeed);
            p.onUpdateAbilities();
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    public static void refreshPlayer(MinecraftServer server, UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p != null) applyPlayer(p);
    }

    public static void refreshAllPlayers(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(StatApplier::applyPlayer);
    }

    /** Vanilla value when the player has no overrides, so stale modifiers get removed. */
    private static Double target(PlayerStats s, double value) {
        return s == null ? null : value;
    }

    /**
     * CraftStats 1.0 wrote its values straight into the player's saved base attributes,
     * including two wrong defaults (burning time 8.0, sweeping ratio 1.0) that never occur in
     * vanilla. If we see that fingerprint, put every managed base value back to vanilla.
     */
    private static void resetLegacyBaseValues(ServerPlayer p) {
        AttributeInstance burning = p.getAttribute(Attributes.BURNING_TIME);
        AttributeInstance sweeping = p.getAttribute(Attributes.SWEEPING_DAMAGE_RATIO);
        if (burning == null || sweeping == null
                || burning.getBaseValue() != 8.0 || sweeping.getBaseValue() != 1.0) return;
        AttributeSupplier defaults = DefaultAttributes.getSupplier(EntityType.PLAYER);
        for (Holder<Attribute> attr : PLAYER_ATTRIBUTES) {
            AttributeInstance inst = p.getAttribute(attr);
            if (inst != null && defaults.hasAttribute(attr)) inst.setBaseValue(defaults.getBaseValue(attr));
        }
        CraftStats.LOGGER.info("CraftStats: reset attribute base values left behind by CraftStats 1.0 for {}",
                Compat.profileName(p.getGameProfile()));
    }

    private static void set(LivingEntity e, Holder<Attribute> attr, Double target) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(MODIFIER_ID);
        if (target == null || target.isNaN()) return;
        double delta = target - inst.getBaseValue();
        if (Math.abs(delta) > 1.0E-9)
            inst.addTransientModifier(new AttributeModifier(MODIFIER_ID, delta, AttributeModifier.Operation.ADD_VALUE));
    }
}
