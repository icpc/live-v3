import React from "react";
import ContestClock from "../../molecules/Clock";
import styled from "styled-components";
import c from "../../../config";

const ClockWrapper = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;

  height: 100%;
  font-family: ${c.FULL_SCREEN_CLOCK_FONT_FAMILY};
  font-size: ${c.FULL_SCREEN_CLOCK_FONT_SIZE};
  font-weight: bold;
  color: ${c.FULL_SCREEN_CLOCK_COLOR};
`;

export const FullScreenClock = ({ widgetData: { settings } }) => {
    return <ClockWrapper>
        <ContestClock noStatusText={""} { ...settings }/>
    </ClockWrapper>;
};

FullScreenClock.overrideTimeout = c.SVG_APPEAR_TIME;

export default FullScreenClock;
