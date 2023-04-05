import React from "react";
import ContestClock from "../../molecules/Clock";
import styled from "styled-components";
import {
    BIG_CLOCK_FONT_SIZE,
    BIG_CLOCK_COLOR,
    GLOBAL_DEFAULT_FONT_FAMILY,
    SVG_APPEAR_TIME,
    TICKER_FONT_SMALL
} from "../../../config";

const ClockWrapper = styled.div`
  color: ${BIG_CLOCK_COLOR};
  font-size: ${BIG_CLOCK_FONT_SIZE};
  font-weight: bold;
  font-family: ${TICKER_FONT_SMALL};

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
