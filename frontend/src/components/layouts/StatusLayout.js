import React from "react";
import { DebugLog } from "../molecules/status/Log";
import { StatusLightbulbs } from "../molecules/status/StatusLightbulbs";

export const StatusLayout = () => {
    return <>
        <StatusLightbulbs/>
        <DebugLog/>
    </>;
};
