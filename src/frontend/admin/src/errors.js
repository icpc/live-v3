import { useSnackbar } from "notistack";

export const errorHandlerWithSnackbar = (snackBarEnqueue) =>
    (cause) => {
        return (error) => {
            console.error(cause + ": " + error);
            snackBarEnqueue(cause, { variant: "error" });
        };
    };

export const useErrorHandlerWithSnackbar = () => {
    const { enqueueSnackbar } = useSnackbar();
    return errorHandlerWithSnackbar(enqueueSnackbar);
};
