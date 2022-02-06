import React from "react";
import { DebugLog } from "../organisms/status/Log";
import { StatusLightbulbs } from "../organisms/status/StatusLightbulbs";

export const StatusLayout = () => {
    return <>
        <StatusLightbulbs/>
        <DebugLog/>
    </>;
};
