import { createClient, type Client } from "minecraft-protocol";
import type { Config } from "../config.js";
import type { RegearOutcome } from "../session.js";
import { clearBlocked, noteFailedLogin, noteKick } from "../antibot.js";
import { getPlainText, log, type ChatComponent } from "../util.js";

const DELAY_TICKS = 100;
const MAX_FAIL_TICKS = 150;
const REGEAR_TIMEOUT_MS = 120_000;
const REGEAR_WAIT_MS = 15_000;
const REGEAR_POLL_MS = 250;
const CONFIRM_ITEM_ID = 421;
const CONFIRM_SLOT = 1;

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

enum Stage {
  None,
  Delay,
  Duel,
  Confirm,
  Kill,
  SpectateAndWarp,
  Wait,
  DropAll,
}

export interface RegearBot {
  readonly username: string;
  ready(): Promise<void>;
  regear(target: string): Promise<RegearOutcome>;
  stop(): void;
}

export interface RegearBotOptions {
  config: Config;
  username: string;
  duelBotName: string;
  onLost?: () => void;
}

const JOIN = /([A-Za-z0-9_]+) has joined the game/;

export function createRegearBot(options: RegearBotOptions): RegearBot {
  const { config, username, duelBotName } = options;

  let stage = Stage.None;
  let cooldown = 0;
  let failCounter = 0;
  let stopped = false;
  let ended = false;
  let loggedIn = false;
  let tickInterval: NodeJS.Timeout | undefined;
  const pending: NodeJS.Timeout[] = [];
  const tabList = new Map<string, string>();

  let resolveReady: (() => void) | undefined;
  let rejectReady: ((error: Error) => void) | undefined;
  const readyPromise = new Promise<void>((resolve, reject) => {
    resolveReady = resolve;
    rejectReady = reject;
  });
  // Nothing awaits this until boot() clears its 15s sleep, so a kick before then would be an
  // unhandled rejection - which kills the process on modern Node.
  readyPromise.catch(() => {});
  let readySettled = false;

  let finishRegear: ((outcome: RegearOutcome) => void) | undefined;

  const client: Client = createClient({
    host: config.mcHost,
    port: config.mcPort,
    username,
    version: config.mcVersion,
    keepAlive: false,
  });

  function schedule(fn: () => void, delayMs: number): void {
    const timeout = setTimeout(() => {
      const index = pending.indexOf(timeout);
      if (index !== -1) {
        pending.splice(index, 1);
      }
      fn();
    }, delayMs);
    pending.push(timeout);
  }

  function clearPending(): void {
    for (const timeout of pending) {
      clearTimeout(timeout);
    }
    pending.length = 0;
  }

  function switchStage(next: Stage): void {
    stage = next;
    failCounter = 0;

    if (next === Stage.Wait) {
      clearBlocked();
      if (!readySettled) {
        readySettled = true;
        resolveReady?.();
      }
      // Back at Wait: the kit was dropped and a fresh one is in hand.
      finishRegear?.("done");
      finishRegear = undefined;
    }
  }

  function startChain(): void {
    switchStage(Stage.Delay);
    cooldown = DELAY_TICKS;
  }

  client.on("keep_alive", (data) => client.write("keep_alive", data));
  client.on("ping", (data) => client.write("pong", data));
  client.on("resource_pack_send", () => client.write("resource_pack_receive", { result: 2 }));
  client.on("death_combat_event", () => client.write("client_command", { actionId: 0 }));

  client.on("login", (data) => {
    if (data.enableRespawnScreen) {
      client.write("client_command", { actionId: 0 });
    }
    loggedIn = true;
    log(`Regear bot ${username} logged in`);
  });

  client.once("position", () => {
    tickInterval = setInterval(tick, 50);
  });

  client.on("player_info", (data) => {
    for (const entry of data.data) {
      if (!entry.name || !entry.uuid) {
        continue;
      }
      if (data.action === "add_player") {
        tabList.set(entry.name, entry.uuid);
        // On a reconnect there is no join message to see, only this entry.
        if (entry.name === duelBotName && stage === Stage.None) {
          startChain();
        }
      }
      if (data.action === "remove_player") {
        tabList.delete(entry.name);
      }
    }
  });

  client.on("window_items", (data) => {
    if (stage !== Stage.Confirm) {
      return;
    }
    const item = data.items[CONFIRM_SLOT];
    if (!item || !item.present || item.itemId !== CONFIRM_ITEM_ID) {
      return;
    }

    client.write("window_click", {
      windowId: data.windowId,
      stateId: data.stateId,
      slot: CONFIRM_SLOT,
      mouseButton: 0,
      mode: 0,
      changedSlots: [],
      cursorItem: { present: false },
    });
    switchStage(Stage.Kill);
    cooldown = 10;
  });

  client.on("chat", (data) => {
    let raw: string;
    try {
      raw = getPlainText(JSON.parse(data.message) as ChatComponent);
    } catch {
      return;
    }

    const join = raw.match(JOIN);
    if (join && join[1] === duelBotName && stage === Stage.None) {
      log("Partner bot joined, starting the duel chain");
      startChain();
    }
  });

  client.on("error", (error) => log(`Regear bot error: ${String(error)}`));
  client.on("kick_disconnect", (data) => {
    log(`Regear bot kicked: ${JSON.stringify(data.reason)}`);
    noteKick(data.reason);
  });
  client.on("end", (reason) => {
    ended = true;
    log(`Regear bot ended: ${reason}`);
    if (!stopped && !loggedIn) {
      noteFailedLogin();
    }
    clearPending();
    if (tickInterval) {
      clearInterval(tickInterval);
      tickInterval = undefined;
    }
    stage = Stage.None;

    finishRegear?.("failed");
    finishRegear = undefined;
    if (!readySettled) {
      readySettled = true;
      rejectReady?.(new Error(`regear bot disconnected before it was ready: ${reason}`));
    }
    if (!stopped) {
      options.onLost?.();
    }
  });

  function tick(): void {
    if (cooldown > 0) {
      cooldown--;
      return;
    }

    // A stage stuck this long has lost track of where it is; rebuild the chain.
    if (stage !== Stage.Wait && stage !== Stage.Delay && stage !== Stage.None) {
      failCounter++;
      if (failCounter >= MAX_FAIL_TICKS) {
        log("Stage timed out, restarting the chain");
        client.chat("/kill");
        clearPending();
        switchStage(Stage.Duel);
        cooldown = DELAY_TICKS;
        return;
      }
    }

    switch (stage) {
      case Stage.Duel:
        if (!tabList.has(duelBotName)) {
          log("Partner bot is not online, waiting");
          switchStage(Stage.Delay);
          cooldown = DELAY_TICKS;
          return;
        }
        client.chat(`/duel ${duelBotName}`);
        switchStage(Stage.Confirm);
        break;

      case Stage.Kill:
        client.chat("/kill");
        schedule(() => client.chat("/kit Reformed08"), 2_000);
        schedule(() => {
          client.chat(`/spectate ${duelBotName}`);
          schedule(() => {
            client.chat("/kitcreator");
            switchStage(Stage.Wait);
            cooldown = 10;
          }, 600);
        }, 4_900);
        switchStage(Stage.SpectateAndWarp);
        break;

      case Stage.DropAll:
        // The kill happens while spectating the requester, which is what puts the kit on them.
        client.chat("/kill");
        switchStage(Stage.Delay);
        cooldown = DELAY_TICKS;
        break;

      case Stage.Delay:
        switchStage(Stage.Duel);
        break;

      case Stage.Confirm:
      case Stage.SpectateAndWarp:
      case Stage.Wait:
      case Stage.None:
        break;
    }
  }

  return {
    username,

    ready(): Promise<void> {
      return readyPromise;
    },

    async regear(target: string): Promise<RegearOutcome> {
      // Straight after a boot the chain can still be finishing, and the tab list can lag the
      // player. Waiting briefly is what stops a first request being wasted.
      const deadline = Date.now() + REGEAR_WAIT_MS;
      while (stage !== Stage.Wait || !tabList.has(target)) {
        if (stopped || ended) {
          return "failed";
        }
        if (Date.now() >= deadline) {
          // No UUID means nobody to spectate, so nowhere to drop the kit.
          return stage === Stage.Wait ? "not-in-tablist" : "failed";
        }
        await sleep(REGEAR_POLL_MS);
      }

      const uuid = tabList.get(target)!;

      return new Promise<RegearOutcome>((resolve) => {
        const timeout = setTimeout(() => {
          if (finishRegear) {
            finishRegear = undefined;
            log(`Regear of ${target} timed out`);
            resolve("failed");
          }
        }, REGEAR_TIMEOUT_MS);

        finishRegear = (outcome) => {
          clearTimeout(timeout);
          resolve(outcome);
        };

        log(`Regearing ${target}`);
        client.write("spectate", { target: uuid });
        switchStage(Stage.DropAll);
        cooldown = 1;
      });
    },

    stop() {
      stopped = true;
      clearPending();
      if (tickInterval) {
        clearInterval(tickInterval);
        tickInterval = undefined;
      }
      try {
        client.end("stopping");
      } catch {
        // Already gone.
      }
    },
  };
}
