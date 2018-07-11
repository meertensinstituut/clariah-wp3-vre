import React from "react";
import {Button} from "react-bootstrap";
import PropTypes from 'prop-types';

export default class RemoveButton extends React.Component {

    render() {
        if(this.props.canRemove === false) return null;

        return (
            <Button
                bsStyle="warning"
                className={this.props.nextToInput ? "" : "xsmall pull-right add-btn"}
                onClick={this.props.onRemove()}
            >
                {this.props.bare ? '' : 'Remove'} <i className="fa fa-trash-o fa-lg"/>
            </Button>
        );
    }
}

RemoveButton.propTypes = {
    canRemove: PropTypes.bool.isRequired,
    onRemove: PropTypes.func.isRequired
};

RemoveButton.defaultProps = {
    nextToInput: false,
    bare: true
};