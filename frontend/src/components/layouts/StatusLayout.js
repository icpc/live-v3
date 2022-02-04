import React from "react";
import { DebugLog } from "../molecules/status/Log";
import { StatusLightbulb } from "../molecules/status/StatusLightbulb";

export const StatusLayout = () => {
    return <>
        <StatusLightbulb/>
        <DebugLog/>
    </>;
};
