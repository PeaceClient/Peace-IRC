import { describe, expect, it } from "vitest";
import { loadConfig, normalizeServer } from "../src/config.js";

describe("loadConfig", () => {
  it("falls back to the documented defaults on an empty environment", () => {
    expect(loadConfig({})).toEqual({
      port: 3000,
      mcHost: "crystalpvp.cc",
      mcPort: 25565,
      mcVersion: "1.17",
      idleMs: 600_000,
      queueMax: 10,
    });
  });

  it("reads every value from the environment", () => {
    const config = loadConfig({
      REGEAR_PORT: "8080",
      REGEAR_MC_HOST: "example.net",
      REGEAR_MC_PORT: "25566",
      REGEAR_MC_VERSION: "1.18",
      REGEAR_IDLE_MS: "1000",
      REGEAR_QUEUE_MAX: "3",
    });
    expect(config).toEqual({
      port: 8080,
      mcHost: "example.net",
      mcPort: 25566,
      mcVersion: "1.18",
      idleMs: 1000,
      queueMax: 3,
    });
  });

  it("rejects a numeric value that is not a number", () => {
    expect(() => loadConfig({ REGEAR_PORT: "no" })).toThrow("REGEAR_PORT");
  });
});

describe("normalizeServer", () => {
  it("lowercases the address", () => {
    expect(normalizeServer("CrystalPvP.CC")).toBe("crystalpvp.cc");
  });

  it("drops a default port suffix", () => {
    expect(normalizeServer("crystalpvp.cc:25565")).toBe("crystalpvp.cc");
  });

  it("keeps a non-default port suffix", () => {
    expect(normalizeServer("crystalpvp.cc:25577")).toBe("crystalpvp.cc:25577");
  });

  it("trims whitespace and a trailing dot", () => {
    expect(normalizeServer("  crystalpvp.cc.  ")).toBe("crystalpvp.cc");
  });
});
