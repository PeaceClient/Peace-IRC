import { log } from "./util.js";

export type RegearOutcome = "done" | "not-in-tablist" | "failed";

export interface RegearBots {
  start(): Promise<void>;
  regear(username: string): Promise<RegearOutcome>;
  stop(): Promise<void>;
}

export type RequestResult =
  | { kind: "booting" }
  | { kind: "now" }
  | { kind: "queued"; ahead: number }
  | { kind: "full" };

export type SessionState = "cold" | "booting" | "ready" | "shuttingDown";

export interface SessionOptions {
  createBots: () => RegearBots;
  idleMs: number;
  queueMax: number;
}

export class BotSession {
  private readonly options: SessionOptions;
  private readonly queue: string[] = [];

  private bots: RegearBots | null = null;
  private currentState: SessionState = "cold";
  private draining = false;
  private idleTimer: NodeJS.Timeout | null = null;

  constructor(options: SessionOptions) {
    this.options = options;
  }

  get state(): SessionState {
    return this.currentState;
  }

  get queueLength(): number {
    return this.queue.length;
  }

  request(username: string): RequestResult {
    // Already waiting: report their spot, don't queue them twice.
    const existing = this.queue.indexOf(username);
    if (existing !== -1) {
      this.armIdleTimer();
      return { kind: "queued", ahead: existing };
    }

    if (this.queue.length >= this.options.queueMax) {
      return { kind: "full" };
    }

    this.queue.push(username);
    this.armIdleTimer();

    if (this.currentState === "cold") {
      void this.boot();
      return { kind: "booting" };
    }

    // Booting mid-shutdown would put four bots on one IP; shutdown() picks the queue up.
    if (this.currentState === "shuttingDown") {
      return { kind: "booting" };
    }

    if (this.currentState === "ready" && this.queue.length === 1 && !this.draining) {
      void this.drain();
      return { kind: "now" };
    }

    if (this.currentState === "ready") {
      void this.drain();
    }
    return { kind: "queued", ahead: this.queue.length - 1 };
  }

  async shutdown(): Promise<void> {
    if (this.currentState === "cold") {
      return;
    }
    this.currentState = "shuttingDown";
    this.clearIdleTimer();

    const bots = this.bots;
    this.bots = null;
    this.queue.length = 0;

    if (bots) {
      try {
        await bots.stop();
      } catch (error) {
        log(`Error stopping bots: ${String(error)}`);
      }
    }

    // The old pair is gone, so anything held back during shutdown can boot now.
    if (this.currentState === "shuttingDown") {
      this.currentState = "cold";
      if (this.queue.length > 0) {
        void this.boot();
      }
    }
  }

  private async boot(): Promise<void> {
    this.currentState = "booting";
    const bots = this.options.createBots();
    this.bots = bots;

    try {
      await bots.start();
    } catch (error) {
      log(`Bots failed to start: ${String(error)}`);
      // A leftover live connection must be closed even if a newer state has since taken over.
      try {
        await bots.stop();
      } catch (stopError) {
        log(`Error stopping bots after failed boot: ${String(stopError)}`);
      }
      // A shutdown (and possibly a fresh boot) may have moved on already; don't clobber it.
      if (this.currentState === "booting") {
        this.bots = null;
        this.queue.length = 0;
        this.clearIdleTimer();
        this.currentState = "cold";
      }
      return;
    }

    // A shutdown that landed mid-connect wins.
    if (this.currentState !== "booting") {
      return;
    }

    this.currentState = "ready";
    void this.drain();
  }

  private async drain(): Promise<void> {
    if (this.draining) {
      return;
    }
    this.draining = true;
    try {
      while (this.queue.length > 0 && this.currentState === "ready" && this.bots) {
        const username = this.queue[0];
        let outcome: RegearOutcome;
        try {
          outcome = await this.bots.regear(username);
        } catch (error) {
          log(`Regear of ${username} threw: ${String(error)}`);
          outcome = "failed";
        }
        this.queue.shift();
        log(`Regear of ${username}: ${outcome}`);
      }
    } finally {
      this.draining = false;
    }
  }

  private armIdleTimer(): void {
    this.clearIdleTimer();
    this.idleTimer = setTimeout(() => this.onIdle(), this.options.idleMs);
  }

  private clearIdleTimer(): void {
    if (this.idleTimer) {
      clearTimeout(this.idleTimer);
      this.idleTimer = null;
    }
  }

  private onIdle(): void {
    this.idleTimer = null;
    // Still busy: re-arm rather than cut the bots off mid-kit.
    if (this.queue.length > 0 || this.draining) {
      this.armIdleTimer();
      return;
    }
    log("Idle timeout reached, disconnecting bots");
    void this.shutdown();
  }
}
