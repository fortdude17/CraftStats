# CraftStats

An in-game editor for almost everything: mobs, blocks, items, players, projectiles, enchantments and the world itself, with 294 stats in total. Pick something, change its numbers, press **Apply**, and it takes effect immediately in the live world. Changes are saved per world.

## Supported versions

| Minecraft | Fabric | NeoForge |
|-----------|--------|----------|
| 1.21.1 (also loads on 1.21) | ✅ | ✅ (NeoForge 21.1) |
| 1.21.11 | ✅ | ✅ (NeoForge 21.11) |

Each Minecraft version has its own jar. Use the one whose name matches your game version (`craftstats-fabric-1.2.0+1.21.11.jar`, ...).

**Required:** [Architectury API](https://modrinth.com/mod/architectury-api) (13.x for 1.21.1, 19.x for 1.21.11), and on Fabric also [Fabric API](https://modrinth.com/mod/fabric-api).
**Optional:** [Mod Menu](https://modrinth.com/mod/modmenu) on Fabric for the config button. Cloth Config is no longer needed.

The mod must be installed on the server *and* on clients.

## Getting the jars

Download them from the [Releases page](https://github.com/fortdude17/CraftStats/releases). See [CHANGELOG.md](CHANGELOG.md) for what changed.

Every push to GitHub also builds both versions for both loaders. Open the **Actions** tab, click the latest successful **Build** run, and download the `craftstats-mc1.21.1` or `craftstats-mc1.21.11` artifact (a zip containing the Fabric and NeoForge jars).

To build locally (Java 21):

```sh
./gradlew build                # Minecraft 1.21.1
./gradlew build -Pmc=1.21.11   # Minecraft 1.21.11
```

Jars end up in `fabric/build/libs/` and `neoforge/build/libs/` (use the ones without `-dev` or `-sources`).

To publish a release: set `mod_version` in `gradle.properties`, add a section to `CHANGELOG.md`, and push a tag `v<mod_version>`. The Release workflow builds every version and attaches the jars to a GitHub pre-release.

## How to use it

- **Pause menu → CraftStats** opens the editor. The tabs at the top pick the type (Mobs, Blocks, Items, Players, Projectiles, Enchants, World), the list on the left picks the target, and the cards on the right are the stat categories. Click a card to edit its stats, or type in **Search stats** to find any stat by name.
- Hover a stat for an explanation and its allowed range. Number fields left empty use the vanilla value, which is shown in grey. Numbers turn green or orange when they are above or below vanilla, and red when they don't parse. The **x** button next to a stat puts it back to default.
- Nothing changes in the world until you press **Apply**. **Reset** removes all changes from the selected target, **Reset All** from the whole world.
- **Craft Wand** (`/craftstats give <player>` or craft it: gold ingots around a nether star, blaze rod below):
  - right-click a block: edit only that block · sneak + right-click: edit that block type
  - right-click a mob: edit that one mob · sneak + right-click: randomize its type
  - right-click a player: edit that player · right-click air: edit yourself
  - right-click air with a spawn egg in your off-hand: edit that mob type
  - left-click a mob: copy its stats to the clipboard as JSON
- **Player Stats Book**: opens the editor on your own stats. Editors get one the first time they join a world.

## What you can change

| Type | Stats | Highlights |
|---|---|---|
| Mobs (whole type or a single mob) | 68 | health, damage, armor, speed, size, every other attribute; damage dealt/taken, thorns, lifesteal, fire/potion/lightning on hit; 14 immunities; no AI, hostile, peaceful, teleports when hurt, permanent effect; no drops, extra loot, explode/lightning/spawn mobs on death |
| Players | 77 | every attribute (health, reach, size, gravity, jump...), flight, no clip, god mode, damage dealt/taken, thorns, lifesteal, on-hit effects, 10 immunities, keep inventory/XP, hunger, 15 permanent effects |
| Items | 56 | damage, speed, durability, stack size, mining speed, 17 attribute bonuses while held or worn, on-hit effects, **throwable items and boomerangs**, rarity, display name, repair material, cooldown, soulbound, never despawns, food (edible, nutrition, eating time, effects, healing, XP, wolf food) |
| Blocks | 52 | hardness, blast resistance, slipperiness, jump/speed factor, bounce, landing damage, light, invisible, sound, redstone power, growth speed, mob spawning; launch pads, conveyors, teleport/heal/damage/effects on step; drops, drop multiplier, silk touch only, explode/lightning/spawn when broken, grows back |
| Projectiles | 19 | damage, speed, gravity, homing, piercing, always critical, lifetime, explode/lightning/fire/spawn/teleport on hit |
| Enchantments | 7 | max level, level bonus, disabled, table weight, anvil cost, any item, ignore conflicts |
| World | 15 | day length, clear weather, gravity, fall damage, explosion power, mob health/damage/speed, spawn amount, despawning, peaceful mobs, player damage, hunger, XP, item despawn time |

Blocks can also be edited for a single position (with the wand). Most stats work there; the ones that Minecraft only has per block type (blast resistance, light, sound...) are hidden in that mode. A per-position edit is ignored once a different block is placed there.

Also: **presets** (built-in and your own, stored in `<game folder>/craftstats/presets/`, shareable as JSON), **copy/paste** of a profile, **Copy From Block** (copy another block's real values), and **randomize** with a seed (mild ±20 %, wild 0.25×–3×, or chaos) with a preview.

## Commands

All need operator level 2. Types: `mob`, `block`, `item`, `player`, `projectile`, `enchantment`, `world`.

```
/craftstats give <player>
/craftstats reload                                   reload config and presets
/craftstats reset all                                remove every change in this world
/craftstats reset <type> <id>                        e.g. reset mob minecraft:zombie, reset world
/craftstats preset load <name> <type> <id>
/craftstats randomize <type> <id> [seed]             mob, block, item or projectile
```

## Permissions and config

`config/craftstats/config.json` (also editable from Mod Menu on Fabric or the Mods screen on NeoForge):

| Option | Default | Meaning |
|---|---|---|
| `requireOp` | `true` | Only operators and the single-player host may edit. |
| `allowSurvival` | `false` | With `requireOp` off, survival players may edit too (otherwise creative only). |
| `enableMobEditor`, `enableBlockEditor`, `enableItemEditor`, `enablePlayerEditor`, `enableProjectileEditor`, `enableEnchantmentEditor`, `enableWorldEditor`, `enableRandomize`, `enablePresets` | `true` | Turn features off. |
| `blacklist` | `[]` | IDs that can't be edited, e.g. `["minecraft:wither"]`. |
| `maxScaleCap` | `16.0` | Largest mob size allowed. |
| `randomizeIntensity` | `mild` | `mild`, `wild` or `chaos`. |

On a server, the server's config decides who may edit. The server checks every change and keeps every value inside its allowed range; refused changes show a red message in chat.

## Data and recovery

Changes are stored in `<world>/craftstats/stats.json`. Worlds made with CraftStats 1.0 are converted automatically when they load. If a world becomes unplayable, use **CraftStats Data** on the title screen to delete the mod's data for one world or all worlds. The world itself is not touched.

Removing an override, or removing the mod, restores vanilla values. Mob and player stats are applied as temporary attribute modifiers and are never written into the save. Thrown items ride on vanilla snowball entities, so the mod adds no new entities or blocks to your world.

## Project layout

```
common/      all mod code (Architectury)
fabric/      Fabric entrypoints and metadata
neoforge/    NeoForge entrypoints and metadata
versions/    dependency versions per Minecraft version
gradle/preprocess.gradle   version-specific code switching (//#if MC >= ...)
```

The sources are written for Minecraft 1.21.1. Code that differs in newer versions sits in `//#if MC >= 1.21.x` blocks, and the build switches them on when you pass `-Pmc=1.21.11`. Version-specific resources live in `common/src/main/resources-<version>/`.
