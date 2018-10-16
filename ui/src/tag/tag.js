import React from "react";
import PropTypes from 'prop-types';
import {Button, Modal, Panel} from "react-bootstrap";
import Dreamfactory from "../common/dreamfactory";
import {AsyncTypeahead} from 'react-bootstrap-typeahead';

import 'react-bootstrap-typeahead/css/Typeahead.css';
import './tag.css';
import TagResource from "./tag-resource";
import ErrorMsg from "../common/error-msg";

export default class Tag extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            object: null,
            isLoading: false,
            options: [],
            selectedTags: []
        };
        this.init();
    }

    async init() {
        const object = await Dreamfactory
            .getObject(this.props.objectId)
            .catch((e) => this.setState({error: e}));
        this.setState({object});
    }

    handleClose() {
        this.props.onClose();
    }

    handleSelectedTag = async (newTags) => {

        // if new tag:
        // - create tag
        // - create object tag
        // if removed object tag
        // - delete object tag
        // if new object tag
        // - create object tag

        const oldTags = this.state.selectedTags;
        if(newTags.length > oldTags.length) {
            let newTag = this.findUnique(newTags, oldTags)[0];
            if(newTag.id === undefined) {
                const result = await this.createNewTag(newTag);
                newTag.id = result.id;
            }
            await this.tagObject(this.state.object.id, newTag.id);
        } else if(newTags.length < oldTags.length) {
            // tags were removed:
            const removedTag = this.findUnique(oldTags, newTags)[0];
            await this.untagObject(this.state.object.id, removedTag.id);
        } else {
            throw new Error("could not determine if tag was added or removed");
        }
        this.setState({selectedTags: newTags});
    };

    async createNewTag(newTag) {
        return await TagResource.postTag(newTag)
            .catch((e) => this.setState({error: e}));
    }

    async tagObject(objectId, tagId) {
        return await TagResource.postObjectTag(objectId, tagId)
            .catch((e) => this.setState({error: e}));
    }

    async untagObject(objectId, tagId) {
        return await TagResource.deleteObjectTag(objectId, tagId)
            .then((data) => {console.log("untagObject", data);})
            .catch((e) => this.setState({error: e}));
    }

    /**
     * Find tags in tags1 that don't exist in tags2
     */
    findUnique(tags1, tags2) {
        return tags1.filter(t1 => {
            const foundIn2 = tags2.filter(t2 => {
                return t2.name === t1.name && t2.type === t1.type;
            });
            return foundIn2.length === 0;
        });
    }

    render() {
        if (!this.state.object)
            return null;

        return <Modal.Dialog>
            <Modal.Header>
                <Modal.Title>Start tagging <strong>{this.state.object.filepath}</strong></Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <Panel>
                    <Panel.Body>
                        {this.state.error ? <ErrorMsg error={this.state.error}/> : null}
                        <p>Type to search for tags, click on a tag to select it.</p>
                        <div>
                            <AsyncTypeahead
                                multiple
                                isLoading={this.state.isLoading}
                                onChange={this.handleSelectedTag}
                                onSearch={async query => {
                                    this.setState({isLoading: true});
                                    const json = await Dreamfactory.searchTags(query);
                                    // add current query string as optional new tag
                                    if(undefined === json.resource.find(t => t.label === query)) {
                                        json.resource.unshift({label: query, name: query, type: 'user'});
                                    }
                                    // add label:
                                    json.resource = json.resource.map(i => {
                                        i.label = `${i.type}:${i.name}`;
                                        return i;
                                    });
                                    this.setState({
                                        isLoading: false,
                                        options: json.resource,
                                    });
                                }}
                                options={this.state.options}
                            />
                        </div>
                    </Panel.Body>
                </Panel>
            </Modal.Body>
            <Modal.Footer>
                <Button className="btn-success" onClick={() => this.handleClose()}>Save and close</Button>
            </Modal.Footer>
        </Modal.Dialog>;
    }
}

Tag.propTypes = {
    objectId: PropTypes.number.isRequired,
    onClose: PropTypes.func.isRequired
};

