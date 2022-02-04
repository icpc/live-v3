import PropTypes from "prop-types";
import React, { useEffect, useState } from "react";
import styled from "styled-components";
import star from "../../../assets/star.svg";

function shuffleArray(array) {
    for (let i = array.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [array[i], array[j]] = [array[j], array[i]];
    }
}

const NUM = 30;
const TASKS = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P"];
const ROWHEIGHT = 44;
const NUMWIDTH = 80;
const NAMEWIDTH = 300;
const STATWIDTH = 80;

const elems = [...Array(NUM).keys()];

const getNewPos = () => {
    let arr = Array.from(elems);
    shuffleArray(arr);
    return arr.map((el) => el);
};

const ScoreboardWrap = styled.div`
  height: 100%;
  width: 100%;
  opacity: 0.8;
  border: none;
  border-collapse: collapse;
  table-layout: fixed;
`;

const ScoreboardRowWrap = styled.div`
  top: ${(props) => props.pos}px;
  left: 0;
  right: 0;
  height: ${ROWHEIGHT}px;
  transition: top 1s ease-in-out;
  width: 100%;
  display: flex;
  position: absolute;
`;

const ScoreboardCell = styled.div`
  background: black;
  color: white;
  height: ${ROWHEIGHT}px;
  border: none;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
  position: relative;
  font-weight: 600;
  font-size: 20px;
`;

const ScoreboardNumCell = styled(ScoreboardCell)`
    width: ${NUMWIDTH}px;
`;

const ScoreboardNameCell = styled(ScoreboardCell)`
    width: ${NAMEWIDTH}px;
    justify-content: start;
`;

const ScoreboardStatCell = styled(ScoreboardCell)`
    width: ${STATWIDTH}px;
`;

const ScoreboardTaskCellWrap = styled(ScoreboardCell)`
  flex-grow: 1;
`;

const STARSIZE = 10;
const StarIconWrap = styled.img`
    position: absolute;
    top: 0;
    right: 0;
    width: ${STARSIZE}px;
    height: ${STARSIZE}px;
`;
const StarIcon = () => {
    return <StarIconWrap src={star} alt="first"/>;
};

const TeamTaskStatus = Object.freeze({
    solved: 1,
    failed: 2,
    untouched: 3,
    unknown: 4,
    first: 5
});

const ScoreboardTaskCell = ({ status, attempts }) => {
    return <ScoreboardTaskCellWrap>
        {status === TeamTaskStatus.first && <StarIcon/>}
        {attempts}
    </ScoreboardTaskCellWrap>;
};

ScoreboardTaskCell.propTypes = {
    status: PropTypes.oneOfType(TeamTaskStatus),
    attempts: PropTypes.number
};

const ScoreboardHeaderWrap = styled(ScoreboardRowWrap)`
  height: ${ROWHEIGHT}px;
`;

const ScoreboardHeaderTitle = styled(ScoreboardCell)`
    background: red;
    width: ${NUMWIDTH + NAMEWIDTH}px;
    font-size: 30px;
`;

const ScoreboardHeaderStatCell = styled(ScoreboardStatCell)`
    background: black;
    width: ${STATWIDTH}px;
    text-align: center;
`;

const ScoreboardHeaderTaskCell = styled(ScoreboardTaskCellWrap)`
    text-align: center;
    border-bottom: red 5px solid;
    box-sizing: border-box;
`;

const ScoreboardRow = ({ data, pos }) => {
    return <ScoreboardRowWrap pos={pos}>
        <ScoreboardNumCell>{pos}</ScoreboardNumCell>
        <ScoreboardNameCell>
            TEAMNAME
        </ScoreboardNameCell>
        <ScoreboardStatCell>
            Aboba
        </ScoreboardStatCell>
        <ScoreboardStatCell>
            Aboba
        </ScoreboardStatCell>
        {TASKS.map((el) =>
            <ScoreboardTaskCell key={el} status={TeamTaskStatus.first} attempts={3}/>
        )}
    </ScoreboardRowWrap>;
};

const ScoreboardHeader = () => {
    return <ScoreboardHeaderWrap>
        <ScoreboardHeaderTitle>CURRENT STANDINGS</ScoreboardHeaderTitle>
        <ScoreboardHeaderStatCell>&#931;</ScoreboardHeaderStatCell>
        <ScoreboardHeaderStatCell>PEN</ScoreboardHeaderStatCell>
        {TASKS.map((el) =>
            <ScoreboardHeaderTaskCell key={el}>
                {el}
            </ScoreboardHeaderTaskCell>
        )}
    </ScoreboardHeaderWrap>;
};

export const Scoreboard = () => {
    const [positions, setPositions] = useState(getNewPos());
    useEffect(() => {
        const id = setInterval(() => {
            setPositions(getNewPos());
        }, 2000);
        return () => clearInterval(id);
    }, []);

    return <ScoreboardWrap>
        <ScoreboardHeader/>
        {elems.map((i) => (
            <ScoreboardRow key={i} pos={positions[i] * ROWHEIGHT + ROWHEIGHT}/>
        ))}
    </ScoreboardWrap>;
};
export default Scoreboard;
