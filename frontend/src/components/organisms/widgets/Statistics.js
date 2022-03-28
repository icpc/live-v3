import React, { useEffect, useState } from "react";
import styled from "styled-components";
import { useSelector } from "react-redux";
import { CELL_FONT_FAMILY, VERDICT_NOK, VERDICT_OK, VERDICT_UNKNOWN } from "../../../config";
import { ProblemCell } from "../../atoms/ContestCells";
import { Cell } from "../../atoms/Cell";

const WidgetWrap = styled.div`
  width: 100%;
  height: 100%;
  display: flex;

  flex-direction: column;
  background: black;
`;

const Title = styled.div`
  background: ${VERDICT_NOK};
  color: white;
  font-size: 30px;
  text-align: center;
  font-family: ${CELL_FONT_FAMILY}
`;

const Table = styled.div`
  display: flex;
  flex-grow: 1;
  flex-direction: column;
  align-items: stretch;
`;


const Submissions = styled.div`
  flex-grow: 1;
  height: 100%;
  overflow: hidden;
  text-align: end;
  font-size: 24pt;
  font-family: Passageway, serif;
  color: white;
`;

const OK = styled(Cell).attrs(({ width }) => ({ style: { width: width } }))`
  background: ${VERDICT_OK};
  transition: width linear 200ms;

  height: 100%;
  font-size: 18pt;
  overflow: hidden;
  float: left;
  text-align: center;

`;
const Progress = styled(Cell).attrs(({ width }) => ({ style: { width: width } }))`
  background-color: ${VERDICT_UNKNOWN};

  transition: width linear 200ms;
  overflow: hidden;
  float: left;
  color: white;
  font-size: 18pt;

  height: 100%;
  text-align: center;
`;
const Wrong = styled(Cell).attrs(({ width }) => ({ style: { width: width } }))`
  background-color: ${VERDICT_NOK};

  transition: width linear 200ms;
  overflow: hidden;
  float: left;

  height: 100%;
  font-size: 18pt;
  text-align: center;
`;

const StatisticsText = styled.div`
  margin: 0 0.1rem 0 0.1rem;
  text-align: center;
  font-family: ${CELL_FONT_FAMILY}
`;

const ProblemRow = styled.div.attrs(({ height }) => ({ style: { height: height } }))`
    display: flex;
`;

export const Advertisement = () => {
    const statistics = useSelector(state => state.statistics.statistics);
    const count = useSelector(state => state.contestInfo?.info?.teams?.length);
    const tasks = useSelector(state => state.contestInfo?.info?.problems);

    return <WidgetWrap>
        <Title>Statistics</Title>
        <Table>
            {statistics?.map(({ success, wrong, pending }, index) =>
                <ProblemRow key={index} height = {1 / statistics.length * 100 + "%"}>
                    <ProblemCell probData={tasks[index]} width={"35px"}/>
                    <Submissions>
                        <OK width={success / count * 100 + "%"}>
                            <StatisticsText>{success}</StatisticsText>
                        </OK>
                        <Progress width={pending / count * 100 + "%"}>
                            <StatisticsText>
                                {pending}
                            </StatisticsText>
                        </Progress>
                        <Wrong width={wrong / count * 100 + "%"}>
                            <StatisticsText>
                                {wrong}
                            </StatisticsText>
                        </Wrong>
                    </Submissions>
                </ProblemRow>
            )}
        </Table>
    </WidgetWrap>;
};
export default Advertisement;
