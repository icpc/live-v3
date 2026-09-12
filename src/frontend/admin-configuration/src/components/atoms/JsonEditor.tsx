import React, { useEffect, useRef, useState } from "react";
import Editor, { OnMount, OnChange, Monaco } from "@monaco-editor/react";

type JSONSchema = Record<string, unknown>;

interface JsonEditorProps {
    schema: JSONSchema;
    defaultValue: string;
    onChange: OnChange;
    readonly: boolean;
}

function JsonCodeEditor({
    schema,
    defaultValue,
    onChange,
    readonly,
}: JsonEditorProps): React.ReactElement {
    const editorRef = useRef<Parameters<OnMount>[0] | null>(null);
    const [monaco, setMonaco] = useState<Monaco | null>(null);

    const handleMount: OnMount = (editor, monacoInstance) => {
        editorRef.current = editor;

        const modelUri = "foo://admin/advanced.json";
        const model = monacoInstance.editor.createModel(
            defaultValue,
            "json",
            monacoInstance.Uri.parse(modelUri),
        );
        editor.setModel(model);
        setMonaco(monacoInstance);
    };

    // Update the editor content when defaultValue changes (e.g., apiRoot switched)
    useEffect(() => {
        const editor = editorRef.current;
        if (!editor || !monaco) return;
        const model = editor.getModel();
        if (!model) return;
        if (model.getValue() !== defaultValue) {
            model.setValue(defaultValue);
        }
    }, [defaultValue, monaco]);

    // Update JSON schema diagnostics when schema changes
    useEffect(() => {
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
    }, [monaco, schema]);

    return (
        <Editor
            language="json"
            onMount={handleMount}
            onChange={onChange}
            options={{ readOnly: readonly }}
        />
    );
}

export default JsonCodeEditor;
export type { JSONSchema };
