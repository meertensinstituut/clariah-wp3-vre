import React from "react";
import PropTypes from 'prop-types';
import {Button, Modal, Panel} from "react-bootstrap";
import Dreamfactory from "../common/dreamfactory";
import {Typeahead} from 'react-bootstrap-typeahead';

import 'react-bootstrap-typeahead/css/Typeahead.css';
import './tag.css';

export default class Tag extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            object: null
        };
        this.init();
    }

    async init () {
        const object = await Dreamfactory
            .getObject(this.props.objectId)
            .catch((e) => this.setState({error: e}));
        console.log("init", object);
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
        if(!this.state.object)
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
                            <Typeahead
                                onChange={this.handleSelectedTag}
                                options={[{id: 1, label: "tag1"}, {id: 1, label: "tag2"}, {id: 1, label: "tagZ"}]}
                                ref={(typeahead) => this.typeahead = typeahead}
                                className="tag-search-bar"
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

