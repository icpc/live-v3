import React, { useState, useEffect } from "react";
import {Button, Container, FormControl, Select, MenuItem} from "@mui/material";
import { errorHandlerWithSnackbar } from "shared-code/errors";
import { useSnackbar } from "notistack";
import {BASE_URL_BACKEND, EXAMPLES_LOCATION, SCHEMAS_LOCATION} from "../config";
import Typography from "@mui/material/Typography";
import { createApiGet, createApiPost } from "shared-code/utils";
import JsonEditor from "./atoms/JsonEditor";
import Box from "@mui/material/Box";
import Editor from "@monaco-editor/react";

const SCHEMA_URL = SCHEMAS_LOCATION + "/advanced.schema.json";
const API_URL = BASE_URL_BACKEND + "/advancedJson";
const EXAMPLES_URL = EXAMPLES_LOCATION + "/advanced";

function ExamplesContainer() {
    const [selection, setSelection] = useState("");
    const [example, setExample] = useState();
    const [options, setOptions] = useState({})
    const examplesApiGet = createApiGet(EXAMPLES_URL);

    useEffect(() => {
        examplesApiGet("/descriptions.json")
            .then(data => {setOptions(data);})
            .catch(error => console.log(error));
    }, [])
    useEffect(() => {
        fetch(EXAMPLES_URL + `/${selection}`)
            .then(response => response.text())
            .then(data => {setExample(data);})
            .catch(error => console.log(error));
    }, [selection])
    return <Container>
        <Typography variant="h3">Examples</Typography>
        <FormControl>
            <Select
                labelId="demo-simple-select-label"
                value={selection}
                label="Examples"
                onChange={value => setSelection(value.target.value)}
                variant="standard"
            >
                {
                    Object.keys(options).map(key => (
                        <MenuItem key={key} value={key}>{options[key]}</MenuItem>
                    ))
                }
            </Select>
        </FormControl>
        {example && <Editor
            options={{
                readOnly: true
            }}

            height="75vh"
            language="json"
            value={example}
        />}
    </Container>
}

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
        <Container>
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
            <ExamplesContainer/>
        </Container>
    );
}

export default AdvancedJson;
