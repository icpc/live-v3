import React from "react";
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import VisibilityIcon from '@mui/icons-material/Visibility';
import './App.css';
import AppNav from "./AppNav";
import {Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material";
import Container from "@mui/material/Container";
import Button from "@mui/material/Button";
import Box from "@mui/material/Box";
import {BACKEND_API_URL} from "./config";

class PresetsPanel extends React.Component {
    constructor(props) {
        super(props);
        this.state = {items: []};
        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    componentDidMount() {
        fetch(BACKEND_API_URL + "/advertisement")
            .then(res => res.json())
            .then(
                (result) => {
                    console.log(this.state);
                    console.log(result);
                    this.setState({
                        isLoaded: true,
                        items: result,
                    });
                    console.log(this.state, result);
                },
                (error) => {
                    this.setState({
                        isLoaded: true,
                        error
                    });
                }
            )
    }

    render() {
        return (
            <div>
                <PresetsTable items={this.state.items} headers={["Text", ""]} keys={["text"]}/>
            </div>
        );
    }

    handleChange(e) {
        this.setState({text: e.target.value});
    }

    handleSubmit(e) {
        e.preventDefault();
        if (this.state.text.length === 0) {
            return;
        }
        const newItem = {
            text: this.state.text,
            id: Date.now()
        };
        this.setState(state => ({
            items: state.items.concat(newItem),
        }));
    }
}

class PresetsTable extends React.Component {
    constructor(props) {
        super(props);
    }

    renderHeader() {
        return (
            <TableRow>
                {this.props.headers.map((row) => (
                    <TableCell component="th" scope="row">{row}</TableCell>
                ))}
            </TableRow>
        );
    }

    renderRow(row) {
        return (
            <TableRow
                key={row["id"]}
                sx={{'&:last-child td, &:last-child th': {border: 0}}}
            >
                {this.props.keys.map((rowKey) => (
                    <TableCell component="th" scope="row">{row[rowKey]}</TableCell>
                ))}
                <TableCell component="th" scope="row" align={"right"}>
                    <Box sx={{'& > button': {m: 1}}}>
                        <Button variant="outlined" size="small"><VisibilityIcon/></Button>
                        <Button variant="outlined" size="small"><EditIcon/></Button>
                        <Button variant="outlined" size="small" color="error"><DeleteIcon/></Button>
                    </Box>
                </TableCell>
            </TableRow>
        )
    }

    render() {
        return (
            // <TableContainer component={Paper}>
            <Table sx={{minWidth: 650}} aria-label="simple table">
                <TableHead>
                    {this.renderHeader()}
                </TableHead>
                <TableBody>
                    {this.props.items.map(row => this.renderRow(row))}
                </TableBody>
            </Table>
        );
    }
}


function App() {
    return (
        <div className="App">
            <AppNav/>
            <Container maxWidth="xl" sx={{pt: 4}}>
                <PresetsPanel/>
            </Container>
        </div>
    );
}

export default App;
