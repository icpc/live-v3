import React from "react";
import { TextWrap } from "./Text";
import ContestClock from "../../molecules/Clock";

export const Clock = ({ tickerSettings, part }) => {
    return <TextWrap part={part}>
        <ContestClock
            timeZone={tickerSettings.timeZone ?? null}
        />
    </TextWrap>;
};

export default Clock;
