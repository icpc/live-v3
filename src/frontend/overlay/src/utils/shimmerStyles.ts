import { css, keyframes } from "styled-components";
import { isShouldUseDarkColor } from "./colors";

const shimmerTranslate = keyframes`
  0% {
    transform: translateX(-150%) skewX(-20deg);
  }
  100% {
    transform: translateX(150%) skewX(-20deg);
  }
`;

export const createShimmerStyles = (
    problemColor?: string,
    diagonal: boolean = false,
) => {
    const bgColor = problemColor || "#4a90e2";
    const useDarkContent = isShouldUseDarkColor(bgColor);

    const textColor = useDarkContent ? "#000" : "#fff";
    
    const textShadow = useDarkContent 
        ? "0 1px 0 rgba(255, 255, 255, 0.4)" 
        : "0 1px 2px rgba(0, 0, 0, 0.5)";

    const shimmerGradient = useDarkContent
        ? `linear-gradient(
            to right,
            transparent 0%,
            rgba(0, 0, 0, 0.05) 20%,
            rgba(0, 0, 0, 0.2) 50%,
            rgba(0, 0, 0, 0.05) 80%,
            transparent 100%
          )`
        : `linear-gradient(
            to right,
            transparent 0%,
            rgba(255, 255, 255, 0.1) 20%,
            rgba(255, 255, 255, 0.6) 50%,
            rgba(255, 255, 255, 0.1) 80%,
            transparent 100%
          )`;

    const blendMode = useDarkContent ? "multiply" : "screen";

    return css`
        position: relative;
        overflow: hidden;
        
        background-color: ${bgColor};
        color: ${textColor};
        font-weight: bold;
        text-shadow: ${textShadow};

        &::after {
            content: "";
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            
            background: ${shimmerGradient};
            mix-blend-mode: ${blendMode};
            
            transform: translateX(-150%);
            animation: ${shimmerTranslate} 6s cubic-bezier(0.4, 0, 0.2, 1) infinite;
            will-change: transform;
            pointer-events: none;
            z-index: 1;
        }
    `;
};

export const conditionalShimmerStyles = (
    isShimmering: boolean,
    problemColor?: string,
    fallbackColor?: string,
    diagonal: boolean = false,
) =>
    isShimmering
        ? createShimmerStyles(problemColor, diagonal)
        : css`
              background-color: ${fallbackColor};
          `;
