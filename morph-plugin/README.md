# EmeraldMorph

Standalone morphing plugin for Paper 1.21.11 — no dependencies, works for
Java and Bedrock (Geyser) viewers because morphs are real entities.

This is the morph system from EmeraldPerks split out on its own, plus villager
styles and an in-game access switch. **Don't run it alongside EmeraldPerks'
morph** — both register `/morph`, so if you use this one, either keep only one
plugin or ask for an EmeraldPerks build with its morph commands removed.

## Player commands

| Command | What |
| --- | --- |
| `/morph <mob>` | Become any mob (tab-complete lists them) |
| `/morph villager [biome] [job]` | Styled villager, e.g. `/morph villager plains librarian` |
| `/unmorph` (or `/morph off`) | Change back |
| `/morphview` | Toggle seeing your own morph (hide it so it stops blocking your aim) |

Villager biomes: desert, jungle, plains, savanna, snow, swamp, taiga.
Jobs: armorer, butcher, cartographer, cleric, farmer, fisherman, fletcher,
leatherworker, librarian, mason, nitwit, shepherd, toolsmith, weaponsmith
(both modifiers optional, either order works).

## Operator commands (`emeraldmorph.admin`, OPs by default)

| Command | What |
| --- | --- |
| `/allowmorph` | Everyone may `/morph` (announced in chat) |
| `/allowmorph <player>` | Grant just one player |
| `/unallowmorph` | Back to OP-only: clears all grants and reverts every morphed non-OP |
| `/unallowmorph <player>` | Revoke one player (reverts them if morphed) |

By default (fresh install) morphing is **OP-only** until you open it up.
Access state persists in `plugins/EmeraldMorph/data.yml` across restarts.

`config.yml` has a `blocked-mobs` list if you want to ban specific morphs
(e.g. `ENDER_DRAGON`).

## Building

```bash
cd morph-plugin
mvn package          # needs repo.papermc.io access
# or, where that repo is unreachable:
./build-offline.sh   # compiles against Paper API source from GitHub + Maven Central
```
