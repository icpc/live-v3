import { DateTime, Settings } from "luxon";
import React, { useCallback, useEffect, useState } from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import { TICKER_CLOCK_FONT_SIZE, TICKER_CLOCK_MARGIN_LEFT } from "../../../config";

Settings.defaultZone = "utc";


const ClockWrap = styled.div`
    //width: 100%;
    block-size: fit-content;
    margin-left: ${TICKER_CLOCK_MARGIN_LEFT};
    font-size: ${TICKER_CLOCK_FONT_SIZE};
`;

export const Clock = () => {
    const contestInfo = useSelector((state) => state.contestInfo.info);
    const getStatus = useCallback(() => {
        if(contestInfo === undefined)
            return "??";
        if(contestInfo.status === "RUNNING") {
            const milliseconds = DateTime.fromMillis(contestInfo.startTimeUnixMs).diffNow().negate().milliseconds *
                (contestInfo.emulationSpeed ?? 1);
            return DateTime.fromMillis(milliseconds).toFormat("H:mm:ss");
        } else {
            return contestInfo.status;
        }
    }, [contestInfo]);
    const [status, setStatus] = useState(getStatus());
    useEffect(() => {
        const interval = setInterval(() => setStatus(getStatus()), 200);
        return () => clearInterval(interval);
    }, [contestInfo]);
    return <ClockWrap>
        {status}
    </ClockWrap>;
};

export default Clock;
