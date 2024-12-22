import React, {useEffect, useState} from "react";
import {Button, Checkbox, Container, Dialog, FormControlLabel, FormGroup, Input, MenuItem, Select} from "@mui/material";
import {errorHandlerWithSnackbar} from "shared-code/errors";
import {useSnackbar} from "notistack";
import {BASE_URL_BACKEND, SCHEMAS_LOCATION} from "../config";
import Typography from "@mui/material/Typography";
import {createApiGet, createApiPost} from "shared-code/utils";
import JsonEditor from "./atoms/JsonEditor";
import Box from "@mui/material/Box";


const SCHEMA_URL = SCHEMAS_LOCATION + "/advanced.schema.json";
const API_URL = BASE_URL_BACKEND + "/advancedJson";

function getFields(type) {
    switch (type) {
        case "override_all_teams":
            return [
                "displayName",
                "fullName",
                "groups",
                "hashTag",
                "medias",
                "customFields",
                "isHidden",
                "isOutOfContest",
                "organizationId",
                "color",
            ];
        case "override_all_groups":
            return ["displayName", "isHidden", "isOutOfContest"];
        case "override_all_problems":
            return [
                "displayName",
                "fullName",
                "color",
                "unsolvedColor",
                "ordinal",
                "minScore",
                "maxScore",
                "scoreMergeMode",
                "isHidden"
            ];
        case "override_all_organizations":
            return ["displayName", "fullName", "logo"];
        case "override_times":
            return ["startTime", "contestLength", "freezeTime", "holdTime"]
        case "all":
            return [
                "startTime", "contestLength", "freezeTime",
                "holdBeforeStartTime", "fullName", "displayName",
                "groups", "organizationId", "hashTag", "medias",
                "customFields", "teamIsHidden",
                "isOutOfContest", "problemDisplayName",
                "problemFullName", "color", "unsolvedColor",
                "ordinal", "minScore", "maxScore",
                "scoreMergeMode", "problemIsHidden",
                "groupDisplayName", "groupIsHidden",
                "isOutOfContest", "orgDisplayName",
                "orgFullName", "logo", "penaltyPerWrongAttempt",
                "penaltyRoundingMode", "awards", "queue"
            ]
        default:
            return [];
    }
}

function getTextFields(type) {
    switch (type) {
        case "add_custom_value":
            return ["name"]
        case "add_group_by_regex":
            return ["name", "from", "regex"]
        default:
            return [];
    }
}

function AddRuleDialog({ onClose, open }) {
    const handleClose = () => {
        setSelectedValue(""); // Reset state on close
        setFields({});
        setTextFields({});
        setExpandedPreviewText(undefined)
        setPreviewText("");
        setIsPreviewOk(false);
        onClose();
    };

    const [selectedValue, setSelectedValue] = useState("");
    const [fields, setFields] = useState({});
    const [textFields, setTextFields] = useState({});
    const previewApiGet = createApiGet(API_URL);
    const [previewText, setPreviewText] = useState("");
    const [expandedPreviewText, setExpandedPreviewText] = useState();
    const [isPreviewOk, setIsPreviewOk] = useState(true);
    useEffect(() => {
        setIsPreviewOk(false)
        if (selectedValue === "") {
            setPreviewText("")
            return
        }
        // Reset state when selectedValue or fields change
        setPreviewText("Loading preview...");

        previewApiGet(`/rulePreview?type=${selectedValue}&fields=${Object.keys(fields).filter(field => fields[field]).join(",")}&${Object.keys(textFields).map(field => field+"="+encodeURIComponent(textFields[field])).join("&")}`)
            .then(data => {
                setPreviewText(JSON.stringify(data.preview, null, 2))
                setExpandedPreviewText(data.expanded ? JSON.stringify(data.expanded, null, 2): undefined)
                setIsPreviewOk(true)
            })
            .catch(() => setPreviewText("Error loading preview"));
    }, [selectedValue, fields, textFields]);
    return (
        <Dialog
            fullWidth={true}
            maxWidth={"lg"}
            onClose={handleClose}
            open={open}
        >
            <Select
                variant={"filled"}
                value={selectedValue}
                onChange={event => {
                    setSelectedValue(event.target.value)
                    setFields(Object.fromEntries(getFields(event.target.value).map(field => [field, false])))
                    setTextFields(Object.fromEntries(getTextFields(event.target.value).map(field => [field, ""])))
                }}
            >
                <MenuItem value={"override_all_teams"}>Override all teams</MenuItem>
                <MenuItem value={"override_all_groups"}>Override all groups</MenuItem>
                <MenuItem value={"override_all_problems"}>Override all problems</MenuItem>
                <MenuItem value={"override_all_organizations"}>Override all organizations</MenuItem>
                <MenuItem value={"override_times"}>Override times</MenuItem>
                <MenuItem value={"add_custom_value"}>Add custom value</MenuItem>
                <MenuItem value={"add_group_by_regex"}>Add group by regex</MenuItem>
                <MenuItem value={"all"}>Override everything</MenuItem>
            </Select>
            <Container>
            <FormGroup row={true}>
                {
                    Object.keys(fields).map(field => (
                        <FormControlLabel
                            key={field}
                            control={
                                <Checkbox
                                    checked={fields[field]}
                                    onChange={(event) => {
                                        setFields((prevFields) => ({
                                            ...prevFields,
                                            [field]: event.target.checked,
                                        }));
                                    }}
                                />
                            }
                            label={field}
                        />
                    ))
                }
            </FormGroup>
            <FormGroup>
                {
                    Object.keys(textFields).map(field => (
                        <FormControlLabel
                            key={field}
                            label={field}
                            control={
                                <Input
                                    value={textFields[field]}
                                    onChange={(event) => {
                                        setTextFields((prevTextFields) => ({
                                            ...prevTextFields,
                                            [field]: event.target.value,
                                        }));
                                    }}
                                />
                            }/>
                    ))
                }
            </FormGroup>
            <Button
                onClick={() => {
                    if (previewText) {
                        navigator.clipboard.writeText(previewText)
                    }
                }}
                disabled={!isPreviewOk}
            >
                Copy
            </Button>
            <Typography>
                <pre>
                {previewText}
                </pre>
            </Typography>
                {
                    expandedPreviewText !== undefined && <Typography>
                        <h2>Expanded to:</h2>
                        <pre>{expandedPreviewText}</pre>
                    </Typography>
                }
            </Container>
        </Dialog>
    );
}


function AdvancedJson() {
    const { enqueueSnackbar } = useSnackbar();
    const errorHandler = errorHandlerWithSnackbar(enqueueSnackbar);

    const schemaGet = createApiGet(SCHEMA_URL);
    const apiGet = createApiGet(API_URL);
    const apiPost = createApiPost(API_URL);

    const [schema, setSchema] = useState();
    const [content, setContent] = useState();
    const [isValid, setIsValid] = useState(true);
    const [isInAdderMode, setIsInAdderMode] = useState(false)

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

    const onAdd = () => {
        setIsInAdderMode(true)
    }

    if (schema === undefined || content === undefined) {
        return (
            <Container maxWidth="md" sx={{ pt: 2 }}>
                <Typography variant="h6">Loading...</Typography>
            </Container>
        );
    }

    return (
        <Container maxWidth="md" sx={{ pt: 2 }}>
            <AddRuleDialog
                open={isInAdderMode}    
                onClose={() => setIsInAdderMode(false)}
            />
            <Box height="75vh">
                <JsonEditor
                    schema={schema}
                    onChange={(value) => setContent(value)}
                    defaultValue={content}
                    onValidate={(markers) => {
                        console.log(markers);
                        setIsValid(markers.length === 0)
                    }
                    }
                />
            </Box>
            <Button type="submit" onClick={onSubmit} disabled={!isValid}>
                Save
            </Button>
            <Button onClick={onAdd} disabled={!isValid}>
                Add Rule
            </Button>
        </Container>
    );
}

export default AdvancedJson;
