import React from "react";

export default class App extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            msg: "Welcome."
        };
    }

    render() {
        return (
            <div className="main">
                <p>{this.state.msg}</p>
            </div>
        );
    }
}
