import React, { useState, useEffect } from "react";
import {Button, Container, FormControl, Select, MenuItem, SelectChangeEvent} from "@mui/material";
import { errorHandlerWithSnackbar } from "shared-code/errors";
import { useSnackbar } from "notistack";
import {BASE_URL_BACKEND, EXAMPLES_LOCATION, SCHEMAS_LOCATION} from "../config";
import Typography from "@mui/material/Typography";
import { createApiGet, createApiPost } from "shared-code/utils";
import JsonEditor, { JSONSchema } from "./atoms/JsonEditor";
import Box from "@mui/material/Box";
import Editor from "@monaco-editor/react";

const SCHEMA_URL = SCHEMAS_LOCATION + "/advanced.schema.json";
const API_URL = BASE_URL_BACKEND + "/advancedJson";
const EXAMPLES_URL = EXAMPLES_LOCATION + "/advanced";

type ExampleOptions = Record<string, string>;

function ExamplesContainer(): React.ReactElement {
    const [selection, setSelection] = useState<string>("");
    const [example, setExample] = useState<string | undefined>(undefined);
    const [options, setOptions] = useState<ExampleOptions>({});

    const examplesApiGet = createApiGet(EXAMPLES_URL);

    useEffect(() => {
        examplesApiGet("/descriptions.json")
            .then((data: unknown) => {setOptions((data as ExampleOptions) ?? {});})
            .catch(error => console.error(error));
    }, []);

    useEffect(() => {
        if (!selection) {
            return;
        }
        fetch(EXAMPLES_URL + `/${selection}`)
            .then(response => response.text())
            .then(data => {setExample(data);})
            .catch(error => console.error(error));
    }, [selection]);

    const currentExample = selection ? example : undefined;

    const handleChange = (event: SelectChangeEvent<string>) => {
        setSelection(event.target.value as string);
    };


    return (
        <Container>
            <Typography variant="h3">Examples</Typography>
            <FormControl variant="standard" sx={{ minWidth: 240, mt: 2, mb: 2 }}>
                <Select
                    labelId="examples-select-label"
                    value={selection}
                    onChange={handleChange}
                    displayEmpty
                >
                    <MenuItem value="">
                        <em>Select an example…</em>
                    </MenuItem>
                    {Object.keys(options).map((key) => (
                        <MenuItem key={key} value={key}>
                        {options[key]}
                        </MenuItem>
                    ))}
                </Select>
            </FormControl>

            {currentExample && (
                <Editor
                options={{ readOnly: true }}
                height="75vh"
                language="json"
                value={currentExample}
                />
            )}
        </Container>
    );
}

function AdvancedJson(): React.ReactElement {
    const { enqueueSnackbar } = useSnackbar();
    const errorHandler = errorHandlerWithSnackbar(enqueueSnackbar);

    const schemaGet = createApiGet(SCHEMA_URL);
    const apiGet = createApiGet(API_URL);
    const apiPost = createApiPost(API_URL);

    const [schema, setSchema] = useState<JSONSchema | undefined>(undefined);
    const [content, setContent] = useState<string | undefined>(undefined);

    useEffect(() => {
        schemaGet("")
            .then((data: unknown) => setSchema(data as JSONSchema))
            .catch(errorHandler("Failed to load advanced json schema"));
    }, []);

    useEffect(() => {
        apiGet("", undefined, true)
            .then((data: unknown) => setContent(data as string))
            .catch(errorHandler("Failed to load advanced json data"));
    }, []);

    const onSubmit = () => {
        if (content === undefined) return;
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
                        defaultValue={content}
                        onChange={(value: string) => setContent(value)}
                    />
                </Box>
                <Button type="submit" onClick={onSubmit} sx={{ mt: 2 }}>
                    Save
                </Button>
            </Container>
            <ExamplesContainer />
        </Container>
    );
}

export default AdvancedJson;
