import _ from "lodash";
import { DateTime } from "luxon";
import c from "../config";
import { DEBUG } from "@/consts";
import { createSlice, PayloadAction } from "@reduxjs/toolkit";

export interface LogEntry {
    text: string;
    timestamp: string;
}

export interface DebugState {
    log: LogEntry[];
    enabled: boolean;
}

const initialState: DebugState = {
    log: [],
    enabled: DEBUG,
};

export const debugSlice = createSlice({
    name: "debug",
    initialState,
    reducers: {
        pushLog: (state, action: PayloadAction<string>) => {
            if (state.enabled) {
                state.log.push({
                    text: _.truncate(action.payload, { length: 100 }),
                    timestamp: DateTime.now().toLocaleString(
                        DateTime.TIME_24_WITH_SECONDS,
                    ),
                });
                if (state.log.length >= c.LOG_LINES) {
                    state.log.shift();
                }
            } else {
                // console.log(action.payload);
            }
        },
        clearLog: (state) => {
            state.log = [];
        },
    },
});

export const { pushLog, clearLog } = debugSlice.actions;

export default debugSlice.reducer;
