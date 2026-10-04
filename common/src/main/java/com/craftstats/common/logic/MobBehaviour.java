package com.craftstats.common.logic;

import com.craftstats.common.mixin.MobAccessor;
import com.craftstats.common.stats.MobStats;
import com.craftstats.common.stats.PlayerStats;
import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.stats.WorldStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

/** AI, per-tick and death behaviour of edited mobs (server side). */
public final class MobBehaviour {

    /** Goals CraftStats added to make a mob hostile, so they can be removed again. */
    private static final Map<Mob, Goal[]> HOSTILE_GOALS = new WeakHashMap<>();

    private MobBehaviour() {}

    /** Adds or removes the "attack players" goals to match the Hostile stat. */
    public static void updateGoals(LivingEntity e) {
        if (!(e instanceof PathfinderMob mob)) return;
        MobStats ms = StatRegistry.forEntity(mob);
        boolean want = ms != null && ms.hostile && !ms.peaceful;
        Goal[] have = HOSTILE_GOALS.get(mob);
        MobAccessor acc = (MobAccessor) mob;
        if (want && have == null) {
            Goal attack = new MeleeAttackGoal(mob, 1.2, false);
            Goal target = new NearestAttackableTargetGoal<>(mob, Player.class, true);
            acc.craftstats$goalSelector().addGoal(1, attack);
            acc.craftstats$targetSelector().addGoal(1, target);
            HOSTILE_GOALS.put(mob, new Goal[]{attack, target});
        } else if (!want && have != null) {
            acc.craftstats$goalSelector().removeGoal(have[0]);
            acc.craftstats$targetSelector().removeGoal(have[1]);
            HOSTILE_GOALS.remove(mob);
            if (mob.getTarget() instanceof Player) mob.setTarget(null);
        }
    }

    /** For the smoke test. */
    public static boolean hasHostileGoals(Mob mob) {
        return HOSTILE_GOALS.containsKey(mob);
    }

    /** True if this mob must never target a player. */
    public static boolean ignoresPlayers(LivingEntity mob) {
        WorldStats w = StatRegistry.world();
        if (w != null && w.mobsIgnorePlayers) return true;
        MobStats ms = StatRegistry.forEntity(mob);
        return ms != null && ms.peaceful;
    }

    /** Every tick, server side, for mobs with overrides. */
    public static void tick(LivingEntity self, MobStats ms) {
        int t = self.tickCount;
        if (t % 20 == 0 && ms.regenPerSecond != null && ms.regenPerSecond > 0 && self.getHealth() < self.getMaxHealth())
            self.heal(ms.regenPerSecond.floatValue());
        if (t % 40 == 0 && !ms.permanentEffect.isBlank())
            Effects.giveEffect(self, ms.permanentEffect, ms.permanentEffectLevel, 6);
        if (self instanceof Mob mob && mob.getTarget() instanceof Player && ignoresPlayers(mob)) mob.setTarget(null);
    }

    /** Every tick, server side, for players with overrides. */
    public static void tickPlayer(Player self, PlayerStats ps) {
        if (self.tickCount % 20 == 0 && ps.regenPerSecond > 0 && self.getHealth() < self.getMaxHealth())
            self.heal((float) ps.regenPerSecond);
    }

    /** Explosions, lightning, spawns and extra drops when an edited mob dies. */
    public static void onDeath(LivingEntity self) {
        if (self instanceof Player || !(self.level() instanceof ServerLevel level)) return;
        MobStats ms = StatRegistry.forEntity(self);
        if (ms == null) return;
        var pos = self.position();
        if (ms.lightningOnDeath) Effects.lightning(level, pos);
        Effects.spawn(level, ms.spawnOnDeath, ms.spawnOnDeathCount, pos);
        Effects.drop(level, ms.dropOnDeath, ms.dropOnDeathCount, pos.add(0, 0.5, 0));
        if (ms.explodeOnDeath != null && ms.explodeOnDeath > 0) Effects.explode(level, self, pos, ms.explodeOnDeath);
    }
}
