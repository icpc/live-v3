export const hexToRgb = (hex) => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})([a-f\d]{2}?)$/i.exec(hex);
    return result ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16)
    } : null;
};

/*
Returns true if dark (black) color better for given backgroundColor and false otherwise.
 */
export const isShouldUseDarkColor = (backgroundColor) => {
    const rgb = hexToRgb(backgroundColor);
    if (!rgb) {
        return false;
    }
    const { r, g, b } = rgb;
    return (r > 160 && g > 160) || (r > 160 && b > 160) || (g > 160 && b > 160);
};
