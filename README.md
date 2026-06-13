# Minecraft server Tebex setup

Everything needed to take payments on a Paper/Spigot Minecraft server with
[Tebex](https://www.tebex.io/):

| Piece | What it is |
| --- | --- |
| [`store/`](store/) | Custom webstore (React + Vite) built on the [Tebex Headless API](https://docs.tebex.io/developers/headless-api/overview) — players browse packages, build a basket, and check out on Tebex's hosted, PCI-compliant checkout page. |
| [`webhook/`](webhook/) | Node.js service that receives Tebex webhooks (purchases, refunds, chargebacks) with signature + IP verification, and can announce purchases to Discord. |
| [`discord-bot/`](discord-bot/) | Discord bot that drafts members into one of the 12 Overworld Cup nations with a button — assigns a random nation role, recolours their name, and unlocks their nation's private channel. Colours match the Minecraft plugin. |
| [`docs/minecraft-server-setup.md`](docs/minecraft-server-setup.md) | Guide for installing the Tebex plugin on the Paper/Spigot server so purchases get delivered in game. |

## Setup order

1. **Create the store** — follow [docs/minecraft-server-setup.md](docs/minecraft-server-setup.md)
   to create your Tebex webstore, link the plugin to your server, and create
   packages. Nothing else works until packages exist.
2. **Run the custom storefront** (optional — Tebex also hosts a default store
   for you at `yourstore.tebex.io`):

   ```bash
   cd store
   cp .env.example .env   # paste your public Headless API token
   npm install
   npm run dev            # local dev at http://localhost:5173
   npm run build          # production build in dist/ — deploy to any static host
   ```

   The token comes from **creator.tebex.io → Webstore → Webstore Settings →
   Developers → Public token**. It is safe to ship in frontend code: it only
   allows browsing the catalog and creating baskets. All payment details are
   entered on Tebex's checkout page, never on your site.

3. **Run the webhook listener** (optional — for Discord purchase
   announcements, chargeback alerts, or your own custom logic):

   ```bash
   cd webhook
   cp .env.example .env   # paste your webhook secret (+ Discord URL if wanted)
   npm install
   npm test
   npm start              # listens on :8080 at /webhooks/tebex
   ```

   Expose it on a public HTTPS URL (reverse proxy, Cloudflare Tunnel, etc.),
   then register that URL at **creator.tebex.io → Webstore Settings →
   Developers → Webhooks**. Tebex sends a one-time `validation.webhook` event
   which this service answers automatically. If you're behind a reverse proxy,
   set `TRUST_PROXY=true` so the Tebex IP allowlist sees the real client IP.

## Secrets cheat sheet

| Credential | Where to get it | Where it goes | Public? |
| --- | --- | --- | --- |
| Game server secret key | Creator panel → Integrations → Game Servers | `tebex secret <key>` in the server **console** | No — treat as a password |
| Headless API public token | Creator panel → Developers | `store/.env` (`VITE_TEBEX_WEBSTORE_TOKEN`) | Yes — read-only catalog access |
| Webhook secret | Creator panel → Developers → Webhooks | `webhook/.env` (`TEBEX_WEBHOOK_SECRET`) | No |

## How money flows

Players pay on Tebex's hosted checkout (cards, PayPal, and whatever methods
you enable). Tebex handles taxes/chargebacks as merchant of record and pays
out to you per their payout schedule — configure payout details and complete
seller verification in the creator panel under **Payments**.
