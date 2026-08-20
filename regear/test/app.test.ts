import { describe, expect, it } from "vitest";
import request from "supertest";
import { antibotState, clearBlocked, noteKick } from "../src/antibot.js";
import { createApp } from "../src/app.js";
import { loadConfig } from "../src/config.js";
import type { RequestResult } from "../src/session.js";

const config = loadConfig({});

function appReturning(result: RequestResult, seen: string[] = []) {
  return createApp(
    {
      request: (username: string) => {
        seen.push(username);
        return result;
      },
    },
    config,
  );
}

function form(fields: Record<string, string>) {
  return new URLSearchParams(fields).toString();
}

function post(app: ReturnType<typeof createApp>, fields: Record<string, string>) {
  return request(app)
    .post("/callback")
    .set("Content-Type", "application/x-www-form-urlencoded")
    .send(form(fields));
}

describe("POST /callback", () => {
  it("accepts a regear and reports the booting status", async () => {
    const seen: string[] = [];
    const response = await post(appReturning({ kind: "booting" }, seen), {
      user: "Alice",
      server: "crystalpvp.cc",
      data: "regear",
    });

    expect(response.status).toBe(200);
    expect(response.headers["content-type"]).toMatch(/text\/plain/);
    expect(response.text).toBe("Bots are connecting - your regear lands in about 40s.");
    expect(seen).toEqual(["Alice"]);
  });

  it("reports an immediate regear", async () => {
    const response = await post(appReturning({ kind: "now" }), {
      user: "Alice",
      server: "crystalpvp.cc",
      data: "regear",
    });
    expect(response.status).toBe(200);
    expect(response.text).toBe("Regearing you now.");
  });

  it("reports a queue position", async () => {
    const response = await post(appReturning({ kind: "queued", ahead: 2 }), {
      user: "Alice",
      server: "crystalpvp.cc",
      data: "regear",
    });
    expect(response.status).toBe(200);
    expect(response.text).toBe("Queued - 2 ahead of you.");
  });

  it("reports a full queue", async () => {
    const response = await post(appReturning({ kind: "full" }), {
      user: "Alice",
      server: "crystalpvp.cc",
      data: "regear",
    });
    expect(response.status).toBe(503);
    expect(response.text).toBe("Regear queue is full, try again shortly.");
  });

  it("matches the server host case-insensitively and ignoring the default port", async () => {
    for (const server of ["CrystalPvP.CC", "crystalpvp.cc:25565", " crystalpvp.cc "]) {
      const response = await post(appReturning({ kind: "now" }), {
        user: "Alice",
        server,
        data: "regear",
      });
      expect(response.status, server).toBe(200);
    }
  });

  it("refuses a request from another server", async () => {
    const seen: string[] = [];
    const response = await post(appReturning({ kind: "now" }, seen), {
      user: "Alice",
      server: "hypixel.net",
      data: "regear",
    });

    expect(response.status).toBe(400);
    expect(response.text).toBe("Regear is not available on this server.");
    expect(seen).toEqual([]);
  });

  it("refuses an unknown command", async () => {
    const response = await post(appReturning({ kind: "now" }), {
      user: "Alice",
      server: "crystalpvp.cc",
      data: "explode",
    });
    expect(response.status).toBe(400);
    expect(response.text).toBe("Unknown command: explode");
  });

  it("does not echo an oversized unknown command back into chat", async () => {
    const response = await post(appReturning({ kind: "now" }), {
      user: "Alice",
      server: "crystalpvp.cc",
      data: "x".repeat(500),
    });
    expect(response.status).toBe(400);
    expect(response.text.length).toBeLessThanOrEqual(200);
  });

  it("ignores case and surrounding space in the command", async () => {
    const response = await post(appReturning({ kind: "now" }), {
      user: "Alice",
      server: "crystalpvp.cc",
      data: "  ReGear  ",
    });
    expect(response.status).toBe(200);
    expect(response.text).toBe("Regearing you now.");
  });

  it("refuses a request with no user", async () => {
    const response = await post(appReturning({ kind: "now" }), {
      server: "crystalpvp.cc",
      data: "regear",
    });
    expect(response.status).toBe(400);
    expect(response.text).toBe("Malformed callback.");
  });

  it("refuses a regear while the IP is blacklisted, without touching the session", async () => {
    const seen: string[] = [];
    noteKick('{"text":"Your IP is blacklisted by the AntiBot system. www.notbot.es"}');
    try {
      const response = await post(appReturning({ kind: "now" }, seen), {
        user: "Alice",
        server: "crystalpvp.cc",
        data: "regear",
      });

      expect(response.status).toBe(503);
      expect(response.text).toBe("Bots are blocked by AntiBot - solve the captcha at notbot.es, then regear again.");

      expect(seen).toEqual([]);
    } finally {
      clearBlocked();
    }
  });

  it("lets a forced regear through the antibot gate and clears the flag", async () => {
    const seen: string[] = [];
    noteKick('{"text":"Your IP is blacklisted by the AntiBot system. www.notbot.es"}');
    try {
      const response = await post(appReturning({ kind: "now" }, seen), {
        user: "Alice",
        server: "crystalpvp.cc",
        data: "regear force",
      });

      expect(response.status).toBe(200);
      expect(response.text).toBe("Regearing you now.");
      expect(seen).toEqual(["Alice"]);
      expect(antibotState().blocked).toBe(false);
    } finally {
      clearBlocked();
    }
  });

  it("reports the antibot state as JSON for the captcha helper", async () => {
    const response = await request(appReturning({ kind: "now" })).get("/antibot");
    expect(response.status).toBe(200);
    expect(response.body).toEqual(antibotState());
  });

  it("answers a health check", async () => {
    const response = await request(appReturning({ kind: "now" })).get("/health");
    expect(response.status).toBe(200);
  });
});
