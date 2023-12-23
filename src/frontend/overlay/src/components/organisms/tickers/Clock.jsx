import React from "react";
import { TextWrap } from "./Text";
import ContestClock from "../../molecules/Clock";

export const Clock = ({tickerSettings, part}) => {
    return <TextWrap>
        <ContestClock
            timeZone={tickerSettings.timeZone.length === 0 ? null : tickerSettings.timeZone}
        />
    </TextWrap>;
};

export default Clock;
