import React from "react";
import {Button} from "react-bootstrap";
import PropTypes from 'prop-types';

export default class RemoveButton extends React.Component {

    render() {
        return (
            <Button
                bsSize="xsmall"
                bsStyle="danger"
                type="button"
                disabled={this.props.canRemove === false}
                className="pull-right add-btn"
                onClick={this.props.onRemove()}
            >
                Remove <i className="fa fa-minus-square-o fa-lg"/>
            </Button>
        );
    }
}

RemoveButton.propTypes = {
    canRemove: PropTypes.bool.isRequired,
    onRemove: PropTypes.func.isRequired
};