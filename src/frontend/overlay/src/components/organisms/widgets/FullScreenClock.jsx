import React from "react";
import ContestClock from "../../molecules/Clock";
import styled from "styled-components";
import c from "../../../config";

const ClockWrapper = styled.div`
  color: ${c.FULL_SCREEN_CLOCK_COLOR};
  font-size: ${c.FULL_SCREEN_CLOCK_FONT_SIZE};
  font-weight: bold;
  font-family: ${c.FULL_SCREEN_CLOCK_FONT_FAMILY};

  display: flex;
  justify-content: center;

  padding-top: 240px;
`;

export const FullScreenClock = ({ widgetData: { settings } }) => {
    return <ClockWrapper>
        <ContestClock noStatusText={""} { ...settings }/>
    </ClockWrapper>;
};

FullScreenClock.overrideTimeout = c.SVG_APPEAR_TIME;

export default FullScreenClock;
