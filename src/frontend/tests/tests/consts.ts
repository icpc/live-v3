import { fileURLToPath } from "node:url";
import * as path from "node:path";
import {getPathParents} from "./utls.js";
import {TestLayout} from "./types.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
export const parents = getPathParents(__dirname);
export const repoDir = parents[4];

const simpleWidgets = ["queue", "scoreboard", "statistics"];
export const simpleLayouts: TestLayout[] = [{ "widgets": simpleWidgets.map(w => ({ path: w })) }]

export const backendHost = "127.0.0.1";
export const backendStartingPort = 8090;

export const backendStartCooldown = 10000;
export const backendFinishCooldown = 1000;
export const overlayDisplayDelay = 2000;
