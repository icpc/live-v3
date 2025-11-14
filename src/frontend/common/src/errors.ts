interface SnackBarEnqueueProps {
    variant: "error";
}

export type SnackBarEnqueue = (
    cause: string,
    props: SnackBarEnqueueProps,
) => void;

export const errorHandlerWithSnackbar =
    (snackBarEnqueue: SnackBarEnqueue) => (cause: string) => {
        return (error: Error) => {
            console.error(cause + ": " + error);
            snackBarEnqueue(cause, { variant: "error" });
        };
    };
