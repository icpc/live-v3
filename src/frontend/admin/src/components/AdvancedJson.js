import React, { useState, useEffect } from "react";
import { Button, Container } from "@mui/material";
import { errorHandlerWithSnackbar } from "../errors";
import { useSnackbar } from "notistack";
import { BASE_URL_BACKEND, SCHEMAS_LOCATION } from "../config";
import Typography from "@mui/material/Typography";
import { createApiGet, createApiPost } from "../utils";
import JsonEditor from "./atoms/JsonEditor";
import Box from "@mui/material/Box";

function AdvancedJson() {
    const { enqueueSnackbar } = useSnackbar();
    const errorHandler = errorHandlerWithSnackbar(enqueueSnackbar);

    const schemaUrl = SCHEMAS_LOCATION + "/advanced.schema.json";
    const apiUrl = BASE_URL_BACKEND + "/advancedJson";

    const schemaGet = createApiGet(schemaUrl);
    const apiGet = createApiGet(apiUrl);
    const apiPost = createApiPost(apiUrl);

    const [schema, setSchema] = useState();
    const [content, setContent] = useState();

    useEffect(() => {
        schemaGet("")
            .then(data => setSchema(data))
            .catch(errorHandler("Failed to load advanced json schema"));
    }, [schemaUrl]);

    useEffect(() => {
        apiGet("")
            .then(data => setContent(data))
            .catch(errorHandler("Failed to load advanced json data"));
    }, [apiUrl]);

    const onSubmit = () => {
        apiPost("", content)
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
                    onChange={(value) => setContent(JSON.parse(value))}
                    defaultValue={JSON.stringify(content, null, 2)}
                />
            </Box>
            <Button type="submit" onClick={onSubmit}>
                Save
            </Button>
        </Container>
    );
}

export default AdvancedJson;
