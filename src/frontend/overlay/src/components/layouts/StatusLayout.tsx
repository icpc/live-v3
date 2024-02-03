import React from "react";
import { FPSCounter } from "../organisms/status/FPSCounter";
import { DebugLog } from "../organisms/status/Log";
import { StatusLightBulbs } from "../organisms/status/StatusLightBulbs";
import { CSSTicker, JSTicker } from "../organisms/status/Tickers";

export const StatusLayout = () => {
    return <>
        <StatusLightBulbs/>
        <JSTicker/>
        <CSSTicker/>
        <FPSCounter/>
        <DebugLog/>
    </>;
};
