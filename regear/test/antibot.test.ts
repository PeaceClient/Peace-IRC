import { afterEach, describe, expect, it, vi } from "vitest";
import {
  antibotState,
  clearBlocked,
  kickText,
  looksLikeAntibot,
  noteFailedLogin,
  noteKick,
} from "../src/antibot.js";

const REAL_KICK = JSON.stringify({
  extra: [
    {
      extra: [
        { color: "dark_gray", text: "« " },
        { color: "aqua", text: "Crystal" },
        { color: "light_purple", text: "PvP " },
        { color: "gray", text: "Your connection has been analyzed!\n" },
        { color: "aqua", text: "Your IP is blacklisted by the AntiBot system.\n\n" },
        { color: "gray", text: "Do you want to play right now? Complete the Google Captcha at:\n" },
        { color: "light_purple", text: "www.notbot.es" },
      ],
      text: "",
    },
  ],
  text: "",
});

afterEach(() => {
  clearBlocked();
  vi.useRealTimers();
});

describe("kickText", () => {
  it("flattens a JSON string holding chat components", () => {
    expect(kickText(REAL_KICK)).toContain("Your IP is blacklisted by the AntiBot system.");
  });

  it("flattens a component object", () => {
    expect(kickText({ text: "You are ", extra: [{ text: "banned" }] })).toBe("You are banned");
  });

  it("passes a plain string through", () => {
    expect(kickText("Kicked for spamming")).toBe("Kicked for spamming");
  });
});

describe("looksLikeAntibot", () => {
  it("recognises the real kick", () => {
    expect(looksLikeAntibot(kickText(REAL_KICK))).toBe(true);
  });

  it("does not fire on an ordinary kick", () => {
    expect(looksLikeAntibot("You were kicked for flying")).toBe(false);
  });
});

describe("the blocked flag", () => {
  it("starts clear", () => {
    expect(antibotState().blocked).toBe(false);
  });

  it("is raised by an antibot kick, with the reason kept on one line", () => {
    noteKick(REAL_KICK);
    const state = antibotState();

    expect(state.blocked).toBe(true);
    expect(state.since).not.toBeNull();
    expect(state.reason).toContain("notbot.es");
    expect(state.reason).not.toContain("\n");
  });

  it("is not raised by an ordinary kick", () => {
    noteKick("Kicked for idling");
    expect(antibotState().blocked).toBe(false);
  });

  it("is raised by a drop before login, which carries no kick text at all", () => {
    noteFailedLogin();
    const state = antibotState();

    expect(state.blocked).toBe(true);
    expect(state.reason).toContain("notbot.es");
  });

  it("stays blocked when a kick is followed by a silent drop", () => {
    noteKick(REAL_KICK);
    noteFailedLogin();

    expect(antibotState().blocked).toBe(true);
    expect(antibotState().reason).toContain("Dropped before login");
  });

  it("lapses on its own, so a solved captcha is not locked out by a stale flag", () => {
    vi.useFakeTimers();
    noteKick(REAL_KICK);
    expect(antibotState().blocked).toBe(true);

    vi.advanceTimersByTime(60_000);
    expect(antibotState().blocked).toBe(false);
  });

  it("is cleared once a bot gets all the way in", () => {
    noteKick(REAL_KICK);
    clearBlocked();
    expect(antibotState()).toEqual({ blocked: false, since: null, reason: "" });
  });
});
