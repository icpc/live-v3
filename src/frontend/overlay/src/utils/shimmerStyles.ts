import { css, keyframes } from "styled-components";

const shimmerTimingFunction = "cubic-bezier(0.4, 0, 0.2, 1)";
const shimmerDuration = "2.4s";

export const shimmerAnimationHorizontal = keyframes`
    0% {
        background-position: 200% 50%;
    }
    100% {
        background-position: -200% 50%;
    }
`;

export const shimmerAnimationDiagonal = keyframes`
    0% {
        background-position: 200% 200%;
    }
    100% {
        background-position: -200% -200%;
    }
`;

export const createShimmerStyles = (
    problemColor?: string,
    diagonal: boolean = false,
) => {
    const baseColor = problemColor || "#4a90e2";
    const gradientAngle = diagonal ? "135deg" : "90deg";

    return css`
        background: linear-gradient(
            ${gradientAngle},
            ${baseColor} 0%,
            ${baseColor} 30%,
            #fff 50%,
            ${baseColor} 70%,
            ${baseColor} 100%
        );
        background-repeat: no-repeat;
        background-size: ${diagonal ? "350% 350%" : "350% 200%"};
        animation: ${diagonal
                ? shimmerAnimationDiagonal
                : shimmerAnimationHorizontal}
            ${shimmerDuration} ${shimmerTimingFunction} infinite;
        color: #fff;
        font-weight: bold;
        text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
        will-change: background-position;
    `;
};

export const conditionalShimmerStyles = (
    isShimmering: boolean,
    problemColor?: string,
    fallbackColor?: string,
    diagonal: boolean = false,
) =>
    isShimmering
        ? css`
              ${createShimmerStyles(problemColor, diagonal)}
          `
        : css`
              background-color: ${fallbackColor};
          `;
