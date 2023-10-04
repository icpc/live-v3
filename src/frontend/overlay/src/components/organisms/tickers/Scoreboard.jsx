import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import c from "../../../config";
import { SCOREBOARD_TYPES } from "../../../consts";
import { ScoreboardRow } from "../widgets/Scoreboard";


const ScoreboardWrap = styled.div.attrs(({ top }) => (
    { style: { top } }
))`
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(${props => props.nrows}, 100%);
  height: 100%;
  width: 100%;
  position: absolute;
  transition: top ${c.TICKER_SCOREBOARD_SCROLL_TRANSITION_TIME}ms ease-in-out;
`;

export const Scoreboard = ({ tickerSettings, state }) => {
    const { from, to, periodMs } = tickerSettings;
    const [row, setRow] = useState(0);
    const rows = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal].rows.slice(from-1, to));
    const nrows = Math.ceil(rows.length / 4);
    useEffect(() => {
        if(state !== "entering") {
            const interval = setInterval(() => {
                if (state !== "exiting") {
                    setRow((row) => (row + 1) % nrows);
                }
            }, (periodMs - c.TICKER_SCROLL_TRANSITION_TIME) / nrows / c.TICKER_SCOREBOARD_REPEATS + 1);
            return () => clearInterval(interval);
        }
    }, [nrows, periodMs, state]);
    return <ScoreboardWrap nrows={nrows} top={-row * 100 + "%"}>
        {rows.map((row) => <ScoreboardRow
            key={row.teamId}
            teamId={row.teamId}
            hideTasks={true}
            rankWidth={c.TICKER_SCOREBOARD_RANK_WIDTH}
            nameGrows={true}
            optimismLevel={SCOREBOARD_TYPES.normal}
        />)}
    </ScoreboardWrap>;
};

export default Scoreboard;

