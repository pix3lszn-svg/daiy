# Overworld Cup Discord bot

A Discord bot that drafts members into one of the **12 nations** of the
Overworld Cup with a single button click. When a member presses the button they:

1. Get a **random nation** assigned to their account,
2. Have their **display-name colour** changed to that nation's colour, and
3. Gain access to that nation's **private channel** (its war room).

The nations and colours match the Minecraft plugin
(`minecraft-plugin/.../nations/NationManager.java`), so a player's Discord name
colour lines up with their in-game team:

> Dark Forest · Desert · Forest · Jungle · Mesa · Mushroom · Plains · Savanna ·
> Snow · Taiga · Cherry · Swamp

## How it works

Each nation maps to a Discord **role** (carries the colour) and a **private
text channel** (locked to `@everyone`, visible only to that role). Assigning the
role does everything at once — it recolours the name *and* unlocks the channel,
because the channel is permissioned by role. One draw per player: pressing the
button again just reminds them which nation they're already in.

## Setup

1. **Create a bot application** at
   <https://discord.com/developers/applications> → *New Application* → *Bot*.
   Copy the **token**.
2. **Invite it** to your server with the `bot` and `applications.commands`
   scopes and these permissions: **Manage Roles**, **Manage Channels**,
   **Send Messages**. (OAuth2 → URL Generator.)
3. In **Server Settings → Roles**, drag the **bot's own role above** all the
   `Nation: …` roles — Discord won't let a bot assign a role higher than itself.
4. Configure the bot:

   ```bash
   cd discord-bot
   cp .env.example .env   # paste your bot token (+ optional GUILD_ID)
   npm install
   npm run dev            # loads .env via node --env-file
   ```

   In production set `DISCORD_TOKEN` (and optionally `GUILD_ID`) in the
   environment and run `npm start`.

## Using it

Two slash commands (admin-only — they require *Manage Server*):

| Command | What it does |
| --- | --- |
| `/nation-setup` | Creates all 12 nation roles and private channels up front. Optional — they're also created lazily the first time someone is drafted into a nation. |
| `/nation-panel` | Posts the **🎲 Choose my nation** button in the current channel. |

Players just press the button. Done.

## Configuration

| Variable | Required | What it is |
| --- | --- | --- |
| `DISCORD_TOKEN` | Yes | Bot token from the Discord developer portal. |
| `GUILD_ID` | No | A server ID. If set, slash commands register to that server **instantly**. Left blank, they register globally (can take ~1h to appear). |

## Notes

- **Name colour** comes from a member's highest *coloured* role. If someone has
  another coloured role above their nation role, that one wins — keep other
  coloured roles below the `Nation: …` roles if you want the nation colour to
  show.
- Nations and colours live in [`src/nations.js`](src/nations.js); edit there to
  change the roster or palette.
