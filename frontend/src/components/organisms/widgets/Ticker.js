import React, { useEffect } from "react";
import styled from "styled-components";
import { useDispatch, useSelector } from "react-redux";
import { startScrolling, stopScrolling } from "../../../redux/ticker";

const TickerWrap = styled.div`
    width: 100%;
    height: 100%;
    background-color: gray;
`;

export const SingleTicker = ({ part }) => {
    const dispatch = useDispatch();
    const curMessage = useSelector((state) => state.ticker.tickers[part].curDisplaying);
    const isLoaded = useSelector((state) => state.ticker.isLoaded);
    useEffect(() => {
        if(!isLoaded) {
            return () => undefined;
        }
        dispatch(startScrolling());
        return () => dispatch(stopScrolling);
    }, [part, isLoaded]);
    return <div>
        {part} {curMessage?.type} {curMessage ? curMessage.text ?? "NO_TEXT" : "NO_MESSAGE"}
    </div>;
};


export const Ticker = ({ widgetData }) => {
    return  <TickerWrap>
        <SingleTicker part={"short"}/>
        <SingleTicker part={"long"}/>
    </TickerWrap>;
};

export default Ticker;
