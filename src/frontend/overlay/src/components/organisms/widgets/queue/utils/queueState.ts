import { QueueRunInfo } from "@shared/api";

export type QueueBatchInfo = { [runId: string]: number };
export type DelegateId = string;

export type QueueState = {
    currentRuns: { [runId: string]: DelegateId };
    batches: { [delegateId: DelegateId]: QueueBatchInfo };
    batchOrder: DelegateId[];
    ftsPositions: { [runId: string]: number };
};

export interface QueueRowInfo extends QueueRunInfo {
    zIndex: number;
    bottom: number;
    right: number;
    isFeatured: boolean;
    isFeaturedRunMediaLoaded: boolean;
    isFts: boolean;
    setIsFeaturedRunMediaLoaded: ((state: boolean) => void) | null;
}

export const createEmptyQueueState = (): QueueState => ({
    currentRuns: {},
    batches: {},
    batchOrder: [],
    ftsPositions: {},
});
