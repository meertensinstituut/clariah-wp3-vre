import React from "react";
import PropTypes from 'prop-types';
import {Button, Modal, Panel} from "react-bootstrap";
import Dreamfactory from "../common/dreamfactory";
import {AsyncTypeahead} from 'react-bootstrap-typeahead';

import 'react-bootstrap-typeahead/css/Typeahead.css';
import './tag.css';

export default class Tag extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            object: null,
            isLoading: false,
            options: []
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

    handleSearch = (event) => {
        const value = event.target.value;
        console.log('handleSearch ', value);
    };

    handleSelectedTag = (selected) => {
        console.log("handleSelectedTag", selected);
        // if (selected.length) {
        //     this.typeahead.getInstance().clear();
        // }
    };

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
                        <p>Type to search for tags, click on a tag to select it.</p>
                        <div>
                            <AsyncTypeahead
                                multiple
                                isLoading={this.state.isLoading}
                                onChange={this.handleSelectedTag}
                                onSearch={async query => {
                                    this.setState({isLoading: true});
                                    const json = await Dreamfactory.searchTags(query);
                                    json.resource = json.resource.map(i => {
                                        i.label = i.name;
                                        return i;
                                    });
                                    // always show current query string as option
                                    // (new tag has to be created when this option is selected)
                                    if(undefined === json.resource.find(t => t.label === query)) {
                                        json.resource.unshift({label: query, name: query, type: 'user'});
                                    }
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

