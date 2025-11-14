export const hexToRgb = (
    hex: string,
): { r: number; g: number; b: number } | null => {
    if (hex === undefined || hex === null) {
        return null;
    }
    if (hex.length !== 4 && hex.length !== 7 && hex.length !== 9) {
        return null;
    }

    const result =
        /^#?([a-f\d]{1,2})([a-f\d]{1,2})([a-f\d]{1,2})([a-f\d]{1,2})?$/i.exec(
            hex,
        );

    const getLastPartOfHex = (code: string): string => {
        return code.length == 1 ? code : "";
    };

    return result
        ? {
              r: parseInt(result[1] + getLastPartOfHex(result[1]), 16),
              g: parseInt(result[2] + getLastPartOfHex(result[2]), 16),
              b: parseInt(result[3] + getLastPartOfHex(result[3]), 16),
          }
        : null;
};

/*
Returns true if dark (black) color better for given backgroundColor and false otherwise.
 */
export const isShouldUseDarkColor = (backgroundColor: string): boolean => {
    const rgb = hexToRgb(backgroundColor);
    if (!rgb) {
        return false;
    }
    const { r, g, b } = rgb;
    // http://www.w3.org/TR/AERT#color-contrast
    const brightness = Math.round((r * 299 + g * 587 + b * 114) / 1000);

    return brightness > 125;
};
