import React from "react";
import {Redirect} from 'react-router-dom';
import PageNumbering from "./page-numbering";
import Dreamfactory from "../common/dreamfactory";

import {Table} from "react-bootstrap";
import ReactTooltip from 'react-tooltip'
import ErrorMsg from "../common/error-msg";

const PAGE_SIZE = 6;

export default class Files extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            redirect: null,
            pageSize: PAGE_SIZE,
            pageCurrent: 0,
            pageTotal: null,
            data: null,
            selectedObject: null,
        };

        this.goToPage = this.goToPage.bind(this);
        this.init();
    }

    async init() {
        const count = await Dreamfactory
            .getObjectCount()
            .catch(this.err());
        if (!count) return;

        let pageTotal = count.resource.length !== 0
            ? Math.ceil(count.resource[0].count / this.state.pageSize)
            : 0;
        this.setState({pageTotal});
        this.updateObjects();
    }

    async updateObjects() {
        const objects = await this.getObjectPage(this.state.pageCurrent, this.state.pageSize)
            .then(data => data.resource)
            .catch((e) => this.setState({error: e}));
        const ids = [];
        objects.forEach(o => ids.push(o.id));
        if (ids.length > 0) {
            await this.findTags(objects);
        }
        this.setState({data: objects});
    }

    async findTags(objects) {
        const objectTags = await Dreamfactory.getObjectTags([15])
            .then(data => data.resource)
            .catch((e) => this.setState({error: e}));
        objects.forEach(o => {
            o.tags = objectTags.filter(ot => ot.object === o.id);
        });
    }

    async getObjectPage(page, size) {
        let params = `limit=${size}&offset=${page * size}`;
        return await Dreamfactory
            .getObjects(params)
            .catch(this.err());
    }

    goToPage(i) {
        this.setState(
            {pageCurrent: i},
            this.updateObjects
        );
    }

    handleViewFileClick(object) {
        const pathParts = object.filepath.split('/');
        const filename = pathParts[pathParts.length - 1];
        this.setState({
            redirect: `/view/${object.id}/${filename}`
        });
    }

    handleProcessFileClick(object) {
        this.setState({
            selectedObject: object,
            redirect: `/deploy?file=${object.id}`
        });
    }

    deselectObject() {
        this.setState({selectedObject: null});
    }

    err() {
        return e => this.setState({error: e});
    }

    render() {

        if (this.state.error)
            return <ErrorMsg error={this.state.error}/>;

        if (this.state.data === null)
            return <div className="main">Loading...</div>;

        if (this.state.redirect !== null)
            return <Redirect to={this.state.redirect}/>;

        let objects = this.state.data;

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
                        <th>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {objects.map((object, i) => {

                        const view = object.format !== "directory"
                            ?
                            <button
                                data-tip="view file"
                                onClick={() => this.handleViewFileClick(object)}
                            >
                                <i className="fa fa-eye"
                                   aria-hidden="true"
                                />
                            </button>
                            :
                            null;

                        return (
                            <tr key={object.id}>
                                <td>{object.id}</td>
                                <td>
                                    <p>{object.filepath}</p>
                                    <p>
                                        {object.tags.map((tag, i) => {
                                            return <span key={i} className="label label-success">{tag.type}:{tag.name}</span>;
                                        })}
                                    </p>
                                </td>
                                <td>{object.format}</td>
                                <td>{object.mimetype}</td>
                                <td>{object.time_created}</td>
                                <td>{object.user_id}</td>
                                <td>
                                    {view}
                                    <button
                                        data-tip="process file with a service"
                                        onClick={() => this.handleProcessFileClick(object)}
                                    >
                                        <i className="fa fa-play"
                                           aria-hidden="true"
                                        />
                                    </button>
                                    <ReactTooltip/>
                                </td>
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
            </div>
        )
    }
}
