import React from "react";
import { Alert, Snackbar } from "@mui/material";
import { ErrorContext } from "../errors";

export default class ErrorHandler extends React.Component {
    render() {
        return (<Snackbar open={this.context.error !== undefined} autoHideDuration={10000}
            anchorOrigin={{ vertical: "bottom", horizontal: "right" }}>
            <Alert onClose={() => {
                this.context.setContext(context => ({ ...context, error: undefined }));
            }} severity="error" sx={{ width: "100%" }}>
                {this.context.error}
            </Alert>
        </Snackbar>);
    }
}

ErrorHandler.contextType = ErrorContext;
