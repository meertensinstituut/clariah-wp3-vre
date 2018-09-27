import React from "react";
import UserResource from "./user-resource";
import ErrorMsg from "../common/error-msg";

export default class User extends React.Component {

    constructor(props) {
        super(props);
        this.state = {};
        this.init();
    }

    init() {
        UserResource
            .whoAmI()
            .then(data => this.setState({user: data.user}))
            .catch(() => this.setState({error: {message: "Could not determine user"}}));
    }

    render() {
        if (this.state.error)
            return <ErrorMsg error={this.state.error}/>;

        if (!this.state.user) {
            return null;
        }
        return (
            <p>Logged in as <em>{this.state.user}</em></p>
        );
    }
}