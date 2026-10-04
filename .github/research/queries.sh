#!/usr/bin/env bash
# Print class member signatures (public/protected) from decompiled Minecraft sources.
cd "$1" || exit 1
outline() {
  for c in "$@"; do
    local f="net/minecraft/$c.java"
    echo "##### $c"
    [ -f "$f" ] || { echo "MISSING"; continue; }
    grep -nP '^(\t|\t\t)(public|protected|static|default|final|abstract|record|interface|enum|class)[^=]*?(\(|;|\{)\s*$|^(\t|\t\t)(public|protected)?\s*(static\s+)?(final\s+)?(class|record|interface|enum)\b|^\t(public|protected)[^(]*=' "$f" \
      | grep -v '^\s*[0-9]*:\s*\t*\(return\|if\|for\|while\|switch\|this\.\)' | cut -c1-200 | head -${LIMIT:-120}
  done
}
LIMIT=60 outline server/permissions/PermissionSet server/permissions/Permissions server/permissions/PermissionLevel server/permissions/LevelBasedPermissionSet server/permissions/Permission
ls net/minecraft/server/players/ | head -40
LIMIT=40 outline server/players/NameAndId server/players/PlayerList
LIMIT=200 outline world/entity/player/Player
LIMIT=60 outline world/entity/player/Abilities world/entity/player/Inventory
LIMIT=80 outline server/level/ServerPlayer
LIMIT=150 outline world/item/Item
LIMIT=80 outline world/item/ItemStack
LIMIT=120 outline core/component/DataComponents
LIMIT=40 outline core/component/DataComponentMap core/component/PatchedDataComponentMap
LIMIT=40 outline world/food/FoodProperties world/item/component/Consumable world/item/component/Consumables world/item/component/Tool world/item/component/ItemAttributeModifiers world/item/component/Unbreakable world/item/component/TooltipDisplay world/item/enchantment/Enchantable world/item/enchantment/Repairable
LIMIT=40 outline world/entity/ai/attributes/AttributeModifier world/entity/ai/attributes/AttributeInstance world/entity/ai/attributes/AttributeSupplier
LIMIT=40 outline world/item/SpawnEggItem world/item/TooltipFlag
LIMIT=60 outline client/Minecraft
LIMIT=30 outline world/entity/projectile/arrow/AbstractArrow world/entity/projectile/AbstractArrow
LIMIT=40 outline world/damagesource/DamageTypes tags/DamageTypeTags
LIMIT=40 outline commands/CommandSourceStack commands/arguments/EntityArgument
LIMIT=40 outline world/entity/EntityType
LIMIT=40 outline world/item/consume_effects/ApplyStatusEffectsConsumeEffect world/effect/MobEffectInstance
grep -n "hurt\b\|boolean hurt(\|void die\|dropEquipment\|dropExperience\|dropAllDeathLoot\|isInvulnerableTo\|causeFoodExhaustion\|onUpdateAbilities\|getAbilities" net/minecraft/world/entity/LivingEntity.java net/minecraft/world/entity/player/Player.java net/minecraft/server/level/ServerPlayer.java | cut -c1-200 | head -30
grep -n "restoreFrom" -A25 net/minecraft/server/level/ServerPlayer.java | cut -c1-160 | head -40
grep -n "public void tick()" -A12 net/minecraft/world/entity/player/Player.java | cut -c1-160
grep -n "appendHoverText" -B2 -A6 net/minecraft/world/item/Item.java | cut -c1-200
grep -rn "getHolder\|Optional<Holder.Reference<T>> get(" net/minecraft/core/Registry.java net/minecraft/core/HolderGetter.java net/minecraft/core/DefaultedRegistry.java 2>/dev/null | cut -c1-200
grep -n "BASE_ATTACK_DAMAGE_ID\|BASE_ATTACK_SPEED_ID" net/minecraft/world/item/Item.java | cut -c1-160
grep -n "class Properties" -A3 net/minecraft/world/item/Item.java | head; grep -n "public Item.Properties" net/minecraft/world/item/Item.java | cut -c1-160 | head -40
