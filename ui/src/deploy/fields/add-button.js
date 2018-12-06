import React from "react";
import {Button} from "react-bootstrap";
import PropTypes from 'prop-types';

export default class AddButton extends React.Component {

    render() {
        if(!this.props.canAdd) {
            return null;
        }
        return (
            <Button
                bsSize="xsmall"
                bsStyle="success"
                type="button"
                className="pull-right add-btn"
                onClick={this.props.onAdd()}
            >
                Add <i className="fa fa-plus-square-o fa-lg"/>
            </Button>
        );
    }
}

AddButton.propTypes = {
    canAdd: PropTypes.bool.isRequired,
    onAdd: PropTypes.func.isRequired
};