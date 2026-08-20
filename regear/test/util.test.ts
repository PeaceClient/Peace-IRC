import { describe, expect, it } from "vitest";
import { getPlainText, randomName } from "../src/util.js";

describe("randomName", () => {
  it("produces a name of the requested length", () => {
    expect(randomName(10)).toHaveLength(10);
  });

  it("starts with a letter, as Minecraft names must", () => {
    for (let i = 0; i < 200; i++) {
      expect(randomName(10)[0]).toMatch(/[A-Za-z]/);
    }
  });

  it("uses only characters Minecraft allows in a name", () => {
    for (let i = 0; i < 200; i++) {
      expect(randomName(10)).toMatch(/^[A-Za-z][A-Za-z0-9_]*$/);
    }
  });

  it("does not repeat itself", () => {
    const names = new Set(Array.from({ length: 100 }, () => randomName(10)));
    expect(names.size).toBe(100);
  });
});

describe("getPlainText", () => {
  it("returns a bare string unchanged", () => {
    expect(getPlainText("hello")).toBe("hello");
  });

  it("flattens text and extras in order", () => {
    expect(
      getPlainText({ text: "Peace", extra: [{ text: " has joined" }, { text: " the game" }] }),
    ).toBe("Peace has joined the game");
  });

  it("recurses into nested extras", () => {
    expect(getPlainText({ extra: [{ text: "a", extra: [{ text: "b" }] }, { text: "c" }] })).toBe("abc");
  });

  it("returns an empty string for a component with no text", () => {
    expect(getPlainText({ translate: "chat.type.text" })).toBe("");
  });
});
