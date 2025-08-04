import React from "react";
import ContestClock from "../../molecules/Clock";
import styled, { css } from "styled-components";
import c from "../../../config";
import { ClockType } from "@shared/api";

const ClockWrapper = styled.div`
  display: flex;
  justify-content: center;

  ${c.FULL_SCREEN_CLOCK_CENTERED
        ? css`
                align-items: center;
                width: 100%;
                height: 100%;
            `
        : css`
                padding-top: ${c.FULL_SCREEN_CLOCK_PADDING_TOP};
            `
}

  font-family: ${c.FULL_SCREEN_CLOCK_FONT_FAMILY};
  font-size: ${c.FULL_SCREEN_CLOCK_FONT_SIZE};
  font-weight: bold;
  color: ${c.FULL_SCREEN_CLOCK_COLOR};
`;

export const FullScreenClock = ({ widgetData: { settings } }) => {
    return <ClockWrapper>
        <ContestClock 
            noStatusText=""
            clockType={settings.clockType ?? ClockType.standard}
            showSeconds={settings.showSeconds ?? true}
            timeZone={settings.timeZone ?? null}
        />
    </ClockWrapper>;
};

FullScreenClock.overrideTimeout = c.SVG_APPEAR_TIME;

export default FullScreenClock;
