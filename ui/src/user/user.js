import React from "react";
import UserResource from "./user-resource";

export default class User extends React.Component {

    constructor(props) {
        super(props);
        this.state = {};
        UserResource.whoAmI().done((data) => {
            this.setState({user: data.user});
        });
    }

    render() {
        if(!this.state.user) {
            return null;
        }
        return (
            <p>Logged in as <em>{this.state.user}</em></p>
        );
    }
}