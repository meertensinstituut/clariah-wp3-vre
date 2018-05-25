import React from "react";
import Table from "react-bootstrap/es/Table";
import $ from "jquery";

export default class Objects extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            data: null,
        };
        getObject().done((data) => {
            this.state.data = data;
            this.forceUpdate();
        });
    }

    render() {
        if (this.state.data === null) {
            return (
                <div className="main">Loading...</div>
            );
        }
        let objects = this.state.data.resource;
        return (
            <Table striped bordered condensed hover>
                <thead>
                <tr>
                    <th>#</th>
                    <th>File</th>
                    <th>Format</th>
                    <th>Mimetype</th>
                    <th>Created</th>
                    <th>User</th>
                </tr>
                </thead>
                <tbody>
                {objects.map(function (object, i) {
                    return (
                        <tr key={object.id}>
                            <td>{object.id}</td>
                            <td>{object.filepath}</td>
                            <td>{object.format}</td>
                            <td>{object.mimetype}</td>
                            <td>{object.time_created}</td>
                            <td>{object.user_id}</td>
                        </tr>
                    );
                })}
                </tbody>
            </Table>
        );
    }

}

function getObject() {
    return $.get({
        url: "http://localhost:8089/api/v2/objects/_table/object",
        beforeSend: function (xhr) {
            xhr.setRequestHeader('X-DreamFactory-Api-Key', process.env.REACT_APP_KEY_GET_OBJECTS)
        }
    });
}
