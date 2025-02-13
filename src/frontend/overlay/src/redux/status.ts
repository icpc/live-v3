import c from "../config";
import { createSlice, PayloadAction } from "@reduxjs/toolkit";

export enum WebsocketStatus {
    CONNECTING = 0,
    CONNECTED = 1,
    DISCONNECTED = -1
}

export interface StatusState {
    websockets: Record<string, WebsocketStatus>
}

const initialState: StatusState = {
    websockets: Object.fromEntries(Object.keys(c.WEBSOCKETS).map((key) => {
        return [key, undefined];
    })),
};

export const statusSlice = createSlice({
    name: "status",
    initialState,
    reducers: {
        setWebsocketStatus: {
            reducer(state, action: PayloadAction<{socket: string, newStatus: WebsocketStatus}>) {
                state.websockets[action.payload.socket] = action.payload.newStatus;
            },
            prepare(wsName, newStatus){
                return {
                    payload: {
                        socket: wsName,
                        newStatus
                    }
                };
            }
        }
    }
});

// export const setWebsocketStatus = (wsName: string, newStatus: WebsocketStatus) => {
//     return statusSlice.actions.setWebsocketStatus({ socket: wsName, newStatus });
// };

export const { setWebsocketStatus } = statusSlice.actions;

export default statusSlice.reducer;
