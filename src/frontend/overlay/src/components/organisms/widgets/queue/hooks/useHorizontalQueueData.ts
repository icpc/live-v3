import { useMemo } from "react";
import c from "@/config";
import {
    QueueRowInfo,
    QueueState,
    createEmptyQueueState,
    QueueBatchInfo,
} from "../utils/queueState";
import { useQueueItemProcessing } from "./useQueueItemProcessing";

const GRID_OFFSET_X = c.QUEUE_ROW_WIDTH + c.QUEUE_HORIZONTAL_ROW_X_PADDING;
const GRID_OFFSET_Y = c.QUEUE_ROW_HEIGHT + c.QUEUE_HORIZONTAL_ROW_Y_PADDING;
const MAX_ROWS_PER_BATCH = c.QUEUE_HORIZONTAL_HEIGHT_NUM;

function calculateNextHorizontalState(
    prevState: QueueState,
    currentQueue: QueueRowInfo[],
    maxFtsPositions: number,
): QueueState {
    const nextState: QueueState = {
        currentRuns: { ...prevState.currentRuns },
        batches: {},
        batchOrder: [...prevState.batchOrder],
        ftsPositions: { ...prevState.ftsPositions },
    };

    for (const [id, batch] of Object.entries(prevState.batches)) {
        nextState.batches[id] = { ...(batch as QueueBatchInfo) };
    }

    for (const run of currentQueue) {
        if (
            (run.featuredRunMedia?.length ?? 0) > 0 ||
            nextState.ftsPositions[run.id] !== undefined
        ) {
            continue;
        }

        if (run.isFts) {
            const usedPositions = Object.values(nextState.ftsPositions);
            const freeSlot = findFirstFreeSlot(maxFtsPositions, usedPositions);

            if (freeSlot !== null) {
                nextState.ftsPositions[run.id] = freeSlot;
                continue;
            }
        }

        const existingDelegate = nextState.currentRuns[run.id];

        if (existingDelegate) {
            nextState.currentRuns[run.id] = existingDelegate;
        } else {
            assignNewBatchPosition(nextState, run.id);
        }
    }

    cleanupStaleRuns(nextState, currentQueue);

    return nextState;
}

function findFirstFreeSlot(max: number, used: number[]): number | null {
    for (let i = 0; i < max; i++) {
        if (!used.includes(i)) return i;
    }
    return null;
}

function assignNewBatchPosition(state: QueueState, runId: string) {
    if (state.batchOrder.length === 0) {
        createNewBatch(state, runId);
        return;
    }

    const firstBatchId = state.batchOrder[0];
    const firstBatchPositions = Object.values(state.batches[firstBatchId]);

    if (firstBatchPositions.length >= MAX_ROWS_PER_BATCH) {
        createNewBatch(state, runId);
    } else {
        const freeSlot = findFirstFreeSlot(
            MAX_ROWS_PER_BATCH,
            firstBatchPositions,
        );
        if (freeSlot !== null) {
            state.batches[firstBatchId][runId] = freeSlot;
            state.currentRuns[runId] = firstBatchId;
        } else {
            createNewBatch(state, runId);
        }
    }
}

function createNewBatch(state: QueueState, runId: string) {
    state.batchOrder.unshift(runId);
    state.batches[runId] = { [runId]: 0 };
    state.currentRuns[runId] = runId;
}

function cleanupStaleRuns(state: QueueState, currentQueue: QueueRowInfo[]) {
    for (const [runId, delegateId] of Object.entries(state.currentRuns)) {
        const stillExists = currentQueue.some((r) => r.id === runId);
        if (!stillExists && state.ftsPositions[runId] === undefined) {
            delete state.currentRuns[runId];
            if (state.batches[delegateId]) {
                delete state.batches[delegateId][runId];
                if (Object.keys(state.batches[delegateId]).length === 0) {
                    state.batchOrder = state.batchOrder.filter(
                        (b) => b !== delegateId,
                    );
                    delete state.batches[delegateId];
                }
            }
        }
    }

    for (const runId of Object.keys(state.ftsPositions)) {
        if (!currentQueue.some((r) => r.id === runId)) {
            delete state.ftsPositions[runId];
        }
    }
}

export const useHorizontalQueueRowsData = ({
    height,
    ftsRowWidth,
    basicZIndex = c.QUEUE_BASIC_ZINDEX,
}: {
    height: number;
    ftsRowWidth: number;
    basicZIndex?: number;
}): [QueueRowInfo | null, QueueRowInfo[]] => {
    const { processingQueue, featured } = useQueueItemProcessing(basicZIndex);

    const allowedMaxBatches = useMemo(
        () => Math.floor(height / GRID_OFFSET_Y) - 1,
        [height],
    );

    const allowedFtsSlots = useMemo(
        () => Math.floor(ftsRowWidth / GRID_OFFSET_X),
        [ftsRowWidth],
    );

    const layoutState = useMemo(() => {
        const emptyState = createEmptyQueueState();
        return calculateNextHorizontalState(
            emptyState,
            processingQueue,
            allowedFtsSlots,
        );
    }, [processingQueue, allowedFtsSlots]);

    const rows = useMemo(() => {
        const resultRows: QueueRowInfo[] = [];

        processingQueue.forEach((row) => {
            if (layoutState.ftsPositions[row.id] !== undefined) {
                row.bottom =
                    height -
                    c.QUEUE_HORIZONTAL_ROW_Y_PADDING -
                    c.QUEUE_ROW_HEIGHT;
                row.right = layoutState.ftsPositions[row.id] * GRID_OFFSET_X;
                resultRows.push(row);
                return;
            }

            const delegateId = layoutState.currentRuns[row.id];
            if (!delegateId) return;

            const batchIndex = layoutState.batchOrder.indexOf(delegateId);
            const slotInBatch = layoutState.batches[delegateId]?.[row.id];

            if (batchIndex === -1 || slotInBatch === undefined) return;
            if (batchIndex >= allowedMaxBatches) return;

            row.bottom =
                (c.QUEUE_ROW_HEIGHT + c.QUEUE_HORIZONTAL_ROW_Y_PADDING) *
                batchIndex;
            row.right = (MAX_ROWS_PER_BATCH - 1 - slotInBatch) * GRID_OFFSET_X;

            resultRows.push(row);
        });

        return resultRows;
    }, [processingQueue, layoutState, height, allowedMaxBatches]);

    return [featured, rows];
};
