import React from "react";
import ContestClock from "../../molecules/Clock";
import styled from "styled-components";
import {
    FULL_SCREEN_CLOCK_COLOR,
    FULL_SCREEN_CLOCK_FONT_FAMILY,
    FULL_SCREEN_CLOCK_FONT_SIZE,
    SVG_APPEAR_TIME
} from "../../../config";

const ClockWrapper = styled.div`
  color: ${FULL_SCREEN_CLOCK_COLOR};
  font-size: ${FULL_SCREEN_CLOCK_FONT_SIZE};
  font-weight: bold;
  font-family: ${FULL_SCREEN_CLOCK_FONT_FAMILY};

  display: flex;
  justify-content: center;

  padding-top: 240px;
`;

export const FullScreenClock = ({ widgetData: { settings } }) => {
    return <ClockWrapper>
        <ContestClock noStatusText={""} { ...settings }/>
    </ClockWrapper>;
};

FullScreenClock.overrideTimeout = SVG_APPEAR_TIME;

export default FullScreenClock;
