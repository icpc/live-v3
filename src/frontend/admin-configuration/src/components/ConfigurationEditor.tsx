import React, { useState, useEffect } from "react";
import {
    Button,
    Container,
    FormControl,
    Select,
    MenuItem,
    SelectChangeEvent,
} from "@mui/material";
import { errorHandlerWithSnackbar } from "shared-code/errors";
import { useSnackbar } from "notistack";
import Typography from "@mui/material/Typography";
import { createApiGet, createApiPost } from "shared-code/utils";
import JsonEditor, { JSONSchema } from "./atoms/JsonEditor";
import Box from "@mui/material/Box";
import Editor from "@monaco-editor/react";
import { BASE_URL_BACKEND } from "../config.ts";
import CsvCodeEditor from "./atoms/CsvEditor.tsx";

type ExampleOptions = Record<string, string>;

function ExamplesContainer({
    apiRoot,
}: {
    apiRoot: string;
}): React.ReactElement {
    const [selection, setSelection] = useState<string>("");
    const [example, setExample] = useState<string | undefined>(undefined);
    const [options, setOptions] = useState<ExampleOptions>({});

    const examplesApiGet = createApiGet(`${apiRoot}/examples`);

    useEffect(() => {
        examplesApiGet("/descriptions.json")
            .then((data: unknown) => {
                setOptions((data as ExampleOptions) ?? {});
            })
            .catch((error) => console.error(error));
    }, [apiRoot]);

    useEffect(() => {
        if (!selection) {
            return;
        }
        fetch(`${apiRoot}/examples/${selection}`)
            .then((response) => response.text())
            .then((data) => {
                setExample(data);
            })
            .catch((error) => console.error(error));
    }, [apiRoot, selection]);

    const currentExample = selection ? example : undefined;

    const handleChange = (event: SelectChangeEvent) => {
        setSelection(event.target.value as string);
    };

    return (
        <Container>
            <Typography variant="h3">Examples</Typography>
            <FormControl
                variant="standard"
                sx={{ minWidth: 240, mt: 2, mb: 2 }}
            >
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

export enum EditorLanguage {
    Json,
    CSV,
}

interface ConfigurationEditorProps {
    apiRoot: string;
    editorType: EditorLanguage;
    readonly?: boolean;
}

export function ConfigurationEditor({
    apiRoot,
    editorType,
    readonly = false,
}: ConfigurationEditorProps): React.ReactElement {
    const { enqueueSnackbar } = useSnackbar();
    const errorHandler = errorHandlerWithSnackbar(enqueueSnackbar);

    const apiGet = createApiGet(apiRoot);
    const apiPost = createApiPost(apiRoot);

    const [schema, setSchema] = useState<JSONSchema | undefined>(undefined);
    const [content, setContent] = useState<string | undefined>(undefined);

    useEffect(() => {
        if (editorType === EditorLanguage.Json) {
            apiGet("/schema")
                .then((data: unknown) => setSchema(data as JSONSchema))
                .catch(errorHandler(`Failed to load ${apiRoot} schema`));
        }
    }, [apiRoot]);

    useEffect(() => {
        apiGet("", undefined, true)
            .then((data: unknown) => {
                console.log(`Reloaded data for ${apiRoot}: ${data}`);
                return data;
            })
            .then((data: unknown) => setContent(data as string))
            .catch(errorHandler(`Failed to load ${apiRoot} data`));
    }, [apiRoot]);

    const onSubmit = () => {
        if (content === undefined) return;
        apiPost("", content, "POST", true).catch(
            errorHandler("Failed to save advanced json data"),
        );
    };

    if (
        (schema === undefined && editorType == EditorLanguage.Json) ||
        content === undefined
    ) {
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
                    {editorType === EditorLanguage.Json && (
                        <JsonEditor
                            schema={schema}
                            defaultValue={content}
                            onChange={(value: string) => setContent(value)}
                            readonly={true}
                        />
                    )}
                    {editorType === EditorLanguage.CSV && (
                        <CsvCodeEditor
                            defaultValue={content}
                            onChange={(value: string) => setContent(value)}
                            readonly={true}
                        />
                    )}
                </Box>
                {readonly || (
                    <Button type="submit" onClick={onSubmit} sx={{ mt: 2 }}>
                        Save
                    </Button>
                )}
            </Container>
            {readonly || <ExamplesContainer apiRoot={apiRoot} />}
        </Container>
    );
}

export function SettingsJsonPage(): React.ReactElement {
    return (
        <ConfigurationEditor
            apiRoot={`${BASE_URL_BACKEND}/settings`}
            editorType={EditorLanguage.Json}
            readonly={true}
        />
    );
}

export function AdvancedJsonPage(): React.ReactElement {
    return (
        <ConfigurationEditor
            apiRoot={`${BASE_URL_BACKEND}/advancedJson`}
            editorType={EditorLanguage.Json}
        />
    );
}

export function VisualConfigPage(): React.ReactElement {
    return (
        <ConfigurationEditor
            apiRoot={`${BASE_URL_BACKEND}/visualConfig`}
            editorType={EditorLanguage.Json}
        />
    );
}

export function CustomFieldsPage(): React.ReactElement {
    return (
        <ConfigurationEditor
            apiRoot={`${BASE_URL_BACKEND}/customFields`}
            editorType={EditorLanguage.CSV}
        />
    );
}

export function OrgCustomFieldsPage(): React.ReactElement {
    return (
        <ConfigurationEditor
            apiRoot={`${BASE_URL_BACKEND}/orgCustomFields`}
            editorType={EditorLanguage.CSV}
        />
    );
}
