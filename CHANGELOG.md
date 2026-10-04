# Changelog

## 1.2.0

A new editor and about 190 new stats: 294 in total, across mobs, blocks, items, players, and three new types (projectiles, enchantments and the world itself).

**Downloads:** pick the jar for your loader and Minecraft version: `craftstats-fabric-1.2.0+1.21.1.jar`, `craftstats-fabric-1.2.0+1.21.11.jar`, `craftstats-neoforge-1.2.0+1.21.1.jar` or `craftstats-neoforge-1.2.0+1.21.11.jar`. Requires Architectury API (13.x for 1.21.1, 19.x for 1.21.11), and Fabric API on Fabric.

> **Beta.** Every build starts a real server and the game client on both loaders and both Minecraft versions. The tests apply every hook into the game, check a sample of the new stats on real mobs, blocks, items, arrows and enchantments, open every editor page, and in a singleplayer world apply stats through the network and throw a boomerang. Not every one of the 294 stats is covered by a test, and it hasn't had much hands-on play yet, so please report anything odd.

### New editor
- Looks like a vanilla menu: the usual background, buttons and text boxes.
- Tabs for all seven types, a searchable list with item icons, and a "changed only" filter.
- Stats are grouped into **category cards** (Combat, Movement, Drops & Death...). Click a card to see its stats, or type in "Search stats" to find any stat.
- Every stat has a tooltip that explains it and shows the allowed range; an **x** button puts it back to default.
- The randomizer shows a preview of the rolled values before you use them.

### New stats
- **Mobs (68):** attack knockback, armor toughness, absorption, regeneration, damage dealt/taken multipliers, thorns, lifesteal, set on fire / potion effect / lightning on hit; immunities to projectiles, freezing, lightning, melee, cactus, potion effects and suffocation; flying speed, step height, gravity, fall damage, water movement, oxygen, no gravity; invisible, can't be pushed; no AI, hostile (even passive mobs attack), peaceful, picks up items, teleports when hurt, permanent effect; no drops, extra loot rolls, explode / lightning / spawn mobs / extra item on death.
- **Players (77):** size, can fly, block break speed, oxygen, regeneration, damage dealt/taken multipliers, thorns, lifesteal, on-hit effects, immunities to explosions, projectiles, freezing, lightning and bad effects, keep XP, and slow falling, dolphin's grace, conduit power, saturation, jump boost and resistance as permanent effects.
- **Items (56):** 17 attribute bonuses while held, worn or carried (health, armor, speed, jump, size, reach, luck...), on-hit effects and lifesteal, **throwable items and boomerangs**, explode / lightning on impact, name color (rarity), display name, repair material, use cooldown, never despawns, soulbound, eat effect level and duration, heal and XP on eating, wolf food.
- **Blocks (52):** jump and walk speed factor, bounciness, landing damage, replaceable, invisible, sound, redstone power, growth speed, mob spawning; launch pads, conveyor belts, random teleport, bounce away, heal / set on fire / put out fire / hunger / feed / XP on step, sticky like cobweb, deletes dropped items; when broken: drop multiplier, replacement drop, silk touch only, explode, lightning, spawn mobs, hurt the breaker, grow back after a delay.
- **Projectiles (19), new:** damage multiplier and bonus, speed, gravity, no gravity, homing, always critical, piercing, lifetime, can't be picked up, explode / lightning / fire / knockback / potion effect / spawn a mob on hit, teleport the shooter (ender-pearl style).
- **Enchantments (7), new:** max level, level bonus, disabled, enchanting table weight, anvil cost, works on any item, ignores conflicts.
- **World (15), new:** day length, always clear weather, gravity, fall damage, explosion power, mob health / damage / speed, mob spawn amount, mobs never despawn, mobs ignore players, player damage taken, hunger, XP, item despawn time.

### Also new
- Built-in presets for the new stats and types (trampoline, launch pad, boomerang, grenade, homing arrows, moon gravity, hard mode...).
- Commands accept the new types: `/craftstats reset world`, `/craftstats randomize projectile minecraft:arrow`, `/craftstats preset load overcharged enchantment minecraft:sharpness`...
- Config toggles for the three new editors.
- The server clamps every value to its allowed range (no more accidental world-ending explosions).

### Changed
- Item stats removed in 1.1.2 because they did nothing (boomerang, throwable...) are back, and they work now.

## 1.1.2

A big bug-fix release, plus Minecraft 1.21.11 support. Worlds and presets from 1.0 are converted automatically.

**Downloads:** pick the jar for your loader and Minecraft version: `craftstats-fabric-1.1.2+1.21.1.jar`, `craftstats-fabric-1.1.2+1.21.11.jar`, `craftstats-neoforge-1.1.2+1.21.1.jar` or `craftstats-neoforge-1.1.2+1.21.11.jar`. Requires Architectury API (13.x for 1.21.1, 19.x for 1.21.11), and Fabric API on Fabric. Cloth Config is no longer needed.

> **Beta.** Every build is checked automatically: a real server and the game client are started on both loaders and both Minecraft versions, every hook into the game is applied, and the main features are exercised. It hasn't had much hands-on play yet, so please report anything odd.

### New
- Minecraft **1.21.11** support (Fabric and NeoForge), alongside 1.21.1.
- Item stats now actually work: attack damage, attack speed, durability, stack size, mining speed, fireproof, unbreakable, enchantment glint, enchantability, and food (make anything edible or inedible, nutrition, saturation, eating time, an effect on eating).
- Editor shows the vanilla value as a grey hint; leave a field empty to keep vanilla.
- Config screen on NeoForge; Mod Menu button on Fabric now works.
- Mob jump strength, separate block/entity reach, invulnerability time and regeneration threshold for players.

### Fixed
- **Multiplayer:** clients now receive the server's stats, so block collision, mining speed, no-clip and item changes no longer desync on dedicated servers.
- Editing a mob type now updates mobs that are already loaded; resetting restores them.
- LAN guests could edit everything; permissions are now enforced by the server (operators and the single-player host; see `requireOp`/`allowSurvival`).
- **Keep Inventory deleted your inventory** on death. It now keeps it.
- No Clip didn't work at all; now it does (and grants flight so you can't fall out of the world).
- Hunger drain rate compounded every tick; fly speed did nothing; magic immunity also blocked fall, drowning and starvation damage.
- Editing any item made it stack to 64 (swords included).
- Every edited player silently got 8× burn time, full sweep damage and +1.5 entity reach (wrong defaults); old saves are corrected.
- Randomize turned unedited mobs into 1-HP, frozen mobs; chaos mode divided by zero.
- Loading a preset in the GUI did nothing; imported presets could crash.
- The Craft Wand recipe was broken, so the wand couldn't be crafted.
- `/craftstats randomize mob minecraft:...` couldn't be parsed; command changes weren't saved.
- Block editor tabs were mislabeled; per-block overrides now go away when the block is replaced.
- "Can Fall", hardness and other block stats now also work on blocks that override vanilla behaviour.
- The stats book is given once instead of on every login; left-click copy with the wand works.

### Removed
- Editor fields that never did anything (mob AI/loot fields, block render/physics fields, item category flags, sprint/swim speed, infinite items).
- The unreachable Propagate screen.

### Technical
- Mob and player stats are temporary attribute modifiers: removing an override, or the mod, restores vanilla exactly.
- Saves are written atomically; an unreadable `stats.json` is backed up instead of being overwritten.
