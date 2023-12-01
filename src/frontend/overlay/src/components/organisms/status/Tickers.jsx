import React, { useEffect, useState } from "react";
import styled, { keyframes } from "styled-components";

const TOTALWIDTH = 300;
const INCREMENT = 10;
const TICKER_WIDTH = 30;
const TPS = 10;
const INTERVAL = 1 / TPS * 1000;

const TickerContainer = styled.div`
    display: inline;
`;

const JSTickerContainer = styled.div`
  display: inline-block;
  width: ${TOTALWIDTH}px;
  height: 30px;
  position: relative;
  background-color: red;
`;

const JSTickerBody = styled.div.attrs(({ pos }) => ({
    style: {
        left: pos
    }
}))`
  height: 100%;
  width: ${TICKER_WIDTH}px;
  background-color: blue;
  position: absolute;
`;

export const JSTicker = () => {
    const [pos, setPos] = useState(0);
    useEffect(() => {
        const id = setInterval(() => {
            setPos((pos) => (pos + INCREMENT) % (TOTALWIDTH - TICKER_WIDTH + INCREMENT));
        }, INTERVAL);
        return () => clearInterval(id);
    }, []);
    return <TickerContainer>
        JS:
        <JSTickerContainer>
            <JSTickerBody pos={pos}/>
        </JSTickerContainer>
    </TickerContainer>;
};

const CSSTickerContainer = JSTickerContainer;

const animation = keyframes`
  from {
    left: 0;
  }

  to {
    left: calc(100% - ${TICKER_WIDTH}px);
  }
`;

const CSSTickerBody = styled.div`
  height: 100%;
  width: ${TICKER_WIDTH}px;
  background-color: blue;
  position: absolute;
  animation: ${animation} linear infinite ${(TOTALWIDTH - TICKER_WIDTH + INCREMENT) / INCREMENT * INTERVAL}ms;
`;

export const CSSTicker = () => {
    return <TickerContainer>
        CSS:
        <CSSTickerContainer>
            <CSSTickerBody/>
        </CSSTickerContainer>
    </TickerContainer>;
};
