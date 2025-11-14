import {
    ButtonGroup as BaseButtonGroup,
    ButtonGroupProps,
    ThemeProvider,
    createTheme,
} from "@mui/material";

const buttonGroupTheme = createTheme({
    components: {
        MuiButton: {
            defaultProps: { size: "small" },
        },
        MuiButtonGroup: {
            defaultProps: {
                size: "small",
            },
        },
        MuiButtonBase: {
            styleOverrides: {
                root: {
                    margin: "0 !important",
                },
            },
        },
    },
});

const StyledButtonGroup = (props: ButtonGroupProps) => {
    return (
        <ThemeProvider theme={buttonGroupTheme}>
            <BaseButtonGroup {...props} />
        </ThemeProvider>
    );
};

export default StyledButtonGroup;
