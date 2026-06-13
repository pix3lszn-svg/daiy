# EmeraldPerks

Custom Paper 1.21.11 plugin for the DailyEmerald server. It syncs with the
[DailyEmerald Patreon](https://www.patreon.com/c/DailyEmerald) and grants:

| Patreon tier | Perk |
| --- | --- |
| **Board Member** | `/morph <mob>` into **any mob except the ender dragon**, + a goat horn |
| **Journalist** | `/morph villager`, + a goat horn |

Both tiers also get (each toggleable in `config.yml`):

- **Auto-whitelisting** — patrons are added to the server whitelist when they
  gain their role and removed when their pledge lapses (ops/admins never).
- **Adventure mode** — patrons are put into adventure mode on join
  (ops/admins exempt).
- **Priority join** — when the server is full, a connecting patron bumps a
  random non-priority player off to take their slot. Ops, admins, patrons,
  and anyone on the manual priority list are never the one bumped.

No other plugins required — morphing is built in (the player turns invisible
and a real, AI-less, invulnerable mob mirrors them every tick, so it renders
for Java *and* Bedrock/Geyser viewers).

## Install

1. Drop `EmeraldPerks-1.2.0.jar` into your server's `plugins/` folder and restart.
2. Open `plugins/EmeraldPerks/config.yml` and paste your Patreon
   **Creator's Access Token** (see below), then restart again
   (the plugin works without it, but only manual grants).

## Getting the Patreon token (~5 minutes, one time)

1. Go to <https://www.patreon.com/portal/registration/register-clients>
   while logged in as the DailyEmerald creator account.
2. Click **Create Client**. Name: anything (e.g. "Minecraft server").
   Redirect URI: `https://localhost` (unused, but required). API version: **2**.
3. On the created client, copy these into `config.yml`:
   - **Creator's Access Token** → `creator-access-token`
   - **Client ID** → `client-id`
   - **Client Secret** → `client-secret`
   - **Creator's Refresh Token** → `refresh-token`

   The last three let the plugin renew the token automatically when Patreon
   expires it (roughly monthly). With only the access token, you'd have to
   paste a fresh one every month.

## How players claim perks

A patron joins the server and runs:

```
/patreon link their-patreon-email@example.com
```

The plugin checks the live patron list; if that email has an active
Board Member or Journalist pledge, the perks unlock instantly and the goat
horn is handed over. Pledges are re-checked every 10 minutes — lapsed
patrons lose perks automatically, new patrons show up within a sync cycle.

**If your whitelist is ON**, new patrons can't join to link themselves.
Have them send you their Minecraft name + Patreon email, then run (in-game
as admin, or from console):

```
/patreon link TheirMcName their-patreon-email@example.com
```

This verifies the pledge, whitelists them, and unlocks their perks the first
time they join. Note: vanilla whitelisting works by Java account name —
Bedrock players joining through Geyser/Floodgate may need a whitelist plugin
that understands Floodgate prefixes.

## Commands

| Command | Who | What |
| --- | --- | --- |
| `/patreon link <email>` | everyone | Claim perks by Patreon email |
| `/patreon status` | everyone | Show your role and perks |
| `/patreon unlink` | everyone | Disconnect your Patreon |
| `/morph <mob>` / `/morph off` | patrons | Morph (tab-complete lists your options) |
| `/unmorph` | patrons | Change back |
| `/morphview` (or `/morph view`) | patrons | Toggle seeing your own morph — hide it so it stops blocking your own clicks/aim |
| `/patreon sync` | admins | Pull the patron list right now |
| `/patreon set <player> <board\|journalist\|none>` | admins | Manual grant/revoke (sync won't touch it) |
| `/patreon horn <player>` | admins | Re-give a lost goat horn |
| `/patreon link <mcname> <email>` | admins/console | Pre-link & whitelist a patron who hasn't joined yet |
| `/patreon priority <add\|remove\|list> [name]` | admins | Manage the extra priority-join list (non-patrons welcome) |
| `/patreon grant <board\|journalist> <name> [name]...` | admins/console | Bulk grant by IGN only (e.g. names collected from a Patreon post) — whitelists everyone listed; perks land on first join. Manual grants: remove with `/patreon set <name> none` |

Admins/ops also get `emeraldperks.morph.all` by default, so you can `/morph`
without a pledge.

## Known limitations

- While morphed you're invisible with a mob mirroring you — but **worn armor
  and held items still render** (a vanilla quirk of invisibility). Patrons who
  want the clean look should stow their armor.
- The follower-mob sits on top of you, so it can intercept your own clicks/aim.
  Run **`/morphview`** to hide the mob from your own screen (others still see
  it) and your attacks pass through normally. One remaining edge case: your own
  arrows can still clip a very large mob's body (ghast, ravager).
- Morphs drop on death, world change, and relog (just `/morph` again).
- The morph is visual + name tag; you keep your own hitbox, health, and abilities
  (no creeper explosions, no ghast fireballs).

## Building

Normal environments (needs internet access to repo.papermc.io):

```bash
cd minecraft-plugin
mvn package        # target/EmeraldPerks-1.0.0.jar
```

`build-offline.sh` documents the alternative build used to produce the first
release: it compiles against the Paper API *source* (cloned from the official
PaperMC GitHub) plus dependencies from Maven Central, for environments where
repo.papermc.io is unreachable.
