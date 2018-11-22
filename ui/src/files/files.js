import React from "react";
import {Redirect} from 'react-router-dom';
import PageNumbering from "./page-numbering";
import Dreamfactory from "../common/dreamfactory";

import {Table} from "react-bootstrap";
import ReactTooltip from 'react-tooltip'
import ErrorMsg from "../common/error-msg";
import Tag from "../tag/tag";

import TagResource from "../tag/tag-resource";
import ObjectTag from "../common/object-tag";

import './files.css';

const PAGE_SIZE = 6;

export default class Files extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            redirect: null,
            pageSize: PAGE_SIZE,
            pageCurrent: 0,
            pageTotal: null,
            objects: null,
            selectedObject: null,
            objectToTag: null,
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
            .catch(this.err());
        if (objects.length > 0) {
            await this.findTags(objects);
        }
        this.setState({objects});
    }

    async findTags(objects) {
        const objectTags = await Dreamfactory.getObjectTags(objects)
            .then(data => data.resource)
            .catch(this.err());
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
        this.setState({
            redirect: `/view/${object.id}/${this.getFilename(object.filepath)}`
        });
    }

    handleEditFileClick(object) {
        this.setState({
            redirect: `/edit/${object.id}/${this.getFilename(object.filepath)}`
        });
    }

    getFilename(path) {
        const pathParts = path.split('/');
        return pathParts[pathParts.length - 1];
    }

    handleProcessFileClick(object) {
        this.setState({
            selectedObject: object,
            redirect: `/deploy?file=${object.id}`
        });
    }

    handleAddTag = (id) => {
        this.setState({objectToTag: Number(id)});
    };

    handleStopTagging = async () => {
        const objects = this.state.objects;
        const object = objects.find(o => {
            return Number(o.id) === this.state.objectToTag;
        });
        await this.findTags([object]);
        this.setState({objects, objectToTag: null});
    };

    handleDeleteTag = async (objectTag) => {
        await TagResource
            .deleteObjectTag(objectTag.object, objectTag.tag)
            .catch((e) => this.setState({error: e}));
        const objects = this.state.objects;
        const object = objects.find(object => object.id === objectTag.object);
        const tagIndex = object.tags.findIndex(tag => tag.tag === objectTag.tag);
        object.tags.splice(tagIndex, 1);
        this.setState({objects});
    };

    deselectObject() {
        this.setState({selectedObject: null});
    }

    err() {
        return e => this.setState({error: e});
    }

    render() {

        if (this.state.error)
            return <ErrorMsg error={this.state.error}/>;

        if (this.state.objects === null)
            return <div className="main">Loading...</div>;

        if (this.state.redirect !== null)
            return <Redirect to={this.state.redirect}/>;

        let objects = this.state.objects;

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
                        return (
                            <tr key={object.id}>
                                <td>{object.id}</td>
                                <td>
                                    <p>{object.filepath}</p>
                                    <p>
                                        {object.tags.map((tag, i) => <ObjectTag
                                            key={i}
                                            objectTag={tag}
                                            onDelete={() => this.handleDeleteTag(tag)}
                                        />)}
                                        <span className="label label-success clickable tag"
                                              onClick={() => this.handleAddTag(object.id)}>
                                            <i className="fa fa-plus fa-1x" aria-hidden="true"/> add tag
                                        </span>
                                    </p>
                                </td>
                                <td>{object.format}</td>
                                <td>{object.mimetype}</td>
                                <td>{object.time_created.split('.')[0]}</td>
                                <td>{object.user_id}</td>
                                <td>
                                    <button
                                        data-tip="view file"
                                        onClick={() => this.handleViewFileClick(object)}>
                                        <i className="fa fa-eye" aria-hidden="true"/>
                                    </button>
                                    <button
                                        data-tip="process file with a service"
                                        onClick={() => this.handleProcessFileClick(object)}>
                                        <i className="fa fa-play" aria-hidden="true"/>
                                    </button>
                                    <button
                                        data-tip="edit file"
                                        onClick={() => this.handleEditFileClick(object)}>
                                        <i className="fa fa-pencil" aria-hidden="true"/>
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
                {this.state.objectToTag ? <Tag
                    objectId={this.state.objectToTag}
                    onClose={() => this.handleStopTagging()}
                /> : null}
            </div>
        )
    }
}
