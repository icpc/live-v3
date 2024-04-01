import { useSnackbar } from "notistack";
import { errorHandlerWithSnackbar } from "shared-code/errors";

export const useErrorHandlerWithSnackbar = () => {
    const { enqueueSnackbar } = useSnackbar();
    return errorHandlerWithSnackbar(enqueueSnackbar);
};
