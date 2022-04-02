import * as React from "react";
import IconButton from "@mui/material/IconButton";
import AddIcon from "@mui/icons-material/Add";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogContentText from "@mui/material/DialogContentText";
import DialogTitle from "@mui/material/DialogTitle";


export default class FormDialog extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            open: false,
            editValue: { name: "", url: "" }
        };
        this.handleClickOpen = this.handleClickOpen.bind(this);
        this.handleClose = this.handleClose.bind(this);
    }

    handleClickOpen() {
        this.setState({ open: true });
    }

    handleClose() {
        this.setState({ open: false });
    }

    render() {
        return (
            <div>
                <IconButton color="primary" size="large" onClick={() => {
                    this.handleClickOpen();
                }}><AddIcon/></IconButton>
                <Dialog open={this.state.open} onClose={this.handleClose}>
                    <DialogTitle>Add picture</DialogTitle>
                    <DialogContent>
                        <DialogContentText>
                        To add a picture enter title and url.
                        </DialogContentText>
                        <TextField
                            autoFocus
                            margin="dense"
                            id="name"
                            label="Title"
                            type="text"
                            fullWidth
                            variant="standard"
                            onChange={(e) => {
                                this.setState({ editValue: { name: e.target.value, url: this.state.editValue.url } });
                            }}
                        />
                        <TextField
                            margin="dense"
                            id="url"
                            label="Url"
                            type="url"
                            fullWidth
                            variant="standard"
                            onChange={(e) => {
                                this.setState({ editValue: { url: e.target.value, name: this.state.editValue.name } });
                            }}
                        />
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={this.handleClose}>Cancel</Button>
                        <Button onClick={() => {
                            this.props.addRequest(this.state.editValue.name, this.state.editValue.url);
                            this.handleClose();
                        }}>
                        Submit
                        </Button>
                    </DialogActions>
                </Dialog>
            </div>
        );
    }
}
