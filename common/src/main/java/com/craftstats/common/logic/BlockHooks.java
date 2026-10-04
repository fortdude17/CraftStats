package com.craftstats.common.logic;

import com.craftstats.common.stats.BlockStats;
import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.util.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** What edited blocks do to entities standing on or inside them. */
public final class BlockHooks {

    private BlockHooks() {}

    /** The overrides of the block an entity is standing on, or null. */
    public static BlockStats standingOn(LivingEntity self) {
        if (!self.onGround()) return null;
        Level level = self.level();
        BlockPos below = Effects.below(self);
        BlockStats bs = StatRegistry.forBlockAt(level.getBlockState(below).getBlock(), level, below);
        return bs != null && bs.hasStepEffects() ? bs : null;
    }

    /**
     * Every tick for every living entity while block overrides exist. Effects run on the
     * server; movement changes for players run on their own client (player movement is
     * client-side), for everything else on the server.
     */
    public static void entityTick(LivingEntity self) {
        Level level = self.level();
        boolean client = level.isClientSide();
        boolean movesHere = self instanceof Player p ? client && p.isLocalPlayer() : !client;

        BlockPos feet = self.blockPosition();
        BlockState insideState = level.getBlockState(feet);
        BlockStats inside = StatRegistry.forBlockAt(insideState.getBlock(), level, feet);
        if (inside != null && inside.stickyInside && movesHere)
            self.makeStuckInBlock(insideState, new Vec3(0.25, 0.05, 0.25));

        BlockStats bs = standingOn(self);
        if (bs == null) return;
        if (movesHere) movement(self, bs);
        if (!client) effects(self, bs);
    }

    private static void movement(LivingEntity self, BlockStats bs) {
        Vec3 v = self.getDeltaMovement();
        if (bs.launchPower > 0) {
            self.setDeltaMovement(v.x, Math.min(10, bs.launchPower), v.z);
            self.resetFallDistance();
            self.hurtMarked = true;
            return;
        }
        if (bs.knockbackOnStep > 0) {
            double hx = -v.x, hz = -v.z, len = Math.sqrt(hx * hx + hz * hz);
            if (len < 1.0E-3) {
                double a = self.getRandom().nextDouble() * Math.PI * 2;
                hx = Math.cos(a); hz = Math.sin(a); len = 1;
            }
            double p = Math.min(10, bs.knockbackOnStep) * 0.5;
            self.setDeltaMovement(hx / len * p, 0.4, hz / len * p);
            self.hurtMarked = true;
            return;
        }
        if (!BlockStats.OFF.equals(bs.conveyorDirection)) {
            double s = Math.min(2, bs.conveyorSpeed);
            double dx = 0, dz = 0;
            switch (bs.conveyorDirection) {
                case "north" -> dz = -s;
                case "south" -> dz = s;
                case "east" -> dx = s;
                case "west" -> dx = -s;
                default -> {}
            }
            // Blend towards the belt speed so walking on it still feels responsive.
            self.setDeltaMovement(v.x * 0.5 + dx * 0.5, v.y, v.z * 0.5 + dz * 0.5);
            self.hurtMarked = true;
        }
    }

    private static void effects(LivingEntity self, BlockStats bs) {
        int t = self.tickCount;
        if (bs.stepDamage > 0 && t % 10 == 0)
            Compat.hurt(self, self.damageSources().generic(), bs.stepDamage);

        if (bs.speedModifier > 0 && bs.speedModifier != 1.0f) {
            int amplifier = Math.round(bs.speedModifier * 10) - 10;
            if (amplifier > 0)
                self.addEffect(new MobEffectInstance(Compat.SPEED, 25, Math.min(255, amplifier - 1), false, false));
            else if (amplifier < 0)
                self.addEffect(new MobEffectInstance(Compat.SLOWNESS, 25, Math.min(9, -amplifier - 1), false, false));
        }
        if (bs.levitate)
            self.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 25, 0, false, false));
        if (bs.glowOnStep)
            self.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
        if (bs.freezeOnStep) {
            self.addEffect(new MobEffectInstance(Compat.SLOWNESS, 25, 3, false, false));
            self.setTicksFrozen(Math.min(self.getTicksFrozen() + 3, self.getTicksRequiredToFreeze() + 10));
        }
        if (!bs.onStepPotion.isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(bs.onStepPotion.trim());
            if (id != null) Compat.effect(id).ifPresent(holder -> {
                MobEffectInstance current = self.getEffect(holder);
                if (current == null || current.getDuration() < 20)
                    self.addEffect(new MobEffectInstance(holder, Math.max(25, bs.onStepPotionDuration),
                            Math.max(0, bs.onStepPotionLevel - 1), false, true));
            });
        }
        if (bs.extinguishOnStep && self.isOnFire()) self.clearFire();
        if (t % 20 != 0) return;
        if (bs.healOnStep > 0) self.heal(bs.healOnStep);
        if (bs.fireOnStep > 0) self.igniteForSeconds(bs.fireOnStep);
        if (bs.teleportOnStep > 0) Effects.randomTeleport(self, bs.teleportOnStep);
        if (self instanceof Player p) {
            if (bs.hungerOnStep > 0) p.causeFoodExhaustion(bs.hungerOnStep);
            if (bs.feedOnStep > 0) p.getFoodData().eat(bs.feedOnStep, 0.1f);
            if (bs.xpOnStep > 0) p.giveExperiencePoints(bs.xpOnStep);
        }
    }

    // ---- breaking ------------------------------------------------------------------------

    private record Regrow(ServerLevel level, BlockPos pos, BlockState state, long due) {}

    private static final List<Regrow> REGROW = new ArrayList<>();

    /** A player broke a block (server). The block is already gone. */
    public static void afterPlayerBreak(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        BlockStats s = StatRegistry.forBlockAt(state.getBlock(), level, pos);
        if (s == null || !s.hasBreakEffects()) return;
        Vec3 center = Vec3.atCenterOf(pos);
        if (s.damageBreaker != null && s.damageBreaker > 0)
            Compat.hurt(player, player.damageSources().magic(), s.damageBreaker);
        if (s.lightningOnBreak) Effects.lightning(level, center);
        Effects.spawn(level, s.spawnOnBreak, s.spawnOnBreakCount, Vec3.atBottomCenterOf(pos));
        if (s.regenSeconds != null && s.regenSeconds > 0) {
            synchronized (REGROW) {
                REGROW.add(new Regrow(level, pos.immutable(), state, level.getGameTime() + s.regenSeconds * 20L));
            }
        }
        if (s.explodeOnBreak != null && s.explodeOnBreak > 0) Effects.explode(level, null, center, s.explodeOnBreak);
    }

    /**
     * Before a broken block drops its items. Returns true if CraftStats handled the drops
     * (nothing, a replacement item, or nothing without silk touch).
     */
    public static boolean replaceDrops(BlockState state, Level level, BlockPos pos, ItemStack tool) {
        if (!(level instanceof ServerLevel server)) return false;
        BlockStats s = StatRegistry.forBlockAt(state.getBlock(), level, pos);
        if (s == null) return false;
        if (s.noDrops) return true;
        if (s.silkTouchOnly && !hasSilkTouch(tool)) return true;
        if (!s.dropOverride.isBlank()) {
            Effects.drop(server, s.dropOverride, s.dropOverrideCount, Vec3.atCenterOf(pos));
            return true;
        }
        return false;
    }

    /** Extra copies of the normal drops ("Drop Multiplier"). */
    public static int extraDropRolls(BlockState state, Level level, BlockPos pos) {
        BlockStats s = StatRegistry.forBlockAt(state.getBlock(), level, pos);
        return s == null || s.dropMultiplier == null ? 0 : Math.max(0, Math.min(63, s.dropMultiplier - 1));
    }

    private static boolean hasSilkTouch(ItemStack tool) {
        if (tool == null || tool.isEmpty()) return false;
        for (var holder : tool.getEnchantments().keySet())
            if (holder.is(Enchantments.SILK_TOUCH)) return true;
        return false;
    }

    /** Puts "grows back" blocks back once their time is up (and the spot is free). */
    public static void serverTick() {
        synchronized (REGROW) {
            if (REGROW.isEmpty()) return;
            Iterator<Regrow> it = REGROW.iterator();
            while (it.hasNext()) {
                Regrow r = it.next();
                if (r.level().getGameTime() < r.due()) continue;
                it.remove();
                BlockState now = r.level().getBlockState(r.pos());
                if (now.isAir() || now.canBeReplaced()) r.level().setBlockAndUpdate(r.pos(), r.state());
            }
        }
    }

    public static void clearRegrow() {
        synchronized (REGROW) { REGROW.clear(); }
    }
}
