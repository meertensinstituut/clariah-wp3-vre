import React from "react";
import {Panel} from "react-bootstrap";
import User from "../user/user";

export default class Home extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            msg: "Welcome to the Clariah WP3 VRE.",
        };
    }

    render() {
        return (
            <Panel>
                <Panel.Body>
                    {this.state.msg}
                    <User/>
                </Panel.Body>
            </Panel>
        );
    }
}