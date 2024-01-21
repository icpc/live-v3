import { WEBSOCKETS } from "@/consts";
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
    websockets: Object.fromEntries(Object.keys(WEBSOCKETS).map((key) => {
        return [key, undefined];
    })),
};

export const statusSlice = createSlice({
    name: "status",
    initialState,
    reducers: {
        setWebsocketStatus: (state, action: PayloadAction<{socket: string, newStatus: WebsocketStatus}>) => {
            state[action.payload.socket] = action.payload.newStatus;
        }
    }
});

export const setWebsocketStatus = (wsName: string, newStatus: WebsocketStatus) => {
    return statusSlice.actions.setWebsocketStatus({ socket: wsName, newStatus });
};

export default statusSlice.reducer;
