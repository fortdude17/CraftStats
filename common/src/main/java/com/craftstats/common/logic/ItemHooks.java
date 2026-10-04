package com.craftstats.common.logic;

import com.craftstats.common.stats.ItemStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
//#if MC >= 1.21.11
//$ import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
//#else
import net.minecraft.world.entity.projectile.Snowball;
//#endif

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/** Item stats that need gameplay hooks: throwing, boomerangs, cooldowns, eating, soulbound. */
public final class ItemHooks {

    /** Snowball entities that carry a thrown CraftStats item. */
    private static final Set<Entity> THROWN = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<Entity> RETURNING = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<UUID, List<Slot>> SOULBOUND = new ConcurrentHashMap<>();

    private record Slot(int index, ItemStack stack) {}

    private ItemHooks() {}

    // ---- using ---------------------------------------------------------------------------

    /** Right-click with a throwable item. Returns true if it was thrown (both sides). */
    public static boolean tryThrow(Level level, Player player, InteractionHand hand, ItemStack stack) {
        ItemStats s = StatRegistry.forItem(stack.getItem());
        if (s == null || !s.throwable || stack.isEmpty()) return false;
        if (!level.isClientSide()) {
            ItemStack thrown = stack.copyWithCount(1);
            //#if MC >= 1.21.2
            //$ Snowball ball = new Snowball(level, player, thrown);
            //#else
            Snowball ball = new Snowball(level, player);
            ball.setItem(thrown);
            //#endif
            float velocity = s.throwVelocity != null ? Math.max(0.1f, Math.min(10f, s.throwVelocity)) : 1.5f;
            ball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, velocity, 1.0f);
            THROWN.add(ball);
            level.addFreshEntity(ball);
            stack.consume(1, player);
        }
        float cooldown = s.useCooldown != null ? s.useCooldown : 0.25f;
        if (cooldown > 0) addCooldown(player, stack, Math.round(cooldown * 20));
        return true;
    }

    /** After a normal item use: apply the item's Use Cooldown. */
    public static void afterUse(Player player, ItemStack stack, boolean success) {
        if (!success || stack.isEmpty()) return;
        ItemStats s = StatRegistry.forItem(stack.getItem());
        if (s != null && s.useCooldown != null && s.useCooldown > 0) addCooldown(player, stack, Math.round(s.useCooldown * 20));
    }

    private static void addCooldown(Player player, ItemStack stack, int ticks) {
        //#if MC >= 1.21.2
        //$ player.getCooldowns().addCooldown(stack, ticks);
        //#else
        player.getCooldowns().addCooldown(stack.getItem(), ticks);
        //#endif
    }

    /** Eating (or drinking) finished: extra healing and XP. */
    public static void afterFinishUsing(Level level, LivingEntity entity, Item item) {
        if (level.isClientSide()) return;
        ItemStats s = StatRegistry.forItem(item);
        if (s == null) return;
        if (s.eatHeal != null && s.eatHeal > 0) entity.heal(s.eatHeal);
        if (s.eatXp != null && s.eatXp > 0 && entity instanceof Player p) p.giveExperiencePoints(s.eatXp);
    }

    // ---- thrown items --------------------------------------------------------------------

    public static boolean isThrown(Entity e) { return THROWN.contains(e); }

    /**
     * A thrown item hit something. Handles damage, explosions and lightning; the item drops
     * where it landed, or a boomerang turns around. Returns true to skip vanilla's snowball
     * logic.
     */
    public static boolean onThrownHit(Snowball ball, HitResult hit) {
        if (!THROWN.contains(ball)) return false;
        if (RETURNING.contains(ball)) return true; // flies through things on the way back
        if (!(ball.level() instanceof ServerLevel level)) return true;
        ItemStack item = ball.getItem();
        ItemStats s = StatRegistry.forItem(item.getItem());
        Entity owner = ball.getOwner();
        if (hit instanceof EntityHitResult eh && eh.getEntity() != owner) {
            float dmg = s != null && s.throwDamage != null ? s.throwDamage : 4f;
            if (eh.getEntity() instanceof LivingEntity target) {
                com.craftstats.common.util.Compat.hurt(target, ball.damageSources().thrown(ball, owner), dmg);
                Effects.applyHitEffects(owner instanceof LivingEntity o ? o : null, target, s, dmg);
            }
        }
        Vec3 at = hit.getLocation();
        boolean consumed = false;
        if (s != null) {
            if (s.lightningOnImpact) Effects.lightning(level, at);
            if (s.explodeOnImpact != null && s.explodeOnImpact > 0) {
                Effects.explode(level, ball, at, s.explodeOnImpact);
                consumed = true;
            }
        }
        if (!consumed && s != null && s.boomerang && owner != null) {
            RETURNING.add(ball);
            ball.setNoGravity(true);
            ball.setDeltaMovement(ball.getDeltaMovement().scale(-0.5));
            return true;
        }
        if (!consumed) dropItem(level, at, item);
        ball.discard();
        return true;
    }

    /** Every tick for thrown items: boomerangs fly back to their owner. */
    public static void thrownTick(Projectile p) {
        if (!RETURNING.contains(p) || !(p.level() instanceof ServerLevel level) || !(p instanceof Snowball ball)) return;
        Entity owner = p.getOwner();
        if (owner == null || !owner.isAlive() || owner.level() != level || p.tickCount > 400) {
            dropItem(level, p.position(), ball.getItem());
            p.discard();
            return;
        }
        Vec3 target = owner.getEyePosition().subtract(0, 0.3, 0);
        Vec3 to = target.subtract(p.position());
        if (to.lengthSqr() < 2.25) {
            if (owner instanceof Player player && !player.getInventory().add(ball.getItem().copy()))
                player.drop(ball.getItem().copy(), false);
            else if (!(owner instanceof Player)) dropItem(level, owner.position(), ball.getItem());
            p.discard();
            return;
        }
        p.setDeltaMovement(to.normalize().scale(Math.min(1.2, 0.4 + p.tickCount * 0.02)));
        p.hurtMarked = true;
    }

    private static void dropItem(ServerLevel level, Vec3 at, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity e = new ItemEntity(level, at.x, at.y, at.z, stack.copy());
        e.setDefaultPickUpDelay();
        level.addFreshEntity(e);
    }

    // ---- soulbound -----------------------------------------------------------------------

    /** Before a player's death drops: take soulbound items out so they aren't dropped. */
    public static void stashSoulbound(Player player) {
        if (StatRegistry.allItems().isEmpty()) return;
        Inventory inv = player.getInventory();
        List<Slot> kept = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            ItemStats s = StatRegistry.forItem(stack.getItem());
            if (s != null && s.soulbound) {
                kept.add(new Slot(i, stack.copy()));
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (!kept.isEmpty()) SOULBOUND.put(player.getUUID(), kept);
    }

    /** After respawning: give the soulbound items back in the same slots. */
    public static void restoreSoulbound(Player newPlayer) {
        List<Slot> kept = SOULBOUND.remove(newPlayer.getUUID());
        if (kept == null) return;
        Inventory inv = newPlayer.getInventory();
        for (Slot s : kept) {
            if (s.index() < inv.getContainerSize() && inv.getItem(s.index()).isEmpty()) inv.setItem(s.index(), s.stack());
            else if (!inv.add(s.stack())) newPlayer.drop(s.stack(), false);
        }
    }
}
