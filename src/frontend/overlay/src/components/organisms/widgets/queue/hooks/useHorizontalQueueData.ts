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
    maxColumnsPerBatch: number,
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
            assignNewBatchPosition(nextState, run.id, maxColumnsPerBatch);
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

function assignNewBatchPosition(
    state: QueueState,
    runId: string,
    maxColumns: number,
) {
    if (state.batchOrder.length === 0) {
        createNewBatch(state, runId);
        return;
    }

    const lastBatchId = state.batchOrder[state.batchOrder.length - 1];
    const lastBatchPositions = Object.values(state.batches[lastBatchId]);

    if (lastBatchPositions.length >= maxColumns) {
        createNewBatch(state, runId);
    } else {
        const freeSlot = findFirstFreeSlot(maxColumns, lastBatchPositions);
        if (freeSlot !== null) {
            state.batches[lastBatchId][runId] = freeSlot;
            state.currentRuns[runId] = lastBatchId;
        } else {
            createNewBatch(state, runId);
        }
    }
}

function createNewBatch(state: QueueState, runId: string) {
    state.batchOrder.push(runId);
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
    width,
    ftsRowWidth,
    basicZIndex = c.QUEUE_BASIC_ZINDEX,
}: {
    height: number;
    width: number;
    ftsRowWidth: number;
    basicZIndex?: number;
}): [QueueRowInfo | null, QueueRowInfo[]] => {
    const { processingQueue, featured } = useQueueItemProcessing(basicZIndex);

    const totalVerticalSlots = useMemo(
        () => Math.max(1, Math.floor(height / GRID_OFFSET_Y)),
        [height],
    );

    const hasFtsRow = totalVerticalSlots >= 2;
    const allowedMaxBatches = hasFtsRow
        ? totalVerticalSlots - 1
        : totalVerticalSlots;

    const effectiveColumnsPerBatch = useMemo(
        () =>
            Math.max(
                1,
                Math.min(MAX_ROWS_PER_BATCH, Math.floor(width / GRID_OFFSET_X)),
            ),
        [width],
    );

    const allowedFtsSlots = useMemo(
        () => (hasFtsRow ? Math.floor(ftsRowWidth / GRID_OFFSET_X) : 0),
        [ftsRowWidth, hasFtsRow],
    );

    const layoutState = useMemo(() => {
        const emptyState = createEmptyQueueState();
        return calculateNextHorizontalState(
            emptyState,
            processingQueue,
            allowedFtsSlots,
            effectiveColumnsPerBatch,
        );
    }, [processingQueue, allowedFtsSlots, effectiveColumnsPerBatch]);

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
            if (slotInBatch >= effectiveColumnsPerBatch) return;

            row.bottom =
                (c.QUEUE_ROW_HEIGHT + c.QUEUE_HORIZONTAL_ROW_Y_PADDING) *
                batchIndex;
            row.right = slotInBatch * GRID_OFFSET_X;

            resultRows.push(row);
        });

        return resultRows;
    }, [
        processingQueue,
        layoutState,
        height,
        allowedMaxBatches,
        effectiveColumnsPerBatch,
    ]);

    return [featured, rows];
};
