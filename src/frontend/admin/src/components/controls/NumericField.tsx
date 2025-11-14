import { useCallback, useRef } from "react";
import { Box, IconButton, TextField } from "@mui/material";
import ArrowBackIosIcon from "@mui/icons-material/ArrowBackIos";
import ArrowForwardIosIcon from "@mui/icons-material/ArrowForwardIos";

export type NumericFieldProps = {
    value: number;
    minValue: number;
    arrowsDelta: number;
    onChange: (newValue: number) => void;
};

export const NumericField = ({
    onChange: _onChange,
    value,
    minValue,
    arrowsDelta,
}: NumericFieldProps) => {
    arrowsDelta = arrowsDelta ?? 1;
    const ref = useRef(null);
    const blockWidth = ref.current?.offsetWidth ?? 0;
    const isPossibleArrows = blockWidth > 150;
    const onChange = useCallback(
        (v) => {
            const newV = Number.parseInt(v);
            _onChange(
                minValue === undefined || newV >= minValue ? newV : minValue,
            );
        },
        [_onChange, minValue],
    );
    return (
        <Box
            display="flex"
            justifyContent="space-between"
            alignItems="center"
            ref={ref}
        >
            {isPossibleArrows && (
                <IconButton onClick={() => onChange(value - arrowsDelta)}>
                    <ArrowBackIosIcon />
                </IconButton>
            )}
            <TextField
                type="number"
                size="small"
                onChange={(e) => onChange(e.target.value)}
                value={value}
                sx={{ maxWidth: isPossibleArrows ? blockWidth - 100 : 1 }}
            />
            {isPossibleArrows && (
                <IconButton onClick={() => onChange(value + arrowsDelta)}>
                    <ArrowForwardIosIcon />
                </IconButton>
            )}
        </Box>
    );
};

export default NumericField;
