export interface Config {
  port: number;
  mcHost: string;
  mcPort: number;
  mcVersion: string;
  idleMs: number;
  queueMax: number;
}

function number(env: NodeJS.ProcessEnv, key: string, fallback: number): number {
  const raw = env[key];
  if (raw === undefined || raw.trim() === "") {
    return fallback;
  }
  const value = Number(raw);
  if (!Number.isFinite(value)) {
    throw new Error(`${key} must be a number, got "${raw}"`);
  }
  return value;
}

function string(env: NodeJS.ProcessEnv, key: string, fallback: string): string {
  const raw = env[key];
  return raw === undefined || raw.trim() === "" ? fallback : raw.trim();
}

export function loadConfig(env: NodeJS.ProcessEnv = process.env): Config {
  return {
    port: number(env, "REGEAR_PORT", 3000),
    mcHost: string(env, "REGEAR_MC_HOST", "crystalpvp.cc"),
    mcPort: number(env, "REGEAR_MC_PORT", 25565),
    mcVersion: string(env, "REGEAR_MC_VERSION", "1.17"),
    idleMs: number(env, "REGEAR_IDLE_MS", 600_000),
    queueMax: number(env, "REGEAR_QUEUE_MAX", 10),
  };
}

export function normalizeServer(address: string): string {
  const trimmed = address.trim().toLowerCase().replace(/\.+$/, "");
  return trimmed.endsWith(":25565") ? trimmed.slice(0, -":25565".length) : trimmed;
}
