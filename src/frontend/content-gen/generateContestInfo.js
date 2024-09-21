"use strict";

import WebSocket from "ws";
import * as fs from "node:fs"
import * as path from "node:path";
export const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

const contestInfo = await new Promise((resolve, reject) => {
    console.log("Opening websocket")
    const websocket = new WebSocket(`${BACKEND_URL.replace('http', 'ws')}/api/overlay/contestInfo`)
    websocket.onmessage = (message) => {
        resolve(JSON.parse(message.data));
        websocket.close()
    }
})

fs.writeFileSync("contestInfo.json", JSON.stringify(contestInfo))