import * as fs from "node:fs";
import * as path from "node:path";
import { fileURLToPath } from "node:url";

export interface ChatComponent {
  text?: string;
  extra?: ChatComponent[];
  translate?: string;
  with?: ChatComponent[];
}

const FIRST_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
const CHARS = FIRST_CHARS + "0123456789_";

export function randomName(length = 10): string {
  let result = FIRST_CHARS[Math.floor(Math.random() * FIRST_CHARS.length)];
  for (let i = 1; i < length; i++) {
    result += CHARS[Math.floor(Math.random() * CHARS.length)];
  }
  return result;
}

export function getPlainText(component: ChatComponent | string): string {
  if (typeof component === "string") {
    return component;
  }
  if (!component || typeof component !== "object") {
    return "";
  }

  let result = component.text ?? "";
  if (Array.isArray(component.extra)) {
    for (const child of component.extra) {
      result += getPlainText(child);
    }
  }
  return result;
}

const packageDir = path.join(path.dirname(fileURLToPath(import.meta.url)), "..");
const logStream = fs.createWriteStream(path.join(packageDir, "latest.log"), { flags: "a" });
logStream.on("error", (error) => console.error("Log stream error:", error));

export function log(message: string): void {
  const line = `${new Date().toISOString()} -> ${message}`;
  console.log(line);
  logStream.write(line + "\n");
}
