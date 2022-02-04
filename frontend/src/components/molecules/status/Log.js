import React from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";

const LogWrap = styled.pre`
`;
// const LogLine = styled

export const DebugLog = () => {
    const log = useSelector(state => state.debug.log);
    return <LogWrap>
        {log.map((obj, index) => {
            return <React.Fragment key={index}>{JSON.stringify(obj)}<br/></React.Fragment>;
        })}
    </LogWrap>;
};
