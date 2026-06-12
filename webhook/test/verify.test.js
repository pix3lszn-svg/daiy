import test from "node:test";
import assert from "node:assert/strict";
import crypto from "node:crypto";
import { isAllowedIp, verifySignature } from "../src/verify.js";

const SECRET = "test-secret";

function sign(rawBody, secret = SECRET) {
  const hash = crypto.createHash("sha256").update(rawBody).digest("hex");
  return crypto.createHmac("sha256", secret).update(hash).digest("hex");
}

test("accepts a correctly signed body", () => {
  const body = Buffer.from(JSON.stringify({ id: "evt_1", type: "payment.completed" }));
  assert.equal(verifySignature(body, sign(body), SECRET), true);
});

test("rejects a tampered body", () => {
  const body = Buffer.from(JSON.stringify({ id: "evt_1" }));
  const signature = sign(body);
  const tampered = Buffer.from(JSON.stringify({ id: "evt_2" }));
  assert.equal(verifySignature(tampered, signature, SECRET), false);
});

test("rejects a signature made with the wrong secret", () => {
  const body = Buffer.from("{}");
  assert.equal(verifySignature(body, sign(body, "other-secret"), SECRET), false);
});

test("rejects missing signature or secret", () => {
  const body = Buffer.from("{}");
  assert.equal(verifySignature(body, undefined, SECRET), false);
  assert.equal(verifySignature(body, sign(body), undefined), false);
});

test("allows only Tebex IPs, including IPv4-mapped IPv6", () => {
  assert.equal(isAllowedIp("18.209.80.3"), true);
  assert.equal(isAllowedIp("::ffff:54.87.231.232"), true);
  assert.equal(isAllowedIp("1.2.3.4"), false);
  assert.equal(isAllowedIp(undefined), false);
});
