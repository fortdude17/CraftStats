# Changelog

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
