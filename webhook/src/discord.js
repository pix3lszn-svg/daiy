const DISCORD_WEBHOOK_URL = process.env.DISCORD_WEBHOOK_URL;

// Posts an embed to a Discord channel webhook. No-op when not configured.
export async function notifyDiscord(embed) {
  if (!DISCORD_WEBHOOK_URL) return;
  const res = await fetch(DISCORD_WEBHOOK_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ embeds: [embed] }),
  });
  if (!res.ok) {
    console.error(`Discord notification failed: ${res.status} ${await res.text()}`);
  }
}
