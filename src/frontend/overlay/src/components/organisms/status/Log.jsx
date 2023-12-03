import React from "react";
import { useDispatch, useSelector } from "react-redux";
import styled from "styled-components";
import { clearLog } from "../../../redux/debug";

const LogWrap = styled.pre`
  overflow-y: scroll;

  box-sizing: border-box;
  height: 600px;
  margin: 0;

  border: black 1px solid;
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
