import React, { useState, useEffect } from "react";
import { Button, Container, TextField } from "@mui/material";
import { errorHandlerWithSnackbar } from "../errors";
import { useSnackbar } from "notistack";
import { BASE_URL_BACKEND, SCHEMAS_LOCATION } from "../config";
import Typography from "@mui/material/Typography";
import { createApiGet, createApiPost } from "../utils";

// TODO: Use https://github.com/josdejong/svelte-jsoneditor
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
            .then(data => setData(JSON.stringify(data, null, 2)))
            .catch(errorHandler("Failed to load advanced json data"));
    }, [apiUrl]);

    const onSubmit = () => {
        const dataJson = JSON.parse(data);
        console.log(dataJson);
        apiPost("", dataJson)
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
            <TextField
                value={data}
                onChange={(e) => setData(e.target.value)}
                fullWidth
                multiline
            />
            <Button type="submit" onClick={onSubmit}>
                Save
            </Button>
        </Container>
    );
}

export default AdvancedJson;
