import PropTypes from "prop-types";
import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Transition, TransitionGroup } from "react-transition-group";
import styled, { keyframes } from "styled-components";
import c from "../../../config";
import { pushLog } from "../../../redux/debug";
import { startScrolling, stopScrolling } from "../../../redux/ticker";
import live from "../../../assets/icons/live.svg";
import Clock from "../tickers/Clock";
import Scoreboard from "../tickers/Scoreboard";
import Text from "../tickers/Text";
import Image from "../tickers/Image";

const rowAppear = keyframes`
  from {
    top: -100%;
  }
  to {
    top: 0;
  }
`;

const rowDisappear = keyframes`
  from {
    top: 0;
  }
  to {
    top: 100%;
  }
`;

const transitionProps = {
    entering: rowAppear,
    exiting: rowDisappear
};

const TickerRowContainer = styled.div`
  position: absolute;

  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;

  width: 100%;
  height: 100%;
  
  font-family: ${c.TICKER_FONT_FAMILY};

  animation: ${props => props.animation} ease-in-out ${c.TICKER_SCROLL_TRANSITION_TIME}ms;
  animation-fill-mode: forwards;
`;

const TickerRow = ({ children, state }) => {
    return (
        <TickerRowContainer animation={transitionProps[state]}>
            {children}
        </TickerRowContainer>
    );
};


const SingleTickerWrap = styled.div`
  position: relative;

  display: flex;
  justify-content: ${props => props.justify};

  box-sizing: border-box;
  width: 100%;
  height: 100%;
  padding-left: ${props => props.padding};

  background-color: ${props => props.color};
  border-radius: ${c.GLOBAL_BORDER_RADIUS};
`;

const widgetTypes = Object.freeze({
    text: Text,
    clock: Clock,
    scoreboard: Scoreboard,
    image: Image,
});

const DefaultTicker = ({ tickerSettings }) => {
    return <div style={{ backgroundColor: "red", wordBreak: "break-all" }}>
        {JSON.stringify(tickerSettings)}
    </div>;
};

export const SingleTickerRows = ({ part }) => {
    const dispatch = useDispatch();
    const curMessage = useSelector((state) => state.ticker.tickers[part].curDisplaying);
    const isFirst = useSelector((state) => state.ticker.tickers[part].isFirst);
    return (
        <TransitionGroup component={null}>
            {curMessage &&
                <Transition key={curMessage?.id} timeout={c.TICKER_SCROLL_TRANSITION_TIME}>
                    {(state) => {
                        const TickerComponent = widgetTypes[curMessage.type] ?? DefaultTicker;
                        if (TickerComponent === undefined) {
                            dispatch(pushLog(`ERROR: Unknown ticker type: ${curMessage.type}`));
                        }
                        const sanitizedState = isFirst && state === "entering" ? "entered" : state; // ignore first entering render
                        return state !== "exited" && <TickerRow state={sanitizedState} part={part}>
                            <TickerComponent tickerSettings={curMessage.settings} state={sanitizedState} part={part}/>
                        </TickerRow>;
                    }}
                </Transition>
            }
        </TransitionGroup>
    );
};


const ShortTickerGrid = styled.div`
  display: grid;
  grid-template-columns: ${c.TICKER_LIVE_ICON_SIZE} auto;
  column-gap: 8px;

  width: 100%;
  margin: 0 8px;
`;

const LiveIcon = styled.img`
  height: ${c.TICKER_LIVE_ICON_SIZE};
  padding: 8px 0;
`;

export const SingleTicker = ({ part, color }) => {
    if (part === "short") {
        return (
            <SingleTickerWrap color={color}>
                <ShortTickerGrid>
                    <LiveIcon src={live}/>
                    <SingleTickerWrap>
                        <SingleTickerRows part={part}/>
                    </SingleTickerWrap>
                </ShortTickerGrid>
            </SingleTickerWrap>
        );
    }
    return (
        <SingleTickerWrap color={color}>
            <SingleTickerRows part={part}/>
        </SingleTickerWrap>
    );
};

SingleTicker.propTypes = {
    part: PropTypes.string.isRequired,
    color: PropTypes.string
};

const TickerWrap = styled.div`
  position: absolute;
  z-index: 2147000000;

  display: grid;
  grid-template-columns: ${c.TICKER_SMALL_SIZE} auto;
  column-gap: 9px;

  width: 100%;
  height: 100%;

  color: ${c.TICKER_FONT_COLOR};
`;

export const Ticker = () => {
    const dispatch = useDispatch();
    const isLoaded = useSelector((state) => state.ticker.isLoaded);
    useEffect(() => {
        if (!isLoaded) {
            return () => undefined;
        }
        dispatch(startScrolling());
        return () => dispatch(stopScrolling());
    }, [isLoaded]);
    return <TickerWrap>
        {isLoaded &&
            <>
                <SingleTicker part={"short"} color={c.TICKER_SMALL_BACKGROUND}/>
                <SingleTicker part={"long"} color={c.SCOREBOARD_BACKGROUND_COLOR}/>
            </>
        }
    </TickerWrap>;
};

export default Ticker;
