package com.craftstats.common.stats;

import com.craftstats.common.util.Compat;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Rarity;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
//#if MC >= 1.21.2
//$ import net.minecraft.tags.DamageTypeTags;
//$ import net.minecraft.world.item.component.Consumable;
//$ import net.minecraft.world.item.component.Consumables;
//$ import net.minecraft.world.item.component.DamageResistant;
//$ import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
//$ import net.minecraft.world.item.enchantment.Enchantable;
//$ import net.minecraft.world.item.enchantment.Repairable;
//#else
import net.minecraft.world.item.component.Unbreakable;
//#endif

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns {@link ItemStats} into data component overrides and layers them over item stacks.
 * Precedence: the stack's own components, then CraftStats overrides, then the item defaults.
 */
public final class ItemOverrides {

    private static final Map<Item, Overrides> CACHE = new ConcurrentHashMap<>();
    private static volatile int cachedRevision = -1;

    private ItemOverrides() {}

    /** Called from the ItemStack#getComponents mixin. */
    public static DataComponentMap apply(Item item, DataComponentMap components, DataComponentPatch stackPatch) {
        ItemStats stats = StatRegistry.forItem(item);
        if (stats == null) return components;
        int rev = StatRegistry.revision();
        if (rev != cachedRevision) {
            CACHE.clear();
            cachedRevision = rev;
        }
        Overrides o = CACHE.computeIfAbsent(item, i -> build(i, stats));
        return o.isEmpty() ? components : new OverlayMap(components, stackPatch, o);
    }

    /** True if CraftStats changes this item at all (used to relax load-time validation). */
    public static boolean isOverridden(Item item) {
        return StatRegistry.forItem(item) != null;
    }

    /** Enchantability for versions where it is not a component yet. Null = vanilla. */
    public static Integer enchantability(Item item) {
        ItemStats s = StatRegistry.forItem(item);
        return s == null ? null : s.enchantability;
    }

    record Overrides(Map<DataComponentType<?>, Object> values, Set<DataComponentType<?>> removed) {
        boolean isEmpty() { return values.isEmpty() && removed.isEmpty(); }
    }

    private static Overrides build(Item item, ItemStats s) {
        DataComponentMap base = item.components();
        Map<DataComponentType<?>, Object> v = new HashMap<>();
        Set<DataComponentType<?>> removed = new HashSet<>();

        // Durability and stacking. Damageable items must not stack (vanilla rejects them).
        boolean damageable = base.has(DataComponents.MAX_DAMAGE);
        if (s.maxDurability != null && s.maxDurability > 0) {
            v.put(DataComponents.MAX_DAMAGE, s.maxDurability);
            if (!base.has(DataComponents.DAMAGE)) v.put(DataComponents.DAMAGE, 0);
            damageable = true;
        }
        if (damageable) {
            if (base.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) != 1) v.put(DataComponents.MAX_STACK_SIZE, 1);
        } else if (s.stackSize != null) {
            v.put(DataComponents.MAX_STACK_SIZE, Math.max(1, Math.min(99, s.stackSize)));
        }

        if (s.attackDamage != null || s.attackSpeed != null || s.hasBonuses())
            v.put(DataComponents.ATTRIBUTE_MODIFIERS, attributeModifiers(base, s));

        if (s.rarity != null && !ItemStats.RARITY_VANILLA.equals(s.rarity)) {
            try {
                v.put(DataComponents.RARITY, Rarity.valueOf(s.rarity.toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {}
        }
        if (s.displayName != null && !s.displayName.isBlank())
            v.put(DataComponents.ITEM_NAME, Component.literal(s.displayName.trim()));

        //#if MC >= 1.21.2
        //$ ResourceLocation repairId = s.repairMaterial == null ? null : ResourceLocation.tryParse(s.repairMaterial.trim());
        //$ if (repairId != null && !s.repairMaterial.isBlank() && BuiltInRegistries.ITEM.containsKey(repairId))
        //$     v.put(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(
        //$             BuiltInRegistries.ITEM.wrapAsHolder(Compat.registryValue(BuiltInRegistries.ITEM, repairId)))));
        //#endif

        if (s.miningSpeed != null)
            v.put(DataComponents.TOOL, tool(base.get(DataComponents.TOOL), Math.max(0f, s.miningSpeed)));

        if (s.itemGlow) v.put(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        //#if MC >= 1.21.5
        //$ if (s.unbreakable) v.put(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        //#else
        if (s.unbreakable) v.put(DataComponents.UNBREAKABLE, new Unbreakable(true));
        //#endif

        //#if MC >= 1.21.2
        //$ if (s.fireproof) v.put(DataComponents.DAMAGE_RESISTANT, new DamageResistant(DamageTypeTags.IS_FIRE));
        //$ if (s.enchantability != null) {
        //$     if (s.enchantability > 0) v.put(DataComponents.ENCHANTABLE, new Enchantable(s.enchantability));
        //$     else removed.add(DataComponents.ENCHANTABLE);
        //$ }
        //#else
        if (s.fireproof) v.put(DataComponents.FIRE_RESISTANT, Unit.INSTANCE);
        //#endif

        if (s.changesFood()) food(base, s, v, removed);

        return new Overrides(Map.copyOf(v), Set.copyOf(removed));
    }

    /** Bonus stat -> attribute, for the "Bonuses" category. */
    private static final List<Map.Entry<java.util.function.Function<ItemStats, Double>, Holder<Attribute>>> BONUSES = List.of(
            Map.entry(st -> st.bonusMaxHealth, Attributes.MAX_HEALTH),
            Map.entry(st -> st.bonusArmor, Attributes.ARMOR),
            Map.entry(st -> st.bonusArmorToughness, Attributes.ARMOR_TOUGHNESS),
            Map.entry(st -> st.bonusKnockbackResist, Attributes.KNOCKBACK_RESISTANCE),
            Map.entry(st -> st.bonusMoveSpeed, Attributes.MOVEMENT_SPEED),
            Map.entry(st -> st.bonusJump, Attributes.JUMP_STRENGTH),
            Map.entry(st -> st.bonusStepHeight, Attributes.STEP_HEIGHT),
            Map.entry(st -> st.bonusGravity, Attributes.GRAVITY),
            Map.entry(st -> st.bonusScale, Attributes.SCALE),
            Map.entry(st -> st.bonusBlockReach, Attributes.BLOCK_INTERACTION_RANGE),
            Map.entry(st -> st.bonusEntityReach, Attributes.ENTITY_INTERACTION_RANGE),
            Map.entry(st -> st.bonusLuck, Attributes.LUCK),
            Map.entry(st -> st.bonusSafeFall, Attributes.SAFE_FALL_DISTANCE),
            Map.entry(st -> st.bonusMiningEfficiency, Attributes.MINING_EFFICIENCY),
            Map.entry(st -> st.bonusAttackKnockback, Attributes.ATTACK_KNOCKBACK),
            Map.entry(st -> st.bonusSweeping, Attributes.SWEEPING_DAMAGE_RATIO),
            Map.entry(st -> st.bonusOxygen, Attributes.OXYGEN_BONUS));

    private static EquipmentSlotGroup bonusSlot(ItemStats s) {
        return switch (s.bonusSlot == null ? "" : s.bonusSlot) {
            case "offhand" -> EquipmentSlotGroup.OFFHAND;
            case "hand" -> EquipmentSlotGroup.HAND;
            case "armor" -> EquipmentSlotGroup.ARMOR;
            case "any" -> EquipmentSlotGroup.ANY;
            default -> EquipmentSlotGroup.MAINHAND;
        };
    }

    private static ItemAttributeModifiers attributeModifiers(DataComponentMap base, ItemStats s) {
        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry e : attackModifiers(base, s).modifiers()) b.add(e.attribute(), e.modifier(), e.slot());
        EquipmentSlotGroup slot = bonusSlot(s);
        int i = 0;
        for (var bonus : BONUSES) {
            Double amount = bonus.getKey().apply(s);
            i++;
            if (amount == null || amount == 0) continue;
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("craftstats", "bonus_" + i);
            b.add(bonus.getValue(), new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE), slot);
        }
        return b.build();
    }

    private static ItemAttributeModifiers attackModifiers(DataComponentMap base, ItemStats s) {
        ItemAttributeModifiers current = base.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry e : current.modifiers()) {
            if (s.attackDamage != null && e.matches(Attributes.ATTACK_DAMAGE, Item.BASE_ATTACK_DAMAGE_ID)) continue;
            if (s.attackSpeed != null && e.matches(Attributes.ATTACK_SPEED, Item.BASE_ATTACK_SPEED_ID)) continue;
            b.add(e.attribute(), e.modifier(), e.slot());
        }
        // Tooltips show player base (1 damage, 4 speed) plus the modifier, so subtract it.
        if (s.attackDamage != null)
            b.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,
                    s.attackDamage - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        if (s.attackSpeed != null)
            b.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,
                    s.attackSpeed - 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        return b.build();
    }

    private static Tool tool(Tool current, float speed) {
        List<Tool.Rule> rules = new ArrayList<>();
        int damagePerBlock = 1;
        if (current != null) {
            for (Tool.Rule r : current.rules())
                rules.add(new Tool.Rule(r.blocks(), r.speed().map(x -> speed), r.correctForDrops()));
            damagePerBlock = current.damagePerBlock();
        }
        float defaultSpeed = current == null ? speed : current.defaultMiningSpeed();
        //#if MC >= 1.21.5
        //$ return new Tool(rules, defaultSpeed, damagePerBlock, current == null || current.canDestroyBlocksInCreative());
        //#else
        return new Tool(rules, defaultSpeed, damagePerBlock);
        //#endif
    }

    private static Optional<MobEffectInstance> eatEffect(ItemStats s) {
        if (s.onEatEffect == null || s.onEatEffect.isBlank()) return Optional.empty();
        ResourceLocation id = ResourceLocation.tryParse(s.onEatEffect.trim());
        if (id == null) return Optional.empty();
        int seconds = s.onEatEffectSeconds != null ? Math.max(1, s.onEatEffectSeconds) : 30;
        int amp = s.onEatEffectLevel != null ? Math.max(0, Math.min(255, s.onEatEffectLevel - 1)) : 0;
        return Compat.effect(id).map(h -> new MobEffectInstance(h, seconds * 20, amp));
    }

    private static void food(DataComponentMap base, ItemStats s, Map<DataComponentType<?>, Object> v,
                             Set<DataComponentType<?>> removed) {
        FoodProperties vanilla = base.get(DataComponents.FOOD);
        if (ItemStats.EDIBLE_NO.equals(s.edible)) {
            removed.add(DataComponents.FOOD);
            //#if MC >= 1.21.2
            //$ if (vanilla != null) removed.add(DataComponents.CONSUMABLE);
            //#endif
            return;
        }
        if (vanilla == null && !ItemStats.EDIBLE_YES.equals(s.edible)) return; // nothing to modify
        int nutrition = s.nutrition != null ? Math.max(0, s.nutrition) : vanilla != null ? vanilla.nutrition() : 1;
        float saturation = s.saturation != null ? Math.max(0f, s.saturation) : vanilla != null ? vanilla.saturation() : 0.6f;
        boolean alwaysEat = s.alwaysEdible || (vanilla != null && vanilla.canAlwaysEat());
        Optional<MobEffectInstance> effect = eatEffect(s);
        //#if MC >= 1.21.2
        //$ v.put(DataComponents.FOOD, new FoodProperties(nutrition, saturation, alwaysEat));
        //$ Consumable current = base.get(DataComponents.CONSUMABLE);
        //$ if (current == null || s.eatSeconds != null || effect.isPresent()) {
        //$     Consumable.Builder c = Consumables.defaultFood();
        //$     c.consumeSeconds(s.eatSeconds != null ? Math.max(0.05f, s.eatSeconds)
        //$             : current != null ? current.consumeSeconds() : Consumable.DEFAULT_CONSUME_SECONDS);
        //$     if (current != null) current.onConsumeEffects().forEach(c::onConsume);
        //$     effect.ifPresent(e -> c.onConsume(new ApplyStatusEffectsConsumeEffect(e)));
        //$     v.put(DataComponents.CONSUMABLE, c.build());
        //$ }
        //#else
        float seconds = s.eatSeconds != null ? Math.max(0.05f, s.eatSeconds) : vanilla != null ? vanilla.eatSeconds() : 1.6f;
        List<FoodProperties.PossibleEffect> effects = new ArrayList<>(vanilla != null ? vanilla.effects() : List.of());
        effect.ifPresent(e -> effects.add(new FoodProperties.PossibleEffect(e, 1.0f)));
        v.put(DataComponents.FOOD, new FoodProperties(nutrition, saturation, alwaysEat, seconds,
                vanilla != null ? vanilla.usingConvertsTo() : Optional.empty(), effects));
        //#endif
    }

    /** A read-only view: stack patch first, then CraftStats overrides, then the original map. */
    record OverlayMap(DataComponentMap delegate, DataComponentPatch patch, Overrides overrides)
            implements DataComponentMap {

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(DataComponentType<? extends T> type) {
            if (patch.get(type) != null) return delegate.get(type);
            if (overrides.removed().contains(type)) return null;
            Object o = overrides.values().get(type);
            return o != null ? (T) o : delegate.get(type);
        }

        @Override
        public Set<DataComponentType<?>> keySet() {
            Set<DataComponentType<?>> keys = new HashSet<>(delegate.keySet());
            keys.addAll(overrides.values().keySet());
            for (DataComponentType<?> t : overrides.removed())
                if (patch.get(t) == null) keys.remove(t);
            return keys;
        }
    }
}
