# Overworld Cup — operating guide

How to run the cup with the **EmeraldPerks** plugin (v1.9.0) + the Overworld
Cup datapack. Admin (op) runs everything unless noted.

## How it fits together

- **Nation** = the broad group anyone signs up to with `/nation join`. On
  joining, a player is **given their nation's coloured leather tunic** and
  their **name tag turns the nation's colour**.
- **Squad** = the ≤12 from a nation who actually play a match. You build it
  with `/nation draw` = your **reserved players + a random fill** of online
  nation members. Only the squad takes the field.
- A player can only ever be in **one nation and one squad** — the plugin
  enforces this. Use `/nation reset` to wipe everything clean.

---

## One-time setup (once, ever)

1. Create your nations (lowercase, matching what you'll type in `startmatch`):
   ```
   /nation create cherry
   /nation create darkforest
   ... (one per nation)
   ```
2. Run the datapack setup once (creates the match_home / match_away teams):
   ```
   /function cup:setup
   ```
3. Run the camera rig setup once (and after any datapack update):
   ```
   /function cup:cams_setup
   ```

Built-in tunic colours: plains, forest, darkforest, desert, jungle, mesa,
snow, swamp, savanna, taiga, mushroom, cherry.

---

## Players sign up

Each player runs (works in adventure mode, no command block needed):
```
/nation join cherry
```
They immediately get the cherry tunic + a cherry-coloured name tag. They can
`/nation leave`, or switch by joining a different nation (which cleanly
removes the old one — a player is never in two nations at once).

**Important:** use *only* `/nation join` for sign-ups. Don't mix in the
datapack's `cup:assign` or manual `/tag` — stacking those is what causes
players to end up in multiple nations/squads.

---

## Before each match (do for BOTH nations playing)

### 1. Reserved players (guaranteed starters) — optional
Locked into every draw until removed:
```
/nation reserve cherry SomePlayer
/nation unreserve cherry SomePlayer
```
Reserved players are honoured even if offline at draw time (they get tagged
when they next log in).

### 2. Draw the squad
With the players online and ready:
```
/nation draw cherry
/nation draw darkforest
```
Each draw = reserved players first, then a random fill of online nation
members up to 12. It prints exactly who was picked. Re-run any time to
re-roll the random spots (reserved stay). Check with `/nation squad cherry`.

### 3. Start the match
```
/function cup:startmatch {home:"cherry",away:"darkforest"}
```
Only the two drawn squads take the pitch (home red, away blue), lined up with
the countdown; the ball spawns at kickoff.

---

## Troubleshooting

- **Players don't spawn on the pitch / no kit:** they probably weren't on a
  match team. Run `/function cup:diag` (no arguments) — each player chats which
  `squad_` tag and which match team they're on. Empty match team = no squad was
  drawn, or tags are tangled.
- **Tags look tangled (someone in two nations/squads):** run `/nation reset` —
  it wipes every nation/squad tag from all online players and empties all
  stored squads, keeping your nation list and reserved lists. Then have
  everyone `/nation join` one nation and re-draw.
- **A vanilla command is "blocked" (`@e`, `/kill`, `/tp`):** EssentialsX is
  shadowing it. Prefix with `minecraft:` (e.g. `/minecraft:kill @e[...]`), or
  run it from a datapack function (functions bypass the override).

---

## Command reference

| Command | Who | What |
| --- | --- | --- |
| `/nation join <name>` | players | Sign up — gives tunic + coloured name tag |
| `/nation leave` | players | Leave your nation |
| `/nation list` | anyone | List nations |
| `/nation squad <name>` | anyone | Show a nation's drawn squad |
| `/nation create <name>` | admin | Add a nation |
| `/nation delete <name>` | admin | Remove a nation |
| `/nation reserve <nation> <player>` | admin | Lock a guaranteed squad spot |
| `/nation unreserve <nation> <player>` | admin | Remove a reserved spot |
| `/nation draw <nation>` | admin | Pick the squad (reserved + random) |
| `/nation cleardraw <nation>` | admin | Empty one nation's squad |
| `/nation reset` | admin | Clean slate: wipe all tags & squads |
| `/function cup:setup` | admin | One-time: create match teams |
| `/function cup:cams_setup` | admin | Spawn/refresh the camera rig |
| `/function cup:startmatch {home:"a",away:"b"}` | admin | Field the two squads |
| `/function cup:diag` | admin | Print squad-tag / team state for debugging |

## Gotchas

- **Draw before you start.** `startmatch` fields the `squad_` tag; a nation
  with no drawn squad spawns empty.
- **Lowercase, consistent names.** `/nation create cherry` then
  `home:"cherry"` — tags are case-sensitive.
- **Random fill = online members only** (reserved players are the exception).
- **Name tags during a match:** while a match is running, squad players are on
  match_home/match_away, so their name tags show red/blue (home/away) instead
  of the nation colour. Outside a match they show the nation colour.
- **Squad size** is 12 by default — change `nations.squad-size` in `config.yml`.
