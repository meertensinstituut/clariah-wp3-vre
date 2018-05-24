import React from "react";
import $ from "jquery";

export default class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            msg: "Welcome..."
        };
        this.apiKey = process.env.REACT_APP_KEY_GET_OBJECTS;
    }

    getObject(id) {
        $.get({
            url: "http://localhost:8089/api/v2/objects/_table/object?limit=1",
            beforeSend: function (xhr) {
                xhr.setRequestHeader('X-DreamFactory-Api-Key', this.apiKey)
            },
        }).then("hello success?");
    }


    render() {
        this.getObject(1);
        return (
            <div className="main">
                <p>{this.state.msg}</p>
            </div>
        );
    }
}
