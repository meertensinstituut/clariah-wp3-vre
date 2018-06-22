import React from "react";

import PageNumbering from "./page-numbering";
import Dreamfactory from "./dreamfactory";
import DeployServiceModal from "./deploy-service-modal";

import {Table} from "react-bootstrap";

const PAGE_SIZE = 6;

export default class Files extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            pageSize: PAGE_SIZE,
            pageCurrent: 0,
            pageTotal: null,
            data: null,
            selectedObject: null,
        };

        this.goToPage = this.goToPage.bind(this);

        Dreamfactory.getObjectCount().done((data) => {
            let pageTotal = data.resource.length !== 0
                ? Math.ceil(data.resource[0].count / this.state.pageSize)
                : 0;
            this.setState({pageTotal: pageTotal});
            this.updateObjects();
        });
    }

    updateObjects() {
        this.getObjectPage(
            this.state.pageCurrent,
            this.state.pageSize
        ).done((data) => {
            this.setState({data: data});
            this.forceUpdate();
        });
    }

    getObjectPage(page, size) {
        let params = `limit=${size}&offset=${page * size}`;
        return Dreamfactory.getObjects(params);
    }

    goToPage(i) {
        this.setState(
            {pageCurrent: i},
            this.updateObjects
        );
    }

    handleRowClick(object) {
        this.setState({selectedObject: object});
    }

    deselectObject() {
        this.setState({selectedObject: null});
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
                            <tr className="clickable"
                                key={object.id}
                                onClick={() => this.handleRowClick(object)}
                            >
                                <td>{object.id}</td>
                                <td>{object.filepath}</td>
                                <td>{object.format}</td>
                                <td>{object.mimetype}</td>
                                <td>{object.time_created}</td>
                                <td>{object.user_id}</td>
                            </tr>
                        );
                    }, this)}
                    </tbody>
                </Table>
                <PageNumbering
                    pageTotal={this.state.pageTotal}
                    pageCurrent={this.state.pageCurrent}
                    onClick={this.goToPage}
                />
                <DeployServiceModal
                    object={this.state.selectedObject}
                    deselectObject={() => this.deselectObject()}
                />
            </div>
        )
    }

}