import React from "react";
import Table from "react-bootstrap/es/Table";
import Pages from "./pages";
import Dreamfactory from "./dreamfactory";

const PAGE_SIZE = 6;

export default class Objects extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            pageSize: PAGE_SIZE,
            // Page index is zero based:
            pageCurrent: 0,
            pageTotal: null,
            data: null,
        };

        this.goToPage = this.goToPage.bind(this);

        getObjectCount().done((data) => {
            let pageTotal = data.resource.length !== 0
                ? Math.ceil(data.resource[0].count / this.state.pageSize)
                : 0;
            this.setState({pageTotal: pageTotal});
            this.updateObjects();
        });
    }

    updateObjects() {
        getObjectPage(
            this.state.pageCurrent,
            this.state.pageSize
        ).done((data) => {
            this.setState({data: data});
            this.forceUpdate();
        });
    }

    goToPage(i) {
        this.setState(
            {pageCurrent: i},
            this.updateObjects
        );
    }

    render() {
        if (this.state.data === null) {
            return (
                <div className="main">Loading...</div>
            );
        }

        let objects = this.state.data.resource;
        return (
            <div>
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

                <Pages
                    pageTotal={this.state.pageTotal}
                    pageCurrent={this.state.pageCurrent}
                    onClick={this.goToPage}
                />

            </div>
        )
    }

}

function getObjectPage(page, size) {
    let params = `limit=${size}&offset=${page * size}`;
    return Dreamfactory.getObjects(params);
}

function getObjectCount() {
    return Dreamfactory.getObjectCount();
}