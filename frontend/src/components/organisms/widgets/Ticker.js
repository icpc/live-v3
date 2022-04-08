import PropTypes from "prop-types";
import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Transition, TransitionGroup } from "react-transition-group";
import styled, { keyframes } from "styled-components";
import {
    TICKER_BACKGROUND,
    TICKER_FONT_COLOR,
    TICKER_FONT_FAMILY,
    TICKER_OPACITY,
    TICKER_SCROLL_TRANSITION_TIME,
    TICKER_SMALL_BACKGROUND,
    TICKER_SMALL_SIZE
} from "../../../config";
import { pushLog } from "../../../redux/debug";
import { startScrolling, stopScrolling } from "../../../redux/ticker";
import Clock from "../tickers/Clock";
import Text from "../tickers/Text";

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
  width: 100%;
  animation: ${props => props.animation} ease-in-out ${TICKER_SCROLL_TRANSITION_TIME}ms;
  
  display: flex;
  justify-content: center;
  align-items: center;
  
  font-family: ${TICKER_FONT_FAMILY};
`;

const TickerRow = ({ children, state }) => {
    return <TickerRowContainer animation={transitionProps[state]}>
        {children}
    </TickerRowContainer>;
};

const SingleTickerWrap = styled.div`
  position: relative;
  height: 100%;
  width: 100%;
  background-color: ${props => props.color};
`;

const widgetTypes = Object.freeze({
    text: Text,
    clock: Clock
});

const DefaultTicker = ({ tickerSettings }) => {
    return <div style={{ backgroundColor: "red", wordBreak: "break-all" }}>
        {JSON.stringify(tickerSettings)}
    </div>;
};

export const SingleTicker = ({ part, color }) => {
    const dispatch = useDispatch();
    const curMessage = useSelector((state) => state.ticker.tickers[part].curDisplaying);
    const isFirst = useSelector((state) => state.ticker.tickers[part].isFirst);
    return <SingleTickerWrap color={color}>
        <TransitionGroup component={null}>
            {curMessage &&
                <Transition key={curMessage?.id} timeout={TICKER_SCROLL_TRANSITION_TIME}>
                    {(state) => {
                        const TickerComponent = widgetTypes[curMessage.type] ?? DefaultTicker;
                        if(TickerComponent === undefined) {
                            dispatch(pushLog(`ERROR: Unknown ticker type: ${curMessage.type}`));
                        }
                        return state !== "exited" && <TickerRow
                            state={isFirst && state === "entering" ? "entered" : state}> {/* ignore first entering render */}
                            <TickerComponent tickerSettings={curMessage.settings}/>
                        </TickerRow>;
                    }
                    }
                </Transition>
            }
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
                <SingleTicker part={"short"} color={TICKER_SMALL_BACKGROUND}/>
                <SingleTicker part={"long"}/>
            </>
        }
    </TickerWrap>;
};

export default Ticker;
