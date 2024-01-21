import React from "react";
import { TextWrap } from "./Text";
import ContestClock from "../../molecules/Clock";

export const Clock = ({ part }) => {
    return <TextWrap part={part}>
        <ContestClock/>
    </TextWrap>;
};

export default Clock;
