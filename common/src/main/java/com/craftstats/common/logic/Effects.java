package com.craftstats.common.logic;

import com.craftstats.common.stats.HitEffects;
import com.craftstats.common.util.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Server-side building blocks for the "fun" stats: lightning, explosions, spawning, effects. */
public final class Effects {

    /** Hard limit so a typo can't level a whole world. */
    public static final float MAX_EXPLOSION = 20f;

    private Effects() {}

    public static void giveEffect(LivingEntity target, String effectId, Integer level, Integer seconds) {
        if (effectId == null || effectId.isBlank()) return;
        ResourceLocation id = ResourceLocation.tryParse(effectId.trim());
        if (id == null) return;
        int amp = Math.max(0, Math.min(255, (level == null ? 1 : level) - 1));
        int ticks = Math.max(1, (seconds == null ? 5 : seconds) * 20);
        Compat.effect(id).ifPresent(h -> target.addEffect(new MobEffectInstance(h, ticks, amp)));
    }

    public static void lightning(ServerLevel level, Vec3 pos) {
        Entity bolt = Compat.createEntity(EntityType.LIGHTNING_BOLT, level);
        if (bolt == null) return;
        bolt.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(bolt);
    }

    public static void explode(ServerLevel level, Entity source, Vec3 pos, float power) {
        if (!(power > 0)) return;
        level.explode(source, pos.x, pos.y, pos.z, Math.min(MAX_EXPLOSION, power), Level.ExplosionInteraction.TNT);
    }

    /** Spawns {@code count} entities of a type by id. Unknown ids and players are ignored. */
    public static void spawn(ServerLevel level, String entityId, Integer count, Vec3 pos) {
        if (entityId == null || entityId.isBlank()) return;
        ResourceLocation id = ResourceLocation.tryParse(entityId.trim());
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return;
        EntityType<?> type = Compat.registryValue(BuiltInRegistries.ENTITY_TYPE, id);
        if (type == EntityType.PLAYER) return;
        int n = Math.max(1, Math.min(16, count == null ? 1 : count));
        for (int i = 0; i < n; i++) {
            Entity e = Compat.createEntity(type, level);
            if (e == null) return;
            double ox = n > 1 ? (level.random.nextDouble() - 0.5) : 0, oz = n > 1 ? (level.random.nextDouble() - 0.5) : 0;
            e.setPos(pos.x + ox, pos.y, pos.z + oz);
            level.addFreshEntity(e);
        }
    }

    public static void drop(ServerLevel level, String itemId, Integer count, Vec3 pos) {
        if (itemId == null || itemId.isBlank()) return;
        ResourceLocation id = ResourceLocation.tryParse(itemId.trim());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return;
        Item item = Compat.registryValue(BuiltInRegistries.ITEM, id);
        if (item == Items.AIR) return;
        int left = Math.max(1, Math.min(64 * 9, count == null ? 1 : count));
        while (left > 0) {
            ItemStack stack = new ItemStack(item);
            int n = Math.min(left, stack.getMaxStackSize());
            stack.setCount(n);
            left -= n;
            ItemEntity e = new ItemEntity(level, pos.x, pos.y, pos.z, stack);
            e.setDefaultPickUpDelay();
            level.addFreshEntity(e);
        }
    }

    /** Teleports to a random safe spot within {@code radius} blocks, like a chorus fruit. */
    public static boolean randomTeleport(LivingEntity e, double radius) {
        if (radius <= 0) return false;
        for (int i = 0; i < 16; i++) {
            double x = e.getX() + (e.getRandom().nextDouble() - 0.5) * 2 * radius;
            double y = Mth.clamp(e.getY() + (e.getRandom().nextInt((int) Math.max(2, radius)) - radius / 2),
                    Compat.minY(e.level()), Compat.maxY(e.level()));
            double z = e.getZ() + (e.getRandom().nextDouble() - 0.5) * 2 * radius;
            if (e.randomTeleport(x, y, z, true)) {
                e.resetFallDistance();
                return true;
            }
        }
        return false;
    }

    /** Fire, potion effect, lifesteal and lightning from whatever hit {@code target}. */
    public static void applyHitEffects(LivingEntity attacker, LivingEntity target, HitEffects h, float damage) {
        if (h == null || !(target.level() instanceof ServerLevel level)) return;
        Integer fire = h.fireOnHit();
        if (fire != null && fire > 0) target.igniteForSeconds(fire);
        giveEffect(target, h.hitEffect(), h.hitEffectLevel(), h.hitEffectSeconds());
        Double steal = h.lifestealPercent();
        if (attacker != null && steal != null && steal > 0 && damage > 0) attacker.heal((float) (damage * steal / 100.0));
        if (h.lightningOnHit()) lightning(level, target.position());
    }

    public static BlockPos below(Entity e) {
        return BlockPos.containing(e.getX(), e.getY() - 0.2, e.getZ());
    }
}
