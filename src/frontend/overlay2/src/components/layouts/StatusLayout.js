import React from "react";
import { DebugLog } from "../organisms/status/Log";
import { StatusLightbulbs } from "../organisms/status/StatusLightbulbs";
import { CSSTicker, JSTicker } from "../organisms/status/Tickers";

export const StatusLayout = () => {
    return <>
        <StatusLightbulbs/>
        <JSTicker/>
        <CSSTicker/>
        <DebugLog/>
    </>;
};
