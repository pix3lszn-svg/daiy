import crypto from "node:crypto";

// Tebex signs webhooks as HMAC-SHA256(secret, SHA256(rawBody)) and sends the
// result in the X-Signature header.
// https://docs.tebex.io/developers/webhooks/overview#validating-webhooks
export function verifySignature(rawBody, signatureHeader, secret) {
  if (!signatureHeader || !secret) return false;
  const bodyHash = crypto.createHash("sha256").update(rawBody).digest("hex");
  const expected = crypto.createHmac("sha256", secret).update(bodyHash).digest("hex");
  const a = Buffer.from(expected, "utf8");
  const b = Buffer.from(signatureHeader, "utf8");
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

// Tebex only sends webhooks from these IPs (per their docs). Keep this in
// sync with https://docs.tebex.io/developers/webhooks/overview
export const TEBEX_WEBHOOK_IPS = new Set(["18.209.80.3", "54.87.231.232"]);

export function isAllowedIp(ip) {
  if (!ip) return false;
  // Normalise IPv4-mapped IPv6 addresses like ::ffff:18.209.80.3
  const normalised = ip.startsWith("::ffff:") ? ip.slice(7) : ip;
  return TEBEX_WEBHOOK_IPS.has(normalised);
}
