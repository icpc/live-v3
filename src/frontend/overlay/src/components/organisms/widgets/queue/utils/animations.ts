import { keyframes } from "styled-components";

export const rowExpand = (fullHeight: number) => keyframes`
    from {
        max-height: 0;
    }
    to {
        max-height: ${fullHeight}px;
    }
`;

export const slideOutToRight = () => keyframes`
    from {
        transform: translate(0, 0);
        opacity: 1;
    }
    50% {
        opacity: 0;
    }
    to {
        transform: translate(100%, 0);
        opacity: 0;
    }
`;

export const slideInFromRight = () => keyframes`
    from {
        transform: translate(100%, 0);
    }
    to {
        transform: translate(0, 0);
    }
`;

export const fadeOut = () => keyframes`
    from {
        opacity: 100%;
    }
    to {
        opacity: 0;
    }
`;
