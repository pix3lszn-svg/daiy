import express from "express";
import { isAllowedIp, verifySignature } from "./verify.js";
import { notifyDiscord } from "./discord.js";

const PORT = Number(process.env.PORT ?? 8080);
const SECRET = process.env.TEBEX_WEBHOOK_SECRET;

if (!SECRET) {
  console.error("TEBEX_WEBHOOK_SECRET is not set. Copy .env.example to .env and fill it in.");
  process.exit(1);
}

const app = express();
if (process.env.TRUST_PROXY === "true") app.set("trust proxy", true);

// Keep the raw body around — the signature is computed over the exact bytes.
app.use(express.json({ verify: (req, _res, buf) => (req.rawBody = buf) }));

app.get("/health", (_req, res) => res.json({ ok: true }));

app.post("/webhooks/tebex", async (req, res) => {
  if (!isAllowedIp(req.ip)) {
    console.warn(`Rejected webhook from unexpected IP ${req.ip}`);
    return res.status(403).json({ error: "forbidden" });
  }
  if (!verifySignature(req.rawBody, req.get("X-Signature"), SECRET)) {
    console.warn("Rejected webhook with bad signature");
    return res.status(401).json({ error: "invalid signature" });
  }

  const event = req.body;

  // Tebex sends this once when you register the endpoint; it expects the
  // event id echoed back to prove you control the URL.
  if (event.type === "validation.webhook") {
    return res.json({ id: event.id });
  }

  // Acknowledge immediately; do follow-up work after responding so Tebex
  // doesn't retry on slow downstream calls.
  res.json({ id: event.id });

  try {
    await handleEvent(event);
  } catch (err) {
    console.error(`Error handling ${event.type} (${event.id}):`, err);
  }
});

async function handleEvent(event) {
  const payment = event.subject;
  const username = payment?.customer?.username?.username ?? "unknown player";
  const amount = payment?.price ? `${payment.price.amount} ${payment.price.currency}` : "?";
  const products = (payment?.products ?? []).map((p) => p.name).join(", ") || "—";

  switch (event.type) {
    case "payment.completed":
      console.log(`✅ ${username} paid ${amount} for: ${products} (txn ${payment.transaction_id})`);
      await notifyDiscord({
        title: "New purchase! 🎉",
        color: 0x4ade80,
        description: `**${username}** bought **${products}** for **${amount}**`,
      });
      break;

    case "payment.declined":
      console.log(`❌ Declined payment from ${username} (${amount})`);
      break;

    case "payment.refunded":
      console.log(`↩️ Refund for ${username}: ${amount} (txn ${payment.transaction_id})`);
      await notifyDiscord({
        title: "Payment refunded",
        color: 0xfacc15,
        description: `Refunded **${amount}** to **${username}** (${products})`,
      });
      break;

    case "payment.dispute.opened":
      console.log(`⚠️ Chargeback opened by ${username} (txn ${payment.transaction_id})`);
      await notifyDiscord({
        title: "⚠️ Chargeback opened",
        color: 0xf87171,
        description: `**${username}** disputed **${amount}** (txn \`${payment.transaction_id}\`)`,
      });
      break;

    case "recurring-payment.renewed":
      console.log(`🔁 Subscription renewed by ${username} (${amount})`);
      break;

    case "recurring-payment.cancellation.requested":
      console.log(`🛑 ${username} cancelled their subscription`);
      break;

    default:
      console.log(`Unhandled event type ${event.type} (${event.id})`);
  }
}

app.listen(PORT, () => {
  console.log(`Tebex webhook listener running on http://localhost:${PORT}/webhooks/tebex`);
});
