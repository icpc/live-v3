import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "shared-code/errors";

// TODO: rewrite shared-code, to ts, or idk, maybe just delete it?
// TODO: Do we really want locator?
export const useErrorHandlerWithSnackbar = () => {
    const { enqueueSnackbar } = useSnackbar();
    return errorHandlerWithSnackbar(enqueueSnackbar);
};
