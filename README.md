# CraftStats

An in-game editor for the stats of mobs, blocks, items and players. Pick something, change its numbers, press **Apply**, and it takes effect immediately in the live world. Changes are saved per world.

## Supported versions

| Minecraft | Fabric | NeoForge |
|-----------|--------|----------|
| 1.21.1 (also loads on 1.21) | ✅ | ✅ (NeoForge 21.1) |
| 1.21.11 | ✅ | ✅ (NeoForge 21.11) |

Each Minecraft version has its own jar. Use the one whose name matches your game version (`craftstats-fabric-1.1.2+1.21.11.jar`, ...).

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

- **Pause menu → CraftStats** opens the full editor: browser on the left, stats on the right.
- **Craft Wand** (`/craftstats give <player>` or craft it: gold ingots around a nether star, blaze rod below):
  - right-click a block: edit only that block · sneak + right-click: edit that block type
  - right-click a mob: edit that one mob · sneak + right-click: randomize its type
  - right-click a player: edit that player · right-click air: edit yourself
  - right-click air with a spawn egg in your off-hand: edit that mob type
  - left-click a mob: copy its stats to the clipboard as JSON
- **Player Stats Book**: quick editor for your own stats. Editors get one the first time they join a world.

Number fields left empty use the vanilla value, which is shown in grey. Numbers turn green or orange when they are above or below vanilla, and red when they don't parse.

## What you can change

**Mobs** (whole type or a single mob): max health, attack damage, armor, knockback resistance, movement speed, jump strength, follow range, size, XP dropped. Toggles: invincible, immune to fire / fall / drowning / explosions / poison / magic, burns in daylight, can despawn, silent, glowing.

**Blocks**: hardness (−1 = unbreakable), blast resistance, slipperiness, light level, XP dropped, no collision, climbable, falls like sand, needs the correct tool, piston behaviour. Step-on effects: damage, speed multiplier, levitation, glowing, freezing, any potion effect.
Per-position edits support hardness, XP, collision, climbing and step-on effects. The rest are type-wide only, because Minecraft doesn't expose a position for them. A per-position edit is ignored once a different block is placed there.

**Items**: attack damage, attack speed, enchantability, durability, stack size (1–99), mining speed, fireproof, unbreakable, enchantment glint, and food: make anything edible or inedible, nutrition, saturation, eating time, and an effect on eating. Tools and armor always stack to 1 (Minecraft requires it).

**Players**: health, damage, attack speed, crit multiplier, knockback, sweep damage, invulnerability time, block and entity reach, walk/fly speed, jump, step height, gravity, sneak speed, swimming/mining efficiency, armor and toughness, knockback and explosion resistance, absorption, burn time, fall damage, luck, max food, regeneration threshold, hunger rate, XP multiplier. Toggles: god mode, keep inventory, immune to fire / drowning / poison / magic, no fall damage, no clip (also grants flight), no hunger from actions, one-hit kill, permanent effects (night vision, water breathing, fire resistance, regeneration, glowing, invisibility, haste, strength, speed).

Also: **presets** (built-in and your own, stored in `<game folder>/craftstats/presets/`, shareable as JSON), **copy/paste** of a profile, **From Block** (copy another block's real values), and **randomize** with a seed (mild ±20 %, wild 0.25×–3×, or chaos).

## Commands

All need operator level 2.

```
/craftstats give <player>
/craftstats reload                                   reload config and presets
/craftstats reset all                                remove every change in this world
/craftstats reset <mob|block|item|player> <id>
/craftstats preset load <name> <mob|block|item|player> <id>
/craftstats randomize <mob|block|item> <id> [seed]
```

## Permissions and config

`config/craftstats/config.json` (also editable from Mod Menu on Fabric or the Mods screen on NeoForge):

| Option | Default | Meaning |
|---|---|---|
| `requireOp` | `true` | Only operators and the single-player host may edit. |
| `allowSurvival` | `false` | With `requireOp` off, survival players may edit too (otherwise creative only). |
| `enableMobEditor`, `enableBlockEditor`, `enableItemEditor`, `enablePlayerEditor`, `enableRandomize`, `enablePresets` | `true` | Turn features off. |
| `blacklist` | `[]` | IDs that can't be edited, e.g. `["minecraft:wither"]`. |
| `maxScaleCap` | `16.0` | Largest mob size allowed. |
| `randomizeIntensity` | `mild` | `mild`, `wild` or `chaos`. |

On a server, the server's config decides who may edit. The server checks every change, and refused changes show a red message in chat.

## Data and recovery

Changes are stored in `<world>/craftstats/stats.json`. Worlds made with CraftStats 1.0 are converted automatically when they load. If a world becomes unplayable, use **CraftStats Data** on the title screen to delete the mod's data for one world or all worlds. The world itself is not touched.

Removing an override, or removing the mod, restores vanilla values. Mob and player stats are applied as temporary attribute modifiers and are never written into the save.

## Project layout

```
common/      all mod code (Architectury)
fabric/      Fabric entrypoints and metadata
neoforge/    NeoForge entrypoints and metadata
versions/    dependency versions per Minecraft version
gradle/preprocess.gradle   version-specific code switching (//#if MC >= ...)
```

The sources are written for Minecraft 1.21.1. Code that differs in newer versions sits in `//#if MC >= 1.21.x` blocks, and the build switches them on when you pass `-Pmc=1.21.11`. Version-specific resources live in `common/src/main/resources-<version>/`.
