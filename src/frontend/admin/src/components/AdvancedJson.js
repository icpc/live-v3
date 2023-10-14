import React, { useState, useEffect, useMemo } from "react";
import { Button, Container } from "@mui/material";
import { errorHandlerWithSnackbar } from "../errors";
import { useSnackbar } from "notistack";
import { BASE_URL_BACKEND, SCHEMAS_LOCATION } from "../config";
import Typography from "@mui/material/Typography";
import { createApiGet, createApiPost } from "../utils";
import VanillaJSONEditor from "./atoms/VanillaJSONEditor";
import { createAjvValidator } from "vanilla-jsoneditor";

function AdvancedJson() {
    const { enqueueSnackbar } = useSnackbar();
    const errorHandler = errorHandlerWithSnackbar(enqueueSnackbar);

    const schemaUrl = SCHEMAS_LOCATION + "/advanced.schema.json";
    const apiUrl = BASE_URL_BACKEND + "/advancedJson";

    const schemaGet = createApiGet(schemaUrl);
    const apiGet = createApiGet(apiUrl);
    const apiPost = createApiPost(apiUrl);

    const [schema, setSchema] = useState();
    const validator = useMemo(() => schema && createAjvValidator({ schema }),
        [schema]);
    const [content, setContent] = useState();

    useEffect(() => {
        schemaGet("")
            .then(data => setSchema(data))
            .catch(errorHandler("Failed to load advanced json schema"));
    }, [schemaUrl]);

    useEffect(() => {
        apiGet("")
            .then(data => setContent({
                json: data,
                text: undefined
            }))
            .catch(errorHandler("Failed to load advanced json data"));
    }, [apiUrl]);

    const onSubmit = () => {
        apiPost("", content.json)
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
            <VanillaJSONEditor
                content={content}
                onChange={setContent}
                validator={validator}
            />
            <Button type="submit" onClick={onSubmit}>
                Save
            </Button>
        </Container>
    );
}

export default AdvancedJson;
