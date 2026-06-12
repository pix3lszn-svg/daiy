# Connecting Tebex to your Paper/Spigot server

This is the part that actually delivers purchases in game. The Tebex plugin
polls Tebex for completed payments and runs the commands you configured for
each package (e.g. `lp user {username} parent add vip`).

## 1. Create your Tebex store

1. Go to [tebex.io](https://www.tebex.io/) and create an account.
2. Create a new webstore and pick **Minecraft: Java Edition** as the platform.
3. Complete the onboarding (store name, currency, payment methods). To receive
   real payouts you'll also need to finish Tebex's seller verification under
   **Payments** in the creator panel.

## 2. Install the plugin

1. In the creator panel, go to **Webstore → Integrations → Game Servers** and
   add a game server. Tebex shows you a **secret key** — you'll need it in a
   moment. Treat it like a password.
2. Download the Tebex plugin for Minecraft Java from the same page (or from
   <https://docs.tebex.io/store/game-servers>).
3. Drop the `.jar` into your server's `plugins/` folder and restart the server.

## 3. Link the server to your store

From the **server console** (not in-game chat — you don't want the secret in
chat logs):

```
tebex secret <your-secret-key>
```

You should see a confirmation that the store was linked. Verify with:

```
tebex info
```

## 4. Create packages and commands

1. In the creator panel, go to **Webstore → Packages** and create categories
   (e.g. "Ranks", "Crate Keys") and packages.
2. On each package, set the **game server commands** to run on purchase, using
   `{username}` as the placeholder, for example:
   - `lp user {username} parent add vip`
   - `crazycrates give physical vote 3 {username}`
3. Optionally set **expiry commands** for time-limited ranks.

## 5. Test it end to end

1. In the creator panel enable **test mode** (or create a 100%-off gift card)
   so you don't charge yourself.
2. Buy a package through your store with your own username.
3. On the server, run `tebex forcecheck` in the console to process the queue
   immediately instead of waiting for the next poll.
4. Confirm the commands ran (rank applied, items given).

## Useful plugin commands

| Command | What it does |
| --- | --- |
| `tebex secret <key>` | Links the server to your webstore (console only) |
| `tebex info` | Shows the linked store and connection status |
| `tebex forcecheck` | Processes the purchase queue immediately |
| `tebex sendlink <player> <packageId>` | Sends a player a checkout link in chat |
| `tebex report` | Generates a report for Tebex support |

## Troubleshooting

- **Purchases not arriving:** run `tebex forcecheck`; check the console for
  errors; make sure the package's commands are set on the *game server* tab,
  and that the player was online if the command requires it (or mark the
  command "require online" appropriately so it queues).
- **"Invalid secret":** the key was regenerated or belongs to another store —
  grab the current one from Integrations → Game Servers and run
  `tebex secret` again from the console.
- **Offline-mode/proxy networks:** install the plugin on the backend servers
  (or the proxy, if you deliver via proxy commands) and make sure your
  username forwarding is set up correctly, otherwise deliveries can target the
  wrong player identity.
