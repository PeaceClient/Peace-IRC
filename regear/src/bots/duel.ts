import { createClient, type Client } from "minecraft-protocol";
import type { Config } from "../config.js";
import { noteFailedLogin, noteKick } from "../antibot.js";
import { getPlainText, log, type ChatComponent } from "../util.js";

export interface DuelBot {
  readonly username: string;
  stop(): void;
}

export interface DuelBotOptions {
  config: Config;
  username: string;
  regearBotName: string;
  onLost?: () => void;
}

const DUEL_REQUEST = /^Duel request received from ([A-Za-z0-9_]+)\./;

export function createDuelBot(options: DuelBotOptions): DuelBot {
  const { config, username, regearBotName } = options;
  let stopped = false;
  let loggedIn = false;

  const client: Client = createClient({
    host: config.mcHost,
    port: config.mcPort,
    username,
    version: config.mcVersion,
    keepAlive: false,
  });

  client.on("keep_alive", (data) => client.write("keep_alive", data));
  client.on("ping", (data) => client.write("pong", data));
  client.on("resource_pack_send", () => client.write("resource_pack_receive", { result: 2 }));
  client.on("death_combat_event", () => client.write("client_command", { actionId: 0 }));

  client.on("login", (data) => {
    if (data.enableRespawnScreen) {
      client.write("client_command", { actionId: 0 });
    }
    loggedIn = true;
    log(`Duel bot ${username} logged in`);
  });

  client.on("chat", (data) => {
    let raw: string;
    try {
      raw = getPlainText(JSON.parse(data.message) as ChatComponent);
    } catch {
      return;
    }

    const match = raw.match(DUEL_REQUEST);
    if (match && match[1] === regearBotName) {
      client.chat(`/duel accept ${match[1]}`);
    }
  });

  client.on("error", (error) => log(`Duel bot error: ${String(error)}`));
  client.on("kick_disconnect", (data) => {
    log(`Duel bot kicked: ${JSON.stringify(data.reason)}`);
    noteKick(data.reason);
  });
  client.on("end", (reason) => {
    log(`Duel bot ended: ${reason}`);
    if (!stopped && !loggedIn) {
      noteFailedLogin();
    }
    if (!stopped) {
      options.onLost?.();
    }
  });

  return {
    username,
    stop() {
      stopped = true;
      try {
        client.end("stopping");
      } catch {
      }
    },
  };
}
