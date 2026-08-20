import type { Config } from "../config.js";
import type { RegearBots, RegearOutcome } from "../session.js";
import { log, randomName } from "../util.js";
import { createDuelBot, type DuelBot } from "./duel.js";
import { createRegearBot, type RegearBot } from "./regear.js";

const PARTNER_DELAY_MS = 15_000;
const RECONNECT_DELAY_MS = 10_000;
const BOOT_TIMEOUT_MS = 90_000;

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export function createBotPair(config: Config): RegearBots {
  let regearBot: RegearBot | null = null;
  let duelBot: DuelBot | null = null;
  let broken = false;
  let stopped = false;

  function teardown(): void {
    regearBot?.stop();
    duelBot?.stop();
    regearBot = null;
    duelBot = null;
  }

  function onLost(): void {
    if (stopped || broken) {
      return;
    }
    broken = true;
    log("A bot was lost, the pair will be rebuilt on the next request");
    teardown();
  }

  async function boot(): Promise<void> {
    try {
      if (stopped) {
        throw new Error("pair stopped before booting");
      }

      const regearName = randomName(10);
      const duelName = randomName(10);
      log(`Booting pair: regear=${regearName} partner=${duelName}`);

      const bot = createRegearBot({ config, username: regearName, duelBotName: duelName, onLost });
      regearBot = bot;

      await sleep(PARTNER_DELAY_MS);

      if (stopped || broken || !regearBot) {
        teardown();
        throw new Error("pair stopped while booting");
      }

      duelBot = createDuelBot({ config, username: duelName, regearBotName: regearName, onLost });

      await Promise.race([
        bot.ready(),
        sleep(BOOT_TIMEOUT_MS).then(() => {
          throw new Error("boot timed out");
        }),
      ]);
      if (stopped || broken) {
        teardown();
        throw new Error("pair stopped while booting");
      }

      broken = false;
      log("Pair is ready");
    } catch (error) {
      teardown();
      broken = true;
      throw error;
    }
  }

  return {
    start: boot,

    async regear(username: string): Promise<RegearOutcome> {
      if (broken || !regearBot) {
        log("Rebuilding the pair before regearing");
        teardown();
        await sleep(RECONNECT_DELAY_MS);
        try {
          await boot();
        } catch (error) {
          log(`Rebuild failed: ${String(error)}`);
          teardown();
          broken = true;
          return "failed";
        }
      }
      const outcome = await regearBot!.regear(username);
      if (outcome === "failed") {
        broken = true;
      }
      return outcome;
    },

    async stop(): Promise<void> {
      log("Stopping pair");
      stopped = true;
      teardown();
      await sleep(RECONNECT_DELAY_MS);
    },
  };
}
