import React, { useEffect } from "react";
import { useTransition } from "react-transition-state";
import styled, { keyframes } from "styled-components";
import c from "../../../config";
import { pushLog } from "@/redux/debug";
import { startScrolling, stopScrolling } from "@/redux/ticker";
import live from "../../../assets/icons/live.svg";
import Clock from "../tickers/Clock";
import Scoreboard from "../tickers/Scoreboard";
import Text from "../tickers/Text";
import Image from "../tickers/Image";
import Empty from "../tickers/Empty";
import { useAppDispatch, useAppSelector } from "@/redux/hooks";

const rowAppear = keyframes`
  from {
    transform: translate3d(0, -100%, 0);
  }
  to {
    transform: translate3d(0, 0, 0);
  }
`;

const rowDisappear = keyframes`
  from {
    transform: translate3d(0, 0, 0);
  }
  to {
    transform: translate3d(0, 100%, 0);
  }
`;

const transitionProps = {
    entering: rowAppear,
    exiting: rowDisappear,
};

const TickerRowContainer = styled.div<{ animation: string }>`
    position: absolute;
    top: 0;
    left: 0;

    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;

    width: 100%;
    height: 100%;

    font-family: ${c.TICKER_FONT_FAMILY};

    animation: ${(props) => props.animation} ease-in-out
        ${c.TICKER_SCROLL_TRANSITION_TIME}ms;
    animation-fill-mode: forwards;
    will-change: transform;
`;

const TickerRow = ({ children, state }) => {
    return (
        <TickerRowContainer animation={transitionProps[state]}>
            {children}
        </TickerRowContainer>
    );
};

const SingleTickerWrap = styled.div<{
    justify?: string;
    padding?: string;
    color?: string;
}>`
    position: relative;

    display: flex;
    justify-content: ${(props) => props.justify};

    box-sizing: border-box;
    width: 100%;
    height: 100%;
    padding-left: ${(props) => props.padding};

    background-color: ${(props) => props.color};
    border-radius: ${c.GLOBAL_BORDER_RADIUS};
`;

const widgetTypes = Object.freeze({
    empty: Empty,
    text: Text,
    clock: Clock,
    scoreboard: Scoreboard,
    image: Image,
});

const DefaultTicker = ({ tickerSettings }) => {
    return (
        <div style={{ backgroundColor: "red", wordBreak: "break-all" }}>
            {JSON.stringify(tickerSettings)}
        </div>
    );
};

export const SingleTickerRows = ({ part }) => {
    const dispatch = useAppDispatch();
    const curMessage = useAppSelector(
        (state) => state.ticker.tickers[part].curDisplaying,
    );
    const isFirst = useAppSelector(
        (state) => state.ticker.tickers[part].isFirst,
    );
    const [transition, toggle] = useTransition({
        timeout: c.TICKER_SCROLL_TRANSITION_TIME,
        mountOnEnter: true,
        unmountOnExit: true,
    });

    useEffect(() => {
        toggle(!!curMessage);
    }, [curMessage, toggle]);

    if (!curMessage) {
        return null;
    }

    const TickerComponent = widgetTypes[curMessage.type] ?? DefaultTicker;
    if (TickerComponent === undefined) {
        dispatch(pushLog(`ERROR: Unknown ticker type: ${curMessage.type}`));
    }
    const sanitizedState =
        isFirst && transition.status === "entering"
            ? "entered"
            : transition.status;

    return (
        transition.isMounted && (
            <TickerRow state={sanitizedState}>
                <TickerComponent
                    tickerSettings={curMessage.settings}
                    state={sanitizedState}
                    part={part}
                />
            </TickerRow>
        )
    );
};

const ShortTickerGrid = styled.div`
    display: grid;
    grid-template-columns: ${c.TICKER_LIVE_ICON_SIZE} auto;
    column-gap: ${c.TICKER_SHORT_COLUMN_GAP};

    width: 100%;
    margin: ${c.TICKER_LIVE_ICON_MARGIN};
`;

const LiveIcon = styled.img`
    height: ${c.TICKER_LIVE_ICON_SIZE};
    padding: ${c.TICKER_LIVE_ICON_PADDING};
`;

interface SingleTickerProps {
    part: string;
    color?: string;
}

export const SingleTicker: React.FC<SingleTickerProps> = ({ part, color }) => {
    const curMessage = useAppSelector(
        (state) => state.ticker.tickers[part].curDisplaying,
    );
    if (part === "short") {
        return (
            <SingleTickerWrap color={color}>
                <ShortTickerGrid>
                    <LiveIcon src={live} />
                    <SingleTickerWrap>
                        <SingleTickerRows part={part} />
                    </SingleTickerWrap>
                </ShortTickerGrid>
            </SingleTickerWrap>
        );
    }

    const wrapColor =
        curMessage?.type === "scoreboard" || curMessage?.type === "empty"
            ? c.TICKER_DEFAULT_SCOREBOARD_BACKGROUND
            : color;
    return (
        <SingleTickerWrap color={wrapColor}>
            <SingleTickerRows part={part} />
        </SingleTickerWrap>
    );
};

const TickerWrap = styled.div`
    position: absolute;
    z-index: ${c.TICKER_ZINDEX};

    display: grid;
    grid-template-columns: ${c.TICKER_SMALL_SIZE} auto;
    column-gap: ${c.TICKER_LONG_COLUMN_GAP};

    width: 100%;
    height: 100%;

    color: ${c.TICKER_FONT_COLOR};
`;

export const Ticker = () => {
    const dispatch = useAppDispatch();
    const isLoaded = useAppSelector((state) => state.ticker.isLoaded);
    useEffect(() => {
        if (!isLoaded) {
            return () => undefined;
        }
        dispatch(startScrolling());
        return () => dispatch(stopScrolling());
    }, [isLoaded, dispatch]);
    return (
        <TickerWrap>
            {isLoaded && (
                <>
                    <SingleTicker
                        part={"short"}
                        color={c.TICKER_SMALL_BACKGROUND}
                    />
                    <SingleTicker part={"long"} color={c.TICKER_BACKGROUND} />
                </>
            )}
        </TickerWrap>
    );
};

export default Ticker;
