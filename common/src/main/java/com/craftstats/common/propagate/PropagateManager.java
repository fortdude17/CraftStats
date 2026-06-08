package com.craftstats.common.propagate;

import com.craftstats.common.network.CraftStatsNetwork;
import com.craftstats.common.stats.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public final class PropagateManager {

    private static UndoState lastUndo;

    public record DiffEntry(String field, String oldValue, String newValue) {}

    public record PropagatePreview(
            ResourceLocation targetType,
            int affectedCount,
            List<DiffEntry> diff
    ) {}

    public static PropagatePreview previewMob(ResourceLocation typeId,
                                               MobStats current,
                                               MobStats original) {
        int count = countLoadedEntities(typeId);
        List<DiffEntry> diffs = buildMobDiff(original, current);
        return new PropagatePreview(typeId, count, diffs);
    }

    public static void propagateMob(ResourceLocation typeId, MobStats stats, boolean perInstance,
                                     MobStats previous) {
        lastUndo = new UndoState("mob", typeId.toString(), previous, null, null, null);
        if (perInstance) {

        }
        CraftStatsNetwork.sendApplyMobStats(typeId.toString(), stats);
    }

    public static boolean canUndo() { return lastUndo != null; }

    public static void undoLastPropagate() {
        if (lastUndo == null) return;
        switch (lastUndo.targetType()) {
            case "mob" -> {
                if (lastUndo.mob() != null)
                    CraftStatsNetwork.sendApplyMobStats(lastUndo.targetId(), lastUndo.mob());
                else
                    CraftStatsNetwork.sendReset("mob", lastUndo.targetId());
            }
            case "block" -> {
                if (lastUndo.block() != null)
                    CraftStatsNetwork.sendApplyBlockStats(lastUndo.targetId(), lastUndo.block());
                else
                    CraftStatsNetwork.sendReset("block", lastUndo.targetId());
            }
        }
        lastUndo = null;
    }

    private static int countLoadedEntities(ResourceLocation typeId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0;
        int count = 0;
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity le) {
                if (typeId.equals(EntityType.getKey(le.getType()))) count++;
            }
        }
        return count;
    }

    private static List<DiffEntry> buildMobDiff(MobStats orig, MobStats curr) {
        List<DiffEntry> list = new ArrayList<>();
        if (orig.maxHealth      != curr.maxHealth)      list.add(new DiffEntry("max_health",      fmt(orig.maxHealth),      fmt(curr.maxHealth)));
        if (orig.attackDamage   != curr.attackDamage)   list.add(new DiffEntry("attack_damage",   fmt(orig.attackDamage),   fmt(curr.attackDamage)));
        if (orig.armor          != curr.armor)          list.add(new DiffEntry("armor",           fmt(orig.armor),          fmt(curr.armor)));
        if (orig.moveSpeed      != curr.moveSpeed)      list.add(new DiffEntry("move_speed",      fmt(orig.moveSpeed),      fmt(curr.moveSpeed)));
        if (orig.sizeScale      != curr.sizeScale)      list.add(new DiffEntry("size_scale",      fmt(orig.sizeScale),      fmt(curr.sizeScale)));
        if (orig.immuneFire     != curr.immuneFire)     list.add(new DiffEntry("immune_fire",     str(orig.immuneFire),     str(curr.immuneFire)));
        if (orig.immuneExplosion!= curr.immuneExplosion)list.add(new DiffEntry("immune_explosion",str(orig.immuneExplosion),str(curr.immuneExplosion)));
        return list;
    }

    private static String fmt(double v) { return String.format("%.4f", v); }
    private static String str(boolean v){ return String.valueOf(v); }

    record UndoState(String targetType, String targetId,
                     MobStats mob, BlockStats block, ItemStats item, PlayerStats player) {}
}
