import { getPlainText, log, type ChatComponent } from "./util.js";

const BLOCK_TTL_MS = 60_000;

let blockedSince: string | null = null;
let blockedAt = 0;
let blockedReason = "";

const MARKERS = ["notbot.es", "antibot", "blacklisted"];

// kick reason sends as a chat component. sometimes as JSON string
export function kickText(reason: unknown): string {
  if (typeof reason === "string") {
    try {
      return getPlainText(JSON.parse(reason) as ChatComponent);
    } catch {
      return reason;
    }
  }
  return getPlainText(reason as ChatComponent);
}

export function looksLikeAntibot(text: string): boolean {
  const lower = text.toLowerCase();
  return MARKERS.some((marker) => lower.includes(marker));
}

export function noteKick(reason: unknown): void {
  const text = kickText(reason);
  if (!looksLikeAntibot(text)) {
    return;
  }
  if (!blockedSince) {
    log("AntiBot has blacklisted this IP - a captcha at notbot.es is needed");
  }
  blockedSince = new Date().toISOString();
  blockedAt = Date.now();
  blockedReason = text.replace(/\s+/g, " ").trim();
}

export function noteFailedLogin(): void {
  if (!blockedSince) {
    log("A bot was dropped before login - likely an IP-level AntiBot block");
  }
  blockedSince = new Date().toISOString();
  blockedAt = Date.now();
  blockedReason =
    "Dropped before login, with no kick message. Usually an IP-level AntiBot block - " +
    "complete the captcha at notbot.es.";
}

export function clearBlocked(): void {
  if (blockedSince) {
    log("AntiBot block cleared - a bot reached the ready stage");
  }
  blockedSince = null;
  blockedAt = 0;
  blockedReason = "";
}

export function antibotState(): { blocked: boolean; since: string | null; reason: string } {
  const blocked = blockedSince !== null && Date.now() - blockedAt < BLOCK_TTL_MS;
  return { blocked, since: blockedSince, reason: blockedReason };
}
