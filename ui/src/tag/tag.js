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
        if (selected.length) {
            this.typeahead.getInstance().clear();
        }
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
                        <p>panel body</p>
                        <div>
                            <AsyncTypeahead
                                isLoading={this.state.isLoading}
                                onSearch={async query => {
                                    this.setState({isLoading: true});
                                    const json = await Dreamfactory.searchTags(query);
                                    json.resource = json.resource.map(i => {
                                        i.label = i.name;
                                        return i;
                                    });
                                    this.setState({
                                        isLoading: false,
                                        options: json.resource,
                                    });
                                }}
                                filterBy={['login']}
                                options={this.state.options}
                            />
                        </div>
                    </Panel.Body>
                </Panel>
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={() => this.handleClose()}>Close</Button>
            </Modal.Footer>
        </Modal.Dialog>;
    }
}

Tag.propTypes = {
    objectId: PropTypes.number.isRequired,
    onClose: PropTypes.func.isRequired
};

