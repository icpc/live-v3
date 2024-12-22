import React from "react";
import Editor from "@monaco-editor/react";
import PropTypes from "prop-types";

const CodeEditor = ({ schema, defaultValue, onChange, onValidate }) => {
    function onMount(editor, monaco) {
        /* Workaround for https://github.com/suren-atoyan/monaco-react/issues/69#issuecomment-612816117 */
        console.log(defaultValue);
        const modelUri = "foo://admin/advanced.json";
        const model = monaco.editor.createModel(
            defaultValue, "json", monaco.Uri.parse(modelUri)
        );
        editor.setModel(model);

        monaco.languages.json.jsonDefaults.setDiagnosticsOptions({
            ...monaco.languages.json.jsonDefaults.diagnosticOptions,
            comments: "ignore",
            trailingCommas: "ignore",
            schemas: [
                {
                    uri: "foo://app/advanced",
                    fileMatch: ["*.json"],
                    schema: schema,
                }
            ],
            validate: true
        });
    }

    return (
        <Editor language="json" onMount={onMount} onChange={onChange} onValidate={onValidate} />
    );
};

CodeEditor.propTypes = {
    schema: PropTypes.object.isRequired,
    defaultValue: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    onValidate: PropTypes.func.isRequired,
};

export default CodeEditor;
