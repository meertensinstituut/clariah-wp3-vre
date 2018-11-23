import React from "react";
import {Redirect} from 'react-router-dom';
import PageNumbering from "./page-numbering";
import Dreamfactory from "../common/dreamfactory";
import ReactTooltip from 'react-tooltip'
import ErrorMsg from "../common/error-msg";
import Tag from "../tag/tag";
import TagResource from "../tag/tag-resource";
import {SearchResultsList} from "../react-gui/search-a-file";
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
            filterTags: true,
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


        const searchResults = objects.map((object, i) => {
            const path = object.filepath;
            return {
                fileName: path.substring(path.lastIndexOf('/') + 1),
                filePath: path,
                fileType: object.format,
                fileUser: object.user_id,
                fileDate: object.time_created.substring(0, 10),
                fileBtns: <span>
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
                </span>,
                fileTags: <span>
                    {object.tags.map((tag, i) => {
                        if(this.state.filterTags) {
                            if(tag.owner === 'system') {
                                return null;
                            }
                        }
                        return <ObjectTag
                        key={i}
                        objectTag={tag}
                        onDelete={() => this.handleDeleteTag(tag)}
                    />})}
                    <span className="label label-success clickable tag"
                          onClick={() => this.handleAddTag(object.id)}>
                    <i className="fa fa-plus fa-1x" aria-hidden="true"/> add tag
                    </span>
                </span>
            };
        });

        return (
            <div>
                <button
                    className="filter-files-btn"
                    onClick={() => this.setState({filterTags: !this.state.filterTags})}
                >
                    <i className={this.state.filterTags ? "fa fa-square-o": "fa fa-check-square-o"} aria-hidden="true"/> Show all tags
                </button>

                <SearchResultsList searchFileResults={searchResults} />
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
