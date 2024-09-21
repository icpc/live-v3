import { ContestInfo } from "../../generated/api"
import WebSocket from 'ws';
import * as fs from 'node:fs';
import * as path from 'node:path';

export const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

export const contestInfo = JSON.parse(fs.readFileSync(path.join(__dirname, "contestInfo.json")).toString("utf-8")) as ContestInfo;