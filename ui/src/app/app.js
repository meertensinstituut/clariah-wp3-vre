import React from "react";
import Objects from "../objects/objects";

export default class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            msg: "Welcome to Clariah's VRE",
        };
    }

    render() {
        return (
            <div className="main">
                <p>{this.state.msg}</p>
                <Objects/>
            </div>
        );
    }
}
