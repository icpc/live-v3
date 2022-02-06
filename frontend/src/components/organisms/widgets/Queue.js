import React from "react";
import { useSelector } from "react-redux";
import styled from "styled-components";

const QueueWrap = styled.div`
  background-color: grey;
  width: 100%;
  height: 100%;
  
  display: flex;
`;

const BreakingNewsContainer = styled.div`
    
`;

const QueueContainer = styled.div`
    
`;

export const Queue = ({ state }) => {
    const queue = useSelector(state => state.queue.queue);
    return <QueueWrap>{JSON.stringify(queue)}</QueueWrap>;
};
export default Queue;
