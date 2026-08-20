import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { BotSession, type RegearBots, type RegearOutcome } from "../src/session.js";

function fakeBots() {
  const regeared: string[] = [];
  let resolveStart: (() => void) | undefined;
  let rejectStart: ((error: Error) => void) | undefined;
  let resolveRegear: ((outcome: RegearOutcome) => void) | undefined;
  let resolveStop: (() => void) | undefined;
  let blocking = false;
  let stopped = 0;

  const bots: RegearBots = {
    start: () =>
      new Promise<void>((resolve, reject) => {
        resolveStart = resolve;
        rejectStart = reject;
      }),
    regear: (username) => {
      regeared.push(username);
      return new Promise<RegearOutcome>((resolve) => {
        resolveRegear = resolve;
      });
    },
    stop: () => {
      stopped++;
      if (!blocking) {
        return Promise.resolve();
      }
      return new Promise<void>((resolve) => {
        resolveStop = resolve;
      });
    },
  };

  return {
    bots,
    regeared,
    stopCount: () => stopped,
    finishStart: () => resolveStart!(),
    failStart: () => rejectStart!(new Error("connection refused")),
    finishRegear: (outcome: RegearOutcome = "done") => resolveRegear!(outcome),
    blockStop: () => {
      blocking = true;
    },
    finishStop: () => resolveStop!(),
  };
}

function makeSession(overrides: Partial<{ idleMs: number; queueMax: number }> = {}) {
  const fake = fakeBots();
  const session = new BotSession({
    createBots: () => fake.bots,
    idleMs: 600_000,
    queueMax: 10,
    ...overrides,
  });
  return { session, fake };
}

beforeEach(() => vi.useFakeTimers());
afterEach(() => vi.useRealTimers());

describe("booting", () => {
  it("tells the first requester the bots are connecting", () => {
    const { session } = makeSession();
    expect(session.request("Alice")).toEqual({ kind: "booting" });
    expect(session.state).toBe("booting");
  });

  it("queues requests that arrive while booting", () => {
    const { session } = makeSession();
    session.request("Alice");
    expect(session.request("Bob")).toEqual({ kind: "queued", ahead: 1 });
  });

  it("drains the queue in order once the bots are ready", async () => {
    const { session, fake } = makeSession();
    session.request("Alice");
    session.request("Bob");

    fake.finishStart();
    await vi.advanceTimersByTimeAsync(0);
    expect(session.state).toBe("ready");
    expect(fake.regeared).toEqual(["Alice"]);

    fake.finishRegear("done");
    await vi.advanceTimersByTimeAsync(0);
    expect(fake.regeared).toEqual(["Alice", "Bob"]);
  });

  it("goes back to cold and drops the queue when the boot fails", async () => {
    const { session, fake } = makeSession();
    session.request("Alice");
    fake.failStart();
    await vi.advanceTimersByTimeAsync(0);

    expect(session.state).toBe("cold");
    expect(session.queueLength).toBe(0);
  });

  it("retries the boot on the next request after a failure", async () => {
    const { session, fake } = makeSession();
    session.request("Alice");
    fake.failStart();
    await vi.advanceTimersByTimeAsync(0);

    expect(session.request("Bob")).toEqual({ kind: "booting" });
  });

  it("stops the bots on a failed start before dropping the reference", async () => {
    const { session, fake } = makeSession();
    session.request("Alice");
    fake.failStart();
    await vi.advanceTimersByTimeAsync(0);

    expect(fake.stopCount()).toBe(1);
  });

  it("does not let a stale boot failure clobber a state that moved on while it was stopping its bots", async () => {
    function delayedFake() {
      let rejectStart: ((error: Error) => void) | undefined;
      const stopResolvers: Array<() => void> = [];
      let stopCount = 0;
      const bots: RegearBots = {
        start: () => new Promise<void>((_resolve, reject) => (rejectStart = reject)),
        regear: () => new Promise<RegearOutcome>(() => {}),
        stop: () => {
          stopCount++;
          return new Promise<void>((resolve) => stopResolvers.push(resolve));
        },
      };
      return {
        bots,
        failStart: () => rejectStart!(new Error("connection refused")),
        stopCount: () => stopCount,
        resolveNextStop: () => stopResolvers.shift()!(),
      };
    }

    const gen1 = delayedFake();
    const session = new BotSession({
      createBots: () => gen1.bots,
      idleMs: 600_000,
      queueMax: 10,
    });

    session.request("Alice");
    gen1.failStart();
    await vi.advanceTimersByTimeAsync(0);
    expect(session.state).toBe("booting");
    expect(gen1.stopCount()).toBe(1);

    void session.shutdown();
    await vi.advanceTimersByTimeAsync(0);
    expect(session.state).toBe("shuttingDown");
    expect(gen1.stopCount()).toBe(2);

    gen1.resolveNextStop();
    await vi.advanceTimersByTimeAsync(0);
    expect(session.state).toBe("shuttingDown");

    gen1.resolveNextStop();
    await vi.advanceTimersByTimeAsync(0);
    expect(session.state).toBe("cold");
  });
});

describe("a ready session", () => {
  async function readySession(overrides = {}) {
    const { session, fake } = makeSession(overrides);
    session.request("Warmup");
    fake.finishStart();
    await vi.advanceTimersByTimeAsync(0);
    fake.finishRegear("done");
    await vi.advanceTimersByTimeAsync(0);
    return { session, fake };
  }

  it("regears an idle-session requester immediately", async () => {
    const { session } = await readySession();
    expect(session.request("Alice")).toEqual({ kind: "now" });
  });

  it("reports how many are ahead of a queued requester", async () => {
    const { session } = await readySession();
    session.request("Alice");
    expect(session.request("Bob")).toEqual({ kind: "queued", ahead: 1 });
    expect(session.request("Carol")).toEqual({ kind: "queued", ahead: 2 });
  });

  it("returns the existing position rather than queueing someone twice", async () => {
    const { session } = await readySession();
    session.request("Alice");
    session.request("Bob");
    expect(session.request("Bob")).toEqual({ kind: "queued", ahead: 1 });
    expect(session.queueLength).toBe(2);
  });

  it("refuses a request once the queue is full", async () => {
    const { session } = await readySession({ queueMax: 2 });
    session.request("Alice");
    session.request("Bob");
    expect(session.request("Carol")).toEqual({ kind: "full" });
  });
});

describe("repeat requests", () => {
  it("lets the same user regear again as soon as the bots are ready", async () => {
    const { session, fake } = makeSession();
    session.request("Alice");
    fake.finishStart();
    await vi.advanceTimersByTimeAsync(0);
    fake.finishRegear("done");
    await vi.advanceTimersByTimeAsync(0);

    // No timers advanced: there is no waiting period between regears.
    expect(session.request("Alice")).toEqual({ kind: "now" });
  });
});

describe("the idle timeout", () => {
  async function idleReadySession(idleMs: number) {
    const { session, fake } = makeSession({ idleMs });
    session.request("Warmup");
    fake.finishStart();
    await vi.advanceTimersByTimeAsync(0);
    fake.finishRegear("done");
    await vi.advanceTimersByTimeAsync(0);
    return { session, fake };
  }

  it("shuts the bots down once nothing has been asked for the idle period", async () => {
    const { session, fake } = await idleReadySession(600_000);
    await vi.advanceTimersByTimeAsync(600_000);

    expect(session.state).toBe("cold");
    expect(fake.stopCount()).toBe(1);
  });

  it("is reset by every request", async () => {
    const { session, fake } = await idleReadySession(600_000);
    await vi.advanceTimersByTimeAsync(400_000);

    session.request("Alice");
    fake.finishRegear("done");
    await vi.advanceTimersByTimeAsync(400_000);
    expect(session.state).toBe("ready");

    await vi.advanceTimersByTimeAsync(200_000);
    expect(session.state).toBe("cold");
  });

  it("does not fire while a regear is still running", async () => {
    const { session, fake } = await idleReadySession(600_000);
    session.request("Alice");

    await vi.advanceTimersByTimeAsync(600_000);
    expect(session.state).toBe("ready");

    fake.finishRegear("done");
    await vi.advanceTimersByTimeAsync(600_000);
    expect(session.state).toBe("cold");
  });

  it("boots again after having gone cold", async () => {
    const { session } = await idleReadySession(600_000);
    await vi.advanceTimersByTimeAsync(600_000);

    expect(session.request("Alice")).toEqual({ kind: "booting" });
    expect(session.state).toBe("booting");
  });

  it("holds a request that lands mid-shutdown and boots for it once the pair is gone", async () => {
    const { session, fake } = await idleReadySession(600_000);
    fake.blockStop();
    await vi.advanceTimersByTimeAsync(600_000);
    expect(session.state).toBe("shuttingDown");

    expect(session.request("Alice")).toEqual({ kind: "booting" });
    expect(session.state).toBe("shuttingDown");

    fake.finishStop();
    await vi.advanceTimersByTimeAsync(0);
    expect(session.state).toBe("booting");
    expect(session.queueLength).toBe(1);
  });
});
