import React from "react";
import String from "./string";
import Select from "./select";
import Integer from "./integer";
import PropTypes from 'prop-types';
import {Button} from "react-bootstrap";
import './field.css';

const PARAM_TO_ClASS = new Map();
PARAM_TO_ClASS.set("string", String);
PARAM_TO_ClASS.set("integer", Integer);
PARAM_TO_ClASS.set("enumeration", Select);

export default class Field extends React.Component {

    constructor(props) {
        super(props);
        this.state = {};
        this.handleChange = this.handleChange.bind(this);
        this.handleAdd = this.handleAdd.bind(this);
    }

    handleChange(value) {
        this.props.param.value = value;
        this.props.onChange(this.props.param);
    }

    handleAdd() {
        console.log("add");
        this.props.onAdd(this.props.param);
    }

    render() {
        let field = null;
        for (let [paramType, classType] of PARAM_TO_ClASS) {
            if (this.props.param.type === paramType) {
                field = React.createElement(classType, {
                    param: this.props.param,
                    onChange: this.handleChange
                });
            }
        }

        let addButton = null;
        if(this.props.canAdd === true && field !== null) {
            addButton = <Button
                bsSize="xsmall"
                bsStyle="success"
                type="button"
                className="pull-right add-btn"
                onClick={() => this.handleAdd()}
            >
                Add <i className="fa fa-plus-square-o fa-lg" />
            </Button>;
        }

        return (
            <div className="param-field">
                {addButton}
                {field}
            </div>
        );
    }
}

Field.propTypes = {
    param: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    onAdd: PropTypes.func.isRequired,
    canAdd: PropTypes.bool.isRequired
};