import React from "react";
import { useDispatch, useSelector } from "react-redux";
import styled from "styled-components";
import { clearLog } from "../../../redux/debug";

const LogWrap = styled.pre`
  height: 600px;
  overflow-y: scroll;
  border: black 1px solid;
  box-sizing: border-box;
  margin: 0;
`;

const DebugLogContainer = styled.div`
    
`;

export const DebugLog = () => {
    const debug = useSelector(state => state.debug);
    const dispatch = useDispatch();
    return <DebugLogContainer>
        <button onClick={() => dispatch(clearLog())}>Clear log</button>
        {!debug.enabled && "LOGGING IS DISABLED!"}
        <LogWrap>
            {debug.log.map((obj, index) => {
                return <React.Fragment key={index}>{obj.timestamp} - {obj.text}<br/></React.Fragment>;
            })}
            &gt;
        </LogWrap>
    </DebugLogContainer>;
};
