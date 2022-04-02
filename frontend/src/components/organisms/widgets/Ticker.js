import PropTypes from "prop-types";
import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Transition, TransitionGroup } from "react-transition-group";
import styled, { keyframes } from "styled-components";
import {
    TICKER_BACKGROUND,
    TICKER_FONT_COLOR,
    TICKER_OPACITY,
    TICKER_SCROLL_TRANSITION_TIME,
    TICKER_SMALL_BACKGROUND,
    TICKER_SMALL_SIZE
} from "../../../config";
import { startScrolling, stopScrolling } from "../../../redux/ticker";

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
  height: 100%;
  animation: ${props => props.animation} linear ${TICKER_SCROLL_TRANSITION_TIME}ms;
`;

const TickerRow = ({ children, state }) => {
    return <TickerRowContainer animation={transitionProps[state]}>
        {children}
    </TickerRowContainer>;
};

const SingleTickerWrap = styled.div`
  height: 100%;
  width: 100%;
  background-color: ${props => props.color};
`;

export const SingleTicker = ({ part, color }) => {
    const curMessage = useSelector((state) => state.ticker.tickers[part].curDisplaying);
    return <SingleTickerWrap color={color}>
        <TransitionGroup component={null}>
            <Transition key={curMessage?.id} timeout={TICKER_SCROLL_TRANSITION_TIME}>
                {state =>
                    <TickerRow state={state}>
                        {part} {curMessage?.type} {curMessage ? curMessage.text ?? "NO_TEXT" : "NO_MESSAGE"}
                    </TickerRow>
                }
            </Transition>
        </TransitionGroup>
    </SingleTickerWrap>;
};

SingleTicker.propTypes = {
    part: PropTypes.string.isRequired,
    color: PropTypes.string
};

const TickerWrap = styled.div`
  width: 100%;
  height: 100%;
  background-color: ${TICKER_BACKGROUND};
  opacity: ${TICKER_OPACITY};
  color: ${TICKER_FONT_COLOR};
  display: grid;
  grid-template-columns: ${TICKER_SMALL_SIZE} auto;
`;

export const Ticker = ({ widgetData }) => {
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
                <SingleTicker part={"short"} color={TICKER_SMALL_BACKGROUND}/>
                <SingleTicker part={"long"}/>
            </>
        }
    </TickerWrap>;
};

export default Ticker;
