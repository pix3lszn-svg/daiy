# EmeraldPerks

Custom Paper 1.21.11 plugin for the DailyEmerald server. It syncs with the
[DailyEmerald Patreon](https://www.patreon.com/c/DailyEmerald) and grants:

| Patreon tier | Perk |
| --- | --- |
| **Board Member** | `/morph <mob>` into **any mob except the ender dragon**, + a goat horn |
| **Journalist** | `/morph villager`, + a goat horn |

No other plugins required — morphing is built in (the player turns invisible
and a real, AI-less, invulnerable mob mirrors them every tick, so it renders
for Java *and* Bedrock/Geyser viewers).

## Install

1. Drop `EmeraldPerks-1.0.0.jar` into your server's `plugins/` folder and restart.
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

## Commands

| Command | Who | What |
| --- | --- | --- |
| `/patreon link <email>` | everyone | Claim perks by Patreon email |
| `/patreon status` | everyone | Show your role and perks |
| `/patreon unlink` | everyone | Disconnect your Patreon |
| `/morph <mob>` / `/morph off` | patrons | Morph (tab-complete lists your options) |
| `/unmorph` | patrons | Change back |
| `/patreon sync` | admins | Pull the patron list right now |
| `/patreon set <player> <board\|journalist\|none>` | admins | Manual grant/revoke (sync won't touch it) |
| `/patreon horn <player>` | admins | Re-give a lost goat horn |

Admins/ops also get `emeraldperks.morph.all` by default, so you can `/morph`
without a pledge.

## Known limitations

- While morphed you're invisible with a mob mirroring you — but **worn armor
  and held items still render** (a vanilla quirk of invisibility). Patrons who
  want the clean look should stow their armor.
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
