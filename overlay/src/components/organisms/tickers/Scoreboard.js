import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";
import {
    TICKER_SCOREBOARD_RANK_WIDTH,
    TICKER_SCOREBOARD_REPEATS,
    TICKER_SCOREBOARD_SCROLL_TRANSITION_TIME,
    TICKER_SCROLL_TRANSITION_TIME
} from "../../../config";
import { SCOREBOARD_TYPES } from "../../../consts";
import { ScoreboardRow } from "../widgets/Scoreboard";


const ScoreboardWrap = styled.div.attrs(({ top }) => (
    { style: { top } }
))`
  display: grid;
  grid-template-columns: repeat(${props => props.cols}, 1fr);
  grid-template-rows: repeat(${props => props.screens * props.rows}, ${props => 100 / props.rows}%);
  height: 100%;
  width: 100%;
  position: absolute;
  transition: top ${TICKER_SCOREBOARD_SCROLL_TRANSITION_TIME}ms ease-in-out;
`;

export const Scoreboard = ({ tickerSettings, state }) => {
    const { from, to, rows, cols, periodMs } = tickerSettings;
    const [firstRow, setFirstRow] = useState(0);
    const oneRowHeight = 100 / rows;
    const entries = useSelector((state) => state.scoreboard[SCOREBOARD_TYPES.normal].rows.slice(from - 1, to));
    const screens = Math.ceil(entries.length / (rows * cols));
    const totalRows = Math.ceil(entries.length / cols);
    useEffect(() => {
        if (state !== "entering") {
            const interval = setInterval(() => {
                if (state !== "exiting") {
                    setFirstRow((firstRow) => {
                        const newFirst = firstRow + rows;
                        const remRows = totalRows - newFirst * rows;
                        console.log(firstRow, newFirst, remRows);
                        if(remRows < rows) {
                            console.log("1");
                            return totalRows - rows;
                        } else if(remRows === rows) {
                            console.log("2");
                            return 0;
                        } else {
                            console.log("3");
                            return newFirst;
                        }
                    });
                }
            }, (periodMs - TICKER_SCROLL_TRANSITION_TIME) / screens / TICKER_SCOREBOARD_REPEATS);
            return () => clearInterval(interval);
        }
    }, [screens, periodMs, state, entries, oneRowHeight]);
    return <ScoreboardWrap screens={screens} top={-firstRow * oneRowHeight + "%"} cols={cols} rows={rows}>
        {entries.map((row) => <ScoreboardRow
            key={row.teamId}
            teamId={row.teamId}
            hideTasks={true}
            rankWidth={TICKER_SCOREBOARD_RANK_WIDTH}
            nameGrows={true}
            optimismLevel={SCOREBOARD_TYPES.normal}
        />)}
    </ScoreboardWrap>;
};

export default Scoreboard;

