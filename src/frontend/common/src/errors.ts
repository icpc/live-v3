interface SnackBarEnqueueProps {
    variant: "error";
    autoHideDuration?: number;
    style?: Record<string, string>;
}

export type SnackBarEnqueue = (
    cause: string,
    props: SnackBarEnqueueProps,
) => void;

export const errorHandlerWithSnackbar =
    (snackBarEnqueue: SnackBarEnqueue) => (cause: string) => {
        return (error: Error) => {
            console.error(cause + ": " + error);
            const reason = error?.message;
            snackBarEnqueue(reason ? `${cause}: ${reason}` : cause, {
                variant: "error",
                autoHideDuration: 15000,
                style: { whiteSpace: "pre-line" },
            });
        };
    };
