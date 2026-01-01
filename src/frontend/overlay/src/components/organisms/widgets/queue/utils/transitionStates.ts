import { rowExpand, fadeOut } from "./animations";

export const queueRowContractionStates = (fullHeight: number) => ({
    entering: {
        animation: rowExpand(fullHeight),
        style: { alignItems: "flex-start" },
    },
    entered: {},
    exiting: {
        animation: fadeOut(),
    },
    exited: {},
});
