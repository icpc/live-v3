import React from "react";

import "./App.css";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import { Table, TableBody, TableCell, TableHead, TableRow } from "@mui/material";
import Button from "@mui/material/Button";
import Box from "@mui/material/Box";

import ShowButton from "./ShowButton";
import { BACKEND_API_URL } from "./config";

class AdvertisementPanel extends React.Component {
    constructor(props) {
        super(props);
        this.state = { items: [] };
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
            );
    }

    render() {
        return (
            <div>
                <AdvertisementTable items={this.state.items} keys={["text"]}/>
            </div>
        );
    }

    handleChange(e) {
        this.setState({ text: e.target.value });
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

class AdvertisementTable extends React.Component {
    constructor(props) {
        super(props);
    }

    renderRow(row) {
        return (
            <TableRow
                key={row["id"]}
                sx={{ "&:last-child td, &:last-child th": { border: 0 } }}
            >
                <TableCell component="th" scope="row" align={"left"}>
                    <ShowButton onClick={() => {console.log("aboba");}}/>
                </TableCell>
                {this.props.keys.map((rowKey) => (
                    <TableCell key="row" component="th" scope="row" align={"left"}>{row[rowKey]}</TableCell>
                ))}
                <TableCell component="th" scope="row" align={"right"}>
                    <Box sx={{ "& > button": { m: 1 } }}>
                        <Button size="small"><EditIcon/></Button>
                        <Button size="small" color="error"><DeleteIcon/></Button>
                    </Box>
                </TableCell>
            </TableRow>
        );
    }

    render() {
        return (
            <Table aria-label="simple table">
                <TableBody>
                    {this.props.items.map(row => this.renderRow(row))}
                </TableBody>
            </Table>
        );
    }
}


function Advertisement() {
    return (
        <div className="Advertisement">
            <AdvertisementPanel/>
        </div>
    );
}

export default Advertisement;
