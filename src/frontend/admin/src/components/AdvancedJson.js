import React, { useState, useEffect } from "react";
import Container from "@mui/material/Container";
import { errorHandlerWithSnackbar } from "../errors";
import { useSnackbar } from "notistack";
import { BASE_URL_BACKEND, SCHEMAS_LOCATION } from "../config";
import { Form } from "@rjsf/mui";
import validator from "@rjsf/validator-ajv8";
import Typography from "@mui/material/Typography";
import { createApiGet, createApiPost } from "../utils";

function AdvancedJson() {
    const { enqueueSnackbar } = useSnackbar();
    const errorHandler = errorHandlerWithSnackbar(enqueueSnackbar);

    const schemaUrl = SCHEMAS_LOCATION + "/advanced.schema.json";
    const apiUrl = BASE_URL_BACKEND + "/advancedJson";

    const schemaGet = createApiGet(schemaUrl);
    const apiGet = createApiGet(apiUrl);
    const apiPost = createApiPost(apiUrl);

    const [schema, setSchema] = useState();
    const [data, setData] = useState();

    useEffect(() => {
        schemaGet("")
            .then(data => setSchema(data))
            .catch(errorHandler("Failed to load advanced json schema"));
    }, [schemaUrl]);

    useEffect(() => {
        apiGet("")
            .then(data => setData(data))
            .catch(errorHandler("Failed to load advanced json data"));
    }, [apiUrl]);

    const uiSchema = {

    };

    const onSubmit = ({ formData }) => {
        apiPost("", formData)
            .catch(errorHandler("Failed to save advanced json data"));
    };

    if (schema === undefined || data === undefined) {
        return (
            <Container maxWidth="md" sx={{ pt: 2 }}>
                <Typography variant="h6">Loading...</Typography>
            </Container>
        );
    }

    return (
        <Container maxWidth="md" sx={{ pt: 2 }}>
            <Form
                schema={schema}
                validator={validator}
                formData={data}
                uiSchema={uiSchema}
                onSubmit={onSubmit}
            />
        </Container>
    );
}

export default AdvancedJson;
