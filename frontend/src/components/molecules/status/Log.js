import React from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";

const LogWrap = styled.pre`
  max-height: 600px;
  overflow-y: scroll;
`;

export const DebugLog = () => {
    const log = useSelector(state => state.debug.log);
    return <LogWrap>
        {log.map((obj, index) => {
            return <React.Fragment key={index}>{obj.timestamp} - {obj.text}<br/></React.Fragment>;
        })}
    </LogWrap>;
};
