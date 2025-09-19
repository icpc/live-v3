import React from "react";
import Editor, { OnMount, OnChange } from "@monaco-editor/react";

type JSONSchema = Record<string, unknown>;

interface CodeEditorProps {
    schema: JSONSchema;
    defaultValue: string;
    onChange: OnChange;
}

function CodeEditor({
    schema,
    defaultValue,
    onChange,
}: CodeEditorProps): React.ReactElement {
    const handleMount: OnMount = (editor, monaco) => {
        const modelUri = "foo://admin/advanced.json";
        const model = monaco.editor.createModel(defaultValue, "json", monaco.Uri.parse(modelUri));
        editor.setModel(model);

        monaco.languages.json.jsonDefaults.setDiagnosticsOptions({
            ...monaco.languages.json.jsonDefaults.diagnosticsOptions,
            comments: "ignore",
            trailingCommas: "ignore",
            schemas: [
                {
                    "uri": "foo://app/advanced",
                    fileMatch: ["*.json"],
                    schema,
                },
            ],
            validate: true,
        });
    };

    return <Editor language="json" onMount={handleMount} onChange={onChange} />;
};

export default CodeEditor;
export type {
    JSONSchema
};
