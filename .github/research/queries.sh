#!/usr/bin/env bash
# Print method/field signatures from the decompiled Minecraft sources.
cd "$1" || exit 1
sig() { # file, regex
  local f="net/minecraft/$1.java"
  echo "##### $1"
  [ -f "$f" ] || { echo "MISSING"; return; }
  grep -nE "$2" "$f" | grep -vE '^\s*[0-9]+:\s*(//|\*)' | cut -c1-220 | head -${3:-40}
}
M='^\s*(public|protected|private)?\s*(static\s+)?(final\s+)?(abstract\s+)?(synchronized\s+)?[A-Za-z<>?,\[\]. ]+\s+'
ls net/minecraft/client/input/ 2>/dev/null
sig client/gui/components/events/GuiEventListener "default .*\(|boolean .*\("
sig client/input/MouseButtonEvent "record|public .*\("
sig client/input/MouseButtonInfo "record|public .*\("
sig client/input/KeyEvent "record|public .*\("
sig client/input/CharacterEvent "record|public .*\("
sig client/gui/GuiGraphics "public .* (fill|drawString|drawCenteredString|enableScissor|disableScissor|renderItem|renderItemDecorations|renderTooltip|setTooltipForNextFrame)\(" 40
sig client/gui/screens/Screen "(renderBackground|render|init|onClose|tick|isPauseScreen|mouseClicked|keyPressed|charTyped|mouseScrolled|hasShiftDown|addRenderableWidget|children)\(" 30
sig client/gui/components/EditBox "public .*(EditBox\(|keyPressed|charTyped|mouseClicked|setResponder|setFocused|setBordered|setTextColor|setCursorPosition|setHighlightPos|setMaxLength|setValue|getValue|setHeight)\("
sig client/gui/components/AbstractWidget "public .*(mouseClicked|setX|setY|setHeight|render|setMessage|isMouseOver|onClick)\("
sig client/gui/components/Button "public .*(builder|bounds|build|onPress|mouseClicked)\("
sig client/gui/screens/PauseScreen "void init|createPauseMenu"
sig client/gui/screens/TitleScreen "void init|createNormalMenuOptions"
sig client/KeyboardHandler "public .*(Clipboard)\("
sig client/multiplayer/ClientPacketListener "public .*getOnlinePlayers\("
sig client/multiplayer/ClientLevel "public .*entitiesForRendering\("
echo "##### GameProfile"; find / -name 'authlib*.jar' 2>/dev/null | head -2
sig resources/ResourceKey "public .*\("
sig resources/Identifier "public static .*\(" 20
sig world/entity/Entity "(public|protected) .* (isOnFire|igniteForSeconds|getServer|level|isClientSide|setSilent|setGlowingTag|setTicksFrozen|getTicksRequiredToFreeze|isInWaterOrBubble|isInWater|hurt|hurtServer|hurtOrSimulate|damageSources|blockPosition|onGround|getUUID|tick)\(|noPhysics|boolean isClientSide" 40
sig world/level/Level "public .* (isClientSide|isDay|isBrightOutside|isRaining|canSeeSky|dimension|scheduleTick|getServer)\(|isClientSide" 20
sig world/entity/LivingEntity "(public|protected) .* (getMaxHealth|causeFallDamage|hurt|hurtServer|actuallyHurt|tick|getExperienceReward|dropAllDeathLoot|onClimbable|getAttribute|addEffect|hasEffect|getEffect|setHealth|getHealth|aiStep|travel|jumpFromGround|getJumpPower)\(" 40
sig world/entity/Mob "(public|protected) .* (removeWhenFarAway|checkDespawn|isPersistenceRequired)\(|invulnerableTime" 10
sig world/entity/player/Player "(public|protected) .* (tick|attack|giveExperiencePoints|getInventory|hasPermissions|permissions|isCreative|getGameProfile|isSpectator)\(|noPhysics =|1\.5F" 30
sig world/entity/player/Inventory "public .*(items|offhand|getNonEquipmentItems|add|getItem|getContainerSize)\b" 15
sig server/level/ServerPlayer "public .* (hasPermissions|permissions|getPermissionLevel|serverLevel|level|getServer|createCommandSourceStack)\(" 15
sig server/MinecraftServer "public .* (isDedicatedServer|isSingleplayerOwner|getProfilePermissions|getPlayerList|getAllLevels|getWorldPath|execute|isSingleplayer)\(" 15
sig commands/CommandSourceStack "public .* (hasPermission|permissions|sendSuccess|sendFailure|getPlayer)\(" 10
sig commands/Commands "public static .* (literal|argument|hasPermission)\(|LEVEL_GAMEMASTERS" 10
ls net/minecraft/server/permissions 2>/dev/null
sig world/food/FoodData "(public|private) .* (tick|addExhaustion|eat)\(|private (int|float) " 15
sig world/item/Item "(public|protected) .* (use|useOn|interactLivingEntity|hurtEnemy|postHurtEnemy|appendHoverText|getDefaultInstance|components)\(" 15
sig world/item/Item "public Properties (stacksTo|setId|component|durability|fireResistant|food|attributes)\(" 15
sig world/item/ItemStack "public .* (getMaxStackSize|getMaxDamage|isDamageableItem|getComponents|get|has)\(" 15
sig core/component/DataComponentHolder "default .*\(|public .*\(" 10
sig world/entity/projectile/AbstractArrow "(public|protected) .* (getBaseDamage|getWeaponItem|setBaseDamage)\(" 10
ls net/minecraft/world/entity/projectile/ | head -50
sig world/level/block/state/BlockBehaviour "(public|protected) .* (onPlace|neighborChanged|tick|getCollisionShape|getDestroySpeed|getLightEmission|getPistonPushReaction|requiresCorrectToolForDrops|spawnAfterBreak|getFriction|getExplosionResistance|affectNeighborsAfterRemoval|onRemove)\(|(float destroyTime|boolean hasCollision|final Properties properties)|BlockBehaviour.Properties properties" 50
sig world/level/block/Block "public .* (getFriction|getExplosionResistance|defaultBlockState)\(" 10
sig world/effect/MobEffects "public static final Holder<MobEffect> " 40
sig world/entity/ai/attributes/Attributes "public static final Holder<Attribute> " 40
sig world/entity/ai/attributes/DefaultAttributes "public static .*\(" 10
sig world/entity/ExperienceOrb "public static .* award\(" 5
sig world/entity/item/FallingBlockEntity "public static .* fall\(" 5
sig world/InteractionResult "SUCCESS|PASS|FAIL|CONSUME|sidedSuccess|record|interface" 20
ls net/minecraft/world/ | grep -i interaction
sig core/Registry "(Optional<Holder.Reference<T>> get|getHolder|containsKey|getValue|getOptional)\(" 10
sig core/DefaultedRegistry "public .*\(" 10
sig world/damagesource/DamageSource "public .* (is|type|getMsgId)\(" 10
sig world/level/storage/LevelResource "ROOT" 3
sig world/level/BlockGetter "interface" 3
