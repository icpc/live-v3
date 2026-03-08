import React, { useEffect, useRef } from "react";
import Editor, { OnMount, OnChange, Monaco } from "@monaco-editor/react";

type JSONSchema = Record<string, unknown>;

interface JsonEditorProps {
    schema: JSONSchema;
    defaultValue: string;
    onChange: OnChange;
}

function JsonCodeEditor({
    schema,
    defaultValue,
    onChange,
}: JsonEditorProps): React.ReactElement {
    const editorRef = useRef<any | null>(null);
    const monacoRef = useRef<Monaco | null>(null);

    const handleMount: OnMount = (editor, monaco) => {
        editorRef.current = editor;
        monacoRef.current = monaco;

        const modelUri = "foo://admin/advanced.json";
        const model = monaco.editor.createModel(
            defaultValue,
            "json",
            monaco.Uri.parse(modelUri),
        );
        editor.setModel(model);
    };

    // Update the editor content when defaultValue changes (e.g., apiRoot switched)
    useEffect(() => {
        const editor = editorRef.current;
        const monaco = monacoRef.current;
        if (!editor || !monaco) return;
        const model = editor.getModel();
        if (!model) return;
        if (model.getValue() !== defaultValue) {
            model.setValue(defaultValue);
        }
    }, [defaultValue]);

    // Update JSON schema diagnostics when schema changes
    useEffect(() => {
        const monaco = monacoRef.current;
        if (!monaco) return;
        monaco.languages.json.jsonDefaults.setDiagnosticsOptions({
            ...monaco.languages.json.jsonDefaults.diagnosticsOptions,
            comments: "ignore",
            trailingCommas: "ignore",
            schemas: [
                {
                    uri: "foo://app/advanced",
                    fileMatch: ["*.json"],
                    schema,
                },
            ],
            validate: true,
        });
    }, [schema]);

    return <Editor language="json" onMount={handleMount} onChange={onChange} />;
}

export default JsonCodeEditor;
export type { JSONSchema };
