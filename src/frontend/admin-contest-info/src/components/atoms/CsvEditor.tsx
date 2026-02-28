import React, { useEffect, useRef } from "react";
import Editor, { OnMount, OnChange, Monaco } from "@monaco-editor/react";

interface CsvEditorProps {
    defaultValue: string;
    onChange: OnChange;
}

function CsvCodeEditor({
    defaultValue,
    onChange,
}: CsvEditorProps): React.ReactElement {
    const editorRef = useRef<unknown | null>(null);
    const monacoRef = useRef<Monaco | null>(null);

    const handleMount: OnMount = (editor, monaco) => {
        editorRef.current = editor;
        monacoRef.current = monaco;

        const modelUri = "foo://admin/custom-fields.csv";
        const model = monaco.editor.createModel(
            defaultValue,
            "csv",
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

    return <Editor language="csv" onMount={handleMount} onChange={onChange} />;
}

export default CsvCodeEditor;
