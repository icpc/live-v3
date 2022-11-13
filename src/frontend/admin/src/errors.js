export const errorHandlerWithSnackbar = (snackBarEnqueue) =>
    (cause) => {
        return (error) => {
            console.error(cause + ": " + error);
            snackBarEnqueue(cause, { variant: "error" });
        };
    };
