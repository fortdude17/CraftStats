package com.craftstats.common;

import com.craftstats.common.network.CraftStatsNetwork;
import com.craftstats.common.stats.*;
import com.craftstats.common.util.StatPersistence;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
//#if MC >= 1.21.11
//$ import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
//#else
import net.minecraft.world.entity.projectile.AbstractArrow;
//#endif
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * Automated check used by CI: start a dedicated server with -Dcraftstats.smokeTest=true and
 * it applies every mixin, exercises the main code paths, logs CRAFTSTATS-SMOKE-PASS or
 * CRAFTSTATS-SMOKE-FAIL, and shuts down. Does nothing in normal play.
 */
public final class SmokeTest {

    private SmokeTest() {}

    public static boolean enabled() {
        return Boolean.getBoolean("craftstats.smokeTest");
    }

    public static void run(MinecraftServer server) {
        List<String> failures = new ArrayList<>();
        step(failures, "mixin audit", () -> MixinEnvironment.getCurrentEnvironment().audit());

        step(failures, "editor field definitions", SmokeTest::checkFieldDefinitions);

        step(failures, "item overrides", () -> {
            ItemStats bread = new ItemStats();
            bread.stackSize = 16;
            bread.nutrition = 10;
            StatRegistry.setItem(id("bread"), bread);
            ItemStats sword = new ItemStats();
            sword.maxDurability = 5000;
            sword.attackDamage = 20.0;
            StatRegistry.setItem(id("diamond_sword"), sword);
            check(new ItemStack(Items.BREAD).getMaxStackSize() == 16, "bread stack size not overridden");
            check(new ItemStack(Items.DIAMOND_SWORD).getMaxDamage() == 5000, "sword durability not overridden");
            check(new ItemStack(Items.DIAMOND_SWORD).getMaxStackSize() == 1, "sword must not stack");
            check(VanillaStats.item(Items.DIAMOND_SWORD).attackDamage != null, "vanilla item stats");
        });

        step(failures, "block overrides", () -> {
            BlockStats stone = new BlockStats();
            stone.hardness = 42f;
            stone.lightEmission = 7;
            StatRegistry.setBlock(id("stone"), stone);
            ServerLevel level = server.overworld();
            check(Blocks.STONE.defaultBlockState().getDestroySpeed(level, BlockPos.ZERO) == 42f, "hardness not overridden");
            check(Blocks.STONE.defaultBlockState().getLightEmission() == 7, "light not overridden");
            check(VanillaStats.block(Blocks.STONE).hardness == 1.5f, "vanilla hardness should be 1.5");
        });

        step(failures, "mob overrides", () -> {
            MobStats zombieStats = new MobStats();
            zombieStats.maxHealth = 80.0;
            StatRegistry.setMob(id("zombie"), zombieStats);
            ServerLevel level = server.overworld();
            LivingEntity zombie = EntityType.ZOMBIE.create(level
                    //#if MC >= 1.21.2
                    //$ , net.minecraft.world.entity.EntitySpawnReason.COMMAND
                    //#endif
            );
            check(zombie != null, "could not create zombie");
            StatApplier.applyMob(zombie);
            check(Math.abs(zombie.getMaxHealth() - 80f) < 0.01f, "zombie max health is " + zombie.getMaxHealth());
            check(Math.abs(zombie.getHealth() - 80f) < 0.01f, "fresh zombie should be at full health");
            StatRegistry.removeMob(id("zombie"));
            StatApplier.applyMob(zombie);
            check(Math.abs(zombie.getMaxHealth() - 20f) < 0.01f, "reset did not restore vanilla health");
            check(VanillaStats.mob(EntityType.ZOMBIE).maxHealth == 20.0, "vanilla zombie health");
        });

        step(failures, "snapshot round trip", () -> {
            var snapshot = StatPersistence.snapshot();
            int items = StatRegistry.allItems().size();
            StatPersistence.restore(snapshot);
            check(StatRegistry.allItems().size() == items, "items lost in snapshot round trip");
            check(StatRegistry.getBlock(id("stone")) != null, "block lost in snapshot round trip");
        });

        step(failures, "legacy migration", () -> {
            MobStats m = StatSchema.mob("{\"max_health\":-1.0,\"attack_damage\":12.0,\"xp_reward\":0}");
            check(m.maxHealth == null && m.attackDamage == 12.0 && m.xpReward == null, "mob migration");
            ItemStats i = StatSchema.item("{\"stack_size\":64,\"attack_damage\":1.0,\"max_durability\":0}");
            check(i.stackSize == null && i.attackDamage == null && i.maxDurability == null, "item migration");
        });

        step(failures, "commands", () -> {
            var source = server.createCommandSourceStack();
            server.getCommands().performPrefixedCommand(source, "craftstats randomize mob minecraft:skeleton 1234");
            check(StatRegistry.getMob(id("skeleton")) != null, "randomize command did nothing");
            check(StatRegistry.getMob(id("skeleton")).maxHealth > 0, "randomized health must be positive");
            server.getCommands().performPrefixedCommand(source, "craftstats preset load tank mob minecraft:skeleton");
            check(StatRegistry.getMob(id("skeleton")).maxHealth == 200.0, "preset load command");
            server.getCommands().performPrefixedCommand(source, "craftstats reset all");
            check(StatRegistry.allMobs().isEmpty() && StatRegistry.allItems().isEmpty(), "reset all command");
        });

        step(failures, "sync encoding", () -> CraftStatsNetwork.broadcastSync(server));

        step(failures, "new mob stats", () -> checkMobStats(server.overworld()));
        step(failures, "combat stats", () -> checkCombat(server.overworld()));
        step(failures, "death effects", () -> checkDeath(server.overworld()));
        step(failures, "new item stats", SmokeTest::checkItems);
        step(failures, "new block stats", () -> checkBlocks(server.overworld()));
        step(failures, "projectile stats", () -> checkProjectiles(server.overworld()));
        step(failures, "enchantment stats", () -> checkEnchantments(server));
        step(failures, "world stats", () -> checkWorld(server.overworld()));
        StatRegistry.clear();

        if (failures.isEmpty()) {
            CraftStats.LOGGER.info("CRAFTSTATS-SMOKE-PASS");
        } else {
            CraftStats.LOGGER.error("CRAFTSTATS-SMOKE-FAIL: {}", String.join("; ", failures));
        }
        server.halt(false);
        // The dedicated server JVM doesn't always exit by itself after halting (lingering
        // non-daemon threads), which would leave CI waiting for a timeout.
        Thread exit = new Thread(() -> {
            try { Thread.sleep(30_000); } catch (InterruptedException ignored) {}
            Runtime.getRuntime().halt(0);
        }, "craftstats-smoke-exit");
        exit.setDaemon(true);
        exit.start();
    }

    /**
     * Every catalog entry must exist on its stats class with a type the editor can edit, every
     * public stats field must be in the catalog (so nothing is hidden from the editor), and
     * the four main types must have at least 50 stats each.
     */
    static void checkFieldDefinitions() throws NoSuchFieldException {
        for (TargetType type : TargetType.values()) {
            Class<?> cls = StatSchema.classFor(type);
            java.util.Set<String> listed = new java.util.HashSet<>();
            for (StatDef def : StatCatalog.all(type)) {
                check(listed.add(def.field()), type + "." + def.field() + " is listed twice");
                Class<?> t = cls.getField(def.field()).getType();
                boolean ok = switch (def.kind()) {
                    case NUMBER -> t == int.class || t == double.class || t == float.class || t == long.class
                            || Number.class.isAssignableFrom(t);
                    case TOGGLE -> t == boolean.class;
                    case CHOICE, TEXT -> t == String.class;
                };
                check(ok, type + "." + def.field() + " has type " + t.getSimpleName() + " but is a " + def.kind());
                if (def.kind() == StatDef.Kind.CHOICE)
                    check(java.util.Arrays.asList(def.options()).contains(String.valueOf(StatAccess.get(StatSchema.empty(type), def.field()))),
                            type + "." + def.field() + " default is not one of its options");
            }
            for (java.lang.reflect.Field f : cls.getFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || f.getName().equals("schema") || f.getName().equals("block")) continue;
                check(listed.contains(f.getName()), type + "." + f.getName() + " is not shown in the editor");
            }
            if (type == TargetType.MOB || type == TargetType.BLOCK || type == TargetType.ITEM || type == TargetType.PLAYER)
                check(listed.size() >= 50, type + " has only " + listed.size() + " stats");
            CraftStats.LOGGER.info("smoke test: {} has {} stats in {} categories", type.plural(), listed.size(),
                    StatCatalog.categories(type).size());
        }
    }

    // ---- checks for the 1.2.0 stats ------------------------------------------------------

    private static <T extends net.minecraft.world.entity.Entity> T spawn(ServerLevel level, EntityType<T> type) {
        @SuppressWarnings("unchecked")
        T e = (T) com.craftstats.common.util.Compat.createEntity(type, level);
        check(e != null, "could not create " + type);
        e.setPos(0.5, 120, 0.5);
        level.addFreshEntity(e);
        return e;
    }

    private static void checkMobStats(ServerLevel level) {
        MobStats z = new MobStats();
        z.attackKnockback = 2.0; z.armorToughness = 5.0; z.absorption = 10.0; z.noGravity = true; z.invisible = true;
        z.noPush = true; z.noAi = true; z.stepHeight = 3.0; z.pickUpLoot = MobStats.LOOT_YES;
        StatRegistry.setMob(id("zombie"), z);
        MobStats cow = new MobStats();
        cow.hostile = true; cow.attackDamage = 6.0;
        StatRegistry.setMob(id("cow"), cow);
        try {
            var zombie = spawn(level, EntityType.ZOMBIE);
            StatApplier.applyMob(zombie);
            check(Math.abs(zombie.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS) - 5) < 0.01, "armor toughness");
            check(Math.abs(zombie.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT) - 3) < 0.01, "step height");
            check(zombie.getAbsorptionAmount() >= 9.9f, "absorption is " + zombie.getAbsorptionAmount());
            check(zombie.isNoGravity() && zombie.isInvisible() && !zombie.isPushable() && zombie.isNoAi(), "no gravity / invisible / no push / no AI");
            check(zombie.canPickUpLoot(), "pick up loot");
            zombie.discard();
            var c = spawn(level, EntityType.COW);
            StatApplier.applyMob(c);
            check(com.craftstats.common.logic.MobBehaviour.hasHostileGoals(c), "hostile cow has no attack goals");
            c.discard();
        } finally {
            StatRegistry.removeMob(id("zombie"));
            StatRegistry.removeMob(id("cow"));
        }
    }

    private static void checkCombat(ServerLevel level) {
        MobStats target = new MobStats();
        target.damageTakenMultiplier = 0.5; target.immuneLightning = true;
        StatRegistry.setMob(id("husk"), target);
        MobStats attacker = new MobStats();
        attacker.lifestealPercent = 100.0; attacker.hitEffect = "minecraft:slowness"; attacker.fireOnHit = 5;
        StatRegistry.setMob(id("skeleton"), attacker);
        try {
            var husk = spawn(level, EntityType.HUSK);
            var skel = spawn(level, EntityType.SKELETON);
            husk.setNoAi(true); skel.setNoAi(true);
            float before = husk.getHealth();
            com.craftstats.common.util.Compat.hurt(husk, husk.damageSources().magic(), 8);
            check(Math.abs((before - husk.getHealth()) - 4f) < 0.01f, "damage taken x0.5: lost " + (before - husk.getHealth()));
            husk.invulnerableTime = 0;
            float h2 = husk.getHealth();
            com.craftstats.common.util.Compat.hurt(husk, husk.damageSources().lightningBolt(), 5);
            check(husk.getHealth() == h2, "immune to lightning");
            // Lifesteal: the attacker heals by the damage dealt.
            husk.invulnerableTime = 0;
            skel.setHealth(5f);
            com.craftstats.common.util.Compat.hurt(husk, husk.damageSources().mobAttack(skel), 4);
            check(husk.hasEffect(com.craftstats.common.util.Compat.SLOWNESS), "hit effect not applied");
            check(husk.isOnFire(), "fire on hit not applied");
            check(skel.getHealth() > 5f, "lifesteal: attacker health " + skel.getHealth());
            // Thorns: the attacker takes damage back.
            target.thornsPercent = 100.0;
            attacker.lifestealPercent = null;
            StatRegistry.changed();
            husk.invulnerableTime = 0;
            skel.invulnerableTime = 0;
            float skelBefore = skel.getHealth();
            com.craftstats.common.util.Compat.hurt(husk, husk.damageSources().mobAttack(skel), 4);
            check(skel.getHealth() < skelBefore, "thorns: attacker health " + skel.getHealth() + " was " + skelBefore);
            husk.discard(); skel.discard();
        } finally {
            StatRegistry.removeMob(id("husk"));
            StatRegistry.removeMob(id("skeleton"));
        }
    }

    private static void checkDeath(ServerLevel level) {
        MobStats s = new MobStats();
        s.spawnOnDeath = "minecraft:silverfish"; s.spawnOnDeathCount = 3;
        StatRegistry.setMob(id("pig"), s);
        try {
            var pig = spawn(level, EntityType.PIG);
            // Count what was spawned rather than searching the world: since 1.21.9 the spawn
            // chunks aren't kept loaded, so a test entity there isn't visible to searches.
            int before = com.craftstats.common.logic.Effects.spawnCount;
            pig.die(pig.damageSources().magic());
            int made = com.craftstats.common.logic.Effects.spawnCount - before;
            check(made == 3, "spawn on death made " + made + " silverfish");
            var area = pig.getBoundingBox().inflate(4);
            level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, area, e -> e.getType() == EntityType.SILVERFISH)
                    .forEach(net.minecraft.world.entity.Entity::discard);
            pig.discard();
        } finally {
            StatRegistry.removeMob(id("pig"));
        }
    }

    private static void checkItems() {
        ItemStats s = new ItemStats();
        s.bonusMoveSpeed = 0.05; s.bonusSlot = "any"; s.rarity = "epic"; s.displayName = "Magic Stick";
        s.repairMaterial = "minecraft:diamond"; s.maxDurability = 100;
        StatRegistry.setItem(id("stick"), s);
        try {
            ItemStack stick = new ItemStack(Items.STICK);
            var mods = stick.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
            check(mods != null && mods.modifiers().stream().anyMatch(m -> m.attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)),
                    "move speed bonus missing");
            check(stick.get(net.minecraft.core.component.DataComponents.RARITY) == net.minecraft.world.item.Rarity.EPIC, "rarity");
            check(stick.getHoverName().getString().equals("Magic Stick"), "display name is " + stick.getHoverName().getString());
            //#if MC >= 1.21.2
            //$ check(stick.isValidRepairItem(new ItemStack(Items.DIAMOND)), "repair material");
            //#endif
        } finally {
            StatRegistry.removeItem(id("stick"));
        }
    }

    private static void checkBlocks(ServerLevel level) {
        BlockStats s = new BlockStats();
        s.jumpFactor = 2f; s.speedFactor = 0.3f; s.redstonePower = 9; s.soundType = "glass"; s.replaceable = true;
        s.invisible = true; s.mobSpawning = "never"; s.noDrops = true;
        StatRegistry.setBlock(id("dirt"), s);
        try {
            var state = Blocks.DIRT.defaultBlockState();
            check(Blocks.DIRT.getJumpFactor() == 2f && Blocks.DIRT.getSpeedFactor() == 0.3f, "jump/speed factor");
            check(state.isSignalSource() && state.getSignal(level, BlockPos.ZERO, net.minecraft.core.Direction.UP) == 9, "redstone power");
            check(state.getSoundType() == net.minecraft.world.level.block.SoundType.GLASS, "sound type");
            check(state.canBeReplaced(), "replaceable");
            check(state.getRenderShape() == net.minecraft.world.level.block.RenderShape.INVISIBLE, "invisible");
            check(!state.isValidSpawn(level, BlockPos.ZERO, EntityType.ZOMBIE), "mob spawning");
            check(com.craftstats.common.logic.BlockHooks.replaceDrops(state, level, BlockPos.ZERO, ItemStack.EMPTY), "no drops");
        } finally {
            StatRegistry.removeBlock(id("dirt"));
        }
    }

    private static void checkProjectiles(ServerLevel level) {
        ProjectileStats s = new ProjectileStats();
        s.speedMultiplier = 2.0; s.alwaysCrit = true; s.gravityMultiplier = 0.5; s.piercing = 3;
        StatRegistry.setProjectile(id("arrow"), s);
        try {
            var arrow = com.craftstats.common.util.Compat.createEntity(EntityType.ARROW, level);
            check(arrow != null, "could not create arrow");
            arrow.setPos(0.5, 150, 0.5);
            arrow.setDeltaMovement(1, 0, 0);
            double vanillaGravity = 0.05;
            level.addFreshEntity(arrow); // CraftStats applies launch stats when it enters the world
            check(Math.abs(arrow.getDeltaMovement().x - 2) < 0.01, "speed x2: " + arrow.getDeltaMovement().x);
            check(arrow instanceof AbstractArrow a && a.isCritArrow() && a.getPierceLevel() == 3, "crit / piercing");
            check(Math.abs(arrow.getGravity() - vanillaGravity * 0.5) < 1.0E-4, "gravity x0.5: " + arrow.getGravity());
            arrow.discard();
        } finally {
            StatRegistry.removeProjectile(id("arrow"));
        }
    }

    private static void checkEnchantments(MinecraftServer server) {
        var registry = com.craftstats.common.util.Compat.enchantments(server.registryAccess());
        StatRegistry.trackEnchantments(registry);
        EnchantmentStats s = new EnchantmentStats();
        s.maxLevel = 10; s.levelBonus = 2;
        StatRegistry.setEnchantment(id("sharpness"), s);
        try {
            var sharpness = com.craftstats.common.util.Compat.registryValue(registry, id("sharpness"));
            check(sharpness != null && sharpness.getMaxLevel() == 10, "max level");
            var holder = registry.wrapAsHolder(sharpness);
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            sword.enchant(holder, 1);
            check(sword.getEnchantments().getLevel(holder) == 3, "level bonus: level is " + sword.getEnchantments().getLevel(holder));
        } finally {
            StatRegistry.removeEnchantment(id("sharpness"));
        }
    }

    private static void checkWorld(ServerLevel level) {
        int vanillaCap = net.minecraft.world.entity.MobCategory.MONSTER.getMaxInstancesPerChunk();
        WorldStats w = new WorldStats();
        w.gravityMultiplier = 0.5; w.spawnCapMultiplier = 2.0; w.mobHealthMultiplier = 2.0;
        StatRegistry.setWorld(w);
        try {
            check(net.minecraft.world.entity.MobCategory.MONSTER.getMaxInstancesPerChunk() == vanillaCap * 2, "spawn cap");
            var zombie = spawn(level, EntityType.ZOMBIE);
            StatApplier.applyMob(zombie);
            check(Math.abs(zombie.getMaxHealth() - 40f) < 0.01f, "mob health x2: " + zombie.getMaxHealth());
            check(Math.abs(zombie.getGravity() - 0.04) < 1.0E-4, "gravity x0.5: " + zombie.getGravity());
            zombie.discard();
        } finally {
            StatRegistry.removeWorld();
        }
    }

    public interface Check { void run() throws Exception; }

    public static void step(List<String> failures, String name, Check check) {
        try {
            check.run();
            CraftStats.LOGGER.info("smoke test: {} ok", name);
        } catch (Throwable t) {
            CraftStats.LOGGER.error("smoke test: {} FAILED", name, t);
            failures.add(name + ": " + t);
        }
    }

    public static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
