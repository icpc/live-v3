import { css, keyframes } from "styled-components";

export const shimmerAnimationHorizontal = keyframes`
  0% {
    background-position: -100% 0;
  }
  100% {
    background-position: 100% 0;
  }
`;

export const shimmerAnimationDiagonal = keyframes`
  0% {
    background-position: 0% 0%;
  }
  100% {
    background-position: 100% 100%;
  }
`;

export const createShimmerStyles = (
    problemColor?: string,
    diagonal: boolean = false,
) => css`
    background: linear-gradient(
        ${diagonal ? "135deg" : "90deg"},
        ${problemColor || "#4a90e2"} 0%,
        ${problemColor || "#4a90e2"} 35%,
        #fff 50%,
        ${problemColor || "#4a90e2"} 65%,
        ${problemColor || "#4a90e2"} 100%
    );
    background-size: ${diagonal ? "200% 200%" : "200% 100%"};
    animation: ${diagonal
            ? shimmerAnimationDiagonal
            : shimmerAnimationHorizontal}
        2s linear infinite;
    color: #fff;
    font-weight: bold;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
`;

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
