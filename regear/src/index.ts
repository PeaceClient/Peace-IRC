import { createApp } from "./app.js";
import { createBotPair } from "./bots/pair.js";
import { loadConfig } from "./config.js";
import { BotSession } from "./session.js";
import { log } from "./util.js";

const config = loadConfig();

const session = new BotSession({
  createBots: () => createBotPair(config),
  idleMs: config.idleMs,
  queueMax: config.queueMax,
});

const app = createApp(session, config);

app.listen(config.port, "127.0.0.1", () => {
  log(`Regear daemon listening on 127.0.0.1:${config.port}, serving ${config.mcHost}:${config.mcPort}`);
});

process.on("unhandledRejection", (reason) => {
  log(`Unhandled rejection: ${String(reason)}`);
});

process.on("uncaughtException", (error) => {
  log(`Uncaught exception: ${error.stack ?? error.message}`);
});

for (const signal of ["SIGINT", "SIGTERM"] as const) {
  process.on(signal, () => {
    log(`${signal} received, shutting down`);
    void session.shutdown().then(() => process.exit(0));
  });
}
