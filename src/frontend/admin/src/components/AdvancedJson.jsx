import React, { useState, useEffect } from "react";
import { Button, Container } from "@mui/material";
import { errorHandlerWithSnackbar } from "shared-code/errors";
import { useSnackbar } from "notistack";
import { BASE_URL_BACKEND, SCHEMAS_LOCATION } from "../config";
import Typography from "@mui/material/Typography";
import { createApiGet, createApiPost } from "shared-code/utils";
import JsonEditor from "./atoms/JsonEditor";
import Box from "@mui/material/Box";

const SCHEMA_URL = SCHEMAS_LOCATION + "/advanced.schema.json";
const API_URL = BASE_URL_BACKEND + "/advancedJson";

function AdvancedJson() {
    const { enqueueSnackbar } = useSnackbar();
    const errorHandler = errorHandlerWithSnackbar(enqueueSnackbar);

    const schemaGet = createApiGet(SCHEMA_URL);
    const apiGet = createApiGet(API_URL);
    const apiPost = createApiPost(API_URL);

    const [schema, setSchema] = useState();
    const [content, setContent] = useState();

    useEffect(() => {
        schemaGet("")
            .then(data => setSchema(data))
            .catch(errorHandler("Failed to load advanced json schema"));
    }, []);

    useEffect(() => {
        apiGet("", undefined, true)
            .then(data => setContent(data))
            .catch(errorHandler("Failed to load advanced json data"));
    }, []);

    const onSubmit = () => {
        apiPost("", content, "POST", true)
            .catch(errorHandler("Failed to save advanced json data"));
    };

    if (schema === undefined || content === undefined) {
        return (
            <Container maxWidth="md" sx={{ pt: 2 }}>
                <Typography variant="h6">Loading...</Typography>
            </Container>
        );
    }

    return (
        <Container maxWidth="md" sx={{ pt: 2 }}>
            <Box height="75vh">
                <JsonEditor
                    schema={schema}
                    onChange={(value) => setContent(value)}
                    defaultValue={content}
                />
            </Box>
            <Button type="submit" onClick={onSubmit}>
                Save
            </Button>
        </Container>
    );
}

export default AdvancedJson;
