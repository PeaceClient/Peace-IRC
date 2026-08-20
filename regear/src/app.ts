import express, { type NextFunction, type Request, type Response } from "express";
import { antibotState, clearBlocked } from "./antibot.js";
import { normalizeServer, type Config } from "./config.js";
import type { BotSession, RequestResult } from "./session.js";
import { log } from "./util.js";

const MAX_BODY = 200;

export function describeResult(result: RequestResult): { status: number; body: string } {
  switch (result.kind) {
    case "booting":
      return { status: 200, body: "Bots are connecting - your regear lands in about 40s." };
    case "now":
      return { status: 200, body: "Regearing you now." };
    case "queued":
      return { status: 200, body: `Queued - ${result.ahead} ahead of you.` };
    case "full":
      return { status: 503, body: "Regear queue is full, try again shortly." };
  }
}

function reply(res: Response, status: number, body: string): void {
  res.status(status)
    .set("Content-Type", "text/plain; charset=utf-8")
    .send(body.slice(0, MAX_BODY));
}

export function createApp(session: Pick<BotSession, "request">, config: Config): express.Express {
  const app = express();
  app.use(express.urlencoded({ extended: true }));

  app.get("/health", (_req: Request, res: Response) => {
    reply(res, 200, "ok");
  });

  app.get("/antibot", (_req: Request, res: Response) => {
    res.status(200).json(antibotState());
  });

  app.post("/callback", (req: Request, res: Response) => {
    const user = typeof req.body?.user === "string" ? req.body.user.trim() : "";
    const server = typeof req.body?.server === "string" ? req.body.server : "";
    const data = typeof req.body?.data === "string" ? req.body.data.trim() : "";

    if (!user || !server || !data) {
      reply(res, 400, "Malformed callback.");
      return;
    }

    const words = data.split(/\s+/);
    const command = words[0].toLowerCase();
    const forced = words.slice(1).some((word: string) => word.toLowerCase() === "force");
    if (command !== "regear") {
      reply(res, 400, `Unknown command: ${command}`);
      return;
    }

    if (normalizeServer(server) !== normalizeServer(config.mcHost)) {
      reply(res, 400, "Regear is not available on this server.");
      return;
    }

    if (forced) {
      clearBlocked();
    }

    if (antibotState().blocked) {
      reply(res, 503, "Bots are blocked by AntiBot - solve the captcha at notbot.es, then regear again.");
      log(`${user} on ${server}: ${command} -> 503 antibot blocked`);
      return;
    }

    const { status, body } = describeResult(session.request(user));
    log(`${user} on ${server}: ${command} -> ${status} ${body}`);
    reply(res, status, body);
  });

  app.use((error: Error, _req: Request, res: Response, _next: NextFunction) => {
    log(`Unhandled error: ${error.stack ?? error.message}`);
    reply(res, 500, "Regear service error.");
  });

  return app;
}
